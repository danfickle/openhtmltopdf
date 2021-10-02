/*
 * {{{ header & license
 * Copyright (c) 2005 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.layout;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.constants.PageElementPosition;
import com.openhtmltopdf.css.newmatch.PageInfo;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.EmptyStyle;
import com.openhtmltopdf.render.*;
import com.openhtmltopdf.render.displaylist.TransformCreator;
import com.openhtmltopdf.util.SearchUtil;
import com.openhtmltopdf.util.SearchUtil.IntComparator;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * All positioned and transformed content as well as content with an overflow value other
 * than visible creates a layer.  Layers which define stacking contexts
 * provide the entry for rendering the box tree to an output device.  The main
 * purpose of this class is to provide an implementation of Appendix E of the
 * spec, but it also provides additional utility services including page
 * management.
 * <br><br>
 * When rendering to a paged output device, the layer is also responsible for laying
 * out absolute content (which is layed out after its containing block has
 * completed layout).
 */
public class Layer {
    public static final short PAGED_MODE_SCREEN = 1;
    public static final short PAGED_MODE_PRINT = 2;

    private Layer _parent;
    private boolean _stackingContext;
    private List<Layer> _children;
    private BlockBox _master;

    private List<BlockBox> _floats;

    private boolean _requiresLayout;

    private List<PageBox> _pages;
    private PageBox _lastRequestedPage = null;

    private Set<BlockBox> _pageSequences;
    private List<BlockBox> _sortedPageSequences;

    private Map<String, List<BlockBox>> _runningBlocks;

    private boolean _forDeletion;
    private boolean _hasFixedAncester;

    /**
     * See {@link #getCurrentTransformMatrix()}
     */
    private AffineTransform _ctm;
    private final boolean _hasLocalTransform;

    private boolean _isolated;

    /**
     * Creates the root layer.
     */
    public Layer(BlockBox master, CssContext c) {
        this(null, master, c);
        setStackingContext(true);
    }

    public Layer(BlockBox master, CssContext c, boolean isolated) {
        this(master, c);
        if (isolated) {
            setIsolated(true);
        }
    }

    private void setIsolated(boolean b) {
        _isolated = b;
    }

    /**
     * Creates a child layer.
     */
    public Layer(Layer parent, BlockBox master, CssContext c) {
        _parent = parent;
        _master = master;
        setStackingContext(
                (master.getStyle().isPositioned() && !master.getStyle().isAutoZIndex()) ||
                (!master.getStyle().isIdent(CSSName.TRANSFORM, IdentValue.NONE)));
        master.setLayer(this);
        master.setContainingLayer(this);
        _hasLocalTransform = !master.getStyle().isIdent(CSSName.TRANSFORM, IdentValue.NONE);
        _hasFixedAncester = (parent != null && parent._hasFixedAncester) || master.getStyle().isFixed();
    }
    
    /** 
     * Recursively propagates the transformation matrix. This must be done after layout of the master
     * box and its children as this method relies on the box width and height for relative units in the 
     * transforms and transform origins.
     */
    public void propagateCurrentTransformationMatrix(CssContext c) {
    	AffineTransform parentCtm = _parent == null ? null : _parent._ctm;
    	_ctm = _hasLocalTransform ?
        		TransformCreator.createDocumentCoordinatesTransform(getMaster(), c, parentCtm) : parentCtm;
        		
        for (Layer child : getChildren()) {
        	child.propagateCurrentTransformationMatrix(c);
        }
    }
    
    /**
     * The document coordinates current transform, this is cumulative from layer to child layer.
     * May be null, if identity transform is in effect.
     * Used to check if a box belonging to this layer sits on a particular page after the
     * transform is applied.
     * This method can only be used after {@link #propagateCurrentTransformationMatrix(CssContext)} has been
     * called on the root layer.
     * @return null or affine transform.
     */
    public AffineTransform getCurrentTransformMatrix() {
    	return _ctm;
    }
    
