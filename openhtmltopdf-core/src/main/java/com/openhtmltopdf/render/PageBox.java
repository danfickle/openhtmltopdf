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
package com.openhtmltopdf.render;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.constants.MarginBoxName;
import com.openhtmltopdf.css.newmatch.PageInfo;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.FSFunction;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.EmptyStyle;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.render.simplepainter.SimplePainter;
import com.openhtmltopdf.util.BoxUtil;
import com.openhtmltopdf.util.LambdaUtil;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.util.ThreadCtx;

public class PageBox {
    private static final MarginArea[] MARGIN_AREA_DEFS = new MarginArea[] {
        new TopLeftCorner(),
        new TopMarginArea(),
        new TopRightCorner(),
        
        new LeftMarginArea(),
        new RightMarginArea(),
        
        new BottomLeftCorner(),
        new BottomMarginArea(),
        new BottomRightCorner(),
    };
    
    private static final int LEADING_TRAILING_SPLIT = 5;
    
    private CalculatedStyle _style;
    
    private int _top;
    private int _bottom;
    
    private int _paintingTop;
    private int _paintingBottom;
    
    private int _pageNo;
    
    private int _outerPageWidth;
    
    private PageDimensions _pageDimensions;
    
    private PageInfo _pageInfo;
    
    private MarginAreaContainer[] _marginAreas = new MarginAreaContainer[MARGIN_AREA_DEFS.length];
    
    private Element _metadata;
    
    private int _basePagePdfPageIndex;
    private int _shadowPageCount;

    private BlockBox _footnoteArea;
    private int _totalFootnoteHeight;

    public void setBasePagePdfPageIndex(int idx) {
        this._basePagePdfPageIndex = idx;
    }
    
    public void setShadowPageCount(int cnt) {
        this._shadowPageCount = cnt;
    }
    
    public int getBasePagePdfPageIndex() {
        return _basePagePdfPageIndex;
    }
    
    public int getShadowPageCount() {
        return _shadowPageCount;
    }
    
    public int getWidth(CssContext cssCtx) {
        resolvePageDimensions(cssCtx);
        
        return _pageDimensions.getWidth();
    }

    public int getHeight(CssContext cssCtx) {
        resolvePageDimensions(cssCtx);
        
        return _pageDimensions.getHeight();
    }
    
    private void resolvePageDimensions(CssContext cssCtx) {
        if (_pageDimensions == null) {
            CalculatedStyle style = getStyle();
            
            int width;
            int height;
            
            if (style.isLength(CSSName.FS_PAGE_WIDTH)) {
                width = (int)style.getFloatPropertyProportionalTo(
                        CSSName.FS_PAGE_WIDTH, 0, cssCtx);
            } else {
                width = resolveAutoPageWidth(cssCtx);
            }
            
            if (style.isLength(CSSName.FS_PAGE_HEIGHT)) {
                height = (int)style.getFloatPropertyProportionalTo(
                        CSSName.FS_PAGE_HEIGHT, 0, cssCtx);
            } else {
                height = resolveAutoPageHeight(cssCtx);
            }
            
            if (style.isIdent(CSSName.FS_PAGE_ORIENTATION, IdentValue.LANDSCAPE)) {
                int temp;
                
                temp = width;
                width = height;
                height = temp;
            }
            
            PageDimensions dim = new PageDimensions();
            dim.setWidth(width);
            dim.setHeight(height);
            
            _pageDimensions = dim;
        }
    }
    
    /**
     * Returns the default page width if defined, else the A4 page size width.
     * <b>Note:</b> We previously returned different sizes
     * based on locale, but this could lead to different results between developement machines
     * and servers so we now always return A4.
     * @param cssCtx
     * @return
     */
    private int resolveAutoPageWidth(CssContext cssCtx) {
      if (ThreadCtx.get().sharedContext().getDefaultPageWidth() != null) {
    	  float defaultPageWidth = ThreadCtx.get().sharedContext().getDefaultPageWidth();
    	  boolean isInches = ThreadCtx.get().sharedContext().isDefaultPageSizeInches();
    	  return (int) LengthValue.calcFloatProportionalValue(getStyle(),
    			  CSSName.FS_PAGE_WIDTH, String.valueOf(defaultPageWidth), defaultPageWidth, isInches ? CSSPrimitiveValue.CSS_IN : CSSPrimitiveValue.CSS_MM, 0, cssCtx);
      }
      else {
    	return (int)LengthValue.calcFloatProportionalValue(
                    getStyle(),
                    CSSName.FS_PAGE_WIDTH,
                    "210mm",
                    210f,
                    CSSPrimitiveValue.CSS_MM,
                    0,
                    cssCtx);
      }
    }
    
