/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2006, 2007 Wisconsin Courts System
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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.newmatch.CascadedStyle;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.layout.BlockBoxing;
import com.openhtmltopdf.layout.BlockFormattingContext;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.BreakAtLineContext;
import com.openhtmltopdf.layout.CounterFunction;
import com.openhtmltopdf.layout.FloatManager;
import com.openhtmltopdf.layout.InlineBoxing;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.PaintingInfo;
import com.openhtmltopdf.layout.PersistentBFC;
import com.openhtmltopdf.layout.Styleable;
import com.openhtmltopdf.newtable.TableRowBox;
import com.openhtmltopdf.util.ThreadCtx;

/**
 * A block box as defined in the CSS spec.  It also provides a base class for
 * other kinds of block content (for example table rows or cells).
 * See {@link ContentType}
 */
public class BlockBox extends Box {

    public static final int POSITION_VERTICALLY = 1;
    public static final int POSITION_HORIZONTALLY = 2;
    public static final int POSITION_BOTH = POSITION_VERTICALLY | POSITION_HORIZONTALLY;

    /**
     * What type of direct child content this block box contains.
     * <br><br>
     * NOTE: A {@link BlockBox} can only contain inline or block content (not both) as direct children.
     * If this constraint is not met by the original document, the {@link BoxBuilder}
     * will insert {@link AnonymousBlockBox} with inline content.
     */
    public static enum ContentType {
        /**
         * The box builder has not yet run to
         * create our child boxes. The box builder can be run
         * with {@link BlockBox#ensureChildren(LayoutContext)}.
         */
        UNKNOWN,

        /**
         * This block box contains inline content in the {@link BlockBox#getInlineContent()}
         * property. If it has also been laid out it will contain
         * children in {@link Box#getChildren()} and associated methods.
         * Children will be only {@link LineBox} objects.
         */
        INLINE,

        /**
         * This block box's direct children consist only of
         * {@link BlockBox} and subclassed objects.
         * The method {@link BlockBox#setInlineContent(List)} must not be used
         * with block content.
         */
        BLOCK,

        /**
         * This block box is empty but may still have border, etc.
         */
        EMPTY;
    }

    protected static final int NO_BASELINE = Integer.MIN_VALUE;

    private MarkerData _markerData;

    private int _listCounter;

    private PersistentBFC _persistentBFC;

    private Box _staticEquivalent;

    private boolean _needPageClear;

    private ReplacedElement _replacedElement;

    private ContentType _childrenContentType = ContentType.UNKNOWN;

    private List<Styleable> _inlineContent;

    private boolean _topMarginCalculated;
    private boolean _bottomMarginCalculated;
    private MarginCollapseResult _pendingCollapseCalculation;

    private int _minWidth;
    private int _maxWidth;
    private boolean _minMaxCalculated;

    private boolean _dimensionsCalculated;
    private boolean _needShrinkToFitCalculatation;

    private CascadedStyle _firstLineStyle;
    private CascadedStyle _firstLetterStyle;

    private FloatedBoxData _floatedBoxData;

    private int _childrenHeight;

    private boolean _fromCaptionedTable;

    private boolean _isReplaced;

    public BlockBox() {
        super();
    }
    
    @Override
    public void setElement(Element element) {
    	super.setElement(element);
    	_isReplaced = ThreadCtx.get().sharedContext().getReplacedElementFactory().isReplacedElement(element);
    }

    public BlockBox copyOf() {
        BlockBox result = new BlockBox();
        result.setStyle(getStyle());
        result.setElement(getElement());

        return result;
    }

    protected String getExtraBoxDescription() {
        return "";
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String className = getClass().getName();
        result.append(className.substring(className.lastIndexOf('.') + 1));
        result.append(": ");
        if (getElement() != null && ! isAnonymous()) {
            result.append("<");
            result.append(getElement().getNodeName());
            result.append("> ");
        }
        if (isAnonymous()) {
            result.append("(anonymous) ");
        }
        if (getPseudoElementOrClass() != null) {
            result.append(':');
            result.append(getPseudoElementOrClass());
            result.append(' ');
        }
        result.append('(');
        result.append(getStyle().getIdent(CSSName.DISPLAY).toString());
        result.append(") ");

        if (getStyle().isRunning()) {
            result.append("(running) ");
        }

        result.append('(');
        switch (getChildrenContentType()) {
            case BLOCK:
                result.append('B');
                break;
            case INLINE:
                result.append('I');
                break;
            case EMPTY:
                result.append('E');
                break;
            case UNKNOWN:
                result.append('U');
                break;
            default:
                result.append('U');
                break;
        }
        result.append(") ");

        result.append(getExtraBoxDescription());

        appendPositioningInfo(result);
        result.append("(" + getAbsX() + "," + getAbsY() + ")->(" + getWidth() + " x " + getHeight() + ")");
        return result.toString();
    }

    protected void appendPositioningInfo(StringBuilder result) {
        if (getStyle().isRelative()) {
            result.append("(relative) ");
        }
        if (getStyle().isFixed()) {
            result.append("(fixed) ");
        }
        if (getStyle().isAbsolute()) {
            result.append("(absolute) ");
        }
        if (getStyle().isFloated()) {
            result.append("(floated) ");
        }
    }

    @Override
    public String dump(LayoutContext c, String indent, int which) {
        StringBuilder result = new StringBuilder(indent);

        ensureChildren(c);

        result.append(this);
        result.append(getMargin(c).toString(" effMargin="));
        result.append(getStyleMargin(c).toString(" styleMargin="));

        if (getChildrenContentType() != ContentType.EMPTY) {
            result.append('\n');
        }

        switch (getChildrenContentType()) {
            case BLOCK:
                dumpBoxes(c, indent, getChildren(), which, result);
                break;

            case INLINE:
                if (which == Box.DUMP_RENDER) {
                    dumpBoxes(c, indent, getChildren(), which, result);
                } else {
                    for (Iterator<Styleable> i = getInlineContent().iterator(); i.hasNext();) {
                        Styleable styleable = i.next();

                        if (styleable instanceof BlockBox) {
                            BlockBox b = (BlockBox) styleable;
                            result.append(b.dump(c, indent + "  ", which));
                            if (result.charAt(result.length() - 1) == '\n') {
                                result.deleteCharAt(result.length() - 1);
                            }
                        } else {
                            result.append(indent + "  ");
                            result.append(styleable.toString());
                        }
                        if (i.hasNext()) {
                            result.append('\n');
                        }
                    }
                }
                break;

            case EMPTY:
                break;
            case UNKNOWN:
                break;
            default:
                break;
        }

        return result.toString();
    }

    public boolean isListItem() {
    	return getStyle().isListItem();
    }

    public void paintListMarker(RenderingContext c) {
        if (! getStyle().isVisible(c, this)) {
            return;
        }

        if (isListItem()) {
            ListItemPainter.paint(c, this);
        }
    }

    @Override
    public Rectangle getPaintingClipEdge(CssContext cssCtx) {
        Rectangle result = super.getPaintingClipEdge(cssCtx);

        // HACK Don't know how wide the list marker is (or even where it is)
        // so extend the bounding box all the way over to the left edge of
        // the canvas
        if (getStyle().isListItem()) {
            int delta = result.x;
            result.x = 0;
            result.width += delta;
        }

        return result;
    }

    public boolean isInline() {
        Box parent = getParent();
        return parent instanceof LineBox || parent instanceof InlineLayoutBox;
    }

    public LineBox getLineBox() {
        if (! isInline()) {
            return null;
        } else {
            return (LineBox) findAncestor(bx -> bx instanceof LineBox);
        }
    }

    public void paintDebugOutline(RenderingContext c) {
        c.getOutputDevice().drawDebugOutline(c, this, FSRGBColor.RED);
    }

    public MarkerData getMarkerData() {
        return _markerData;
    }

    public void setMarkerData(MarkerData markerData) {
        _markerData = markerData;
    }

    public void createMarkerData(LayoutContext c) {
        if (getMarkerData() != null)
        {
            return;
        }

        StrutMetrics strutMetrics = InlineBoxing.createDefaultStrutMetrics(c, this);

        boolean imageMarker = false;

        MarkerData result = new MarkerData();
        result.setStructMetrics(strutMetrics);

        CalculatedStyle style = getStyle();
        IdentValue listStyle = style.getIdent(CSSName.LIST_STYLE_TYPE);

        String image = style.getStringProperty(CSSName.LIST_STYLE_IMAGE);
        if (! image.equals("none")) {
            result.setImageMarker(makeImageMarker(c, strutMetrics, image));
            imageMarker = result.getImageMarker() != null;
        }

        if (listStyle != IdentValue.NONE && ! imageMarker) {
            if (listStyle == IdentValue.CIRCLE || listStyle == IdentValue.SQUARE ||
                    listStyle == IdentValue.DISC) {
                result.setGlyphMarker(makeGlyphMarker(strutMetrics));
            } else {
                result.setTextMarker(makeTextMarker(c, listStyle));
            }
        }

        setMarkerData(result);
    }

    private MarkerData.GlyphMarker makeGlyphMarker(StrutMetrics strutMetrics) {
        int diameter = (int) ((strutMetrics.getAscent() + strutMetrics.getDescent()) / 3);

        MarkerData.GlyphMarker result = new MarkerData.GlyphMarker();
        result.setDiameter(diameter);
        result.setLayoutWidth(diameter * 3);

        return result;
    }


    private MarkerData.ImageMarker makeImageMarker(
            LayoutContext c, StrutMetrics structMetrics, String image) {
        FSImage img = null;
        if (! image.equals("none")) {
            img = c.getUac().getImageResource(image).getImage();
            if (img != null) {
                StrutMetrics strutMetrics = structMetrics;
                if (img.getHeight() > strutMetrics.getAscent()) {
                    img.scale(-1, (int) strutMetrics.getAscent());
                }
                MarkerData.ImageMarker result = new MarkerData.ImageMarker();
                result.setImage(img);
                result.setLayoutWidth(img.getWidth() * 2);
                return result;
            }
        }
        return null;
    }