    public boolean hasLocalTransform() {
    	return _hasLocalTransform;
    }
    
    public void setForDeletion(boolean forDeletion) {
        this._forDeletion = forDeletion;
    }
    
    public boolean isForDeletion() {
        return this._forDeletion;
    }
    
    public boolean hasFixedAncester() {
        return _hasFixedAncester;
    }
    
    public Layer getParent() {
        return _parent;
    }
    
    public boolean isStackingContext() {
        return _stackingContext;
    }

    public void setStackingContext(boolean stackingContext) {
        _stackingContext = stackingContext;
    }

    public int getZIndex() {
    	if (_master.getStyle().isIdent(CSSName.Z_INDEX, IdentValue.AUTO)) {
    		return 0;
    	}
        return (int) _master.getStyle().asFloat(CSSName.Z_INDEX);
    }

    public boolean isZIndexAuto() {
    	return _master.getStyle().isIdent(CSSName.Z_INDEX, IdentValue.AUTO);
    }

    public BlockBox getMaster() {
        return _master;
    }

    public void addChild(Layer layer) {
        if (_children == null) {
            _children = new ArrayList<>();
        }
        _children.add(layer);
    }

    public static PageBox createPageBox(CssContext c, String pseudoPage) {
        PageBox result = new PageBox();

        String pageName = null;
        // HACK We only create pages during layout, but the OutputDevice
        // queries page positions and since pages are created lazily, changing
        // this method to use LayoutContext is tricky
        if (c instanceof LayoutContext) {
            pageName = ((LayoutContext)c).getPageName();
        }

        PageInfo pageInfo = c.getCss().getPageStyle(pageName, pseudoPage);
        result.setPageInfo(pageInfo);

        CalculatedStyle cs = new EmptyStyle().deriveStyle(pageInfo.getPageStyle());
        result.setStyle(cs);
        result.setOuterPageWidth(result.getWidth(c));

        return result;
    }

    /**
     * FIXME: Only used when we reset a box, so trying to remove at sometime in the future.
     */
    public void removeFloat(BlockBox floater) {
        if (_floats != null) {
            _floats.remove(floater);
        }
    }

    public static final int POSITIVE = 1;
    public static final int ZERO = 2;
    public static final int NEGATIVE = 3;
    public static final int AUTO = 4;

    public void addFloat(BlockBox floater, BlockFormattingContext bfc) {
        if (_floats == null) {
            _floats = new ArrayList<>();
        }

        _floats.add(floater);

        floater.getFloatedBoxData().setDrawingLayer(this);
    }

    /**
     * Called recusively to collect all descendant layers in a layer tree so
     * they can be painted in correct order.
     * @param which NEGATIVE ZERO POSITIVE AUTO corresponding to z-index property.
     */
    public List<Layer> collectLayers(int which) {
        List<Layer> result = new ArrayList<>();
        List<Layer> children = getChildren();

        result.addAll(getStackingContextLayers(which));

        for (Layer child : children) {
            if (! child.isStackingContext()) {
                if (child.isForDeletion() || child._isolated) {
                    // Do nothing...
                } else if (which == AUTO && child.isZIndexAuto()) {
            		result.add(child);
            	} else if (which == NEGATIVE && child.getZIndex() < 0) {
            		result.add(child);
            	} else if (which == POSITIVE && child.getZIndex() > 0) {
            		result.add(child);
            	} else if (which == ZERO && !child.isZIndexAuto() && child.getZIndex() == 0) {
            		result.add(child);
            	}
                result.addAll(child.collectLayers(which));
            }
        }

        return result;
    }