    /**
     * Return the default page height if defined, else A4.
     * @param cssCtx
     * @return
     */
    private int resolveAutoPageHeight(CssContext cssCtx) {
        if (ThreadCtx.get().sharedContext().getDefaultPageHeight() != null) {
      	  float defaultPageHeight = ThreadCtx.get().sharedContext().getDefaultPageHeight();
      	  boolean isInches = ThreadCtx.get().sharedContext().isDefaultPageSizeInches();
      	  return (int) LengthValue.calcFloatProportionalValue(getStyle(),
      			  CSSName.FS_PAGE_WIDTH, String.valueOf(defaultPageHeight), defaultPageHeight, isInches ? CSSPrimitiveValue.CSS_IN : CSSPrimitiveValue.CSS_MM, 0, cssCtx);
        }
        else {
            return (int)LengthValue.calcFloatProportionalValue(
                    getStyle(),
                    CSSName.FS_PAGE_HEIGHT,
                    "297mm",
                    297f,
                    CSSPrimitiveValue.CSS_MM,
                    0,
                    cssCtx);
        }
    }    

    public int getContentHeight(CssContext cssCtx) {
        int retval = getHeight(cssCtx) - getMarginBorderPadding(cssCtx, CalculatedStyle.TOP)
                - getMarginBorderPadding(cssCtx, CalculatedStyle.BOTTOM);
        if (retval <= 0) {
            throw new IllegalArgumentException(
                    "The content height cannot be zero or less.  Check your document margin definition.");
        }
        return retval;
    }

    public int getContentWidth(CssContext cssCtx) {
        int retval = getWidth(cssCtx) - getMarginBorderPadding(cssCtx, CalculatedStyle.LEFT)
                - getMarginBorderPadding(cssCtx, CalculatedStyle.RIGHT);
        if (retval <= 0) {
            throw new IllegalArgumentException(
                    "The content width cannot be zero or less.  Check your document margin definition.");
        }
        return retval;
    }

    
    public CalculatedStyle getStyle() {
        return _style;
    }

    public void setStyle(CalculatedStyle style) {
        _style = style;
    }

    /**
     * Gets the document Y position in top down units (20 per CSS pixel for PDF)
     * <br><br>
     * Example: If each page is 500px high and has a 50px margin, then if this is called
     * on the second page it will return 16_000, which is 2 x 400px x 20.
     * <br><br>
     * Note: Does not take into account footnotes and any other <code>float: bottom</code>
     * content. For this, you can use {@link #getBottomUsable()}.
     * <br><br>
     * Important: Prefer {@link #getBottom(CssContext)} over this method.
     */
    public int getBottom() {
        return _bottom;
    }

    /**
     * Gets the document Y position taking into account <code>float: bottom</code>
     * content such as footnotes. When laying in-flow content we use this method
     * while when in footnotes we use {@link #getBottom()}.
     * <br><br>
     * Important: Prefer {@link #getBottom(CssContext)} over this method.
     */
    public int getBottomUsable() {
        return _bottom - _totalFootnoteHeight;
    }

    /**
     * If we are in a footnote calls {@link #getBottom()}, otherwise uses
     * {@link #getBottomUsable()}.
     */
    public int getBottom(CssContext c) {
        return c.isInFloatBottom() ? getBottom() : getBottomUsable();
    }

    public int getTop() {
        return _top;
    }
    
    public void setTopAndBottom(CssContext cssCtx, int top) {
        _top = top;
        _bottom = top + getContentHeight(cssCtx);
    }

    public int getPaintingBottom() {
        return _paintingBottom;
    }

    public void setPaintingBottom(int paintingBottom) {
        _paintingBottom = paintingBottom;
    }

    /**
     * Example: If a page is 100 units high and has a 10 unit margin,
     * this will return 0 for the first page and 80 for the second and so on.
     * 
     * @return the y index into the document coordinates.
     */
    public int getPaintingTop() {
        return _paintingTop;
    }

    public void setPaintingTop(int paintingTop) {
        _paintingTop = paintingTop;
    }
    