    private MarkerData.TextMarker makeTextMarker(LayoutContext c, IdentValue listStyle) {
        String text;

        int listCounter = getListCounter();
        text = CounterFunction.createCounterText(listStyle, listCounter);

        IdentValue listDirection = getParent().getStyle().getDirection();

        if (listDirection == IdentValue.RTL) {
            text = "  .".concat(text);
        } else {
            assert listDirection == IdentValue.LTR || listDirection == IdentValue.AUTO;
            text = text.concat(".  ");
        }

        int w = c.getTextRenderer().getWidth(
                c.getFontContext(),
                getStyle().getFSFont(c),
                text);

        MarkerData.TextMarker result = new MarkerData.TextMarker();

        result.setLayoutWidth(w);
        result.setText(text);

        return result;
    }

    public int getListCounter() {
        return _listCounter;
    }

    public void setListCounter(int listCounter) {
        _listCounter = listCounter;
    }

    public PersistentBFC getPersistentBFC() {
        return _persistentBFC;
    }

    public void setPersistentBFC(PersistentBFC persistentBFC) {
        _persistentBFC = persistentBFC;
    }

    public Box getStaticEquivalent() {
        return _staticEquivalent;
    }

    public void setStaticEquivalent(Box staticEquivalent) {
        _staticEquivalent = staticEquivalent;
    }

    public boolean shouldBeReplaced() {
    	return _isReplaced;
    }
    
    public boolean isReplaced() {
        return _replacedElement != null;
    }

    @Override
    public void calcCanvasLocation() {
        if (isFloated()) {
            FloatManager manager = _floatedBoxData.getManager();
            if (manager != null) {
                Point offset = manager.getOffset(this);
                setAbsX(manager.getMaster().getAbsX() + getX() - offset.x);
                setAbsY(manager.getMaster().getAbsY() + getY() - offset.y);
            }
        }

        LineBox lineBox = getLineBox();
        if (lineBox == null) {
            Box parent = getParent();
            if (parent != null) {
                setAbsX(parent.getAbsX() + parent.getTx() + getX());
                setAbsY(parent.getAbsY() + parent.getTy() + getY());
            } else if (isStyled() && getStyle().isAbsFixedOrInlineBlockEquiv()) {
                Box cb = getContainingBlock();
                if (cb != null) {
                    setAbsX(cb.getAbsX() + getX());
                    setAbsY(cb.getAbsY() + getY());
                }
            }
        } else {
            setAbsX(lineBox.getAbsX() + getX());
            setAbsY(lineBox.getAbsY() + getY());
        }

        if (isReplaced()) {
            Point location = getReplacedElement().getLocation();
            if (location.x != getAbsX() || location.y != getAbsY()) {
                getReplacedElement().setLocation(getAbsX(), getAbsY());
            }
        }
    }

    public void calcInitialFloatedCanvasLocation(LayoutContext c) {
        Point offset = c.getBlockFormattingContext().getOffset();
        FloatManager manager = c.getBlockFormattingContext().getFloatManager();
        setAbsX(manager.getMaster().getAbsX() + getX() - offset.x);
        setAbsY(manager.getMaster().getAbsY() + getY() - offset.y);
    }

    @Override
    public void calcChildLocations() {
        super.calcChildLocations();

        if (_persistentBFC != null) {
            _persistentBFC.getFloatManager().calcFloatLocations();
        }
    }

    public boolean isNeedPageClear() {
        return _needPageClear;
    }

    public void setNeedPageClear(boolean needPageClear) {
        _needPageClear = needPageClear;
    }


    private void alignToStaticEquivalent() {
        if (_staticEquivalent.getAbsY() != getAbsY()) {
            setY(_staticEquivalent.getAbsY() - getAbsY());
            setAbsY(_staticEquivalent.getAbsY());
        }
    }

    public void positionAbsolute(CssContext cssCtx, int direction) {
        CalculatedStyle style = getStyle();

        Rectangle boundingBox = null;

        int cbContentHeight = getContainingBlock().getContentAreaEdge(0, 0, cssCtx).height;

        if (getContainingBlock() instanceof BlockBox) {
            boundingBox = getContainingBlock().getPaddingEdge(0, 0, cssCtx);
        } else {
            boundingBox = getContainingBlock().getContentAreaEdge(0, 0, cssCtx);
        }

        if ((direction & POSITION_HORIZONTALLY) != 0) {
            setX(0);
            if (!style.isIdent(CSSName.LEFT, IdentValue.AUTO)) {
                setX((int) style.getFloatPropertyProportionalWidth(CSSName.LEFT, getContainingBlock().getContentWidth(), cssCtx));
            } else if (!style.isIdent(CSSName.RIGHT, IdentValue.AUTO)) {
                setX(boundingBox.width -
                        (int) style.getFloatPropertyProportionalWidth(CSSName.RIGHT, getContainingBlock().getContentWidth(), cssCtx) - getWidth());
            }
            setX(getX() + boundingBox.x);
        }

        if ((direction & POSITION_VERTICALLY) != 0) {
            setY(0);
            if (!style.isIdent(CSSName.TOP, IdentValue.AUTO)) {
                setY((int) style.getFloatPropertyProportionalHeight(CSSName.TOP, cbContentHeight, cssCtx));
            } else if (!style.isIdent(CSSName.BOTTOM, IdentValue.AUTO)) {
                setY(boundingBox.height -
                        (int) style.getFloatPropertyProportionalWidth(CSSName.BOTTOM, cbContentHeight, cssCtx) - getHeight());
            }

            // Can't do this before now because our containing block
            // must be completed layed out
            int pinnedHeight = calcPinnedHeight(cssCtx);
            if (pinnedHeight != -1 && getCSSHeight(cssCtx) == -1) {
                setHeight(pinnedHeight);
                applyCSSMinMaxHeight(cssCtx);
            }

            setY(getY() + boundingBox.y);
        }

        calcCanvasLocation();

        if ((direction & POSITION_VERTICALLY) != 0 &&
                getStyle().isTopAuto() && getStyle().isBottomAuto()) {
            alignToStaticEquivalent();
        }

        calcChildLocations();
    }

	/**
     * Using the css:
     *
     * -fs-page-break-min-height: 5cm;
     *
     * on a block element you can force a pagebreak before this block, if not
     * enough space (e.g. 5cm in this case) is remaining on the current page for the block.
     *
     * @return true if a pagebreak is needed before this block because
     * there is not enough space left on the current page.
     */
    public boolean isPageBreakNeededBecauseOfMinHeight(LayoutContext context){
        float minHeight = getStyle().getFSPageBreakMinHeight(context);
        PageBox page = context.getRootLayer().getFirstPage(context, this);
        return page != null && getAbsY() + minHeight > page.getBottom(context);
    }


    public void positionAbsoluteOnPage(LayoutContext c) {
        if (c.isPrint() &&
                (getStyle().isForcePageBreakBefore() || isNeedPageClear() || isPageBreakNeededBecauseOfMinHeight(c))) {
            forcePageBreakBefore(c, getStyle().getIdent(CSSName.PAGE_BREAK_BEFORE), false);
            calcCanvasLocation();
            calcChildLocations();

            setNeedPageClear(false);
        }
    }

    public ReplacedElement getReplacedElement() {
        return _replacedElement;
    }

    public void setReplacedElement(ReplacedElement replacedElement) {
        _replacedElement = replacedElement;
    }

    @Override
    public void reset(LayoutContext c) {
        super.reset(c);
        setTopMarginCalculated(false);
        setBottomMarginCalculated(false);
        setDimensionsCalculated(false);
        setMinMaxCalculated(false);
        setChildrenHeight(0);
        if (isReplaced()) {
            getReplacedElement().detach(c);
            setReplacedElement(null);
        }
        if (getChildrenContentType() == ContentType.INLINE) {
            removeAllChildren();
        }

        if (isFloated()) {
            _floatedBoxData.getManager().removeFloat(this);
            _floatedBoxData.getDrawingLayer().removeFloat(this);
        }

        if (getStyle().isRunning()) {
            c.getRootLayer().removeRunningBlock(this);
        }
    }

    private int calcPinnedContentWidth(CssContext c) {
        if (! getStyle().isIdent(CSSName.LEFT, IdentValue.AUTO) &&
                ! getStyle().isIdent(CSSName.RIGHT, IdentValue.AUTO)) {
            Rectangle paddingEdge = getContainingBlock().getPaddingEdge(0, 0, c);

            int left = (int) getStyle().getFloatPropertyProportionalTo(
                    CSSName.LEFT, paddingEdge.width, c);
            int right = (int) getStyle().getFloatPropertyProportionalTo(
                    CSSName.RIGHT, paddingEdge.width, c);

            int result = paddingEdge.width - left - right - getLeftMBP() - getRightMBP();
            return result < 0 ? 0 : result;
        }

        return -1;
    }

    private int calcPinnedHeight(CssContext c) {
        if (! getStyle().isIdent(CSSName.TOP, IdentValue.AUTO) &&
                ! getStyle().isIdent(CSSName.BOTTOM, IdentValue.AUTO)) {
            Rectangle paddingEdge = getContainingBlock().getPaddingEdge(0, 0, c);

            int top = (int) getStyle().getFloatPropertyProportionalTo(
                    CSSName.TOP, paddingEdge.height, c);
            int bottom = (int) getStyle().getFloatPropertyProportionalTo(
                    CSSName.BOTTOM, paddingEdge.height, c);


            int result = paddingEdge.height - top - bottom;
            return result < 0 ? 0 : result;
        }

        return -1;
    }

