/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2005, 2006 Wisconsin Court System
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
package com.openhtmltopdf.render;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.PaintingInfo;
import com.openhtmltopdf.layout.Styleable;
import com.openhtmltopdf.render.FlowingColumnContainerBox.ColumnBreakStore;
import com.openhtmltopdf.util.LambdaUtil;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public abstract class Box implements Styleable, DisplayListItem {
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private Element _element;

    private int _x;
    private int _y;

    private int _absY;
    private int _absX;

    /**
     * Box width.
     */
    private int _contentWidth;
    private int _rightMBP = 0;
    private int _leftMBP = 0;

    /**
     * Box height.
     */
    private int _height;

    private Layer _layer = null;
    private Layer _containingLayer;

    private Box _parent;

    private List<Box> _boxes;

    /**
     * Keeps track of the start of childrens containing block.
     */
    private int _tx;
    private int _ty;

    private CalculatedStyle _style;
    private Box _containingBlock;

    private Dimension _relativeOffset;

    private PaintingInfo _paintingInfo;

    private RectPropertySet _workingMargin;

    private int _index;

    private String _pseudoElementOrClass;

    private boolean _anonymous;
    
    private Area _absoluteClipBox;
    private boolean _clipBoxCalculated = false;
    
    private Object _accessibilityObject;
    
    protected Box() {
    }
    
    /**
     * Gets the combined clip of this box relative to the containing layer.
     * The returned clip is in document coordinate space (not transformed in any way).
     * For example, if we have the following nesting:
     *
     * overflow hidden := transformed box := overflow hidden := overflow hidden := overflow visible
     * 
     * this function called on the overflow visible box will return the combined clip of its
     * two immediate ancestors in document coordinate space. It stops at the transformed box because
     * the transform triggers a layer.
     * 
     * Currently this method is used for getting the clip to apply to a float, which are nested in layers
     * but taken out of the default block list and therefore clip stack.
     * 
     * Since it is only used for floats, the result is not cached. Revisit this decision if using for every box.
     * 
     * There are several other clip methods available:
     * + {@link #getChildrenClipEdge(CssContext)} - gets the local clip for a single box.
     * + {@link #getParentClipBox(RenderingContext, Layer)} - gets the layer relative clip for the parent box.
     * + {@link #getAbsoluteClipBox(CssContext)} - gets the absolute clip box in document coordinates
     */
    public Rectangle getClipBox(RenderingContext c, Layer layer) {
        return calcClipBox(c, layer);
    }
    
    private Box getClipParent() {
        if (getStyle() != null && getStyle().isPositioned()) {
            return getContainingBlock();
        } else if (this instanceof BlockBox && 
                ((BlockBox) this).isFloated()) {
            return getContainingBlock();
        } else {
            return getParent();
        }
    }
    
    /**
     * Gets the layer relative clip for the parent box.
     * See {@link #getClipBox(RenderingContext, Layer)}
     */
    public Rectangle getParentClipBox(RenderingContext c, Layer layer) {
        Box clipParent = getClipParent();
        
        if (clipParent == null || clipParent.getContainingLayer() != layer) {
            return null;
        }
        
        return clipParent.getClipBox(c, layer);
    }
    
    private Rectangle calcClipBox(RenderingContext c, Layer layer) {
        if (getContainingLayer() != layer) {
            return null;
        } else if (getStyle() != null && getStyle().isIdent(CSSName.OVERFLOW, IdentValue.HIDDEN)) {
            Rectangle parentClip = getParentClipBox(c, layer);
            return parentClip != null ? getChildrenClipEdge(c).intersection(parentClip) : getChildrenClipEdge(c);
        } else {
            return getParentClipBox(c, layer);
        }
    }
    
    /**
     * Returns the absolute (ie transformed if needed) clip area for this box.
     * Cached as this will be needed on every box to check if the clip area is inside a page. 
     */
    public Area getAbsoluteClipBox(CssContext c) {
        if (!_clipBoxCalculated) {
            _absoluteClipBox = calcAbsoluteClipBox(c);
            _clipBoxCalculated = true;
        }
        return _absoluteClipBox != null ? (Area) _absoluteClipBox.clone() : null;
    }
    
    private Area calcAbsoluteClipBox(CssContext c) {
        Rectangle localClip = getStyle() != null && getStyle().isIdent(CSSName.OVERFLOW, IdentValue.HIDDEN) ? getChildrenClipEdge(c) : null;
        Box parentBox = getClipParent();
        Area parentClip = parentBox != null ? parentBox.getAbsoluteClipBox(c) : null;

        if (localClip != null) {
            AffineTransform transform = getContainingLayer().getCurrentTransformMatrix();
            Area ourClip = new Area(transform != null ? transform.createTransformedShape(localClip) : localClip);
            if (parentClip != null) {
                ourClip.intersect(parentClip);
            }
            return ourClip;
        } else {
            return parentClip;
        }
    }
    
    public abstract String dump(LayoutContext c, String indent, int which);

    protected void dumpBoxes(
            LayoutContext c, String indent, List<Box> boxes,
            int which, StringBuilder result) {
        for (Iterator<Box> i = boxes.iterator(); i.hasNext(); ) {
            Box b = i.next();
            result.append(b.dump(c, indent + "  ", which));
            if (i.hasNext()) {
                result.append('\n');
            }
        }
    }

    public int getWidth() {
        return getContentWidth() + getLeftMBP() + getRightMBP();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Box: ");
        sb.append(" (" + getAbsX() + "," + getAbsY() + ")->(" + getWidth() + " x " + getHeight() + ")");
        return sb.toString();
    }

    public void addChildForLayout(LayoutContext c, Box child) {
        addChild(child);

        child.initContainingLayer(c);
    }

    public void addChild(Box child) {
        if (_boxes == null) {
            _boxes = new ArrayList<>();
        }
        if (child == null) {
            throw new NullPointerException("trying to add null child");
        }
        child.setParent(this);
        child.setIndex(_boxes.size());
        _boxes.add(child);
    }

    public void addAllChildren(List<Box> children) {
        children.forEach(this::addChild);
    }

    public void removeAllChildren() {
        if (_boxes != null) {
            _boxes.clear();
        }
    }

    /**
     * Removes a child box if it is indeed a child and adjusts
     * the index of subsequent children.
     * Returns whether this was a child.
     */
    public boolean removeChild(Box target) {
        if (_boxes != null) {
            if (target.getIndex() < getChildCount() &&
                getChild(target.getIndex()).equals(target)) {
                // Found it by index.
                return removeChild(target.getIndex());
            }

            // Linear search - should not be needed.
            int index = getChildren().indexOf(target);
            return index >= 0 && removeChild(index);
        }

        return false;
    }

    public Box getPreviousSibling() {
        Box parent = getParent();
        return parent == null ? null : parent.getPrevious(this);
    }

    public Box getNextSibling() {
        Box parent = getParent();
        return parent == null ? null : parent.getNext(this);
    }

    protected Box getPrevious(Box child) {
        return child.getIndex() == 0 ? null : getChild(child.getIndex()-1);
    }

    protected Box getNext(Box child) {
        return child.getIndex() == getChildCount() - 1 ? null : getChild(child.getIndex()+1);
    }

    /**
     * Removes child by index and adjusts the index of subsequent children.
     * Returns true if this box has children, throws if the index is out-of-bounds.
     * <br><br>
     * IMPORTANT: This method must be kept in sync with {@link #removeChild(Box)}
     */
    public boolean removeChild(int index) {
        if (_boxes != null) {
            getChildren().remove(index);
            int size = getChildCount();

            for (int i = index; i < size; i++) {
                Box child = getChild(i);
                child.setIndex(child.getIndex() - 1);
            }

            return true;
        }

        return false;
    }

    public void setParent(Box box) {
        _parent = box;
    }

    public Box getParent() {
        return _parent;
    }

    public Box getDocumentParent() {
        return getParent();
    }

    public int getChildCount() {
        return _boxes == null ? 0 : _boxes.size();
    }

    public Box getChild(int i) {
        if (_boxes == null) {
            throw new IndexOutOfBoundsException();
        } else {
            return _boxes.get(i);
        }
    }

    public Iterator<Box> getChildIterator() {
        return (_boxes == null ? Collections.emptyIterator() : _boxes.iterator());
    }

    public List<Box> getChildren() {
        return _boxes == null ? Collections.emptyList() : _boxes;
    }
    
    public static class ChildIteratorOfType<T> implements Iterator<T>  {
        private final Iterator<Box> iter;
        private final Class<T> type;
        
        private ChildIteratorOfType(Iterator<Box> parent, Class<T> clazz) {
            this.iter = parent;
            this.type = clazz;
        }
        
        @Override
        public boolean hasNext() {
            return this.iter.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            Box box = this.iter.next();
            
            if (this.type.isAssignableFrom(box.getClass())) {
                return (T) box;
            }

            XRLog.log(Level.SEVERE, LogMessageId.LogMessageId2Param.GENERAL_EXPECTING_BOX_CHILDREN_OF_TYPE_BUT_GOT,
                    this.type.getCanonicalName(), box.getClass().getCanonicalName());
            return null;
        }
    }
    
    /**
     * Returns an iterator of boxes cast to type.
     * If a box is not of type, an error will be logged and 
     * null will be returned for that box.
     * Therefore, this method should only be used when it is certain
     * all children are of a particular type.
     * Eg: TableBox has children only of type TableSectionBox.
     */
    public <T> Iterator<T> getChildIteratorOfType(Class<T> type) {
        return new ChildIteratorOfType<>(getChildIterator(), type);
    }

    public static final int NOTHING = 0;
    public static final int FLUX = 1;
    public static final int CHILDREN_FLUX = 2;
    public static final int DONE = 3;

    private int _state = NOTHING;

    public static final int DUMP_RENDER = 2;

    public static final int DUMP_LAYOUT = 1;

    public synchronized int getState() {
        return _state;
    }

    public synchronized void setState(int state) {
        _state = state;
    }

    public static String stateToString(int state) {
        switch (state) {
            case NOTHING:
                return "NOTHING";
            case FLUX:
                return "FLUX";
            case CHILDREN_FLUX:
                return "CHILDREN_FLUX";
            case DONE:
                return "DONE";
            default:
                return "unknown";
        }
    }

    public final CalculatedStyle getStyle() {
        return _style;
    }

    public void setStyle(CalculatedStyle style) {
        _style = style;
    }

    public Box getContainingBlock() {
        return _containingBlock == null ? getParent() : _containingBlock;
    }

    public void setContainingBlock(Box containingBlock) {
        _containingBlock = containingBlock;
    }

    public Rectangle getMarginEdge(int left, int top, CssContext cssCtx, int tx, int ty) {
        // Note that negative margins can mean this rectangle is inside the border
        // edge, but that's the way it's supposed to work...
        Rectangle result = new Rectangle(left, top, getWidth(), getHeight());
        result.translate(tx, ty);
        return result;
    }

    public Rectangle getMarginEdge(CssContext cssCtx, int tx, int ty) {
        return getMarginEdge(getX(), getY(), cssCtx, tx, ty);
    }

    public Rectangle getPaintingBorderEdge(CssContext cssCtx) {
        return getBorderEdge(getAbsX(), getAbsY(), cssCtx);
    }

    public Rectangle getPaintingPaddingEdge(CssContext cssCtx) {
        return getPaddingEdge(getAbsX(), getAbsY(), cssCtx);
    }

    public Rectangle getPaintingClipEdge(CssContext cssCtx) {
        return getPaintingBorderEdge(cssCtx);
    }

    public Rectangle getChildrenClipEdge(CssContext c) {
        return getPaintingPaddingEdge(c);
    }

    public Rectangle getBorderEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        Rectangle result = new Rectangle(left + (int) margin.left(),
                top + (int) margin.top(),
                getWidth() - (int) margin.left() - (int) margin.right(),
                getHeight() - (int) margin.top() - (int) margin.bottom());
        return result;
    }

    public Rectangle getPaddingEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        RectPropertySet border = getBorder(cssCtx);
        Rectangle result = new Rectangle(left + (int) margin.left() + (int) border.left(),
                top + (int) margin.top() + (int) border.top(),
                getWidth() - (int) margin.width() - (int) border.width(),
                getHeight() - (int) margin.height() - (int) border.height());
        return result;
    }

    protected int getPaddingWidth(CssContext cssCtx) {
        RectPropertySet padding = getPadding(cssCtx);
        return (int)padding.left() + getContentWidth() + (int)padding.right();
    }

    public Rectangle getContentAreaEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        RectPropertySet border = getBorder(cssCtx);
        RectPropertySet padding = getPadding(cssCtx);

        Rectangle result = new Rectangle(
                left + (int)margin.left() + (int)border.left() + (int)padding.left(),
                top + (int)margin.top() + (int)border.top() + (int)padding.top(),
                getWidth() - (int)margin.width() - (int)border.width() - (int)padding.width(),
                getHeight() - (int) margin.height() - (int) border.height() - (int) padding.height());
        return result;
    }

    public Layer getLayer() {
        return _layer;
    }

    public void setLayer(Layer layer) {
        _layer = layer;
    }

    public Dimension positionRelative(CssContext cssCtx) {
        int initialX = getX();
        int initialY = getY();

        CalculatedStyle style = getStyle();
        if (! style.isIdent(CSSName.LEFT, IdentValue.AUTO)) {
            setX(getX() + (int)style.getFloatPropertyProportionalWidth(
                    CSSName.LEFT, getContainingBlock().getContentWidth(), cssCtx));
        } else if (! style.isIdent(CSSName.RIGHT, IdentValue.AUTO)) {
            setX(getX() - (int)style.getFloatPropertyProportionalWidth(
                    CSSName.RIGHT, getContainingBlock().getContentWidth(), cssCtx));
        }

        int cbContentHeight = 0;
        if (! getContainingBlock().getStyle().isAutoHeight()) {
            CalculatedStyle cbStyle = getContainingBlock().getStyle();
            cbContentHeight = (int)cbStyle.getFloatPropertyProportionalHeight(
                    CSSName.HEIGHT, 0, cssCtx);
        } else if (isInlineBlock()) {
            // FIXME Should be content height, not overall height
            cbContentHeight = getContainingBlock().getHeight();
        }

        if (!style.isIdent(CSSName.TOP, IdentValue.AUTO)) {
            setY(getY() + ((int)style.getFloatPropertyProportionalHeight(
                    CSSName.TOP, cbContentHeight, cssCtx)));
        } else if (!style.isIdent(CSSName.BOTTOM, IdentValue.AUTO)) {
            setY(getY() - ((int)style.getFloatPropertyProportionalHeight(
                    CSSName.BOTTOM, cbContentHeight, cssCtx)));
        }

        setRelativeOffset(new Dimension(getX() - initialX, getY() - initialY));
        return getRelativeOffset();
    }

    protected boolean isInlineBlock()
    {
        return false;
    }

    public void setAbsY(int absY) {
        _absY = absY;
    }

    public int getAbsY() {
        return _absY;
    }

    public void setAbsX(int absX) {
        _absX = absX;
    }

    public int getAbsX() {
        return _absX;
    }

    public boolean isStyled() {
        return _style != null;
    }

    public int getBorderSides() {
        return BorderPainter.ALL;
    }

    public void paintBorder(RenderingContext c) {
        c.getOutputDevice().paintBorder(c, this);
    }

    private boolean isPaintsRootElementBackground() {
        return (isRoot() && getStyle().isHasBackground()) ||
                (isBody() && ! getParent().getStyle().isHasBackground());
    }

    public void paintBackground(RenderingContext c) {
        if (! isPaintsRootElementBackground()) {
            c.getOutputDevice().paintBackground(c, this);
        }
    }
    
    public boolean hasNonTextContent(CssContext c) {
        if (getStyle().getBackgroundColor() != null && getStyle().getBackgroundColor() != FSRGBColor.TRANSPARENT) {
            return true;
        } else if (!getStyle().isIdent(CSSName.BACKGROUND_IMAGE, IdentValue.NONE)) {
            return true;
        } else {
            BorderPropertySet border = this.getBorder(c);
            
            if (!border.isAllZeros()) {
                return true; 
            }
        }
        
        return false;
    }
    
    public void setAccessiblityObject(Object object) {
        this._accessibilityObject = object;
    }
    
    public Object getAccessibilityObject() {
        return this._accessibilityObject;
    }

    public void paintRootElementBackground(RenderingContext c) {
        PaintingInfo pI = getPaintingInfo();
        if (pI != null) {
            if (getStyle().isHasBackground()) {
                paintRootElementBackground(c, pI);
            } else if (getChildCount() > 0) {
                Box body = getChild(0);
                body.paintRootElementBackground(c, pI);
            }
        }
    }

    private void paintRootElementBackground(RenderingContext c, PaintingInfo pI) {
        Dimension marginCorner = pI.getOuterMarginCorner();
        Rectangle canvasBounds = new Rectangle(0, 0, marginCorner.width, marginCorner.height);
        canvasBounds.add(c.getViewportRectangle());
        c.getOutputDevice().paintBackground(c, getStyle(), canvasBounds, canvasBounds, BorderPropertySet.EMPTY_BORDER);
    }
    
    /**
     * If the html or body box have a background return true.
     */
    public boolean hasRootElementBackground(RenderingContext c) {
    	if (getStyle().isHasBackground()) {
    		return true;
    	} else if (getChildCount() > 0 &&
    			   getChild(0).getStyle().isHasBackground()) {
    		return true;
    	}
    	
    	return false;
    }

    public Layer getContainingLayer() {
        return _containingLayer;
    }

    public void setContainingLayer(Layer containingLayer) {
        _containingLayer = containingLayer;
    }

    public void initContainingLayer(LayoutContext c) {
        if (getLayer() != null) {
            setContainingLayer(getLayer());
        } else if (getContainingLayer() == null) {
            setContainingLayer(getParent().getContainingLayer());
        }
    }

    public void connectChildrenToCurrentLayer(LayoutContext c) {

        for (int i = 0; i < getChildCount(); i++) {
            Box box = getChild(i);
            box.setContainingLayer(c.getLayer());
            box.connectChildrenToCurrentLayer(c);
        }
    }

    public List<Box> getElementBoxes(Element elem) {
        List<Box> result = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            if (child.getElement() == elem) {
                result.add(child);
            }
            result.addAll(child.getElementBoxes(elem));
        }
        return result;
    }

    /**
     * Responsible for resetting the state of the box before a repeat
     * call to {@link BlockBox#layout(LayoutContext)} or other layout methods.
     * <br><br>
     * Any layout operation that is not idempotent MUST be reset in this method.
     * Layout may be called several times on the one box.
     */
    public void reset(LayoutContext c) {
        resetChildren(c);

        if (_layer != null) {
            _layer.detach();
            _layer = null;
        }

        setContainingLayer(null);
        setLayer(null);
        setPaintingInfo(null);
        setContentWidth(0);

        _workingMargin = null;

        String anchorName = c.getNamespaceHandler().getAnchorName(getElement());
        if (anchorName != null) {
            c.removeBoxId(anchorName);
        }

        Element e = getElement();
        if (e != null) {
            String id = c.getNamespaceHandler().getID(e);
            if (id != null) {
                c.removeBoxId(id);
            }
        }
    }

    public void detach(LayoutContext c) {
        reset(c);

        if (getParent() != null) {
            getParent().removeChild(this);
            setParent(null);
        }
    }

    public void resetChildren(LayoutContext c, int start, int end) {
        for (int i = start; i <= end; i++) {
            Box box = getChild(i);
            box.reset(c);
        }
    }

    protected void resetChildren(LayoutContext c) {
        int remaining = getChildCount();
        for (int i = 0; i < remaining; i++) {
            Box box = getChild(i);
            box.reset(c);
        }
    }

    public abstract void calcCanvasLocation();

    public void calcChildLocations() {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            child.calcCanvasLocation();
            child.calcChildLocations();
        }
    }

    public int forcePageBreakBefore(LayoutContext c, IdentValue pageBreakValue, boolean pendingPageName) {
        return forcePageBreakBefore(c, pageBreakValue, pendingPageName, getAbsY());
    }

    public int forcePageBreakBefore(LayoutContext c, IdentValue pageBreakValue, boolean pendingPageName, int absY) {
        PageBox page = c.getRootLayer().getFirstPage(c, absY);
        if (page == null) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.LAYOUT_BOX_HAS_NO_PAGE);
            return 0;
        } else {
            int pageBreakCount = 1;
            if (page.getTop() == absY) {
                pageBreakCount--;
                if (pendingPageName && page == c.getRootLayer().getLastPage()) {
                    c.getRootLayer().removeLastPage();
                    c.setPageName(c.getPendingPageName());
                    c.getRootLayer().addPage(c);
                }
            }
            if ((page.isLeftPage() && pageBreakValue == IdentValue.LEFT) ||
                    (page.isRightPage() && pageBreakValue == IdentValue.RIGHT)) {
                pageBreakCount++;
            }

            if (pageBreakCount == 0) {
                return 0;
            }

            if (pageBreakCount == 1 && pendingPageName) {
                c.setPageName(c.getPendingPageName());
            }

            int delta = page.getBottom() + c.getExtraSpaceTop() - absY;
            if (page == c.getRootLayer().getLastPage()) {
                c.getRootLayer().addPage(c);
            }

            if (pageBreakCount == 2) {
                page = c.getRootLayer().getPages().get(page.getPageNo()+1);
                delta += page.getContentHeight(c);

                if (pendingPageName) {
                    c.setPageName(c.getPendingPageName());
                }

                if (page == c.getRootLayer().getLastPage()) {
                    c.getRootLayer().addPage(c);
                }
            }

            setY(getY() + delta);

            return delta;
        }
    }

    /**
     * Forces a page break after this box.
     */
    public void forcePageBreakAfter(LayoutContext c, IdentValue pageBreakValue) {
        boolean needSecondPageBreak = false;
        PageBox page = c.getRootLayer().getLastPage(c, this);

        if (page != null) {
            if ((page.isLeftPage() && pageBreakValue == IdentValue.LEFT) ||
                    (page.isRightPage() && pageBreakValue == IdentValue.RIGHT)) {
                needSecondPageBreak = true;
            }

            int delta = page.getBottom() + c.getExtraSpaceTop() - (getAbsY() +
                    getMarginBorderPadding(c, CalculatedStyle.TOP) + getHeight());

            if (page == c.getRootLayer().getLastPage()) {
                c.getRootLayer().addPage(c);
            }

            if (needSecondPageBreak) {
                page = c.getRootLayer().getPages().get(page.getPageNo()+1);
                delta += page.getContentHeight(c);

                if (page == c.getRootLayer().getLastPage()) {
                    c.getRootLayer().addPage(c);
                }
            }

            setHeight(getHeight() + delta);
        }
    }

    /**
     * Whether this box would cross a page break.
     * <br><br>
     * See {@link Layer#crossesPageBreak(LayoutContext, int, int)} for extra info.
     */
    public boolean crossesPageBreak(LayoutContext c) {
        if (! c.isPageBreaksAllowed()) {
            return false;
        }

        PageBox pageBox = c.getRootLayer().getFirstPage(c, this);
        if (pageBox == null) {
            return false;
        } else {
            if (c.isInFloatBottom()) {
                return getAbsY() + getHeight() >= pageBox.getBottom();
            } else {
                return getAbsY() + getHeight() >= pageBox.getBottom(c) - c.getExtraSpaceBottom();
            }
        }
    }

    public Dimension getRelativeOffset() {
        return _relativeOffset;
    }

    public void setRelativeOffset(Dimension relativeOffset) {
        _relativeOffset = relativeOffset;
    }

    public Box find(CssContext cssCtx, int absX, int absY, boolean findAnonymous) {
        PaintingInfo pI = getPaintingInfo();
        if (pI != null && ! pI.getAggregateBounds().contains(absX, absY)) {
            return null;
        }

        Box result = null;
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            result = child.find(cssCtx, absX, absY, findAnonymous);
            if (result != null) {
                return result;
            }
        }

        Rectangle edge = getContentAreaEdge(getAbsX(), getAbsY(), cssCtx);
        return edge.contains(absX, absY) && getStyle().isVisible(null, this) ? this : null;
    }

    public boolean isRoot() {
        return getElement() != null && ! isAnonymous() && getElement().getParentNode().getNodeType() == Node.DOCUMENT_NODE;
    }

    public boolean isBody() {
        return getParent() != null && getParent().isRoot();
    }

    public static boolean isBody(Box child) {
        return child.getElement() != null && child.getElement().getNodeName().equals("body");
    }

    public Element getElement() {
        return _element;
    }

    public void setElement(Element element) {
        _element = element;
    }

    public void setMarginTop(CssContext cssContext, int marginTop) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setTop(marginTop);
    }

    public void setMarginBottom(CssContext cssContext, int marginBottom) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setBottom(marginBottom);
    }

    public void setMarginLeft(CssContext cssContext, int marginLeft) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setLeft(marginLeft);
    }

    public void setMarginRight(CssContext cssContext, int marginRight) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setRight(marginRight);
    }

    private void ensureWorkingMargin(CssContext cssContext) {
        if (_workingMargin == null) {
            _workingMargin = getStyleMargin(cssContext).copyOf();
        }
    }

    public RectPropertySet getMargin(CssContext cssContext) {
        return _workingMargin != null ? _workingMargin : getStyleMargin(cssContext);
    }

    protected RectPropertySet getStyleMargin(CssContext cssContext) {
        return getStyle().getMarginRect(getContainingBlockWidth(), cssContext);
    }

    protected RectPropertySet getStyleMargin(CssContext cssContext, boolean useCache) {
        return getStyle().getMarginRect(getContainingBlockWidth(), cssContext, useCache);
    }

    public RectPropertySet getPadding(CssContext cssCtx) {
        return getStyle().getPaddingRect(getContainingBlockWidth(), cssCtx);
    }

    public BorderPropertySet getBorder(CssContext cssCtx) {
        return getStyle().getBorder(cssCtx);
    }

    protected int getContainingBlockWidth() {
        return getContainingBlock().getContentWidth();
    }

    protected void resetTopMargin(CssContext cssContext) {
        if (_workingMargin != null) {
            RectPropertySet styleMargin = getStyleMargin(cssContext);

            _workingMargin.setTop(styleMargin.top());
        }
    }

    public PaintingInfo calcPaintingInfo(CssContext c, boolean useCache) {
        PaintingInfo cached = getPaintingInfo();
        if (cached != null && useCache) {
            return cached;
        }

        final PaintingInfo result = new PaintingInfo();

        Rectangle bounds = getMarginEdge(getAbsX(), getAbsY(), c, 0, 0);
        result.setOuterMarginCorner(
            new Dimension(bounds.x + bounds.width, bounds.y + bounds.height));

        result.setAggregateBounds(getPaintingClipEdge(c));

        if (!getStyle().isOverflowApplies() || getStyle().isOverflowVisible()) {
            calcChildPaintingInfo(c, result, useCache);
        }

        setPaintingInfo(result);

        return result;
    }

    protected void calcChildPaintingInfo(
            CssContext c, PaintingInfo result, boolean useCache) {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            PaintingInfo info = child.calcPaintingInfo(c, useCache);
            moveIfGreater(result.getOuterMarginCorner(), info.getOuterMarginCorner());
            result.getAggregateBounds().add(info.getAggregateBounds());
        }
    }

    public int getMarginBorderPadding(CssContext cssCtx, int which) {
        BorderPropertySet border = getBorder(cssCtx);
        RectPropertySet margin = getMargin(cssCtx);
        RectPropertySet padding = getPadding(cssCtx);

        switch (which) {
            case CalculatedStyle.LEFT:
                return (int)(margin.left() + border.left() + padding.left());
            case CalculatedStyle.RIGHT:
                return (int)(margin.right() + border.right() + padding.right());
            case CalculatedStyle.TOP:
                return (int)(margin.top() + border.top() + padding.top());
            case CalculatedStyle.BOTTOM:
                return (int)(margin.bottom() + border.bottom() + padding.bottom());
            default:
                throw new IllegalArgumentException();
        }
    }

    protected void moveIfGreater(Dimension result, Dimension test) {
        if (test.width > result.width) {
            result.width = test.width;
        }
        if (test.height > result.height) {
            result.height = test.height;
        }
    }

    /**
     * The zero based index of this child amongst its fellow children of its parent.
     */
    protected int getIndex() {
        return _index;
    }

    /**
     * See {@link #getIndex()}
     * Must make sure this is correct when removing children/rearranging children.
     */
    protected void setIndex(int index) {
        _index = index;
    }

    public String getPseudoElementOrClass() {
        return _pseudoElementOrClass;
    }

    public void setPseudoElementOrClass(String pseudoElementOrClass) {
        _pseudoElementOrClass = pseudoElementOrClass;
    }

    public void setX(int x) {
        _x = x;
    }

    public int getX() {
        return _x;
    }

    public void setY(int y) {
        _y = y;
    }

    public int getY() {
        return _y;
    }

    public void setTy(int ty) {
        _ty = ty;
    }

    public int getTy() {
        return _ty;
    }

    public void setTx(int tx) {
        _tx = tx;
    }

    public int getTx() {
        return _tx;
    }

    public void setRightMBP(int rightMBP) {
        _rightMBP = rightMBP;
    }

    public int getRightMBP() {
        return _rightMBP;
    }

    public void setLeftMBP(int leftMBP) {
        _leftMBP = leftMBP;
    }

    public int getLeftMBP() {
        return _leftMBP;
    }

    /**
     * Uh oh! This refers to content height during layout but total height after layout!
     */
    public void setHeight(int height) {
        _height = height;
    }

    /**
     * Uh oh! This refers to content height during layout but total height after layout!
     */
    public int getHeight() {
        return _height;
    }
    
    protected void setBorderBoxHeight(CssContext c, int h) {
        BorderPropertySet border = getBorder(c);
        RectPropertySet padding = getPadding(c);
        setHeight((int) Math.max(0f, h - border.height() - padding.height()));
    }

    public int getBorderBoxHeight(CssContext c) {
        BorderPropertySet border = getBorder(c);
        RectPropertySet padding = getPadding(c);
        return (int) (getHeight() + border.height() + padding.height());
    }

    /**
     * Only to be called after layout, due to double use of getHeight().
     */
    public Rectangle getBorderBox(CssContext c) {
        RectPropertySet margin = getMargin(c);

        int w = getBorderBoxWidth(c);
        int h = getHeight() - (int) margin.top() - (int) margin.bottom();
        int x = getAbsX() + (int) margin.left();
        int y = getAbsY() + (int) margin.top();
        
        return new Rectangle(x, y, w, h);
    }

    public void setContentWidth(int contentWidth) {
        _contentWidth = contentWidth < 0 ? 0 : contentWidth;
    }

    public int getContentWidth() {
        return _contentWidth;
    }
    
    public int getBorderBoxWidth(CssContext c) {
        BorderPropertySet border = getBorder(c);
        RectPropertySet padding = getPadding(c);
        return (int) (getContentWidth() + border.width() + padding.width());
    }
    
    public void setBorderBoxWidth(CssContext c, int borderBoxWidth) {
        BorderPropertySet border = getBorder(c);
        RectPropertySet padding = getPadding(c);
        setContentWidth((int) (borderBoxWidth - border.width() - padding.width()));
    }

    public PaintingInfo getPaintingInfo() {
        return _paintingInfo;
    }

    private void setPaintingInfo(PaintingInfo paintingInfo) {
        _paintingInfo = paintingInfo;
    }

    public boolean isAnonymous() {
        return _anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        _anonymous = anonymous;
    }

    public BoxDimensions getBoxDimensions() {
        BoxDimensions result = new BoxDimensions();

        result.setLeftMBP(getLeftMBP());
        result.setRightMBP(getRightMBP());
        result.setContentWidth(getContentWidth());
        result.setHeight(getHeight());

        return result;
    }

    public void setBoxDimensions(BoxDimensions dimensions) {
        setLeftMBP(dimensions.getLeftMBP());
        setRightMBP(dimensions.getRightMBP());
        setContentWidth(dimensions.getContentWidth());
        setHeight(dimensions.getHeight());
    }

    public void collectText(RenderingContext c, StringBuilder buffer) {
        for (Box b : getChildren()) {
            b.collectText(c, buffer);
        }
    }

    /**
     * Similar to {@link #collectText(RenderingContext, StringBuilder)} but
     * dynamic functions such as counter(pages) are not calculated.
     */
    public void collectLayoutText(LayoutContext c, StringBuilder builder) {
        for (Box b : getChildren()) {
            b.collectLayoutText(c, builder);
        }
    }

    public void exportText(RenderingContext c, Writer writer) throws IOException {
        if (c.isPrint() && isRoot()) {
            c.setPage(0, c.getRootLayer().getPages().get(0));
            c.getPage().exportLeadingText(c, writer);
        }

        for (Box b : getChildren()) {
            b.exportText(c, writer);
        }
        
        if (c.isPrint() && isRoot()) {
            exportPageBoxText(c, writer);
        }
    }

    private void exportPageBoxText(RenderingContext c, Writer writer) throws IOException {
        c.getPage().exportTrailingText(c, writer);
        if (c.getPage() != c.getRootLayer().getLastPage()) {
            List<PageBox> pages = c.getRootLayer().getPages();
            do {
                PageBox next = pages.get(c.getPageNo()+1);
                c.setPage(next.getPageNo(), next);
                next.exportLeadingText(c, writer);
                next.exportTrailingText(c, writer);
            } while (c.getPage() != c.getRootLayer().getLastPage());
        }
    }

    protected void exportPageBoxText(RenderingContext c, Writer writer, int yPos) throws IOException {
        c.getPage().exportTrailingText(c, writer);
        List<PageBox> pages = c.getRootLayer().getPages();
        PageBox next = pages.get(c.getPageNo()+1);
        c.setPage(next.getPageNo(), next);
        while (next.getBottom() < yPos) {
            next.exportLeadingText(c, writer);
            next.exportTrailingText(c, writer);
            next = pages.get(c.getPageNo()+1);
            c.setPage(next.getPageNo(), next);
        }
        next.exportLeadingText(c, writer);
    }

    public boolean isInDocumentFlow() {
        Box flowRoot = rootBox();
        return flowRoot.isRoot();
    }

    public void analyzePageBreaks(LayoutContext c, ContentLimitContainer container) {
        container.updateTop(c, getAbsY());
        for (Box b : getChildren()) {
            b.analyzePageBreaks(c, container);
        }
        container.updateBottom(c, getAbsY() + getHeight());
    }

    public FSColor getEffBackgroundColor(RenderingContext c) {
        FSColor result = null;
        Box current = this;
        while (current != null) {
            result = current.getStyle().getBackgroundColor();
            if (result != null) {
                return result;
            }

            current = current.getContainingBlock();
        }

        PageBox page = c.getPage();
        result = page.getStyle().getBackgroundColor();
        if (result == null) {
            return new FSRGBColor(255, 255, 255);
        } else {
            return result;
        }
    }

    protected boolean isMarginAreaRoot() {
        return false;
    }

    public boolean isContainedInMarginBox() {
        return rootBox().isMarginAreaRoot();
    }

    public int getEffectiveWidth() {
        return getWidth();
    }

    protected boolean isInitialContainingBlock() {
        return false;
    }

    /**
     * Is this box the first child of its parent?
     */
    public boolean isFirstChild() {
        return getParent() != null &&
               getParent().getChildCount() > 0 &&
               getParent().getChild(0) == this;
    }
    
    /**
     * Is this box unbreakable in regards to column break opportunities?
     */
    public boolean isTerminalColumnBreak() {
        return getChildCount() == 0;
    }
    
    /**
     * Creates a list of ancestors by walking up the chain of parent,
     * grandparent, etc. Stops when the provided predicate returns false
     * or the root box otherwise.
     */
    public List<Box> ancestorsWhile(Predicate<Box> predicate) {
        List<Box> ancestors = new ArrayList<>(4);
        Box parent = this.getParent();
        
        while (parent != null && predicate.test(parent)) {
            ancestors.add(parent);
            parent = parent.getParent();
        }
        
        return ancestors;
    }
    
    /**
     * Get all ancestors, up until the root box.
     */
    public List<Box> ancestors() {
        return ancestorsWhile(LambdaUtil.alwaysTrue());
    }
    
    /**
     * Walks up the ancestor tree to the root testing ancestors agains
     * the predicate.
     * NOTE: Does not test against the current box (this).
     * @return the box for which predicate returned true or null if none found.
     */
    public Box findAncestor(Predicate<Box> predicate) {
        Box parent = getParent();
        
        while (parent != null && !predicate.test(parent)) {
            parent = parent.getParent();
        }
        
        return parent;
    }
    
    /**
     * Returns the highest ancestor box. May be current box (this).
     */
    public Box rootBox() {
        return this.getParent() != null ? findAncestor(bx -> bx.getParent() == null) : this;
    }

    /**
     * Recursive method to find column break opportunities.
     * @param store - use to report break opportunities.
     */
    public void findColumnBreakOpportunities(ColumnBreakStore store) {
        if (this.isTerminalColumnBreak() && this.isFirstChild()) {
            // We report unprocessed ancestor container boxes so that they
            // can be moved with the first child.
            List<Box> ancestors = this.ancestorsWhile(store::checkContainerShouldProcess);
            store.addBreak(this, ancestors);
        } else if (this.isTerminalColumnBreak()) {
            store.addBreak(this, null);
        } else {
            // This must be a container box so don't add it as a break opportunity.
            // Recursively query children for their break opportunities.
            for (Box child : getChildren()) {
                child.findColumnBreakOpportunities(store);
            }
        }
    }

}