    public Rectangle getScreenPaintingBounds(CssContext cssCtx, int additionalClearance) {
        return new Rectangle(
                additionalClearance, getPaintingTop(),
                getWidth(cssCtx), getPaintingBottom()-getPaintingTop());
    }
    
    public Rectangle getPrintPaintingBounds(CssContext cssCtx) {
        return new Rectangle(
                0, 0,
                getWidth(cssCtx), getHeight(cssCtx));
    }
    
    /**
     * Get the rectangle that this page's content area will cover of the layed out document.
     * For example: If a page is 100 units high and 150 wide and has a margin of 10 then this method will
     * return a rect(0, 0, 130, 80) for the first page and a rect(0, 80, 130, 80) for the second and so on.
     */
    public Rectangle getDocumentCoordinatesContentBounds(CssContext c) {
    	return new Rectangle(
    			0,
    			getPaintingTop(),
    			getContentWidth(c),
    			getContentHeight(c));
    }
    
    /**
     * Get the shadow page (a page inserted to carry cut off content) content area of the layed out document.
     * For example: If a page one is 100 units high and 150 wide and has a margin of 10 then this will return a
     * rect(130, 0, 130, 80) for the first shadow page and a rect(260, 0, 130, 80) for the second shadow page
     * assuming cut-off direction is LTR.
     * 
     * For RTL the rects would be rect(-130, 0, 130, 80) and rect(-260, 0, 130, 80).
     */
    public Rectangle getDocumentCoordinatesContentBoundsForInsertedPage(CssContext c, int shadowPageNumber) {
        return new Rectangle(
                getContentWidth(c) * (shadowPageNumber + 1) * (getCutOffPageDirection() == IdentValue.LTR ? 1 : -1),
                getPaintingTop(),
                getContentWidth(c),
                getContentHeight(c));
    }
    

    /**
     * Returns the number of shadow pages needed for a given x coordinate.
     * For example if x = 800 and content width = 1000 returns 0 (assumes LTR).
     * For example if x = 2400 and content width = 900 returns 2 (assumes LTR).
     */
    public int getMaxShadowPagesForXPos(CssContext c, int x) {
        IdentValue dir = getCutOffPageDirection();
        float fx = x;
        float fw = getContentWidth(c);
        
        if (fw == 0f) {
            return 0;
        }
        
        if (dir == IdentValue.LTR) { 
            return (int) (x > 0 ? (Math.ceil(fx / fw) - 1) : 0);
        }
        
        return (int) (x < 0 ? (Math.ceil(Math.abs(fx) / fw)) : 0);
    }
    
    /**
     * Should shadow pages be inserted for cut off content for this page.
     */
    public boolean shouldInsertPages() {
        return getMaxInsertedPages() > 0;
    }
    
    /**
     * The maximum number of shadow pages to insert for cut-off content.
     */
    public int getMaxInsertedPages() {
        return getStyle().fsMaxOverflowPages();
    }
    
    /**
     * @return Either ltr (should insert cut-off content to the right of the page) or
     * rtl (should insert cut-off content to the left of the page).
     */
    public IdentValue getCutOffPageDirection() {
        return getStyle().getIdent(CSSName.FS_OVERFLOW_PAGES_DIRECTION);
    }
    
    public Rectangle getPagedViewClippingBounds(CssContext cssCtx, int additionalClearance) {
        Rectangle result = new Rectangle(
                additionalClearance + 
                    getMarginBorderPadding(cssCtx, CalculatedStyle.LEFT),
                getPaintingTop() + 
                    getMarginBorderPadding(cssCtx, CalculatedStyle.TOP),
                getContentWidth(cssCtx),
                getContentHeight(cssCtx));

        return result;
    }
    
    public Rectangle getPrintClippingBounds(CssContext cssCtx) {
        Rectangle result = new Rectangle(
                getMarginBorderPadding(cssCtx, CalculatedStyle.LEFT),
                getMarginBorderPadding(cssCtx, CalculatedStyle.TOP),
                getContentWidth(cssCtx),
                getContentHeight(cssCtx));
        
        result.height -= 1;

        return result;
    }
    
    public RectPropertySet getMargin(CssContext cssCtx) {
        return getStyle().getMarginRect(_outerPageWidth, cssCtx);
    }