    protected void resolveAutoMargins(
            LayoutContext c, int cssWidth,
            RectPropertySet padding, BorderPropertySet border) {
        int withoutMargins =
                (int) border.left() + (int) padding.left() +
                        cssWidth +
                        (int) padding.right() + (int) border.right();
        if (withoutMargins < getContainingBlockWidth()) {
            int available = getContainingBlockWidth() - withoutMargins;

            boolean autoLeft = getStyle().isAutoLeftMargin();
            boolean autoRight = getStyle().isAutoRightMargin();

            if (autoLeft && autoRight) {
                setMarginLeft(c, available / 2);
                setMarginRight(c, available / 2);
            } else if (autoLeft) {
                setMarginLeft(c, available);
            } else if (autoRight) {
                setMarginRight(c, available);
            }
        }
    }

    private int calcEffPageRelativeWidth(LayoutContext c) {
        int totalLeftMBP = 0;
        int totalRightMBP = 0;

        boolean usePageRelativeWidth = true;

        Box current = this;
        while (true) {
            CalculatedStyle style = current.getStyle();
            if (style.isAutoWidth() && ! style.isCanBeShrunkToFit()) {
                totalLeftMBP += current.getLeftMBP();
                totalRightMBP += current.getRightMBP();
            } else {
                usePageRelativeWidth = false;
                break;
            }

            if (current.getContainingBlock().isInitialContainingBlock()) {
                break;
            } else {
                current = current.getContainingBlock();
            }
        }

        if (usePageRelativeWidth) {
            PageBox currentPage = c.getRootLayer().getFirstPage(c, this);
            return currentPage.getContentWidth(c) - totalLeftMBP - totalRightMBP;
        } else {
            return getContainingBlockWidth() - getLeftMBP() - getRightMBP();
        }
    }

    /**
     * Creates the replaced element as required. This method should be idempotent.
     */
    private void createReplaced(LayoutContext c) {
        ReplacedElement re = getReplacedElement();
        
        if (re == null) {
            int cssWidth = getCSSWidth(c);
            int cssHeight = getCSSHeight(c);
            
            // Since the interface doesn't allow us to pass min-width/height
            // we implement it here.
            int minWidth = getCSSMinWidth(c);
            int minHeight = getCSSMinHeight(c);
            
            if (minWidth > cssWidth &&
                minWidth > 0) {
                cssWidth = minWidth;
            }
            
            if (minHeight > cssHeight &&
                minHeight > 0) {
                cssHeight = minHeight;
            }
            
            re = c.getReplacedElementFactory().createReplacedElement(
                    c, this, c.getUac(), cssWidth, cssHeight);
            
            if (re != null) {
                setReplacedElement(re);
                sizeReplacedElement(c, re);
            }
        }
    }
    
    /**
     * Size a replaced element taking into account size properties including min/max,
     * border-box/content-box and the natural size/aspect ratio of the replaced object.
     * 
     * This method may be called multiple times so must be idempotent.
     */
    private void sizeReplacedElement(LayoutContext c, ReplacedElement re) {
        int cssWidth = getCSSWidth(c);
        int cssHeight = getCSSHeight(c);
        
        boolean haveExactDims = cssWidth >= 0 && cssHeight >= 0;
        
        boolean usedMinWidth = false;
        boolean usedMinHeight = false;
        boolean usedMaxWidth = false;
        boolean usedMaxHeight = false;
        
        int intrinsicWidth = re.getIntrinsicWidth();
        int intrinsicHeight = re.getIntrinsicHeight();
        
        int minWidth = getCSSMinWidth(c);
        int minHeight = getCSSMinHeight(c);
        
        // Clamp w to max-width if required.
        if (!getStyle().isMaxWidthNone() &&
            (intrinsicWidth > getCSSMaxWidth(c) || cssWidth > getCSSMaxWidth(c))) {
            cssWidth = getCSSMaxWidth(c);
            usedMaxWidth = true;
        }

        // Clamp w to min-width if required.
        if (cssWidth >= 0 &&
            minWidth > 0 &&
            cssWidth < minWidth) {
            cssWidth = minWidth;
            usedMinWidth = true;
        }
        
        // Clamp h to max-height if required.
        if (!getStyle().isMaxHeightNone() &&
            (intrinsicHeight > getCSSMaxHeight(c) || cssHeight > getCSSMaxHeight(c))) {
            cssHeight = getCSSMaxHeight(c);
            usedMaxHeight = true;
        }

        // Clamp h to min-height if required.
        if (cssHeight >= 0 &&
            minHeight > 0 && 
            cssHeight < minHeight) {
            cssHeight = minHeight;
            usedMinHeight = true;
        }
                          
        if (getStyle().isBorderBox()) {
            BorderPropertySet border = getBorder(c);
            RectPropertySet padding = getPadding(c);
            cssWidth = cssWidth < 0 ? cssWidth : (int) Math.max(0, cssWidth - border.width() - padding.width());
            cssHeight = cssHeight < 0 ? cssHeight : (int) Math.max(0, cssHeight - border.height() - padding.height());
        }

        int nw;
        int nh;
        
        boolean useExact = 
                (haveExactDims && !usedMaxHeight && !usedMaxWidth && !usedMinWidth && !usedMinHeight);
        
        if (cssWidth > 0 && cssHeight > 0) {
            if (useExact) {
                // We can warp the aspect ratio if we have explicit width and height values
                // and the max/min values have not taken precedence.
                nw = cssWidth;
                nh = cssHeight;
            } else if (intrinsicWidth > cssWidth || intrinsicHeight > cssHeight) {
                // Too large, so reduce respecting the aspect ratio.
                double rw = (double) intrinsicWidth / (double) cssWidth;
                double rh = (double) intrinsicHeight / (double) cssHeight;

                if (rw > rh) {
                    nw = cssWidth;
                    nh = (int) (intrinsicHeight / rw);
                } else {
                    nw = (int) (intrinsicWidth / rh);
                    nh = cssHeight;
                }
            } else {
                // Too small.
                double rw = (double) intrinsicWidth / (double) cssWidth;
                double rh = (double) intrinsicHeight / (double) cssHeight;

                if (rw > rh) {
                    nw = cssWidth;
                    nh = ((int) (intrinsicHeight / rw));
                } else {
                    nw = ((int) (intrinsicWidth / rh));
                    nh = cssHeight;
                }
            }
        } else if (cssWidth > 0) {
            // Explicit min/max/width with auto height so keep aspect ratio.
            nw = cssWidth;
            nh = ((int) (((double) cssWidth / (double) intrinsicWidth) * intrinsicHeight));
        } else if (cssHeight > 0) {
            // Explicit min/max/height with auto width.
            nh = cssHeight;
            nw = ((int) (((double) cssHeight / (double) intrinsicHeight) * intrinsicWidth));
        } else if (cssWidth == 0 || cssHeight == 0) {
            // Empty.
            nw = cssWidth;
            nh = cssHeight;
        } else {
            // Auto width and height so use the natural dimensions of the replaced object.
            nw = intrinsicWidth;
            nh = intrinsicHeight;
        }
        
        setContentWidth(nw);
        setHeight(nh);
    }
    
    public void calcDimensions(LayoutContext c) {
        calcDimensions(c, getCSSWidth(c));
    }

    protected void calcDimensions(LayoutContext c, int cssWidth) {
        if (! isDimensionsCalculated()) {
            CalculatedStyle style = getStyle();

            RectPropertySet padding = getPadding(c);
            BorderPropertySet border = getBorder(c);

            if (cssWidth != -1 && !isAnonymous() &&
                    (getStyle().isIdent(CSSName.MARGIN_LEFT, IdentValue.AUTO) ||
                            getStyle().isIdent(CSSName.MARGIN_RIGHT, IdentValue.AUTO)) &&
                    getStyle().isNeedAutoMarginResolution()) {
                resolveAutoMargins(c, cssWidth, padding, border);
            }

            recalcMargin(c);
            RectPropertySet margin = getMargin(c);

            // CLEAN: cast to int
            setLeftMBP((int) margin.left() + (int) border.left() + (int) padding.left());
            setRightMBP((int) padding.right() + (int) border.right() + (int) margin.right());
            
            createReplaced(c);
            if (isReplaced()) {
                setDimensionsCalculated(true);
                return;
            }
            
            if (c.isPrint() && getStyle().isDynamicAutoWidth()) {
                setContentWidth(calcEffPageRelativeWidth(c));
            } else {
                setContentWidth((getContainingBlockWidth() - getLeftMBP() - getRightMBP()));
            }

            setHeight(0);

            if (! isAnonymous() || (isFromCaptionedTable() && isFloated())) {
                int pinnedContentWidth = -1;

                if (cssWidth != -1) {
                    if (style.isBorderBox()) {
                        setBorderBoxWidth(c, cssWidth);
                    } else {
                        setContentWidth(cssWidth);
                    }
                } else if (getStyle().isAbsolute() || getStyle().isFixed()) {
                    pinnedContentWidth = calcPinnedContentWidth(c);
                    if (pinnedContentWidth != -1) {
                        setContentWidth(pinnedContentWidth);
                    }
                }

                int cssHeight = getCSSHeight(c);
                if (cssHeight != -1) {
                    if (style.isBorderBox()) {
                        setBorderBoxHeight(c, cssHeight);
                    } else {
                        setHeight(cssHeight);
                    }
                }

                ReplacedElement re = getReplacedElement();

                if (re != null) {

                } else if (cssWidth == -1 && pinnedContentWidth == -1 &&
                        style.isCanBeShrunkToFit()) {
                    setNeedShrinkToFitCalculatation(true);
                }

                if (! isReplaced()) {
                    applyCSSMinMaxWidth(c);
                }
            }

            setDimensionsCalculated(true);
        }
    }

    private void calcClearance(LayoutContext c) {
        if (getStyle().isCleared() && ! getStyle().isFloated()) {
            c.translate(0, -getY());
            c.getBlockFormattingContext().clear(c, this);
            c.translate(0, getY());
            calcCanvasLocation();
        }
    }

