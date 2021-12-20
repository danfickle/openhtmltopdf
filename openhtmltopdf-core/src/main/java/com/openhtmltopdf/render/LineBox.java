/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2006, 2007 Wisconsin Court System
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

import java.awt.Rectangle;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.layout.InlineBoxing;
import com.openhtmltopdf.layout.InlinePaintable;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.PaintingInfo;
import com.openhtmltopdf.util.XRRuntimeException;

/**
 * A line box contains a single line of text (or other inline content).  It
 * is created during layout.  It also tracks floated and absolute content
 * added while laying out the line.
 */
public class LineBox extends Box implements InlinePaintable {
    private static final float JUSTIFY_NON_SPACE_SHARE = 0.20f;
    private static final float JUSTIFY_SPACE_SHARE = 1 - JUSTIFY_NON_SPACE_SHARE;
    
    private boolean _containsContent;
    private boolean _containsBlockLevelContent;
    private boolean _isEndsOnNL;
    
    private FloatDistances _floatDistances;
    
    private List<TextDecoration> _textDecorations;
    
    private int _paintingTop;
    private int _paintingHeight;
    
    private List<Box> _nonFlowContent;
    
    private MarkerData _markerData;
    
    private boolean _containsDynamicFunction;
    
    private int _contentStart;
    
    private int _baseline;
    
    private JustificationInfo _justificationInfo;
    
    private byte direction = BidiSplitter.LTR;

    private List<BlockBox> referencedFootnoteBodies;

    public LineBox() {
    }
    
    @Override
    public String dump(LayoutContext c, String indent, int which) {
        if (which != Box.DUMP_RENDER) {
            throw new IllegalArgumentException();
        }

        StringBuilder result = new StringBuilder(indent);
        result.append(this);
        result.append('\n');
        
        dumpBoxes(c, indent, getNonFlowContent(), Box.DUMP_RENDER, result);
        if (getNonFlowContent().size() > 0  ) {
            result.append('\n');
        }
        dumpBoxes(c, indent, getChildren(), Box.DUMP_RENDER, result);
        
        return result.toString();
    }

    @Override
    public String toString() {
        return "LineBox: (" + getAbsX() + "," + getAbsY() + ")->(" + getWidth() + "," + getHeight() + ")";
    }

    @Override
    public Rectangle getMarginEdge(CssContext cssCtx, int tx, int ty) {
        Rectangle result = new Rectangle(getX(), getY(), getContentWidth(), getHeight());
        result.translate(tx, ty);
        return result;
    }
    
    @Override
    public void paintInline(RenderingContext c) {
        if (! getParent().getStyle().isVisible(c, this)) {
            return;
        }

        if (isContainsDynamicFunction()) {
            lookForDynamicFunctions(c, true);
            int totalLineWidth;

            if (direction == BidiSplitter.RTL) {
            	totalLineWidth = InlineBoxing.positionHorizontallyRTL(c, this, 0, 0);
            }
            else {
                totalLineWidth = InlineBoxing.positionHorizontally(c, this, 0);
            }
            setContentWidth(totalLineWidth);
            calcChildLocations();
            align(true, c);
            calcPaintingInfo(c, false);
        }
        
        if (_textDecorations != null) {
            Object token = c.getOutputDevice().startStructure(StructureType.BACKGROUND, this);
            c.getOutputDevice().drawTextDecoration(c, this);
            c.getOutputDevice().endStructure(token);
        }
        
        if (c.debugDrawLineBoxes()) {
            c.getOutputDevice().drawDebugOutline(c, this, FSRGBColor.GREEN);
        }
    }