    private List<Layer> getStackingContextLayers(int which) {
        List<Layer> result = new ArrayList<>();
        List<Layer> children = getChildren();

        for (Layer target : children) {
            if (target.isForDeletion() || target._isolated) {
                // Do nothing...
            } else if (target.isStackingContext()) {
            	if (!target.isZIndexAuto()) {
                    int zIndex = target.getZIndex();
                    if (which == NEGATIVE && zIndex < 0) {
                        result.add(target);
                    } else if (which == POSITIVE && zIndex > 0) {
                        result.add(target);
                    } else if (which == ZERO && zIndex == 0) {
                        result.add(target);
                    }
            	} else if (which == AUTO) {
            		result.add(target);
            	}
            }
        }

        return result;
    }

    public List<Layer> getSortedLayers(int which) {
        List<Layer> result = collectLayers(which);

        Collections.sort(result, (l1, l2) -> l1.getZIndex() - l2.getZIndex());

        return result;
    }

    public Dimension getPaintingDimension(LayoutContext c) {
        return calcPaintingDimension(c).getOuterMarginCorner();
    }

    public List<BlockBox> getFloats() {
        return _floats == null ? Collections.emptyList() : _floats;
    }

    public void positionFixedLayer(RenderingContext c) {
        Rectangle rect = c.getFixedRectangle(true);

        Box fixed = getMaster();

        fixed.setX(0);
        fixed.setY(0);
        fixed.setAbsX(0);
        fixed.setAbsY(0);

        fixed.setContainingBlock(new ViewportBox(rect));
        ((BlockBox)fixed).positionAbsolute(c, BlockBox.POSITION_BOTH);

        fixed.calcPaintingInfo(c, false);
    }

    public boolean isRootLayer() {
        return getParent() == null && isStackingContext();
    }

    private void moveIfGreater(Dimension result, Dimension test) {
        if (test.width > result.width) {
            result.width = test.width;
        }
        if (test.height > result.height) {
            result.height = test.height;
        }
    }

    public void positionChildren(LayoutContext c) {
        for (Layer child : getChildren()) {
            child.position(c);
        }
    }

    private PaintingInfo calcPaintingDimension(LayoutContext c) {
        getMaster().calcPaintingInfo(c, true);
        PaintingInfo result = getMaster().getPaintingInfo().copyOf();

        for (Layer child : getChildren()) {
            if (child.getMaster().getStyle().isFixed()) {
                continue;
            } else if (child.getMaster().getStyle().isAbsolute()) {
                PaintingInfo info = child.calcPaintingDimension(c);
                moveIfGreater(result.getOuterMarginCorner(), info.getOuterMarginCorner());
            }
        }

        return result;
    }

    /**
     * The resulting list should not be modified.
     */
    public List<Layer> getChildren() {
        return _children == null ? Collections.emptyList() : _children;
    }

    private void remove(Layer layer) {
        boolean removed = false;

        if (_children != null) {
                for (Iterator<Layer> i = _children.iterator(); i.hasNext(); ) {
                    Layer child = i.next();
                    if (child == layer) {
                        removed = true;
                        i.remove();
                        break;
                    }
                }
        }

        if (! removed) {
            throw new RuntimeException("Could not find layer to remove");
        }
    }

    public void detach() {
        if (getParent() != null) {
            getParent().remove(this);
        }
        setForDeletion(true);
    }

    public boolean isRequiresLayout() {
        return _requiresLayout;
    }

    public void setRequiresLayout(boolean requiresLayout) {
        _requiresLayout = requiresLayout;
    }

    public void finish(LayoutContext c) {
        if (c.isPrint()) {
            layoutAbsoluteChildren(c);
        }

        positionChildren(c);
    }