    private void calcExtraPageClearance(LayoutContext c) {
        if (c.isPageBreaksAllowed() &&
                c.getExtraSpaceTop() > 0 && (getStyle().isSpecifiedAsBlock() || getStyle().isListItem())) {
            PageBox first = c.getRootLayer().getFirstPage(c, this);
            if (first != null && first.getTop() + c.getExtraSpaceTop() > getAbsY()) {
                int diff = first.getTop() + c.getExtraSpaceTop() - getAbsY();
                setY(getY() + diff);
                c.translate(0, diff);
                calcCanvasLocation();
            }
        }
    }

    protected void addBoxID(LayoutContext c) {
        if (! isAnonymous()) {
            String name = c.getNamespaceHandler().getAnchorName(getElement());
            if (name != null) {
                c.addBoxId(name, this);
            }
            String id = c.getNamespaceHandler().getID(getElement());
            if (id != null) {
                c.addBoxId(id, this);
            }
        }
    }

    public void layout(LayoutContext c) {
        layout(c, 0);
    }

    public void layout(LayoutContext c, int contentStart) {
        CalculatedStyle style = getStyle();

        boolean pushedLayer = checkPushLayer(c, style);

        calcClearance(c);

        checkPushBfc(c);

        addBoxID(c);

        if (c.isPrint() && getStyle().isIdent(CSSName.FS_PAGE_SEQUENCE, IdentValue.START)) {
            c.getRootLayer().addPageSequence(this);
        }

        createReplaced(c);
        calcDimensions(c);
        calcShrinkToFitWidthIfNeeded(c);
        collapseMargins(c);

        calcExtraPageClearance(c);

        if (c.isPrint()) {
            PageBox firstPage = c.getRootLayer().getFirstPage(c, this);
            if (firstPage != null && firstPage.getTop() == getAbsY() - getPageClearance()) {
                resetTopMargin(c);
            }
        }

        BorderPropertySet border = getBorder(c);
        RectPropertySet margin = getMargin(c);
        RectPropertySet padding = getPadding(c);

        // save height in case fixed height
        int originalHeight = getHeight();

        if (! isReplaced()) {
            setHeight(0);
        }

        boolean didSetMarkerData = false;
        if (getStyle().isListItem()) {
            createMarkerData(c);
            c.setCurrentMarkerData(getMarkerData());
            didSetMarkerData = true;
        }

        // do children's layout
        int tx = (int) margin.left() + (int) border.left() + (int) padding.left();
        int ty = (int) margin.top() + (int) border.top() + (int) padding.top();
        setTx(tx);
        setTy(ty);
        c.translate(getTx(), getTy());
        if (! isReplaced()) {
            layoutChildren(c, contentStart);
        } else {
            setState(Box.DONE);
        }
        c.translate(-getTx(), -getTy());

        setChildrenHeight(getHeight());

        if (! isReplaced()) {
            if (! isAutoHeight()) {
                int delta = originalHeight - getHeight();
                if (delta > 0 || isAllowHeightToShrink()) {
                    setHeight(originalHeight);
                }
            }

            applyCSSMinMaxHeight(c);
        }

        if (isRoot() || getStyle().establishesBFC()) {
            if (getStyle().isAutoHeight()) {
                int delta =
                        c.getBlockFormattingContext().getFloatManager().getClearDelta(
                                c, getTy() + getHeight());
                if (delta > 0) {
                    setHeight(getHeight() + delta);
                    setChildrenHeight(getChildrenHeight() + delta);
                }
            }
        }

        if (didSetMarkerData) {
            c.setCurrentMarkerData(null);
        }

        calcLayoutHeight(c, border, margin, padding);

        checkPopBfc(c);

        if (pushedLayer) {
            c.popLayer();
        }
    }

    protected boolean checkPushLayer(LayoutContext c, CalculatedStyle style) {
        if (isRoot()) {
            c.pushLayer(this);

            if (c.isPrint()) {
                if (!style.isIdent(CSSName.PAGE, IdentValue.AUTO)) {
                    c.setPageName(style.getStringProperty(CSSName.PAGE));
                }
                c.getRootLayer().addPage(c);
            }

            return true;
        } else if (style.requiresLayer() && this.getLayer() == null) {
            c.pushLayer(this);
            return true;
        } else if (style.requiresLayer()) {
            // FIXME: HACK. Some boxes can be layed out many times (to satisfy page constraints for example).
            // If this happens we just mark our old layer for deletion and create a new layer.
            // Not sure this is right, but doesn't break any correct tests.
            //
            // NOTE: This only happens if someone has called layout multiple times
            // without calling reset beforehand.
            this.getLayer().setForDeletion(true);
            c.pushLayer(this);
            return true;
        }

        return false;
    }

    /**
     * Checks if this box established a block formatting context and if so
     * removes the last bfc from the stack.
     * See also {@link #checkPushBfc(LayoutContext)}
     */
    protected void checkPopBfc(LayoutContext c) {
        if (isRoot() || getStyle().establishesBFC()) {
            c.popBFC();
        }
    }

    /**
     * Checks if this box establishes a block formatting context and if
     * so creates one and pushes it to the stack of bfcs.
     * See also {@link #checkPopBfc(LayoutContext)}
     */
    protected void checkPushBfc(LayoutContext c) {
        if (isRoot() || getStyle().establishesBFC() || isMarginAreaRoot()) {
            BlockFormattingContext bfc = new BlockFormattingContext(this, c);
            c.pushBFC(bfc);
        }
    }

    protected boolean isAllowHeightToShrink() {
        return true;
    }

    protected int getPageClearance() {
        return 0;
    }

    /**
     * Oh oh! Up to this method height is used to track content height. After this method it is used
     * to track total layout height! 
     */
    protected void calcLayoutHeight(
            LayoutContext c, BorderPropertySet border,
            RectPropertySet margin, RectPropertySet padding) {
        setHeight(getHeight() + ((int) margin.top() + (int) border.top() + (int) padding.top() +
                (int) padding.bottom() + (int) border.bottom() + (int) margin.bottom()));
        setChildrenHeight(getChildrenHeight() + ((int) margin.top() + (int) border.top() + (int) padding.top() +
                (int) padding.bottom() + (int) border.bottom() + (int) margin.bottom()));
    }


    private void calcShrinkToFitWidthIfNeeded(LayoutContext c) {
        if (isNeedShrinkToFitCalculatation()) {
            setContentWidth(calcShrinkToFitWidth(c) - getLeftMBP() - getRightMBP());
            applyCSSMinMaxWidth(c);
            setNeedShrinkToFitCalculatation(false);
        }
    }

    private void applyCSSMinMaxWidth(CssContext c) {
        int w = getStyle().isBorderBox() ? getBorderBoxWidth(c) : getContentWidth();
        
        if (! getStyle().isMaxWidthNone()) {
            int cssMaxWidth = getCSSMaxWidth(c);
            if (w > cssMaxWidth) {
                if (getStyle().isBorderBox()) {
                    setBorderBoxWidth(c, cssMaxWidth);
                } else {
                    setContentWidth(cssMaxWidth);
                }
            }
        }
        
        int cssMinWidth = getCSSMinWidth(c);
        if (cssMinWidth > 0 && w < cssMinWidth) {
            if (getStyle().isBorderBox()) {
                setBorderBoxWidth(c, cssMinWidth);
            } else {
                setContentWidth(cssMinWidth);
            }
        }
    }

    private void applyCSSMinMaxHeight(CssContext c) {
        int currentHeight = getStyle().isBorderBox() ? getBorderBoxHeight(c) : getHeight();
        
        if (! getStyle().isMaxHeightNone()) {
            int cssMaxHeight = getCSSMaxHeight(c);
            if (currentHeight > cssMaxHeight) {
                if (getStyle().isBorderBox()) {
                    setBorderBoxHeight(c, cssMaxHeight);
                } else {
                    setHeight(cssMaxHeight);
                }
            }
        }
        
        int cssMinHeight = getCSSMinHeight(c);
        if (cssMinHeight > 0 && currentHeight < cssMinHeight) {
            if (getStyle().isBorderBox()) {
                setBorderBoxHeight(c, cssMinHeight);
            } else {
                setHeight(cssMinHeight);
            }
        }
    }

    public void ensureChildren(LayoutContext c) {
        if (getChildrenContentType() == ContentType.UNKNOWN) {
            BoxBuilder.createChildren(c, this);
        }
    }

    protected void layoutChildren(LayoutContext c, int contentStart) {
        setState(Box.CHILDREN_FLUX);
        ensureChildren(c);

        if (getFirstLetterStyle() != null) {
            c.setFirstLettersTracker(
                c.getFirstLettersTracker().withStyle(getFirstLetterStyle()));
        }
        if (getFirstLineStyle() != null) {
            c.setFirstLinesTracker(
                c.getFirstLinesTracker().withStyle(getFirstLineStyle()));
        }

        switch (getChildrenContentType()) {
            case INLINE:
                layoutInlineChildren(c, contentStart, calcInitialBreakAtLine(c), true);
                break;
            case BLOCK:
                BlockBoxing.layoutContent(c, this, contentStart);
                break;
            case UNKNOWN:
                // FALL-THRU - Can not happen due to ensureChildren call above.
            case EMPTY:
                // FALL-THRU
            default:
                break;
        }

        if (getFirstLetterStyle() != null) {
            c.setFirstLettersTracker(
                c.getFirstLettersTracker().withOutLast());
        }
        if (getFirstLineStyle() != null) {
            c.setFirstLinesTracker(
                c.getFirstLinesTracker().withOutLast());
        }

        setState(Box.DONE);
    }