    /**
     * See {@link InlineLayoutBox#lookForDynamicFunctions(RenderingContext, boolean)}
     */
    private void lookForDynamicFunctions(RenderingContext c, boolean evaluateLeaders) {
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                Box b = getChild(i);
                if (b instanceof InlineLayoutBox) {
                    ((InlineLayoutBox)b).lookForDynamicFunctions(c, evaluateLeaders);
                }
            }
        }
    }

    public boolean isFirstLine() {
        return super.isFirstChild();
    }
    
    public void prunePendingInlineBoxes(LayoutContext c) {
        if (getChildCount() > 0) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                Box b = getChild(i);
                if (! (b instanceof InlineLayoutBox)) {
                    break;
                }

                InlineLayoutBox iB = (InlineLayoutBox)b;
                iB.prunePending(c);

                if (iB.isPending()) {
                    if (iB.getElement() != null &&
                        iB.getElement().hasAttribute("id")) {
                        c.removeBoxId(iB.getElement().getAttribute("id"));
                    }

                    removeChild(i);
                }
            }
        }
    }

    /**
     * Whether this line contains any actual text content.
     */
    public boolean isContainsContent() {
        return _containsContent;
    }

    /**
     * See {@link #isContainsContent()}
     */
    public void setContainsContent(boolean containsContent) {
        _containsContent = containsContent;
    }

    public void align(boolean dynamic, CssContext c) {
        IdentValue align = getParent().getStyle().getIdent(CSSName.TEXT_ALIGN);

        int calcX = 0;
        byte dir = -1;

        if (align == IdentValue.START) {
            dir = direction == BidiSplitter.RTL ? BidiSplitter.RTL : BidiSplitter.LTR; 
        }

        if (align == IdentValue.JUSTIFY && direction == BidiSplitter.RTL) {
            int floatDistance = getFloatDistances().getRightFloatDistance();
            calcX = getParent().getContentWidth() - floatDistance - getContentWidth();

            if (align == IdentValue.JUSTIFY && dynamic) {
                justify(c);
            }
        } else if (align == IdentValue.LEFT || align == IdentValue.JUSTIFY || dir == BidiSplitter.LTR) {
            int floatDistance = getFloatDistances().getLeftFloatDistance();
            calcX = getContentStart() + floatDistance;
            if (align == IdentValue.JUSTIFY && dynamic) {
                justify(c);
            }
        } else if (align == IdentValue.CENTER) {
            int leftFloatDistance = getFloatDistances().getLeftFloatDistance();
            int rightFloatDistance = getFloatDistances().getRightFloatDistance();
            
            int midpoint = leftFloatDistance +
                (getParent().getContentWidth() - leftFloatDistance - rightFloatDistance) / 2;
            
            calcX = midpoint - (getContentWidth() + getContentStart()) / 2;
        } else if (align == IdentValue.RIGHT || dir == BidiSplitter.RTL) {
            int floatDistance = getFloatDistances().getRightFloatDistance();
            calcX = getParent().getContentWidth() - floatDistance - getContentWidth();
        }
        
        if (calcX != getX()) {
            setX(calcX);
            calcCanvasLocation();
            calcChildLocations();
        }
    }
    
    public void justify(CssContext c) {
        if (getParent().getStyle().hasLetterSpacing()) {
            // Do nothing, letter-spacing turns off text justification.
        } else if (!isLastLineWithContent() && !isEndsOnNL()) {
            int leftFloatDistance = getFloatDistances().getLeftFloatDistance();
            int rightFloatDistance = getFloatDistances().getRightFloatDistance();
            
            int available = getParent().getContentWidth() - 
                leftFloatDistance - rightFloatDistance - getContentStart(); 
            
            if (available > getContentWidth()) {
                float maxInterChar = getParent().getStyle().getFloatPropertyProportionalWidth(CSSName.FS_MAX_JUSTIFICATION_INTER_CHAR, getParent().getWidth(), c);
                float maxInterWord = getParent().getStyle().getFloatPropertyProportionalWidth(CSSName.FS_MAX_JUSTIFICATION_INTER_WORD, getParent().getWidth(), c);
                
                int toAdd = available - getContentWidth();

                CharCounts counts = countJustifiableChars();
                
                JustificationInfo info = new JustificationInfo();

                if (counts.getSpaceCount() > 0) {
                    if (counts.getNonSpaceCount() > 1) {
                        info.setNonSpaceAdjust(Math.min(toAdd * JUSTIFY_NON_SPACE_SHARE / (counts.getNonSpaceCount() - 1), maxInterChar));
                    } else {
                        info.setNonSpaceAdjust(0.0f);
                    }
                    
                    if (counts.getSpaceCount() > 0) {
                        info.setSpaceAdjust(Math.min(toAdd * JUSTIFY_SPACE_SHARE / counts.getSpaceCount(), maxInterWord));
                    } else {
                        info.setSpaceAdjust(0.0f);
                    }
                } else if (counts.getNonSpaceCount() > 1) {
                    info.setSpaceAdjust(0f);
                    info.setNonSpaceAdjust(Math.min((float) toAdd / (counts.getNonSpaceCount() - 1), maxInterChar)); 
                } else {
                    info.setSpaceAdjust(0f);
                    info.setNonSpaceAdjust(0f);
                }

                adjustChildren(info);
                setJustificationInfo(info);
            }
        }
    }

    private void adjustChildren(JustificationInfo info) {
        if (isLayedOutRTL()) {
            adjustChildrenRTL(info);
            return;
        }

        float adjust = 0.0f;
        for (Box b : getChildren()) {
            b.setX(b.getX() + Math.round(adjust));

            if (b instanceof InlineLayoutBox) {
                adjust += ((InlineLayoutBox)b).adjustHorizontalPosition(info, adjust);
            }
        }

        calcChildLocations();
    }

    private void adjustChildrenRTL(JustificationInfo info) {
        float adjust = 0.0f;
        for (Box b : getChildren()) {
            b.setX(b.getX() - Math.round(adjust));

            if (b instanceof InlineLayoutBox) {
                adjust += ((InlineLayoutBox)b).adjustHorizontalPositionRTL(info, adjust);
            }
        }

        setContentWidth(getContentWidth() + Math.round(adjust));
        calcChildLocations();
    }

    private boolean isLastLineWithContent() {
        LineBox current = (LineBox)getNextSibling();
        while (current != null) {
            if (current.isContainsContent()) {
                return false;
            } else {
                current = (LineBox)current.getNextSibling();
            }
        }
        
        return true;
    }
    
    private CharCounts countJustifiableChars() {
        CharCounts result = new CharCounts();
        
        for (Box b : getChildren()) {
            if (b instanceof InlineLayoutBox) {
                ((InlineLayoutBox)b).countJustifiableChars(result);
            }
        }
        
        return result;
    }
    
	public FloatDistances getFloatDistances() {
		return _floatDistances;
	}

	public void setFloatDistances(FloatDistances floatDistances) {
		_floatDistances = floatDistances;
	}

    public boolean isContainsBlockLevelContent() {
        return _containsBlockLevelContent;
    }

    public void setContainsBlockLevelContent(boolean containsBlockLevelContent) {
        _containsBlockLevelContent = containsBlockLevelContent;
    }

    @Override
    public Rectangle getPaintingClipEdge(CssContext cssCtx) {
        Box parent = getParent();
        Rectangle result = null;
        if (parent.getStyle().isIdent(
                CSSName.FS_TEXT_DECORATION_EXTENT, IdentValue.BLOCK) || 
                    getJustificationInfo() != null) {
            result = new Rectangle(
                    getAbsX(), getAbsY() + _paintingTop, 
                    parent.getAbsX() + parent.getTx() + parent.getContentWidth() - getAbsX(), 
                    _paintingHeight);
        } else {
            result = new Rectangle(
                    getAbsX(), getAbsY() + _paintingTop, getContentWidth(), _paintingHeight);
        }
        return result;
    }

    public List<TextDecoration> getTextDecorations() {
        return _textDecorations;
    }

    public void setTextDecorations(List<TextDecoration> textDecorations) {
        _textDecorations = textDecorations;
    }

    public int getPaintingHeight() {
        return _paintingHeight;
    }

    public void setPaintingHeight(int paintingHeight) {
        _paintingHeight = paintingHeight;
    }

    public int getPaintingTop() {
        return _paintingTop;
    }

    public void setPaintingTop(int paintingTop) {
        _paintingTop = paintingTop;
    }

    public int getMinPaintingTop() {
        int paintingAbsTop = getAbsY() + getPaintingTop();
        int lineAbsTop = getAbsY();

        return Math.min(lineAbsTop, paintingAbsTop);
    }

    public int getMaxPaintingBottom() {
        int paintingAbsBottom = getAbsY() + getPaintingTop() + getPaintingHeight();
        int lineAbsBottom = getAbsY() + getHeight();

        return Math.max(paintingAbsBottom, lineAbsBottom);
    }

    public void addAllChildren(List<? super Box> list, Layer layer) {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            if (getContainingLayer() == layer) {
                list.add(child);
                if (child instanceof InlineLayoutBox) {
                    ((InlineLayoutBox)child).addAllChildren(list, layer);
                }
            }
        }
    }
    
    public List<Box> getNonFlowContent() {
        return _nonFlowContent == null ? Collections.emptyList() : _nonFlowContent;
    }
    
    public void addNonFlowContent(BlockBox box) {
        if (_nonFlowContent == null) {
            _nonFlowContent = new ArrayList<>();
        }
        
        _nonFlowContent.add(box);
    }
    
    @Override
    public void reset(LayoutContext c) {
        if (hasFootnotes()) {
            // Reset usually happens when satisfying widows and orphans.
            // Reset means we and our descendants are about to be layed out again
            // so we have to remove footnotes as they will be added again, possible on
            // a new page.
            c.getFootnoteManager().removeFootnoteBodies(c, getReferencedFootnoteBodies(), this);
        }

        for (int i = 0; i < getNonFlowContent().size(); i++) {
            Box content = getNonFlowContent().get(i);
            content.reset(c);
        }

        if (_markerData != null) {
            _markerData.restorePreviousReferenceLine(this);
        }

        super.reset(c);
    }

    @Override
    public void calcCanvasLocation() {
        Box parent = getParent();
        if (parent == null) {
            throw new XRRuntimeException("calcCanvasLocation() called with no parent");
        }

        setAbsX(parent.getAbsX() + parent.getTx() + getX());
        setAbsY(parent.getAbsY() + parent.getTy() + getY());        
    }

    @Override
    public void calcChildLocations() {
        super.calcChildLocations();
        
        // Update absolute boxes too.  Not necessary most of the time, but
        // it doesn't hurt (revisit this)
        for (int i = 0; i < getNonFlowContent().size(); i++) {
            Box content = getNonFlowContent().get(i);
            if (content.getStyle().isAbsolute()) {
                content.calcCanvasLocation();
                content.calcChildLocations();
            }
        }
    }

    public MarkerData getMarkerData() {
        return _markerData;
    }

    public void setMarkerData(MarkerData markerData) {
        _markerData = markerData;
    }

    public boolean isContainsDynamicFunction() {
        return _containsDynamicFunction;
    }

    public void setContainsDynamicFunction(boolean containsPageCounter) {
        _containsDynamicFunction |= containsPageCounter;
    }

    public int getContentStart() {
        return _contentStart;
    }

    public void setContentStart(int contentOffset) {
        _contentStart = contentOffset;
    }
    
    public InlineText findTrailingText() {
        if (getChildCount() == 0) {
            return null;
        }
        
        for (int offset = getChildCount() - 1; offset >= 0; offset--) {
            Box child = getChild(offset);
            if (child instanceof InlineLayoutBox) {
                InlineText result = ((InlineLayoutBox)child).findTrailingText();
                if (result != null && result.isEmpty()) {
                    continue;
                }
                return result;
            } else {
                return null;
            }
        }
        
        return null;
    }
    
    public void trimTrailingSpace(LayoutContext c) {
        InlineText text = findTrailingText();
        
        if (text != null) {
            InlineLayoutBox iB = text.getParent();
            IdentValue whitespace = iB.getStyle().getWhitespace();
            if (whitespace == IdentValue.NORMAL || whitespace == IdentValue.NOWRAP) {
                text.trimTrailingSpace(c);
            }
        }
    }    
    
    @Override
    public Box find(CssContext cssCtx, int absX, int absY, boolean findAnonymous) {
        PaintingInfo pI = getPaintingInfo();
        if (pI !=null && ! pI.getAggregateBounds().contains(absX, absY)) {
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
        
        return null;
    }

    public int getBaseline() {
        return _baseline;
    }

    public void setBaseline(int baseline) {
        _baseline = baseline;
    }
    
    public boolean isContainsOnlyBlockLevelContent() {
        if (! isContainsBlockLevelContent()) {
            return false;
        }
        
        for (int i = 0; i < getChildCount(); i++) {
            Box b = getChild(i);
            if (! (b instanceof BlockBox)) {
                return false;
            }
        }
        
        return true;
    }

    public boolean isContainsVisibleContent() {
        for (int i = 0; i < getChildCount(); i++) {
            Box b = getChild(i);
            if (b instanceof BlockBox) {
                if (b.getWidth() > 0 || b.getHeight() > 0) {
                    return true;
                }
            } else {
                boolean maybeResult = ((InlineLayoutBox)b).isContainsVisibleContent();
                if (maybeResult) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public void collectText(RenderingContext c, StringBuilder buffer) {
        for (Box b : getNonFlowContent()) {
            b.collectText(c, buffer);
        }
        if (isContainsDynamicFunction()) {
            lookForDynamicFunctions(c, true);
        }
        super.collectText(c, buffer);
    }

    @Override
    public void exportText(RenderingContext c, Writer writer) throws IOException {
        int baselinePos = getAbsY() + getBaseline();
        if (baselinePos >= c.getPage().getBottom() && isInDocumentFlow()) {
            exportPageBoxText(c, writer, baselinePos);
        }
        
        for (Box b : getNonFlowContent()) {
            b.exportText(c, writer);
        }
        
        if (isContainsContent()) {
            StringBuilder result = new StringBuilder();
            collectText(c, result);
            writer.write(result.toString().trim());
            writer.write(LINE_SEPARATOR);
        }
    }
    
    @Override
    public void analyzePageBreaks(LayoutContext c, ContentLimitContainer container) {
        container.updateTop(c, getAbsY());
        container.updateBottom(c, getAbsY() + getHeight());
    }

    /**
     * Checks if this line box crosses a page break and if so moves it to
     * the next page.
     * Also takes care that in-flow lines do not overlap footnote content.
     */
    public void checkPagePosition(LayoutContext c, boolean alwaysBreak) {
        if (! c.isPageBreaksAllowed()) {
            return;
        }

        PageBox pageBox = c.getRootLayer().getFirstPage(c, this);

        if (pageBox != null) {
            // We need to force a page break if any of our content goes over a page break,
            // otherwise we will get repeated content in page margins (because content is
            // printed on both pages).

            // Painting top and bottom take account of line-height other than 1.
            int greatestAbsY = getMaxPaintingBottom();
            int leastAbsY = getMinPaintingTop();

            boolean overflowsPage;
            if (c.isInFloatBottom()) {
                // For now we don't support paginated tables in float:bottom content.
                overflowsPage = greatestAbsY >= pageBox.getBottom();
            } else {
                overflowsPage = greatestAbsY >= pageBox.getBottom(c) - c.getExtraSpaceBottom();
            }

            boolean tooBig = (greatestAbsY - leastAbsY) > pageBox.getContentHeight(c);
            boolean needsPageBreak = alwaysBreak || (overflowsPage && !tooBig); 

           if (needsPageBreak) {
               beforeChangePage(c);

               forcePageBreakBefore(c, IdentValue.ALWAYS, false, leastAbsY);
               calcCanvasLocation();

               checkFootnoteReservedPage(c, c.getRootLayer().getFirstPage(c, this), false);

               afterChangePage(c);
           } else if (pageBox.getTop() + c.getExtraSpaceTop() > getAbsY()) {
               // It is in the extra room at the top!
               int diff = pageBox.getTop() + c.getExtraSpaceTop() - getAbsY();
               setY(getY() + diff);
               calcCanvasLocation();

               checkFootnoteReservedPage(c, pageBox, true);
           } else {
               checkFootnoteReservedPage(c, pageBox, true);
           }
        }
    }

    private void afterChangePage(LayoutContext c) {
        if (hasFootnotes()) {
           List<BlockBox> footnotes = getReferencedFootnoteBodies();

           for (BlockBox footnote : footnotes) {
               c.getFootnoteManager().addFootnoteBody(c, footnote, this);
           }
        }
    }

    private void beforeChangePage(LayoutContext c) {
        if (hasFootnotes()) {
            // Oh oh, we need to move the footnotes to the next page with
            // this line.
            List<BlockBox> footnotes = getReferencedFootnoteBodies();
            c.getFootnoteManager().removeFootnoteBodies(c, footnotes, this);
        }
    }

    /**
     * Checks that the line box is not on a footnote reserved page and if so pushes
     * it down to the first non-reserved page. 
     */
    private void checkFootnoteReservedPage(
            LayoutContext c, PageBox pageBoxAfter, boolean runHooks) {

        if (c.hasActiveFootnotes() && !c.isInFloatBottom()) {
           while (pageBoxAfter != null &&
                  pageBoxAfter.isFootnoteReserved(c) ||
                  overlapsFootnote(pageBoxAfter)) {

               if (runHooks) {
                   beforeChangePage(c);
               }

               int delta = pageBoxAfter.getBottom() + c.getExtraSpaceTop() - getMinPaintingTop();
               setY(getY() + delta);
               calcCanvasLocation();

               pageBoxAfter = c.getRootLayer().getFirstPage(c, this);

               if (runHooks) {
                   afterChangePage(c);
               }
           }
        }
    }

    private boolean overlapsFootnote(PageBox pageBox) {
        return pageBox.getFootnoteAreaHeight() > 0 &&
           getMaxPaintingBottom() > pageBox.getBottom() - pageBox.getFootnoteAreaHeight();
    }

    public JustificationInfo getJustificationInfo() {
        return _justificationInfo;
    }

    private void setJustificationInfo(JustificationInfo justificationInfo) {
        _justificationInfo = justificationInfo;
    }
    
    public void setDirectionality(byte direction) {
    	this.direction = direction;
    }
    
    public boolean isLayedOutRTL() {
    	return this.direction == BidiSplitter.RTL;
    }

    @Override
    public boolean hasNonTextContent(CssContext c) {
        return _textDecorations != null && _textDecorations.size() > 0;
    }
    
    @Override
    public boolean isTerminalColumnBreak() {
        // A line box can not be further broken for the purpose of column breaks.
        return true;
    }

    public boolean isEndsOnNL() {
        return _isEndsOnNL;
    }

    public void setEndsOnNL(boolean endsOnNL) {
        _isEndsOnNL = endsOnNL;
    }

    /**
     * Gets the list of footnote bodies which have calls in this line
     * of text. Useful for moving those footnotes when this line is moved
     * to a new page.
     */
    public List<BlockBox> getReferencedFootnoteBodies() {
        return referencedFootnoteBodies;
    }

    /**
     * See {@link #getReferencedFootnoteBodies()}
     */
    public boolean hasFootnotes() {
        return referencedFootnoteBodies != null;
    }

    /**
     * See {@link #getReferencedFootnoteBodies()}
     */
    public void addReferencedFootnoteBody(BlockBox footnoteBody) {
        if (referencedFootnoteBodies == null) {
            referencedFootnoteBodies = new ArrayList<>(2);
        }
        referencedFootnoteBodies.add(footnoteBody);
    }

    /**
     * Narrows the return type of LineBox to a BlockBox.
     * Reduces the need to cast everywhere.
     */
    @Override
    public BlockBox getParent() {
        return (BlockBox) super.getParent();
    }
}