    private void layoutAbsoluteChildren(LayoutContext c) {
        List<Layer> children = getChildren();
        
        if (children.size() > 0) {
            LayoutState state = c.captureLayoutState();
            
            for (int i = 0; i < children.size(); i++) {
                Layer child = children.get(i);
                boolean isFixed = child.getMaster().getStyle().isFixed();

                if (child.isRequiresLayout()) {
                    layoutAbsoluteChild(c, child);
                    
                    if (!isFixed &&
                        child.getMaster().getStyle().isAvoidPageBreakInside() &&
                        child.getMaster().crossesPageBreak(c)) {
                        
                        BlockBox master = child.getMaster();
                        
                        master.reset(c);
                        master.setNeedPageClear(true);
                        
                        layoutAbsoluteChild(c, child);
                        
                        if (master.crossesPageBreak(c)) {
                            master.reset(c);
                            layoutAbsoluteChild(c, child);
                        }
                    }
                    
                    child.setRequiresLayout(false);
                    child.finish(c);
                    
                    if (!isFixed) {
                        c.getRootLayer().ensureHasPage(c, child.getMaster());
                    }
                }
            }
            
            c.restoreLayoutState(state);
        }
    }

    private void position(LayoutContext c) {
        if (getMaster().getStyle().isAbsolute() && ! c.isPrint()) {
            getMaster().positionAbsolute(c, BlockBox.POSITION_BOTH);
        }
    }

    public List<PageBox> getPages() {
		if (_pages == null)
			return _parent == null ? Collections. emptyList() : _parent.getPages();
		return _pages;
    }

    public void setPages(List<PageBox> pages) {
        _pages = pages;
    }

    public boolean isLastPage(PageBox pageBox) {
        return _pages.get(_pages.size()-1) == pageBox;
    }

    private void layoutAbsoluteChild(LayoutContext c, Layer child) {
        BlockBox master = child.getMaster();

        if (child.getMaster().getStyle().isBottomAuto()) {
            // Set top, left
            master.positionAbsolute(c, BlockBox.POSITION_BOTH);
            master.positionAbsoluteOnPage(c);
            c.reInit(true);
            child.getMaster().layout(c);
            // Set right
            master.positionAbsolute(c, BlockBox.POSITION_HORIZONTALLY);
        } else {
            // FIXME Not right in the face of pagination, but what
            // to do?  Not sure if just laying out and positioning
            // repeatedly will converge on the correct position,
            // so just guess for now
            c.reInit(true);
            master.layout(c);

            BoxDimensions before = master.getBoxDimensions();
            master.reset(c);
            BoxDimensions after = master.getBoxDimensions();
            master.setBoxDimensions(before);
            master.positionAbsolute(c, BlockBox.POSITION_BOTH);
            master.positionAbsoluteOnPage(c);
            master.setBoxDimensions(after);

            c.reInit(true);
            child.getMaster().layout(c);
        }
    }

    public void removeLastPage() {
        PageBox pageBox = _pages.remove(_pages.size()-1);
        if (pageBox == getLastRequestedPage()) {
            setLastRequestedPage(null);
        }
    }

    public void addPage(CssContext c) {
        String pseudoPage = null;
        if (_pages == null) {
            _pages = new ArrayList<>();
        }

        List<PageBox> pages = getPages();
        if (pages.size() == 0) {
            pseudoPage = "first";
        } else if (pages.size() % 2 == 0) {
            pseudoPage = "right";
        } else {
            pseudoPage = "left";
        }
        PageBox pageBox = createPageBox(c, pseudoPage);
        if (pages.size() == 0) {
            pageBox.setTopAndBottom(c, 0);
        } else {
            PageBox previous = pages.get(pages.size()-1);
            pageBox.setTopAndBottom(c, previous.getBottom());
        }

        pageBox.setPageNo(pages.size());
        pages.add(pageBox);
    }

    /**
     * Returns the page box for a Y position.
     * If the y position is less than 0 then the first page will
     * be returned if available.
     * Only returns null if there are no pages available.
     * <br><br>
     * IMPORTANT: If absY is past the end of the last page,
     * pages will be created as required to include absY and the last
     * page will be returned.
     * 
     */
    public PageBox getFirstPage(CssContext c, int absY) {
        PageBox page = getPage(c, absY);

        if (page == null && absY < 0) {
            List<PageBox> pages = getPages();

            if (!pages.isEmpty()) {
                return pages.get(0);
            }
        }

        return page;
    }