    protected void layoutInlineChildren(
            LayoutContext c, int contentStart, int breakAtLine, boolean tryAgain) {
        InlineBoxing.layoutContent(c, this, contentStart, breakAtLine);

        if (c.isPrint() && c.isPageBreaksAllowed() && getChildCount() > 1) {
            satisfyWidowsAndOrphans(c, contentStart, tryAgain);
        }

        if (tryAgain && (getStyle().isTextJustify())) {
            justifyText(c);
        }
    }

    private void justifyText(LayoutContext c) {
        for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            LineBox line = (LineBox)i.next();
            line.justify(c);
        }
    }

    /**
     * TERMINOLOGY:
     * Orphans refers to the number of lines of content in this
     * box before the first page break.
     * Widows refers to the number of lines of content on the last page.
     * <br><br>
     * METHOD AIM:
     * This method aims (but can not guarantee) to satisfy the <code>orphans</code> and
     * <code>widows</code> CSS properties. Each of these provide a number
     * specifying a minimum number of content lines.
     * <br><br>
     * HOW:
     * By inserting page breaks, either before this box or between certain
     * lines in this box.
     * <br><br>
     * PREREQUISITES:
     * That the content of this box is <code>CONTENT_INLINE</code> and layout has
     * been done on this box. This means that the children of this box will consist
     * entirely of LineBox objects.
     */
    private void satisfyWidowsAndOrphans(LayoutContext c, int contentStart, boolean tryAgain) {
        int orphans = (int) getStyle().asFloat(CSSName.ORPHANS);
        int widows = (int) getStyle().asFloat(CSSName.WIDOWS);

        if (orphans == 0 && widows == 0) {
            return;
        }

        LineBox firstLineBox = (LineBox)getChild(0);
        PageBox firstPage = c.getRootLayer().getFirstPage(c, firstLineBox);

        if (firstPage == null) {
            return;
        }

        int noContentLBs = 0;
        int i = 0;
        int cCount = getChildCount();

        // First count the number of lines on the first page.
        while (i < cCount) {
            LineBox lB = (LineBox)getChild(i);

            if (lB.getAbsY() >= firstPage.getBottom(c)) {
                break;
            }

            if (! lB.isContainsContent()) {
                noContentLBs++;
            }

            i++;
        }

        // Check if all lines are on the one page.
        if (i != cCount) {

            if (i - noContentLBs < orphans) {
                // We don't have enough lines on first page.
                setNeedPageClear(true);
            } else {
                // We have to check the last page for widows.
                LineBox lastLineBox = (LineBox)getChild(cCount-1);
                PageBox lastPage = c.getRootLayer().getFirstPage(c, lastLineBox.getAbsY());

                noContentLBs = 0;
                i = cCount - 1;
                int lastPageLineCount = 0;

                // Going backwards, count lines on the last page.
                while (i >= 0) {
                    LineBox lB = (LineBox) getChild(i);

                    if (lB.getAbsY() < lastPage.getTop()) {
                        break;
                    }

                    if (! lB.isContainsContent()) {
                        noContentLBs++;
                    }

                    i--;
                    lastPageLineCount++;
                }

                lastPageLineCount -= noContentLBs;

                if (lastPageLineCount < widows) {
                    // We don't have enough lines on last page.

                    if (cCount - 1 - widows < orphans) {
                        // If adding a page break to satisfy widows property would
                        // break orphans constraint insert a page break at start.
                        setNeedPageClear(true);
                    } else if (tryAgain) {
                        // Else, if we are allowed, lay out our line boxes with
                        // a page break inserted after breakAtLine.
                        int breakAtLine = cCount - 1 - widows;

                        resetChildren(c);
                        removeAllChildren();

                        layoutInlineChildren(c, contentStart, breakAtLine, false);
                    }
                }
            }
        }
    }

    /**
     * See {@link ContentType}
     */
    public ContentType getChildrenContentType() {
        return _childrenContentType;
    }

    /**
     * See {@link ContentType}
     */
    public void setChildrenContentType(ContentType contentType) {
        _childrenContentType = contentType;
    }

    /**
     * See {@link #setInlineContent(List)}
     */
    public List<Styleable> getInlineContent() {
        return _inlineContent;
    }

    /**
     * Inline content is created by the box builder.
     * It is important to note that the inline content here is stored in
     * the pre-layout state. Ie. It has not been flowed out into
     * {@link LineBox} and {@link InlineLayoutBox} objects but is stored
     * as {@link InlineBox} and block boxes that are laid out inline such
     * as inline-block and inline-table.
     * <br><br>
     * During layout inline-content is laid out into lines and so on but
     * the inline content is left untouched so as to be able to run layout
     * multiple times to satisfy constraints.
     * <br><br>
     * This method should be called with {@link #setChildrenContentType(ContentType)} set
     * to {@link ContentType#INLINE} as block boxes can not contain mixed content.
     */
    public void setInlineContent(List<Styleable> inlineContent) {
        _inlineContent = inlineContent;

        if (inlineContent != null) {
            for (Styleable child : inlineContent) {
                if (child instanceof Box) {
                    ((Box) child).setContainingBlock(this);
                }
            }
        }
    }

    protected boolean isSkipWhenCollapsingMargins() {
        return false;
    }

    protected boolean isMayCollapseMarginsWithChildren() {
        return (! isRoot()) && getStyle().isMayCollapseMarginsWithChildren();
    }

    // This will require a rethink if we ever truly layout incrementally
    // Should only ever collapse top margin and pick up collapsable
    // bottom margins by looking back up the tree.
    protected void collapseMargins(LayoutContext c) {
        if (! isTopMarginCalculated() || ! isBottomMarginCalculated()) {
            recalcMargin(c);
            RectPropertySet margin = getMargin(c);

            if (! isTopMarginCalculated() && ! isBottomMarginCalculated() && isVerticalMarginsAdjoin(c)) {
                MarginCollapseResult collapsedMargin =
                        _pendingCollapseCalculation != null ?
                                _pendingCollapseCalculation : new MarginCollapseResult();
                collapseEmptySubtreeMargins(c, collapsedMargin);
                setCollapsedBottomMargin(c, margin, collapsedMargin);
            } else {
                if (! isTopMarginCalculated()) {
                    MarginCollapseResult collapsedMargin =
                            _pendingCollapseCalculation != null ?
                                    _pendingCollapseCalculation : new MarginCollapseResult();

                    collapseTopMargin(c, true, collapsedMargin);
                    if ((int) margin.top() != collapsedMargin.getMargin()) {
                        setMarginTop(c, collapsedMargin.getMargin());
                    }
                }

                if (! isBottomMarginCalculated()) {
                    MarginCollapseResult collapsedMargin = new MarginCollapseResult();
                    collapseBottomMargin(c, true, collapsedMargin);

                    setCollapsedBottomMargin(c, margin, collapsedMargin);
                }
            }
        }
    }

    private void setCollapsedBottomMargin(LayoutContext c, RectPropertySet margin, MarginCollapseResult collapsedMargin) {
        BlockBox next = null;
        if (! isInline()) {
            next = getNextCollapsableSibling(collapsedMargin);
        }
        if (! (next == null || next instanceof AnonymousBlockBox) &&
                collapsedMargin.hasMargin()) {
            next._pendingCollapseCalculation = collapsedMargin;
            setMarginBottom(c, 0);
        } else if ((int) margin.bottom() != collapsedMargin.getMargin()) {
            setMarginBottom(c, collapsedMargin.getMargin());
        }
    }

    protected BlockBox getNextCollapsableSibling(MarginCollapseResult collapsedMargin) {
        BlockBox next = (BlockBox) getNextSibling();
        while (next != null) {
            if (next instanceof AnonymousBlockBox) {
                ((AnonymousBlockBox) next).provideSiblingMarginToFloats(
                        collapsedMargin.getMargin());
            }
            if (! next.isSkipWhenCollapsingMargins()) {
                break;
            } else {
                next = (BlockBox) next.getNextSibling();
            }
        }
        return next;
    }

    private void collapseTopMargin(
            LayoutContext c, boolean calculationRoot, MarginCollapseResult result) {
        if (! isTopMarginCalculated()) {
            if (! isSkipWhenCollapsingMargins()) {
                calcDimensions(c);
                if (c.isPrint() && getStyle().isDynamicAutoWidthApplicable()) {
                    // Force recalculation once box is positioned
                    setDimensionsCalculated(false);
                }
                RectPropertySet margin = getMargin(c);
                result.update((int) margin.top());

                if (! calculationRoot && (int) margin.top() != 0) {
                    setMarginTop(c, 0);
                }

                if (isMayCollapseMarginsWithChildren() && isNoTopPaddingOrBorder(c)) {
                    ensureChildren(c);
                    if (getChildrenContentType() == ContentType.BLOCK) {
                        for (Iterator<Box> i = getChildIterator(); i.hasNext();) {
                            BlockBox child = (BlockBox) i.next();
                            child.collapseTopMargin(c, false, result);

                            if (child.isSkipWhenCollapsingMargins()) {
                                continue;
                            }

                            break;
                        }
                    }
                }
            }

            setTopMarginCalculated(true);
        }
    }

    private void collapseBottomMargin(
            LayoutContext c, boolean calculationRoot, MarginCollapseResult result) {
        if (! isBottomMarginCalculated()) {
            if (! isSkipWhenCollapsingMargins()) {
                calcDimensions(c);
                if (c.isPrint() && getStyle().isDynamicAutoWidthApplicable()) {
                    // Force recalculation once box is positioned
                    setDimensionsCalculated(false);
                }
                RectPropertySet margin = getMargin(c);
                result.update((int) margin.bottom());

                if (! calculationRoot && (int) margin.bottom() != 0) {
                    setMarginBottom(c, 0);
                }

                if (isMayCollapseMarginsWithChildren() &&
                        ! getStyle().isTable() && isNoBottomPaddingOrBorder(c)) {
                    ensureChildren(c);
                    if (getChildrenContentType() == ContentType.BLOCK) {
                        for (int i = getChildCount() - 1; i >= 0; i--) {
                            BlockBox child = (BlockBox) getChild(i);

                            if (child.isSkipWhenCollapsingMargins()) {
                                continue;
                            }

                            child.collapseBottomMargin(c, false, result);

                            break;
                        }
                    }
                }
            }

            setBottomMarginCalculated(true);
        }
    }

    private boolean isNoTopPaddingOrBorder(LayoutContext c) {
        RectPropertySet padding = getPadding(c);
        BorderPropertySet border = getBorder(c);

        return (int) padding.top() == 0 && (int) border.top() == 0;
    }

    private boolean isNoBottomPaddingOrBorder(LayoutContext c) {
        RectPropertySet padding = getPadding(c);
        BorderPropertySet border = getBorder(c);

        return (int) padding.bottom() == 0 && (int) border.bottom() == 0;
    }

    private void collapseEmptySubtreeMargins(LayoutContext c, MarginCollapseResult result) {
        RectPropertySet margin = getMargin(c);
        result.update((int) margin.top());
        result.update((int) margin.bottom());

        setMarginTop(c, 0);
        setTopMarginCalculated(true);
        setMarginBottom(c, 0);
        setBottomMarginCalculated(true);

        ensureChildren(c);
        if (getChildrenContentType() == ContentType.BLOCK) {
            for (Iterator<Box> i = getChildIterator(); i.hasNext();) {
                BlockBox child = (BlockBox) i.next();
                child.collapseEmptySubtreeMargins(c, result);
            }
        }
    }

    private boolean isVerticalMarginsAdjoin(LayoutContext c) {
        CalculatedStyle style = getStyle();

        BorderPropertySet borderWidth = style.getBorder(c);
        RectPropertySet padding = getPadding(c);

        boolean bordersOrPadding =
                (int) borderWidth.top() != 0 || (int) borderWidth.bottom() != 0 ||
                        (int) padding.top() != 0 || (int) padding.bottom() != 0;

        if (bordersOrPadding) {
            return false;
        }

        ensureChildren(c);
        if (getChildrenContentType() == ContentType.INLINE) {
            return false;
        } else if (getChildrenContentType() == ContentType.BLOCK) {
            for (Iterator<Box> i = getChildIterator(); i.hasNext();) {
                BlockBox child = (BlockBox) i.next();
                if (child.isSkipWhenCollapsingMargins() || ! child.isVerticalMarginsAdjoin(c)) {
                    return false;
                }
            }
        }

        return style.asFloat(CSSName.MIN_HEIGHT) == 0 &&
                (isAutoHeight() || style.asFloat(CSSName.HEIGHT) == 0);
    }

    public boolean isTopMarginCalculated() {
        return _topMarginCalculated;
    }

    public void setTopMarginCalculated(boolean topMarginCalculated) {
        _topMarginCalculated = topMarginCalculated;
    }

    public boolean isBottomMarginCalculated() {
        return _bottomMarginCalculated;
    }

    public void setBottomMarginCalculated(boolean bottomMarginCalculated) {
        _bottomMarginCalculated = bottomMarginCalculated;
    }

    protected int getCSSWidth(CssContext c) {
        return getCSSWidth(c, false);
    }

    protected int getCSSWidth(CssContext c, boolean shrinkingToFit) {
        if (! isAnonymous()) {
            if (! getStyle().isAutoWidth()) {
                if (shrinkingToFit && ! getStyle().isAbsoluteWidth()) {
                    return -1;
                } else {
                    int result = (int) getStyle().getFloatPropertyProportionalWidth(
                            CSSName.WIDTH, getContainingBlock().getContentWidth(), c);
                    return result >= 0 ? result : -1;
                }
            }
        }

        return -1;
    }

    protected int getCSSFitToWidth(CssContext c) {
        if (! isAnonymous()) {
            if (! getStyle().isIdent(CSSName.FS_FIT_IMAGES_TO_WIDTH, IdentValue.AUTO))
            {
                int result = (int) getStyle().getFloatPropertyProportionalWidth(
                        CSSName.FS_FIT_IMAGES_TO_WIDTH, getContainingBlock().getContentWidth(), c);
                return result >= 0 ? result : -1;
            }
        }

        return -1;
    }

    protected int getCSSHeight(CssContext c) {
        if (! isAnonymous()) {
            if (! isAutoHeight()) {
                if (getStyle().hasAbsoluteUnit(CSSName.HEIGHT)) {
                    return (int)getStyle().getFloatPropertyProportionalHeight(CSSName.HEIGHT, 0, c);
                } else {
                    return (int)getStyle().getFloatPropertyProportionalHeight(
                            CSSName.HEIGHT,
                            ((BlockBox)getContainingBlock()).getCSSHeight(c),
                            c);
                }
            }
        }

        return -1;
    }

    public boolean isAutoHeight() {
        if (getStyle().isAutoHeight()) {
            return true;
        } else if (getStyle().hasAbsoluteUnit(CSSName.HEIGHT)) {
            return false;
        } else {
            // We have a percentage height, defer to our block parent (if applicable)
            Box cb = getContainingBlock();
            if (cb.isStyled() && (cb instanceof BlockBox)) {
                return ((BlockBox)cb).isAutoHeight();
            } else return !(cb instanceof BlockBox) || !cb.isInitialContainingBlock();
        }
    }

    private int getCSSMinWidth(CssContext c) {
        return getStyle().getMinWidth(c, getContainingBlockWidth());
    }

    private int getCSSMaxWidth(CssContext c) {
        return getStyle().getMaxWidth(c, getContainingBlockWidth());
    }

    private int getCSSMinHeight(CssContext c) {
        return getStyle().getMinHeight(c, getContainingBlockCSSHeight(c));
    }

    private int getCSSMaxHeight(CssContext c) {
        return getStyle().getMaxHeight(c, getContainingBlockCSSHeight(c));
    }

    // Use only when the height of the containing block is required for
    // resolving percentage values.  Does not represent the actual (resolved) height
    // of the containing block.
    private int getContainingBlockCSSHeight(CssContext c) {
        if (! getContainingBlock().isStyled() ||
                getContainingBlock().getStyle().isAutoHeight()) {
            return 0;
        } else {
            if (getContainingBlock().getStyle().hasAbsoluteUnit(CSSName.HEIGHT)) {
                return (int) getContainingBlock().getStyle().getFloatPropertyProportionalTo(
                        CSSName.HEIGHT, 0, c);
            } else {
                return 0;
            }
        }
    }

    private int calcShrinkToFitWidth(LayoutContext c) {
        calcMinMaxWidth(c);

        return Math.min(Math.max(getMinWidth(), getAvailableWidth(c)), getMaxWidth());
    }

    protected int getAvailableWidth(LayoutContext c) {
        if (! getStyle().isAbsolute()) {
            return getContainingBlockWidth();
        } else {
            int left = 0;
            int right = 0;
            if (! getStyle().isIdent(CSSName.LEFT, IdentValue.AUTO)) {
                left =
                        (int) getStyle().getFloatPropertyProportionalTo(CSSName.LEFT,
                                getContainingBlock().getContentWidth(), c);
            }

            if (! getStyle().isIdent(CSSName.RIGHT, IdentValue.AUTO)) {
                right =
                        (int) getStyle().getFloatPropertyProportionalTo(CSSName.RIGHT,
                                getContainingBlock().getContentWidth(), c);
            }

            return getContainingBlock().getPaddingWidth(c) - left - right;
        }
    }

    protected boolean isFixedWidthAdvisoryOnly() {
        return false;
    }


    private void recalcMargin(LayoutContext c) {
        if (isTopMarginCalculated() && isBottomMarginCalculated()) {
            return;
        }

        // Check if we're a potential candidate upfront to avoid expensive
        // getStyleMargin(c, false) call
        FSDerivedValue topMargin = getStyle().valueByName(CSSName.MARGIN_TOP);
        boolean resetTop = topMargin instanceof LengthValue && ! topMargin.hasAbsoluteUnit();

        FSDerivedValue bottomMargin = getStyle().valueByName(CSSName.MARGIN_BOTTOM);
        boolean resetBottom = bottomMargin instanceof LengthValue && ! bottomMargin.hasAbsoluteUnit();

        if (! resetTop && ! resetBottom) {
            return;
        }

        RectPropertySet styleMargin = getStyleMargin(c, false);
        RectPropertySet workingMargin = getMargin(c);

        // A shrink-to-fit calculation may have set incorrect values for
        // percentage margins (as the containing block width
        // hasn't been calculated yet).  Reset top and bottom margins
        // in this case.
        if (! isTopMarginCalculated() &&
                styleMargin.top() != workingMargin.top()) {
            setMarginTop(c, (int) styleMargin.top());
        }

        if (! isBottomMarginCalculated() &&
                styleMargin.bottom() != workingMargin.bottom()) {
            setMarginBottom(c, (int) styleMargin.bottom());
        }
    }

    public void calcMinMaxWidth(LayoutContext c) {
        if (! isMinMaxCalculated()) {
            RectPropertySet margin = getMargin(c);
            BorderPropertySet border = getBorder(c);
            RectPropertySet padding = getPadding(c);

            int width = getCSSWidth(c, true);

            createReplaced(c);
            if (isReplaced() && width == -1) {
                // FIXME: We need to special case this for issue 313.
                width = getContentWidth();
            }
 
            if (width != -1 && !isFixedWidthAdvisoryOnly()) {
                _minWidth = _maxWidth =
                        (int) margin.left() + (int) border.left() + (int) padding.left() +
                                width +
                                (int) margin.right() + (int) border.right() + (int) padding.right();
            } else {
                int cw = -1;
                if (width != -1) {
                    // Set a provisional content width on table cells so
                    // percentage values resolve correctly (but save and reset
                    // the existing value)
                    cw = getContentWidth();
                    setContentWidth(width);
                }

                _minWidth = _maxWidth =
                        (int) margin.left() + (int) border.left() + (int) padding.left() +
                                (int) margin.right() + (int) border.right() + (int) padding.right();

                int minimumMaxWidth = _maxWidth;
                if (width != -1) {
                    minimumMaxWidth += width;
                }

                ensureChildren(c);

                if (getChildrenContentType() == ContentType.BLOCK) {
                    calcMinMaxWidthBlockChildren(c);
                } else if (getChildrenContentType() == ContentType.INLINE) {
                    calcMinMaxWidthInlineChildren(c);
                }

                if (minimumMaxWidth > _maxWidth) {
                    _maxWidth = minimumMaxWidth;
                }

                if (cw != -1) {
                    setContentWidth(cw);
                }
            }

            if (! isReplaced()) {
                calcMinMaxCSSMinMaxWidth(c, margin, border, padding);
            }
            setMinMaxCalculated(true);
        }
    }

    private void calcMinMaxCSSMinMaxWidth(
            LayoutContext c, RectPropertySet margin, BorderPropertySet border,
            RectPropertySet padding) {
        int cssMinWidth = getCSSMinWidth(c);
        if (cssMinWidth > 0) {
            cssMinWidth +=
                    (int) margin.left() + (int) border.left() + (int) padding.left() +
                            (int) margin.right() + (int) border.right() + (int) padding.right();
            if (_minWidth < cssMinWidth) {
                _minWidth = cssMinWidth;
            }
        }
        if (! getStyle().isMaxWidthNone()) {
            int cssMaxWidth = getCSSMaxWidth(c);
            cssMaxWidth +=
                    (int) margin.left() + (int) border.left() + (int) padding.left() +
                            (int) margin.right() + (int) border.right() + (int) padding.right();
            if (_maxWidth > cssMaxWidth) {
                if (cssMaxWidth > _minWidth) {
                    _maxWidth = cssMaxWidth;
                } else {
                    _maxWidth = _minWidth;
                }
            }
        }
    }

    private void calcMinMaxWidthBlockChildren(LayoutContext c) {
        int childMinWidth = 0;
        int childMaxWidth = 0;

        for (Iterator<Box> i = getChildIterator(); i.hasNext();) {
            BlockBox child = (BlockBox) i.next();
            child.calcMinMaxWidth(c);
            if (child.getMinWidth() > childMinWidth) {
                childMinWidth = child.getMinWidth();
            }
            if (child.getMaxWidth() > childMaxWidth) {
                childMaxWidth = child.getMaxWidth();
            }
        }

        _minWidth += childMinWidth;
        _maxWidth += childMaxWidth;
    }

    private void calcMinMaxWidthInlineChildren(LayoutContext c) {
        int textIndent = (int) getStyle().getFloatPropertyProportionalWidth(
                CSSName.TEXT_INDENT, getContentWidth(), c);

        if (getStyle().isListItem() && getStyle().isListMarkerInside()) {
            createMarkerData(c);
            textIndent += getMarkerData().getLayoutWidth();
        }

        int childMinWidth = 0;
        int childMaxWidth = 0;
        int lineWidth = 0;

        InlineBox trimmableIB = null;

        for (Styleable child : _inlineContent) {
            if (child.getStyle().isAbsolute() || child.getStyle().isFixed() || child.getStyle().isRunning()) {
                continue;
            }

            if (child.getStyle().isFloated() || child.getStyle().isInlineBlock() ||
                    child.getStyle().isInlineTable()) {
                if (child.getStyle().isFloated() && child.getStyle().isCleared()) {
                    if (trimmableIB != null) {
                        lineWidth -= trimmableIB.getTrailingSpaceWidth(c);
                    }
                    if (lineWidth > childMaxWidth) {
                        childMaxWidth = lineWidth;
                    }
                    lineWidth = 0;
                }
                trimmableIB = null;
                BlockBox block = (BlockBox) child;
                block.calcMinMaxWidth(c);
                lineWidth += block.getMaxWidth();
                if (block.getMinWidth() > childMinWidth) {
                    childMinWidth = block.getMinWidth();
                }
            } else { /* child.getStyle().isInline() */
                InlineBox iB = (InlineBox) child;
                IdentValue whitespace = iB.getStyle().getWhitespace();

                iB.calcMinMaxWidth(c, getContentWidth(), lineWidth == 0);

                if (whitespace == IdentValue.NOWRAP) {
                    lineWidth += textIndent + iB.getMaxWidth();
                    if (iB.getMinWidth() > childMinWidth) {
                        childMinWidth = iB.getMinWidth();
                    }
                    trimmableIB = iB;
                } else if (whitespace == IdentValue.PRE) {
                    if (trimmableIB != null) {
                        lineWidth -= trimmableIB.getTrailingSpaceWidth(c);
                    }
                    trimmableIB = null;
                    if (lineWidth > childMaxWidth) {
                        childMaxWidth = lineWidth;
                    }
                    lineWidth = textIndent + iB.getFirstLineWidth();
                    if (lineWidth > childMinWidth) {
                        childMinWidth = lineWidth;
                    }
                    lineWidth = iB.getMaxWidth();
                    if (lineWidth > childMinWidth) {
                        childMinWidth = lineWidth;
                    }
                    if (childMinWidth > childMaxWidth) {
                        childMaxWidth = childMinWidth;
                    }
                    lineWidth = 0;
                } else if (whitespace == IdentValue.PRE_WRAP || whitespace == IdentValue.PRE_LINE) {
                    lineWidth += textIndent + iB.getFirstLineWidth();
                    if (trimmableIB != null) {
                        lineWidth -= trimmableIB.getTrailingSpaceWidth(c);
                    }
                    if (lineWidth > childMaxWidth) {
                        childMaxWidth = lineWidth;
                    }

                    if (iB.getMaxWidth() > childMaxWidth) {
                        childMaxWidth = iB.getMaxWidth();
                    }
                    if (iB.getMinWidth() > childMinWidth) {
                        childMinWidth = iB.getMinWidth();
                    }
                    if (whitespace == IdentValue.PRE_LINE) {
                        trimmableIB = iB;
                    } else {
                        trimmableIB = null;
                    }
                    lineWidth = 0;
                } else /* if (whitespace == IdentValue.NORMAL) */ {
                    lineWidth += textIndent + iB.getMaxWidth();
                    if (iB.getMinWidth() > childMinWidth) {
                        childMinWidth = textIndent + iB.getMinWidth();
                    }
                    trimmableIB = iB;
                }

                if (textIndent > 0) {
                    textIndent = 0;
                }
            }
        }

        if (trimmableIB != null) {
            lineWidth -= trimmableIB.getTrailingSpaceWidth(c);
        }
        if (lineWidth > childMaxWidth) {
            childMaxWidth = lineWidth;
        }

        _minWidth += childMinWidth;
        _maxWidth += childMaxWidth;
    }

    public int getMaxWidth() {
        return _maxWidth;
    }

    protected void setMaxWidth(int maxWidth) {
        _maxWidth = maxWidth;
    }

    public int getMinWidth() {
        return _minWidth;
    }

    protected void setMinWidth(int minWidth) {
        _minWidth = minWidth;
    }

    public void styleText(LayoutContext c) {
        styleText(c, getStyle());
    }

    // FIXME Should be expanded into generic restyle facility
    public void styleText(LayoutContext c, CalculatedStyle style) {
        if (getChildrenContentType() == ContentType.INLINE) {
            LinkedList<CalculatedStyle> styles = new LinkedList<>();
            styles.add(style);
            for (Object a_inlineContent : _inlineContent) {
                Styleable child = (Styleable) a_inlineContent;
                if (child instanceof InlineBox) {
                    InlineBox iB = (InlineBox) child;

                    if (iB.isStartsHere()) {
                        CascadedStyle cs = null;
                        if (iB.getElement() != null) {
                            if (iB.getPseudoElementOrClass() == null) {
                                cs = c.getCss().getCascadedStyle(iB.getElement(), false);
                            } else {
                                cs = c.getCss().getPseudoElementStyle(
                                        iB.getElement(), iB.getPseudoElementOrClass());
                            }
                            styles.add(styles.getLast().deriveStyle(cs));
                        } else {
                            styles.add(style.createAnonymousStyle(IdentValue.INLINE));
                        }
                    }

                    iB.setStyle(styles.getLast());
                    iB.applyTextTransform();

                    if (iB.isEndsHere()) {
                        styles.removeLast();
                    }
                }
            }
        }
    }

    @Override
    protected void calcChildPaintingInfo(
            final CssContext c, final PaintingInfo result, final boolean useCache) {
        if (getPersistentBFC() != null) {
            (this).getPersistentBFC().getFloatManager().performFloatOperation(
                    new FloatManager.FloatOperation() {
                        @Override
                        public void operate(Box floater) {
                            PaintingInfo info = floater.calcPaintingInfo(c, useCache);
                            moveIfGreater(
                                    result.getOuterMarginCorner(),
                                    info.getOuterMarginCorner());
                        }
                    });
        }
        super.calcChildPaintingInfo(c, result, useCache);
    }

    public CascadedStyle getFirstLetterStyle() {
        return _firstLetterStyle;
    }

    public void setFirstLetterStyle(CascadedStyle firstLetterStyle) {
        _firstLetterStyle = firstLetterStyle;
    }

    public CascadedStyle getFirstLineStyle() {
        return _firstLineStyle;
    }

    public void setFirstLineStyle(CascadedStyle firstLineStyle) {
        _firstLineStyle = firstLineStyle;
    }

    protected boolean isMinMaxCalculated() {
        return _minMaxCalculated;
    }

    protected void setMinMaxCalculated(boolean minMaxCalculated) {
        _minMaxCalculated = minMaxCalculated;
    }

    protected void setDimensionsCalculated(boolean dimensionsCalculated) {
        _dimensionsCalculated = dimensionsCalculated;
    }

    private boolean isDimensionsCalculated() {
        return _dimensionsCalculated;
    }

    protected void setNeedShrinkToFitCalculatation(boolean needShrinkToFitCalculatation) {
        _needShrinkToFitCalculatation = needShrinkToFitCalculatation;
    }

    private boolean isNeedShrinkToFitCalculatation() {
        return _needShrinkToFitCalculatation;
    }

    public void initStaticPos(LayoutContext c, BlockBox parent, int childOffset) {
        setX(0);
        setY(childOffset);
    }

    public int calcBaseline(LayoutContext c) {
        for (int i = 0; i < getChildCount(); i++) {
            Box b = getChild(i);
            if (b instanceof LineBox) {
                return b.getAbsY() + ((LineBox) b).getBaseline();
            } else {
                if (b instanceof TableRowBox) {
                    return b.getAbsY() + ((TableRowBox) b).getBaseline();
                } else {
                    int result = ((BlockBox) b).calcBaseline(c);
                    if (result != NO_BASELINE) {
                        return result;
                    }
                }
            }
        }

        return NO_BASELINE;
    }

    protected int calcInitialBreakAtLine(LayoutContext c) {
        BreakAtLineContext bContext = c.getBreakAtLineContext();
        if (bContext != null && bContext.getBlock() == this) {
            return bContext.getLine();
        }
        return 0;
    }

    public boolean isCurrentBreakAtLineContext(LayoutContext c) {
        BreakAtLineContext bContext = c.getBreakAtLineContext();
        return bContext != null && bContext.getBlock() == this;
    }

    public BreakAtLineContext calcBreakAtLineContext(LayoutContext c) {
        if (! c.isPrint() || ! getStyle().isKeepWithInline()) {
            return null;
        }

        LineBox breakLine = findLastNthLineBox((int)getStyle().asFloat(CSSName.WIDOWS));
        if (breakLine != null) {
            PageBox linePage = c.getRootLayer().getLastPage(c, breakLine);
            PageBox ourPage = c.getRootLayer().getLastPage(c, this);
            if (linePage != null && ourPage != null && linePage.getPageNo() + 1 == ourPage.getPageNo()) {
                BlockBox breakBox = breakLine.getParent();
                return new BreakAtLineContext(breakBox, breakBox.findOffset(breakLine));
            }
        }

        return null;
    }

    public int calcInlineBaseline(CssContext c) {
        if (isReplaced() && getReplacedElement().hasBaseline()) {
            Rectangle bounds = getContentAreaEdge(getAbsX(), getAbsY(), c);
            return bounds.y + getReplacedElement().getBaseline() - getAbsY();
        } else {
            LineBox lastLine = findLastLineBox();
            if (lastLine == null) {
                return getHeight();
            } else {
                return lastLine.getAbsY() + lastLine.getBaseline() - getAbsY();
            }
        }
    }

    public int findOffset(Box box) {
        int ccount = getChildCount();
        for (int i = 0; i < ccount; i++) {
            if (getChild(i) == box) {
                return i;
            }
        }
        return -1;
    }

    public LineBox findLastNthLineBox(int count) {
        LastLineBoxContext context = new LastLineBoxContext(count);
        findLastLineBox(context);
        return context.line;
    }

    private static class LastLineBoxContext {
        public int current;
        public LineBox line;

        public LastLineBoxContext(int i) {
            this.current = i;
        }
    }

    private void findLastLineBox(LastLineBoxContext context) {
        ContentType type = getChildrenContentType();
        int ccount = getChildCount();
        if (ccount > 0) {
            if (type == ContentType.INLINE) {
                for (int i = ccount - 1; i >= 0; i--) {
                    LineBox child = (LineBox) getChild(i);
                    if (child.getHeight() > 0) {
                        context.line = child;
                        if (--context.current == 0) {
                            return;
                        }
                    }
                }
            } else if (type == ContentType.BLOCK) {
                for (int i = ccount - 1; i >= 0; i--) {
                    ((BlockBox) getChild(i)).findLastLineBox(context);
                    if (context.current == 0) {
                        break;
                    }
                }
            }
        }
    }

    private LineBox findLastLineBox() {
        ContentType type = getChildrenContentType();
        int ccount = getChildCount();
        if (ccount > 0) {
            if (type == ContentType.INLINE) {
                for (int i = ccount - 1; i >= 0; i--) {
                    LineBox result = (LineBox) getChild(i);
                    if (result.getHeight() > 0) {
                        return result;
                    }
                }
            } else if (type == ContentType.BLOCK) {
                for (int i = ccount - 1; i >= 0; i--) {
                    LineBox result = ((BlockBox) getChild(i)).findLastLineBox();
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    private LineBox findFirstLineBox() {
        ContentType type = getChildrenContentType();
        int ccount = getChildCount();
        if (ccount > 0) {
            if (type == ContentType.INLINE) {
                for (int i = 0; i < ccount; i++) {
                    LineBox result = (LineBox) getChild(i);
                    if (result.getHeight() > 0) {
                        return result;
                    }
                }
            } else if (type == ContentType.BLOCK) {
                for (int i = 0; i < ccount; i++) {
                    LineBox result = ((BlockBox) getChild(i)).findFirstLineBox();
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    public boolean isNeedsKeepWithInline(LayoutContext c) {
        if (c.isPrint() && getStyle().isKeepWithInline()) {
            LineBox line = findFirstLineBox();
            if (line != null) {
                PageBox linePage = c.getRootLayer().getFirstPage(c, line);
                PageBox ourPage = c.getRootLayer().getFirstPage(c, this);
                return linePage != null && ourPage != null && linePage.getPageNo() == ourPage.getPageNo()+1;
            }
        }

        return false;
    }

    public boolean isFloated() {
        return _floatedBoxData != null;
    }

    public FloatedBoxData getFloatedBoxData() {
        return _floatedBoxData;
    }

    public void setFloatedBoxData(FloatedBoxData floatedBoxData) {
        _floatedBoxData = floatedBoxData;
    }

    public int getChildrenHeight() {
        return _childrenHeight;
    }

    protected void setChildrenHeight(int childrenHeight) {
        _childrenHeight = childrenHeight;
    }

    public boolean isFromCaptionedTable() {
        return _fromCaptionedTable;
    }

    public void setFromCaptionedTable(boolean fromTable) {
        _fromCaptionedTable = fromTable;
    }

    @Override
    protected boolean isInlineBlock() {
        return isInline();
    }

    public boolean isInMainFlow() {
        Box flowRoot = rootBox();
        return flowRoot.isRoot();
    }

    @Override
    public Box getDocumentParent() {
        Box staticEquivalent = getStaticEquivalent();
        if (staticEquivalent != null) {
            return staticEquivalent;
        } else {
            return getParent();
        }
    }

    public boolean isContainsInlineContent(LayoutContext c) {
        ensureChildren(c);
        switch (getChildrenContentType()) {
            case INLINE:
                return true;
            case EMPTY:
                return false;
            case BLOCK:
                return getChildren().stream().anyMatch(box -> ((BlockBox) box).isContainsInlineContent(c));
            case UNKNOWN:
                // FALL-THRU - Can not happen due to ensureChildren call above.
            default:
                throw new RuntimeException("internal error: no children");
        }
    }

    public boolean checkPageContext(LayoutContext c) {
        if (! getStyle().isIdent(CSSName.PAGE, IdentValue.AUTO)) {
            String pageName = getStyle().getStringProperty(CSSName.PAGE);
            if (!pageName.equals(c.getPageName()) && 
                isInDocumentFlow() &&
                (shouldBeReplaced() || isContainsInlineContent(c))) {
                c.setPendingPageName(pageName);
                return true;
            }
        } else if (c.getPageName() != null && isInDocumentFlow()) {
            c.setPendingPageName(null);
            return true;
        }

        return false;
    }

    public boolean isNeedsClipOnPaint(CssContext c) {
        return ! isReplaced() &&
            getStyle().isIdent(CSSName.OVERFLOW, IdentValue.HIDDEN) &&
            getStyle().isOverflowApplies();
    }


    protected void propagateExtraSpace(
            LayoutContext c,
            ContentLimitContainer parentContainer, ContentLimitContainer currentContainer,
            int extraTop, int extraBottom) {
        int start = currentContainer.getInitialPageNo();
        int end = currentContainer.getLastPageNo();
        int current = start;

        while (current <= end) {
            ContentLimit contentLimit =
                currentContainer.getContentLimit(current);

            if (current != start) {
                int top = contentLimit.getTop();
                if (top != ContentLimit.UNDEFINED) {
                    parentContainer.updateTop(c, top - extraTop);
                }
            }

            if (current != end) {
                int bottom = contentLimit.getBottom();
                if (bottom != ContentLimit.UNDEFINED) {
                    parentContainer.updateBottom(c, bottom + extraBottom);
                }
            }

            current++;
        }
    }

    @Override
    public void collectLayoutText(LayoutContext c, StringBuilder builder) {
        if (_childrenContentType == BlockBox.ContentType.INLINE) {
            for (Styleable s : getInlineContent()) {
                if (s instanceof InlineBox) {
                    builder.append(((InlineBox) s).getText());
                } else if (s instanceof BlockBox) {
                    ((BlockBox) s).collectLayoutText(c, builder);
                }
            }
        } else if (_childrenContentType == BlockBox.ContentType.BLOCK) {
            for (Box box : getChildren()) {
                if (box instanceof BlockBox) {
                    ((BlockBox) box).collectLayoutText(c, builder);
                }
            }
        }
    }

    public static class MarginCollapseResult {
        private int maxPositive;
        private int maxNegative;

        public void update(int value) {
            if (value < 0 && value < maxNegative) {
                maxNegative = value;
            }

            if (value > 0 && value > maxPositive) {
                maxPositive = value;
            }
        }

        public int getMargin() {
            return maxPositive + maxNegative;
        }

        public boolean hasMargin() {
            return maxPositive != 0 || maxNegative != 0;
        }
    }

}