    private Rectangle getBorderEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        Rectangle result = new Rectangle(left + (int) margin.left(),
                top + (int) margin.top(),
                getWidth(cssCtx) - (int) margin.left() - (int) margin.right(),
                getHeight(cssCtx) - (int) margin.top() - (int) margin.bottom());
        return result;
    }
    
    public void paintBorder(RenderingContext c, int additionalClearance, short mode) {
        int top = 0;
        if (mode == Layer.PAGED_MODE_SCREEN) {
            top = getPaintingTop();
        }
        c.getOutputDevice().paintBorder(c, 
                getStyle(),
                getBorderEdge(additionalClearance, top, c),
                BorderPainter.ALL);
    }
    
    public void paintBackground(RenderingContext c, int additionalClearance, short mode) {
        Rectangle bounds;
        if (mode == Layer.PAGED_MODE_SCREEN) {
            bounds = getScreenPaintingBounds(c, additionalClearance);
        } else {
            bounds = getPrintPaintingBounds(c);
        }
        
        c.getOutputDevice().paintBackground(c, getStyle(), bounds, bounds, getStyle().getBorder(c));
    }

    private MarginAreaContainer currentMarginAreaContainer;
    public void paintMarginAreas(RenderingContext c, int additionalClearance, short mode) {
        for (int i = 0; i < MARGIN_AREA_DEFS.length; i++) {
            MarginAreaContainer container = _marginAreas[i];
      
            if (container != null) {
                currentMarginAreaContainer = container;
                TableBox table = _marginAreas[i].getTable();
                Point p = container.getArea().getPaintingPosition(
                        c, this, additionalClearance, mode);

                c.getOutputDevice().translate(p.x, p.y);
                if (c.getOutputDevice().isFastRenderer()) {
                    table.getLayer().propagateCurrentTransformationMatrix(c);
                    SimplePainter painter = new SimplePainter(p.x, p.y);
                    Object token = c.getOutputDevice().startStructure(StructureType.RUNNING, table);
                    painter.paintLayer(c, table.getLayer());
                    c.getOutputDevice().endStructure(token);
                } else {
                    table.getLayer().paint(c);
                }
                c.getOutputDevice().translate(-p.x, -p.y);
            }
        }
        currentMarginAreaContainer = null;
    }

    /**
     * Paint the footnote area layer if it exists.
     */
    public void paintFootnoteArea(RenderingContext c) {
        if (_footnoteArea != null) {
            Box body = BoxUtil.getBodyOrNull(c.getRootLayer().getMaster());
            int start = getHeight(c) - (_totalFootnoteHeight + getMarginBorderPadding(c, CalculatedStyle.BOTTOM));
            int x = getMarginBorderPadding(c, CalculatedStyle.LEFT);

            if (body != null) {
                x += body.getMarginBorderPadding(c, CalculatedStyle.LEFT);
            }

            // FIXME: Wrong StructureType here.
            Object token = c.getOutputDevice().startStructure(StructureType.RUNNING, _footnoteArea);

            _footnoteArea.setAbsX(x);
            _footnoteArea.setAbsY(start);
            _footnoteArea.calcChildLocations();

            // SimplePainter paints everything in the layer by
            // assuming it is all on this page.
            SimplePainter painter = new SimplePainter(0, 0);
            painter.paintLayer(c, _footnoteArea.getLayer());

            c.getOutputDevice().endStructure(token);
        }
    }

    public MarginBoxName[] getCurrentMarginBoxNames() {
        if( currentMarginAreaContainer == null )
            return null;
        return currentMarginAreaContainer.getArea().getMarginBoxNames();
    }

    public int getPageNo() {
        return _pageNo;
    }

    public void setPageNo(int pageNo) {
        _pageNo = pageNo;
    }

    public int getOuterPageWidth() {
        return _outerPageWidth;
    }

    public void setOuterPageWidth(int containingBlockWidth) {
        _outerPageWidth = containingBlockWidth;
    }
    
    public int getMarginBorderPadding(CssContext cssCtx, int which) {
        return getStyle().getMarginBorderPadding(
                cssCtx, getOuterPageWidth(), which);
    }

    public PageInfo getPageInfo() {
        return _pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        _pageInfo = pageInfo;
    }
    
    public Element getMetadata() {
        return _metadata;
    }
    
    public void layout(LayoutContext c) {
        c.setPage(this);
        retrievePageMetadata(c);
        layoutMarginAreas(c);
    }
    
    // HACK Would much prefer to do this in ITextRenderer or ITextOutputDevice
    // but given the existing API, this is about the only place it can be done
    private void retrievePageMetadata(LayoutContext c) {
        List<PropertyDeclaration> props = getPageInfo().getXMPPropertyList();
        if (props != null && props.size() > 0)
        {
            for (Iterator<PropertyDeclaration> i = props.iterator(); i.hasNext(); ) {
                PropertyDeclaration decl = i.next();
                if (decl.getCSSName() == CSSName.CONTENT) {
                    PropertyValue value = (PropertyValue)decl.getValue();
                    List<PropertyValue> values = value.getValues();
                    if (values.size() == 1) {
                        PropertyValue funcVal = values.get(0);
                        if (funcVal.getPropertyValueType() == PropertyValue.VALUE_TYPE_FUNCTION) {
                            FSFunction func = funcVal.getFunction();
                            if (BoxBuilder.isElementFunction(func)) {
                                BlockBox metadata = BoxBuilder.getRunningBlock(c, funcVal);
                                if (metadata != null) {
                                    _metadata = metadata.getElement();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private void layoutMarginAreas(LayoutContext c) {
        RectPropertySet margin = getMargin(c);
        for (int i = 0; i < MARGIN_AREA_DEFS.length; i++) {
            MarginArea area = MARGIN_AREA_DEFS[i];
            
            Dimension dim = area.getLayoutDimension(c, this, margin);
            TableBox table = BoxBuilder.createMarginTable(
                    c, _pageInfo, 
                    area.getMarginBoxNames(),
                    (int)dim.getHeight(),
                    area.getDirection());
            if (table != null) {
                table.setContainingBlock(new MarginBox(new Rectangle((int)dim.getWidth(), (int)dim.getHeight())));
                try {
                    c.setNoPageBreak(1);
                    
                    c.reInit(false);
                    c.pushLayer(table);
                    c.getRootLayer().addPage(c);
                    
                    table.layout(c);
                    
                    c.popLayer();
                } finally {
                    c.setNoPageBreak(0);
                }
                _marginAreas[i] = new MarginAreaContainer(area, table);
            }
        }
    }
    
    public boolean isLeftPage() {
        return _pageNo % 2 != 0;
    }
    
    public boolean isRightPage() {
        return _pageNo % 2 == 0;
    }
    
    public void exportLeadingText(RenderingContext c, Writer writer) throws IOException {
        for (int i = 0; i < LEADING_TRAILING_SPLIT; i++) {
            MarginAreaContainer container = _marginAreas[i];
            if (container != null) {
                container.getTable().exportText(c, writer);
            }
        }
    }
    
    public void exportTrailingText(RenderingContext c, Writer writer) throws IOException {
        for (int i = LEADING_TRAILING_SPLIT; i < _marginAreas.length; i++) {
            MarginAreaContainer container = _marginAreas[i];
            if (container != null) {
                container.getTable().exportText(c, writer);
            }
        }
    }
    
    private static final class PageDimensions {
        private int _width;
        private int _height;

        public int getHeight() {
            return _height;
        }

        public void setHeight(int height) {
            _height = height;
        }

        public int getWidth() {
            return _width;
        }

        public void setWidth(int width) {
            _width = width;
        }
    }
    
    private static class MarginAreaContainer {
        private final MarginArea _area;
        private final TableBox _table;
        
        public MarginAreaContainer(MarginArea area, TableBox table) {
            _area = area;
            _table = table;
        }

        public MarginArea getArea() {
            return _area;
        }

        public TableBox getTable() {
            return _table;
        }
    }
    
    private static abstract class MarginArea {
        private final MarginBoxName[] _marginBoxNames;
        
        public abstract Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin);
        public abstract Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode);
        
        public MarginArea(MarginBoxName marginBoxName) {
            _marginBoxNames = new MarginBoxName[] { marginBoxName };
        }
        
        public MarginArea(MarginBoxName[] marginBoxNames) {
            _marginBoxNames = marginBoxNames;
        }

        
        public MarginBoxName[] getMarginBoxNames() {
            return _marginBoxNames;
        }
        
        public int getDirection() {
            return BoxBuilder.MARGIN_BOX_HORIZONTAL;
        }
    }
    
    private static class TopLeftCorner extends MarginArea {
        public TopLeftCorner() {
            super(MarginBoxName.TOP_LEFT_CORNER);
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension((int)margin.left(), (int)margin.top());
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance;
            int top;
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingTop();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = 0;
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }
            
            return new Point(left, top);
        }

    }
    
    private static class TopRightCorner extends MarginArea {
        public TopRightCorner() {
            super(MarginBoxName.TOP_RIGHT_CORNER);
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension((int)margin.right(), (int)margin.top());
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance + page.getWidth(c) - (int)page.getMargin(c).right();
            int top;
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingTop();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = 0;
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }
            
            return new Point(left, top);
        }
    }
    
    private static class BottomRightCorner extends MarginArea {
        public BottomRightCorner() {
            super(MarginBoxName.BOTTOM_RIGHT_CORNER);
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension((int)margin.right(), (int)margin.bottom());
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance + page.getWidth(c) - (int)page.getMargin(c).right();
            int top;
            
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingBottom() - (int)page.getMargin(c).bottom();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = page.getHeight(c) - (int)page.getMargin(c).bottom();
            } else {
                throw new IllegalArgumentException("Illegal mode");
            } 
            
            return new Point(left, top);
        }
    }
    
    private static class BottomLeftCorner extends MarginArea {
        public BottomLeftCorner() {
            super(MarginBoxName.BOTTOM_LEFT_CORNER);
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension((int)margin.left(), (int)margin.bottom());
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance;
            int top;
            
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingBottom() - (int)page.getMargin(c).bottom();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = page.getHeight(c) - (int)page.getMargin(c).bottom();
            } else {
                throw new IllegalArgumentException("Illegal mode");
            } 
            
            return new Point(left, top);
        }
    }
    
    private static class LeftMarginArea extends MarginArea {
        public LeftMarginArea() {
            super(new MarginBoxName[] {
                    MarginBoxName.LEFT_TOP, 
                    MarginBoxName.LEFT_MIDDLE, 
                    MarginBoxName.LEFT_BOTTOM });
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension((int)margin.left(), page.getContentHeight(c));
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance;
            int top;
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingTop() + (int)page.getMargin(c).top();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = (int)page.getMargin(c).top();
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }
            
            return new Point(left, top);
        }
        
        public int getDirection() {
            return BoxBuilder.MARGIN_BOX_VERTICAL;
        }
    } 
    
    private static class RightMarginArea extends MarginArea {
        public RightMarginArea() {
            super(new MarginBoxName[] {
                    MarginBoxName.RIGHT_TOP, 
                    MarginBoxName.RIGHT_MIDDLE, 
                    MarginBoxName.RIGHT_BOTTOM });
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension((int)margin.left(), page.getContentHeight(c));
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance + page.getWidth(c) - (int)page.getMargin(c).right();
            int top;
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingTop() + (int)page.getMargin(c).top();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = (int)page.getMargin(c).top();
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }
            
            return new Point(left, top);
        }
        
        public int getDirection() {
            return BoxBuilder.MARGIN_BOX_VERTICAL;
        }        
    }
    
    private static class TopMarginArea extends MarginArea {
        public TopMarginArea() {
            super(new MarginBoxName[] { 
                    MarginBoxName.TOP_LEFT, 
                    MarginBoxName.TOP_CENTER, 
                    MarginBoxName.TOP_RIGHT });
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension(page.getContentWidth(c), (int)margin.top());
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance + (int)page.getMargin(c).left();
            int top;
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingTop();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = 0;
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }   
            
            return new Point(left, top);
        }
    }   
    
    private static class BottomMarginArea extends MarginArea {
        public BottomMarginArea() {
            super(new MarginBoxName[] { 
                    MarginBoxName.BOTTOM_LEFT, 
                    MarginBoxName.BOTTOM_CENTER, 
                    MarginBoxName.BOTTOM_RIGHT });
        }

        public Dimension getLayoutDimension(CssContext c, PageBox page, RectPropertySet margin) {
            return new Dimension(page.getContentWidth(c), (int)margin.bottom());
        }

        public Point getPaintingPosition(
                RenderingContext c, PageBox page, int additionalClearance, short mode) {
            int left = additionalClearance + (int)page.getMargin(c).left();
            int top;
            
            if (mode == Layer.PAGED_MODE_SCREEN) {
                top = page.getPaintingBottom() - (int)page.getMargin(c).bottom();
            } else if (mode == Layer.PAGED_MODE_PRINT) {
                top = page.getHeight(c) - (int)page.getMargin(c).bottom();
            } else {
                throw new IllegalArgumentException("Illegal mode");
            }    
            
            return new Point(left, top);
        }
    }


    /**
     * Sets up the footnote area for this page.
     */
    private void createFootnoteArea(LayoutContext c) {
        // Create style from @footnote page rules with absolute
        // positioning and block display.
        CalculatedStyle style = new EmptyStyle().deriveStyle(
                _pageInfo.createFootnoteAreaStyle());

        Box rootBox = c.getRootLayer().getMaster();
        Element root = rootBox.getElement();

        // We get a NPE if our new BlockBox doesn't have an element
        // so just create one.
        Element me = root.getOwnerDocument().createElement("fs-footnote");
        Element body = BoxUtil.getBodyElementOrSomething(root);

        body.appendChild(me);

        Box bodyBox = BoxUtil.getBodyBoxOrSomething(c.getRootLayer().getMaster());
        int containingBlockWidth = OpenUtil.firstNonZero(
                         bodyBox.getContentWidth(), bodyBox.getWidth(),
                         rootBox.getContentWidth(), rootBox.getWidth(),
                         getContentWidth(c), getWidth(c));

        _footnoteArea = new BlockBox();
        _footnoteArea.setContainingBlock(new ViewportBox(new Rectangle(0, 0, containingBlockWidth, 0)));
        _footnoteArea.setStyle(style);

        // For now we make sure all footnote bodies have block display.
        _footnoteArea.setChildrenContentType(BlockBox.CONTENT_BLOCK);
        _footnoteArea.setElement(me);

        // Create an isolated layer, not connected to any other layers.
        // This means we have to explicitly paint it. See #paintFootnoteArea.
        Layer footnoteLayer = new Layer(_footnoteArea, c, true);
        _footnoteArea.setLayer(footnoteLayer);
        _footnoteArea.setContainingLayer(footnoteLayer);
    }

    /**
     * Set the containing layer of all descendants.
     * <br><br>
     * This means that the descendants can not have layers of their own,
     * meaning no absolute, fixed or transforms.
     */
    private void correctLayer(Box parent, Layer l) {
        LambdaUtil.descendants(parent)
          .forEach(box -> {
              box.setLayer(null);
              box.setContainingLayer(l);
          });
    }

    /**
     * Adds a footnote body to this page, creating the footnote area as required.
     * <br><br>
     * Important: This changes the page break point by expanding the footnote area.
     */
    public void addFootnoteBody(LayoutContext c, BlockBox footnoteBody) {
        // We need to know that we are in the footnote area during layout so we
        // know which page bottom to use.
        c.setIsInFloatBottom(true);

        if (_footnoteArea == null) {
            createFootnoteArea(c);
        }

        footnoteBody.setContainingBlock(_footnoteArea.getContainingBlock());
        _footnoteArea.addChild(footnoteBody);

        // FIXME: Not very efficient to layout all footnotes again after one
        // is added.
        _footnoteArea.layout(c);

        // FIXME: The layer for _footnoteArea may swap around in BlockBox::layout
        // so just fix up all descendants to point to the correct layer.
        correctLayer(_footnoteArea, _footnoteArea.getLayer());

        _totalFootnoteHeight = _footnoteArea.getHeight();

        c.setIsInFloatBottom(false);
    }

    /**
     * Removes footnotes from a page. This is used when a line is moved to a
     * new page. We remove from first page and add to the next.
     */
    public void removeFootnoteBodies(LayoutContext c, List<BlockBox> footnoteBodies) {
        if (_footnoteArea != null) {
            for (BlockBox footnote : footnoteBodies) {
                _footnoteArea.removeChild(footnote);
            }

            if (_footnoteArea.getChildCount() > 0) {
                c.setIsInFloatBottom(true);

                _footnoteArea.layout(c);

                correctLayer(_footnoteArea, _footnoteArea.getLayer());

                // TODO: Test this thoroughly, an unstable page break point
                // at this stage may prove problematic!
                // Confirmed - breaks layout.
                //_totalFootnoteHeight = _footnoteArea.getHeight();

                c.setIsInFloatBottom(false);
            } else {
                _footnoteArea = null;
                //_totalFootnoteHeight = 0;
            }
        }
    }

    /**
     * Checks if this page has footnotes. May change if we have to move footnotes
     * to subsequent page.
     */
    public boolean hasFootnotes() {
        return _footnoteArea != null;
    }
}