    public PageBox getFirstPage(CssContext c, Box box) {
        if (box instanceof LineBox) {
            LineBox lb = (LineBox) box;
            return getPage(c, lb.getMinPaintingTop());
        }

        return getPage(c, box.getAbsY());
    }

    public PageBox getLastPage(CssContext c, Box box) {
        return getPage(c, box.getAbsY() + box.getHeight() - 1);
    }

    public void ensureHasPage(CssContext c, Box box) {
        getLastPage(c, box);
    }

    /**
     * Tries to return a list of pages that cover top to bottom.
     * If top and bottom are less-than zero returns null.
     * <br><br>
     * IMPORTANT: If bottom is past the end of the last page, pages
     * are created until bottom is included.
     */
    public List<PageBox> getPages(CssContext c, int top, int bottom) {
        if (top > bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        PageBox first = getPage(c, top);

        if (first == null) {
            return null;
        } else if (bottom < first.getBottom()) {
            return Collections.singletonList(first);
        }

        List<PageBox> pages = new ArrayList<>();
        pages.add(first);

        int current = first.getBottom() + 1;
        PageBox curPage = first;

        while (bottom > curPage.getBottom()) {
            curPage = getPage(c, current);
            current = curPage.getBottom() + 1;
            pages.add(curPage);
        }

        return pages;
    }

    /**
     * Returns a comparator that determines if the given pageBox is a match, too late or too early.
     * For use with {@link SearchUtil#intBinarySearch(IntComparator, int, int)}
     */
    private IntComparator pageFinder(List<PageBox> pages, int yOffset, Predicate<PageBox> matcher) {
        return idx -> {
            PageBox pageBox = pages.get(idx);

            if (matcher.test(pageBox)) {
                return 0;
            }

            return pageBox.getTop() < yOffset ? -1 : 1;
        };
    }

    /**
     * Returns a predicate that determines if yOffset sits on the given page.
     */
    private Predicate<PageBox> pagePredicate(int yOffset) {
        return pageBox -> yOffset >= pageBox.getTop() && yOffset < pageBox.getBottom();
    }

    /**
     * Gets the page box for the given document y offset. If y offset is 
     * less-than zero returns null.
     * <br><br>
     * IMPORTANT: If y offset is past the end of the last page, pages
     * are created until y offset is included and the last page is returned.
     */
    public PageBox getPage(CssContext c, int yOffset) {
        return getPage(c, yOffset, true);
    }

    private static final int MAX_REAR_SEARCH = 5;

    /**
     * Differs from {@link #getPage(CssContext, int)} by allowing the caller to
     * control whether a yOffset past the last page will add pages or just
     * return null.
     */
    public PageBox getPage(CssContext c, int yOffset, boolean addPages) {
        List<PageBox> pages = getPages();

        if (yOffset < 0) {
            return null;
        } else {
            PageBox lastRequested = getLastRequestedPage();
            Predicate<PageBox> predicate = pagePredicate(yOffset);

            if (lastRequested != null) {
                if (predicate.test(lastRequested)) {
                    return lastRequested;
                }
            }

            PageBox last = pages.get(pages.size() - 1);

            if (yOffset < last.getBottom()) {
                PageBox needle;

                int pageIdx = searchLastPages(pages, predicate, MAX_REAR_SEARCH);

                if (pageIdx < 0) {
                    int needleIndex = SearchUtil.intBinarySearch(
                        pageFinder(pages, yOffset, predicate), 0, pages.size() - MAX_REAR_SEARCH);

                    needle = pages.get(needleIndex);
                } else {
                    needle = pages.get(pageIdx);
                }

                setLastRequestedPage(needle);
                return needle;

            } else {
                addPagesUntilPosition(c, yOffset);
                PageBox result = pages.get(pages.size()-1);
                setLastRequestedPage(result);
                return result;
            }
        }
    }

    /**
     * Returns the zero based index of a page containing yOffset or
     * -1 if yOffset is before the first page or after the last.
     */
    public int getPageIndex(int yOffset) {
        List<PageBox> pages = getPages();
        Predicate<PageBox> predicate = pagePredicate(yOffset);

        int pageIdx = searchLastPages(pages, predicate, MAX_REAR_SEARCH);

        if (pageIdx >= 0) {
            return pageIdx;
        }

        return SearchUtil.intBinarySearch(
                pageFinder(pages, yOffset, predicate), 0, pages.size() - MAX_REAR_SEARCH);
    }

    /**
     * Searches the last maxRearPages for a page that matches predicate,
     * else returns a value less than zero.
     */
    private int searchLastPages(
            List<PageBox> pages, Predicate<PageBox> predicate, final int maxRearPages) {
        // The page we're looking for is probably at the end of the
        // document so do a linear search for the first few pages
        // and then fall back to a binary search if that doesn't work
        // out
        int count = pages.size();

        for (int i = count - 1; i >= 0 && i >= count - maxRearPages; i--) {
            PageBox pageBox = pages.get(i);

            if (predicate.test(pageBox)) {
                setLastRequestedPage(pageBox);
                return i;
            }
        }

        return -1;
    }

    private void addPagesUntilPosition(CssContext c, int position) {
        List<PageBox> pages = getPages();
        PageBox last = pages.get(pages.size()-1);
        while (position >= last.getBottom()) {
            addPage(c);
            last = pages.get(pages.size()-1);
        }
    }

    public void trimEmptyPages(CssContext c, int maxYHeight) {
        // Empty pages may result when a "keep together" constraint
        // cannot be satisfied and is dropped
        List<PageBox> pages = getPages();
        for (int i = pages.size() - 1; i > 0; i--) {
            PageBox page = pages.get(i);
            if (page.getTop() >= maxYHeight) {
                if (page == getLastRequestedPage()) {
                    setLastRequestedPage(null);
                }
                pages.remove(i);
            } else {
                break;
            }
        }
    }

    public void trimPageCount(int newPageCount) {
        while (_pages.size() > newPageCount) {
            PageBox pageBox = _pages.remove(_pages.size()-1);
            if (pageBox == getLastRequestedPage()) {
                setLastRequestedPage(null);
            }
        }
    }

    public void assignPagePaintingPositions(CssContext cssCtx, short mode) {
        assignPagePaintingPositions(cssCtx, mode, 0);
    }

    public void assignPagePaintingPositions(
            CssContext cssCtx, int mode, int additionalClearance) {
        List<PageBox> pages = getPages();
        int paintingTop = additionalClearance;
        for (PageBox page : pages) {
            page.setPaintingTop(paintingTop);
            if (mode == PAGED_MODE_SCREEN) {
                page.setPaintingBottom(paintingTop + page.getHeight(cssCtx));
            } else if (mode == PAGED_MODE_PRINT) {
                page.setPaintingBottom(paintingTop + page.getContentHeight(cssCtx));
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }
            paintingTop = page.getPaintingBottom() + additionalClearance;
        }
    }

    public int getMaxPageWidth(CssContext cssCtx, int additionalClearance) {
        List<PageBox> pages = getPages();
        int maxWidth = 0;
        for (PageBox page : pages) {
            int pageWidth = page.getWidth(cssCtx) + additionalClearance * 2;
            if (pageWidth > maxWidth) {
                maxWidth = pageWidth;
            }
        }

        return maxWidth;
    }

    public PageBox getLastPage() {
        List<PageBox> pages = getPages();
        return pages.size() == 0 ? null : pages.get(pages.size()-1);
    }

    /**
     * Returns whether the a box with the given top and bottom would cross a page break.
     * <br><br>
     * Requirements: top is >= 0.
     * <br><br>
     * Important: This method will take into account any <code>float: bottom</code>
     * content when used in in-flow content. For example, if the top/bottom pair overlaps
     * the footnote area, returns true. It also takes into account space set aside for
     * paginated table header/footer.
     * <br><br>
     * See {@link CssContext#isInFloatBottom()} {@link LayoutContext#getExtraSpaceBottom()}
     */
    public boolean crossesPageBreak(LayoutContext c, int top, int bottom) {
        if (top < 0) {
            return false;
        }
        PageBox page = getPage(c, top);
        if (c.isInFloatBottom()) {
            // For now we don't support paginated tables in float:bottom content.
            return bottom >= page.getBottom();
        } else {
            return bottom >= page.getBottom(c) - c.getExtraSpaceBottom();
        }
    }

    public Layer findRoot() {
        if (isRootLayer()) {
            return this;
        } else {
            return getParent().findRoot();
        }
    }

    public void addRunningBlock(BlockBox block) {
        if (_runningBlocks == null) {
            _runningBlocks = new HashMap<>();
        }

        String identifier = block.getStyle().getRunningName();

        List<BlockBox> blocks = _runningBlocks.get(identifier);
        if (blocks == null) {
            blocks = new ArrayList<>();
            _runningBlocks.put(identifier, blocks);
        }

        blocks.add(block);

        Collections.sort(blocks, (b1, b2) -> b1.getAbsY() - b2.getAbsY());
    }

    public void removeRunningBlock(BlockBox block) {
        if (_runningBlocks == null) {
            return;
        }

        String identifier = block.getStyle().getRunningName();

        List<BlockBox> blocks = _runningBlocks.get(identifier);
        if (blocks == null) {
            return;
        }

        blocks.remove(block);
    }

    public BlockBox getRunningBlock(String identifer, PageBox page, PageElementPosition which) {
        if (_runningBlocks == null) {
            return null;
        }

        List<BlockBox> blocks = _runningBlocks.get(identifer);
        if (blocks == null) {
            return null;
        }

        if (which == PageElementPosition.START) {
            BlockBox prev = null;
            for (Iterator<BlockBox> i = blocks.iterator(); i.hasNext(); ) {
                BlockBox b = i.next();
                if (b.getStaticEquivalent().getAbsY() >= page.getTop()) {
                    break;
                }
                prev = b;
            }
            return prev;
        } else if (which == PageElementPosition.FIRST) {
            for (Iterator<BlockBox> i = blocks.iterator(); i.hasNext(); ) {
                BlockBox b = i.next();
                int absY = b.getStaticEquivalent().getAbsY();
                if (absY >= page.getTop() && absY < page.getBottom()) {
                    return b;
                }
            }
            return getRunningBlock(identifer, page, PageElementPosition.START);
        } else if (which == PageElementPosition.LAST) {
            BlockBox prev = null;
            for (Iterator<BlockBox> i = blocks.iterator(); i.hasNext(); ) {
                BlockBox b = i.next();
                if (b.getStaticEquivalent().getAbsY() > page.getBottom()) {
                    break;
                }
                prev = b;
            }
            return prev;
        } else if (which == PageElementPosition.LAST_EXCEPT) {
            BlockBox prev = null;
            for (Iterator<BlockBox> i = blocks.iterator(); i.hasNext(); ) {
                BlockBox b = i.next();
                int absY = b.getStaticEquivalent().getAbsY();
                if (absY >= page.getTop() && absY < page.getBottom()) {
                    return null;
                }
                if (absY > page.getBottom()) {
                    break;
                }
                prev = b;
            }
            return prev;
        }

        throw new RuntimeException("bug: internal error");
    }

    public void layoutPages(LayoutContext c) {
        c.setRootDocumentLayer(c.getRootLayer());
        for (PageBox pageBox : _pages) {
            pageBox.layout(c);
        }
    }

    public void addPageSequence(BlockBox start) {
        if (_pageSequences == null) {
            _pageSequences = new HashSet<>();
        }

        _pageSequences.add(start);
    }

    private List<BlockBox> getSortedPageSequences() {
        if (_pageSequences == null) {
            return null;
        }

        if (_sortedPageSequences == null) {
            List<BlockBox> result = new ArrayList<>(_pageSequences);

            Collections.sort(result, new Comparator<BlockBox>() {
                public int compare(BlockBox b1, BlockBox b2) {
                    return b1.getAbsY() - b2.getAbsY();
                }
            });

            _sortedPageSequences  = result;
        }

        return _sortedPageSequences;
    }

    public int getRelativePageNo(RenderingContext c, int absY) {
        List<BlockBox> sequences = getSortedPageSequences();
        int initial = 0;
        if (c.getInitialPageNo() > 0) {
            initial = c.getInitialPageNo() - 1;
        }
        if ((sequences == null) || sequences.isEmpty()) {
            return initial + getPage(c, absY).getPageNo();
        } else {
            BlockBox pageSequence = findPageSequence(sequences, absY);
            int sequenceStartAbsolutePageNo = getPage(c, pageSequence.getAbsY()).getPageNo();
            int absoluteRequiredPageNo = getPage(c, absY).getPageNo();
            return absoluteRequiredPageNo - sequenceStartAbsolutePageNo;
        }
    }

    private BlockBox findPageSequence(List<BlockBox> sequences, int absY) {
        BlockBox result = null;

        for (int i = 0; i < sequences.size(); i++) {
            result = sequences.get(i);
            if ((i < sequences.size() - 1) && ((sequences.get(i + 1)).getAbsY() > absY)) {
                break;
            }
        }

        return result;
    }

    public int getRelativePageNo(RenderingContext c) {
        List<BlockBox> sequences = getSortedPageSequences();
        int initial = 0;
        if (c.getInitialPageNo() > 0) {
            initial = c.getInitialPageNo() - 1;
        }
        if (sequences == null) {
            return initial + c.getPageNo();
        } else {
            int sequenceStartIndex = getPageSequenceStart(c, sequences, c.getPage());
            if (sequenceStartIndex == -1) {
                return initial + c.getPageNo();
            } else {
                BlockBox block = sequences.get(sequenceStartIndex);
                return c.getPageNo() - getFirstPage(c, block).getPageNo();
            }
        }
    }

    public int getRelativePageCount(RenderingContext c) {
        List<BlockBox> sequences = getSortedPageSequences();
        int initial = 0;
        if (c.getInitialPageNo() > 0) {
            initial = c.getInitialPageNo() - 1;
        }
        if (sequences == null) {
            return initial + c.getPageCount();
        } else {
            int firstPage;
            int lastPage;

            int sequenceStartIndex = getPageSequenceStart(c, sequences, c.getPage());

            if (sequenceStartIndex == -1) {
                firstPage = 0;
            } else {
                BlockBox block = sequences.get(sequenceStartIndex);
                firstPage = getFirstPage(c, block).getPageNo();
            }

            if (sequenceStartIndex < sequences.size() - 1) {
                BlockBox block = sequences.get(sequenceStartIndex+1);
                lastPage = getFirstPage(c, block).getPageNo();
            } else {
                lastPage = c.getPageCount();
            }

            int sequenceLength = lastPage - firstPage;
            if (sequenceStartIndex == -1) {
                sequenceLength += initial;
            }

            return sequenceLength;
        }
    }

    private int getPageSequenceStart(RenderingContext c, List<BlockBox> sequences, PageBox page) {
        for (int i = sequences.size() - 1; i >= 0; i--) {
            BlockBox start = sequences.get(i);
            if (start.getAbsY() < page.getBottom() - 1) {
                return i;
            }
        }

        return -1;
    }

    private PageBox getLastRequestedPage() {
        return _lastRequestedPage;
    }

    private void setLastRequestedPage(PageBox lastRequestedPage) {
        _lastRequestedPage = lastRequestedPage;
    }

    public boolean isIsolated() {
        return _isolated;
    }
}
