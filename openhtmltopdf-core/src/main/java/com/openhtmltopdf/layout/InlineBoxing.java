/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.openhtmltopdf.util.LogMessageId;
import org.w3c.dom.Element;

import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.ParagraphSplitter.Paragraph;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.layout.Breaker.BreakTextResult;
import com.openhtmltopdf.render.AnonymousBlockBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.FloatDistances;
import com.openhtmltopdf.render.InlineBox;
import com.openhtmltopdf.render.InlineLayoutBox;
import com.openhtmltopdf.render.InlineText;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.MarkerData;
import com.openhtmltopdf.render.StrutMetrics;
import com.openhtmltopdf.render.TextDecoration;
import com.openhtmltopdf.util.XRLog;

/**
 * This class is responsible for flowing inline content into lines.  Block
 * content which participates in an inline formatting context is also handled
 * here as well as floating and absolutely positioned content.
 */
public class InlineBoxing {
    private InlineBoxing() {
    }
    
    static class SpaceVariables {
        final int maxAvailableWidth;
        int remainingWidth;
        int pendingLeftMBP;
        int pendingRightMBP;
        
        SpaceVariables(int maxWidth) {
            this.remainingWidth = maxWidth;
            this.maxAvailableWidth = maxWidth;
        }
    }
    
    static class StateVariables {
        LineBox line;
        InlineLayoutBox layoutBox;
    }

    public static void layoutContent(
            LayoutContext c, BlockBox box, int initialY, int breakAtLine) {

        Element blockElement = box.getElement();
        Paragraph para = c.getParagraphSplitter().lookupBlockElement(blockElement);
        byte blockLayoutDirection = para.getActualDirection();

        SpaceVariables space = new SpaceVariables(box.getContentWidth());
        StateVariables current = new StateVariables();
        StateVariables previous = new StateVariables();

        current.line = newLine(c, initialY, box);
        current.line.setDirectionality(blockLayoutDirection);

        int contentStart = 0;

        List<InlineBox> openInlineBoxes = null;

        Map<InlineBox, InlineLayoutBox> iBMap = new HashMap<>();

        if (box instanceof AnonymousBlockBox) {
            openInlineBoxes = ((AnonymousBlockBox)box).getOpenInlineBoxes();
            if (openInlineBoxes != null) {
                openInlineBoxes = new ArrayList<>(openInlineBoxes);
                current.layoutBox = addOpenInlineBoxes(
                        c, current.line, openInlineBoxes, space.maxAvailableWidth, iBMap);
            }
        }

        if (openInlineBoxes == null) {
            openInlineBoxes = new ArrayList<>();
        }

        space.remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, current.line, space.remainingWidth);

        CalculatedStyle parentStyle = box.getStyle();
        int minimumLineHeight = (int) parentStyle.getLineHeight(c);
        int indent = (int) parentStyle.getFloatPropertyProportionalWidth(CSSName.TEXT_INDENT, space.maxAvailableWidth, c);
        space.remainingWidth -= indent;
        contentStart += indent;

        MarkerData markerData = c.getCurrentMarkerData();
        if (markerData != null && box.getStyle().isListMarkerInside()) {
            space.remainingWidth -= markerData.getLayoutWidth();
            contentStart += markerData.getLayoutWidth();
        }
        c.setCurrentMarkerData(null);

        List<FloatLayoutResult> pendingFloats = new ArrayList<>();

        boolean hasFirstLinePEs = false;

        if (c.getFirstLinesTracker().hasStyles()) {
            box.styleText(c, c.getFirstLinesTracker().deriveAll(box.getStyle()));
            hasFirstLinePEs = true;
        }

        boolean needFirstLetter = c.getFirstLettersTracker().hasStyles();
        boolean zeroWidthInlineBlock = false;

        int lineOffset = 0;

        for (Styleable node : box.getInlineContent()) {

            if (node.getStyle().isInline()) {
                InlineBox inlineBox = (InlineBox)node;

                CalculatedStyle style = inlineBox.getStyle();

                if (inlineBox.hasFootnote() && c.isPrint()) {
                    c.getFootnoteManager().addFootnoteBody(c, inlineBox.getFootnoteBody(), current.line);

                    // We also need to associate it with a line box in case the line moves
                    // pages.
                    current.line.addReferencedFootnoteBody(inlineBox.getFootnoteBody());
                }

                if (inlineBox.isStartsHere()) {
                    startInlineBox(c, space, current, previous, openInlineBoxes, iBMap, inlineBox, style);
                }

                LineBreakContext lbContext = new LineBreakContext();

                if (inlineBox.isDynamicFunction()) {
                    lbContext.setMaster(
                         inlineBox.getContentFunction().getPostBoxingLayoutReplacementText(
                            c, current.layoutBox.getParent().getElement(), inlineBox.getFunction()));
                } else {
                    lbContext.setMaster(inlineBox.getText());
                }

                boolean inCharBreakingMode = false;

                do {
                    lbContext.reset();

                    lbContext.setFirstCharInLine(lbContext.getStart() == 0 && !current.line.isContainsContent());

                    int fit = 0;
                    if (lbContext.getStart() == 0) {
                        fit += space.pendingLeftMBP + space.pendingRightMBP;
                    }

                    boolean trimmedLeadingSpace = false;
                    if (hasTrimmableLeadingSpace(current.line, style, lbContext, zeroWidthInlineBlock)) {
                        trimmedLeadingSpace = true;
                        trimLeadingSpace(lbContext);
                    }

                    lbContext.setEndsOnNL(false);

                    zeroWidthInlineBlock = false;

                    if (lbContext.getStartSubstring().length() == 0) {
                        break;
                    }

                    if (needFirstLetter && !lbContext.isFinished()) {
                        startFirstLetterInlineLayoutBox(c, space, current, inlineBox, lbContext);
                        needFirstLetter = false;
                    } else {
                        if (style.getWordWrap() != IdentValue.BREAK_WORD) {
                            StartInlineTextResult result = startInlineText(c, lbContext, inlineBox, space, current, fit, trimmedLeadingSpace, false, lbContext.possibleEndlessLoop());
                            if (result == StartInlineTextResult.RECONSUME_BELOW_FLOATS) {
                                lbContext.newLine();
                                continue;
                            }
                        } else {
                            StartInlineTextResult result = startInlineText(c, lbContext, inlineBox, space, current, fit, trimmedLeadingSpace, inCharBreakingMode, lbContext.possibleEndlessLoop());
                            inCharBreakingMode = lbContext.isFinishedInCharBreakingMode();

                            if (result == StartInlineTextResult.RECONSUME_BELOW_FLOATS) {
                                lbContext.newLine();
                                continue;
                            }                        }
                    }

                    if (lbContext.isNeedsNewLine()) {
                        lbContext.newLine();

                        startNewInlineLine(c, box, breakAtLine, blockLayoutDirection, space, current, previous,
                                contentStart, openInlineBoxes, iBMap, minimumLineHeight, markerData, pendingFloats,
                                hasFirstLinePEs, lineOffset, inlineBox, lbContext);

                        lineOffset++;
                        markerData = null;
                        contentStart = 0;
                    }
                } while (!lbContext.isFinished());

                if (inlineBox.isEndsHere()) {
                    endInlineBox(c, space, current, previous, openInlineBoxes, inlineBox, style);
                }
            } else {
               BlockBox child = (BlockBox)node;

               if (child.getStyle().isNonFlowContent()) {
                   
                   space.remainingWidth -= processOutOfFlowContent(
                           c, current.line, child, space.remainingWidth, pendingFloats);
                   
               } else if (child.getStyle().isInlineBlock() || child.getStyle().isInlineTable()) {
                   startInlineBlock(c, box, initialY, breakAtLine, blockLayoutDirection, space, current, previous,
                        contentStart, openInlineBoxes, iBMap, minimumLineHeight, markerData, pendingFloats,
                        hasFirstLinePEs, lineOffset, child);

                   needFirstLetter = false;

                   if (child.getWidth() == 0) {
                       zeroWidthInlineBlock = true;
                   }
                   
                   lineOffset++;
                   markerData = null;
                   contentStart = 0;
               }
            }
        }

        current.line.trimTrailingSpace(c);
        saveLine(current.line, c, box, minimumLineHeight,
                space.maxAvailableWidth, pendingFloats, hasFirstLinePEs,
                markerData, contentStart,
                isAlwaysBreak(c, box, breakAtLine, lineOffset));
        if (current.line.isFirstLine() && current.line.getHeight() == 0 && markerData != null) {
            c.setCurrentMarkerData(markerData);
        }
        markerData = null;

        box.setContentWidth(space.maxAvailableWidth);
        box.setHeight(current.line.getY() + current.line.getHeight());
    }

    private static void startInlineBlock(LayoutContext c, BlockBox box, int initialY, int breakAtLine,
            byte blockLayoutDirection, SpaceVariables space, StateVariables current, StateVariables previous,
            int contentStart, List<InlineBox> openInlineBoxes, Map<InlineBox, InlineLayoutBox> iBMap,
            int minimumLineHeight, MarkerData markerData, List<FloatLayoutResult> pendingFloats,
            boolean hasFirstLinePEs, int lineOffset, BlockBox child) {

           layoutInlineBlockContent(c, box, child, initialY);

           if (child.getWidth() > space.remainingWidth && current.line.isContainsContent()) {
               saveLine(current.line, c, box, minimumLineHeight,
                       space.maxAvailableWidth, pendingFloats,  hasFirstLinePEs,
                       markerData, contentStart,
                       isAlwaysBreak(c, box, breakAtLine, lineOffset));

               previous.line = current.line;
               current.line = newLine(c, previous.line, box);
               current.line.setDirectionality(blockLayoutDirection);
               current.layoutBox = addOpenInlineBoxes(
                       c, current.line, openInlineBoxes, space.maxAvailableWidth, iBMap);
               previous.layoutBox = current.layoutBox == null || current.layoutBox.getParent() instanceof LineBox ?
                       null : (InlineLayoutBox) current.layoutBox.getParent();
               space.remainingWidth = space.maxAvailableWidth;
               space.remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, current.line, space.remainingWidth);

               child.reset(c);
               layoutInlineBlockContent(c, box, child, initialY);
           }

           if (current.layoutBox == null) {
               current.line.addChildForLayout(c, child);
           } else {
               current.layoutBox.addInlineChild(c, child);
           }

           current.line.setContainsContent(true);
           current.line.setContainsBlockLevelContent(true);

           space.remainingWidth -= child.getWidth();

           if (current.layoutBox != null && current.layoutBox.isStartsHere()) {
               space.pendingLeftMBP -= current.layoutBox.getStyle().getMarginBorderPadding(
                       c, space.maxAvailableWidth, CalculatedStyle.LEFT);
           }
    }

    private static void endInlineBox(
            LayoutContext c, SpaceVariables space, StateVariables current,
            StateVariables previous, List<InlineBox> openInlineBoxes,
            InlineBox inlineBox, CalculatedStyle style) {

        int rightMBP = style.getMarginBorderPadding(
                c, space.maxAvailableWidth, CalculatedStyle.RIGHT);

        space.pendingRightMBP -= rightMBP;
        space.remainingWidth -= rightMBP;

        openInlineBoxes.remove(openInlineBoxes.size() - 1);

        if (current.layoutBox.isPending()) {
            current.layoutBox.unmarkPending(c);

            // Reset to correct value
            current.layoutBox.setStartsHere(inlineBox.isStartsHere());
        }

        current.layoutBox.setEndsHere(true);

        previous.layoutBox = current.layoutBox;
        current.layoutBox = current.layoutBox.getParent() instanceof LineBox ?
                null : (InlineLayoutBox) current.layoutBox.getParent();
    }

    private static void startNewInlineLine(LayoutContext c, BlockBox box, int breakAtLine, byte blockLayoutDirection,
            SpaceVariables space, StateVariables current, StateVariables previous, int contentStart,
            List<InlineBox> openInlineBoxes, Map<InlineBox, InlineLayoutBox> iBMap, int minimumLineHeight,
            MarkerData markerData, List<FloatLayoutResult> pendingFloats, boolean hasFirstLinePEs,
            int lineOffset, InlineBox inlineBox, LineBreakContext lbContext) {

        IdentValue align = inlineBox.getStyle().getIdent(CSSName.TEXT_ALIGN);
        if (align != IdentValue.LEFT &&
            (align != IdentValue.START || inlineBox.getTextDirection() != BidiSplitter.LTR)) {
            current.line.trimTrailingSpace(c);
        }

        current.line.setEndsOnNL(lbContext.isEndsOnNL());

        saveLine(current.line, c, box, minimumLineHeight,
                space.maxAvailableWidth, pendingFloats,
                hasFirstLinePEs, markerData,
                contentStart, isAlwaysBreak(c, box, breakAtLine, lineOffset));

        if (current.line.isFirstLine() && hasFirstLinePEs) {
            lbContext.setMaster(TextUtil.transformText(inlineBox.getText(), inlineBox.getStyle()));
        }
        
        previous.line = current.line;
        current.line = newLine(c, previous.line, box);
        current.line.setDirectionality(blockLayoutDirection);
        
        current.layoutBox = addOpenInlineBoxes(
                c, current.line, openInlineBoxes, space.maxAvailableWidth, iBMap);
        
        previous.layoutBox = current.layoutBox.getParent() instanceof LineBox ?
                null : (InlineLayoutBox) current.layoutBox.getParent();
        
        space.remainingWidth = space.maxAvailableWidth;
        space.remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, current.line, space.remainingWidth);
    }

    private enum StartInlineTextResult {
        RECONSUME_BELOW_FLOATS,
        RECONSUME_UNBREAKABLE_ON_NEW_LINE,
        LINE_FINISHED
    }
    
    /**
     * Trys to consume the text in lbContext. If successful it creates an InlineText and adds it to the current inline
     * layout box.
     * Otherwise, if there are floats and the current line is otherwise empty, moves below float and trys again.
     * Otherwise, trys again on a new line.
     */
    private static StartInlineTextResult startInlineText(
            LayoutContext c,
            LineBreakContext lbContext,
            InlineBox inlineBox,
            SpaceVariables space,
            StateVariables current,
            int fit,
            boolean trimmedLeadingSpace,
            boolean tryToBreakAnywhere,
            boolean forceOutput) {

        lbContext.saveEnd();
        CalculatedStyle style = inlineBox.getStyle();
        
        // Layout the text into the remaining width on this line. Will only go to the end of the line (at most)
        // and will produce one InlineText object.
        InlineText inlineText = layoutText(
                c, style, space.remainingWidth - fit, lbContext, false, inlineBox.getTextDirection(), tryToBreakAnywhere, space.maxAvailableWidth - fit, forceOutput);
        
        if (style.hasLetterSpacing()) {
            inlineText.setLetterSpacing(style.getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c));
        }

        if (lbContext.isUnbreakable() && 
            !current.line.isContainsContent() &&
            !forceOutput) {

            int delta = c.getBlockFormattingContext().getNextLineBoxDelta(c, current.line, space.maxAvailableWidth);
            
            if (delta > 0) {
                // Move current line to below float(s) so that hopefully some content can fit in.
                current.line.setY(current.line.getY() + delta);
                current.line.calcCanvasLocation();
                
                space.remainingWidth = space.maxAvailableWidth;
                space.remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, current.line, space.maxAvailableWidth);
                
                // Go back to before the troublesome unbreakable content.
                lbContext.resetEnd();
                
                // Return appropriate ret val so that we continue with line breaking with the new remaining width.
                // The InlineText we produced above is not used.
                return StartInlineTextResult.RECONSUME_BELOW_FLOATS;
            }
        }

        if (!lbContext.isUnbreakable() ||
            (lbContext.isUnbreakable() && 
             !current.line.isContainsContent() &&
             lbContext.getEnd() > lbContext.getStart()) ||
            forceOutput) {

            if (forceOutput) {
                XRLog.log(Level.SEVERE, LogMessageId.LogMessageId1Param.GENERAL_FORCED_OUTPUT_TO_AVOID_INFINITE_LOOP, lbContext.getCalculatedSubstring());
            }

            // We can use the inline text by adding it to the current inline layout box.
            // We also mark the text as consumed by the line break context and reduce the width
            // we have remaining on this line.

            if (inlineBox.isDynamicFunction()) {
                if (!inlineBox.getContentFunction().isCalculableAtLayout()) {
                    inlineText.setFunctionData(new FunctionData(
                        inlineBox.getContentFunction(), inlineBox.getFunction()));
                }
            }

            inlineText.setTrimmedLeadingSpace(trimmedLeadingSpace);
            current.line.setContainsDynamicFunction(inlineText.isDynamicFunction());
            current.layoutBox.addInlineChild(c, inlineText);
            current.line.setContainsContent(true);
            lbContext.setStart(lbContext.getEnd());
            space.remainingWidth -= inlineText.getWidth();

            if (current.layoutBox.isStartsHere()) {
                int marginBorderPadding =
                        current.layoutBox.getStyle().getMarginBorderPadding(
                        c, space.maxAvailableWidth, CalculatedStyle.LEFT);
                space.pendingLeftMBP -= marginBorderPadding;
                space.remainingWidth -= marginBorderPadding;
            }

            // Line is finsished, consume content afterward on new line.
            return StartInlineTextResult.LINE_FINISHED;
        } else {
            // We could not fit this text on the current line and it was unbreakable.
            // So rewind to reconsume the troublesome text.
            // This will be done on a new line as lbContext.isNeedsNewLine is true (see context
            // of this method in layoutContent).
            // The inline text object is not consumed.
            lbContext.resetEnd();
            return StartInlineTextResult.RECONSUME_UNBREAKABLE_ON_NEW_LINE;
        }
    }
    
    private static void startFirstLetterInlineLayoutBox(LayoutContext c, SpaceVariables space, StateVariables current,
            InlineBox inlineBox, LineBreakContext lbContext) {

        InlineLayoutBox firstLetter =
            addFirstLetterBox(c, current.line, current.layoutBox, lbContext,
                    space.maxAvailableWidth, space.remainingWidth, inlineBox.getTextDirection());

        space.remainingWidth -= firstLetter.getInlineWidth();

        if (current.layoutBox.isStartsHere()) {
            space.pendingLeftMBP -= current.layoutBox.getStyle().getMarginBorderPadding(
                    c, space.maxAvailableWidth, CalculatedStyle.LEFT);
        }
    }

    private static void startInlineBox(LayoutContext c, SpaceVariables space, StateVariables current,
            StateVariables previous, List<InlineBox> openInlineBoxes, Map<InlineBox, InlineLayoutBox> iBMap,
            InlineBox inlineBox, CalculatedStyle style) {
        previous.layoutBox = current.layoutBox;
        current.layoutBox = new InlineLayoutBox(c, inlineBox.getElement(), style, space.maxAvailableWidth);

        openInlineBoxes.add(inlineBox);
        iBMap.put(inlineBox, current.layoutBox);

        if (previous.layoutBox == null) {
            current.line.addChildForLayout(c, current.layoutBox);
        } else {
            previous.layoutBox.addInlineChild(c, current.layoutBox);
        }

        addBoxId(c, current);

        //To break the line well, assume we don't just want to paint padding on next line
        space.pendingLeftMBP += style.getMarginBorderPadding(
                c, space.maxAvailableWidth, CalculatedStyle.LEFT);
        space.pendingRightMBP += style.getMarginBorderPadding(
                c, space.maxAvailableWidth, CalculatedStyle.RIGHT);
    }

    private static void addBoxId(LayoutContext c, StateVariables current) {
        if (current.layoutBox.getElement() != null) {
            String name = c.getNamespaceHandler().getAnchorName(current.layoutBox.getElement());
            if (name != null) {
                c.addBoxId(name, current.layoutBox);
            }
            String id = c.getNamespaceHandler().getID(current.layoutBox.getElement());
            if (id != null) {
                c.addBoxId(id, current.layoutBox);
            }
        }
    }

    private static boolean isAlwaysBreak(LayoutContext c, BlockBox parent, int breakAtLine, int lineOffset) {
        if (parent.isCurrentBreakAtLineContext(c)) {
            return lineOffset == breakAtLine;
        } else {
            return breakAtLine > 0 && lineOffset == breakAtLine;
        }
    }


    private static InlineLayoutBox addFirstLetterBox(LayoutContext c, LineBox current,
            InlineLayoutBox currentIB, LineBreakContext lbContext, int maxAvailableWidth,
            int remainingWidth, byte textDirection) {
        CalculatedStyle previous = currentIB.getStyle();

        currentIB.setStyle(c.getFirstLettersTracker().deriveAll(currentIB.getStyle()));

        InlineLayoutBox iB = new InlineLayoutBox(c, null, currentIB.getStyle(), maxAvailableWidth);
        iB.setStartsHere(true);
        iB.setEndsHere(true);

        currentIB.addInlineChild(c, iB);
        current.setContainsContent(true);

        InlineText text = layoutText(c, iB.getStyle(), remainingWidth, lbContext, true, textDirection, true, maxAvailableWidth, false);

        if (iB.getStyle().hasLetterSpacing()) {
            text.setLetterSpacing(iB.getStyle().getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c));
        }
        
        iB.addInlineChild(c, text);
        iB.setInlineWidth(text.getWidth());

        lbContext.setStart(lbContext.getEnd());

        c.setFirstLettersTracker(
            StyleTracker.withNoStyles());

        currentIB.setStyle(previous);

        return iB;
    }

    private static void layoutInlineBlockContent(
            LayoutContext c, BlockBox containingBlock, BlockBox inlineBlock, int initialY) {
        inlineBlock.setContainingBlock(containingBlock);
        inlineBlock.setContainingLayer(c.getLayer());
        inlineBlock.initStaticPos(c, containingBlock, initialY);
        inlineBlock.calcCanvasLocation();
        inlineBlock.layout(c);
    }
    
    /**
     * Attempts to layout inline boxes from right to left.
     * @param start should be the right edge of the line
     * @param current should be the line box.
     * @return width of line.
     */
    public static int positionHorizontallyRTL(CssContext c, Box current, int start, int width) {
    	int x = start;

    	InlineLayoutBox currentIB = null;
    	
    	if (current instanceof InlineLayoutBox) {
    		currentIB = (InlineLayoutBox) current;
    		x -= currentIB.getRightMarginPaddingBorder(c);
    	}
    	
    	for (int i = 0; i < current.getChildCount(); i++) {
    		Box b = current.getChild(i);
    		
    		if (b instanceof InlineLayoutBox) {
    			InlineLayoutBox iB = (InlineLayoutBox) b;
    			int w = positionHorizontallyILBRTL(c, iB, x, width);
    			positionHorizontallyILBRTL(c, iB, x, w);
    			x -= w;
    			iB.setX(x);
    		}
    		else {
    			x -= b.getWidth();
    			b.setX(x);
    		}
    	}

    	if (currentIB != null) {
    		x -= currentIB.getLeftMarginBorderPadding(c);
    		currentIB.setInlineWidth((start - x));
    	}
    	
    	return (start - x);
    }
    
    private static int positionHorizontallyILBRTL(CssContext c, InlineLayoutBox current, int start, int width) {
    	// WARNING: This function was created mostly by trial and error!

        int xAbs = start;
        int xRel = width;
        int w;
        
        w = current.getRightMarginPaddingBorder(c);
        xAbs -= w;
        xRel -= w;
        
        for (int i = 0; i < current.getInlineChildCount(); i++) {
        	
            Object child = current.getInlineChild(i);
            
            if (child instanceof InlineLayoutBox) {
            	InlineLayoutBox iB = (InlineLayoutBox) child;
                
            	// FIXME: Inefficient, but we need to call once to get the width and then the
            	// second time to do the actual layout.
            	w = positionHorizontallyILBRTL(c, iB, xAbs, width);
                w = positionHorizontallyILBRTL(c, iB, xAbs, w);
                iB.setX(xAbs - w);
                xAbs -= w;
                xRel -= w;
            } else if (child instanceof InlineText) {
            	InlineText iT = (InlineText) child;
            	xAbs -= iT.getWidth();
                xRel -= iT.getWidth();
                iT.setX(xRel);
            } else if (child instanceof Box) {
                Box b = (Box) child;
                xAbs -= b.getWidth();
                xRel -= b.getWidth();
                b.setX(xAbs);
            }
        }

        w = current.getLeftMarginBorderPadding(c);
        xAbs -= w;
        xRel -= w;
        
        current.setInlineWidth(start - xAbs);
        return start - xAbs;
    }
    

    public static int positionHorizontally(CssContext c, Box current, int start) {
        int x = start;

        InlineLayoutBox currentIB = null;

        if (current instanceof InlineLayoutBox) {
            currentIB = (InlineLayoutBox)current;
            x += currentIB.getLeftMarginBorderPadding(c);
        }

        for (int i = 0; i < current.getChildCount(); i++) {
            Box b = current.getChild(i);
            if (b instanceof InlineLayoutBox) {
                InlineLayoutBox iB = (InlineLayoutBox) current.getChild(i);
                iB.setX(x);
                x += positionHorizontally(c, iB, x);
            } else {
                b.setX(x);
                x += b.getWidth();
            }
        }

        if (currentIB != null) {
            x += currentIB.getRightMarginPaddingBorder(c);
            currentIB.setInlineWidth(x - start);
        }

        return x - start;
    }

    private static int positionHorizontally(CssContext c, InlineLayoutBox current, int start) {
        int x = start;

        x += current.getLeftMarginBorderPadding(c);

        for (int i = 0; i < current.getInlineChildCount(); i++) {
            Object child = current.getInlineChild(i);
            if (child instanceof InlineLayoutBox) {
                InlineLayoutBox iB = (InlineLayoutBox) child;
                iB.setX(x);
                x += positionHorizontally(c, iB, x);
            } else if (child instanceof InlineText) {
                InlineText iT = (InlineText) child;
                iT.setX(x - start);
                x += iT.getWidth();
            } else if (child instanceof Box) {
                Box b = (Box) child;
                b.setX(x);
                x += b.getWidth();
            }
        }

        x += current.getRightMarginPaddingBorder(c);

        current.setInlineWidth(x - start);

        return x - start;
    }

    public static StrutMetrics createDefaultStrutMetrics(LayoutContext c, Box container) {
        FSFontMetrics strutM = container.getStyle().getFSFontMetrics(c);
        InlineBoxMeasurements measurements = getInitialMeasurements(c, container, strutM);

        return new StrutMetrics(
                strutM.getAscent(), measurements.getBaseline(), strutM.getDescent());
    }

    private static void positionVertically(
            LayoutContext c, Box container, LineBox current, MarkerData markerData) {
        if (current.getChildCount() == 0 || ! current.isContainsVisibleContent()) {
            current.setHeight(0);
        } else {
            FSFontMetrics strutM = container.getStyle().getFSFontMetrics(c);
            VerticalAlignContext vaContext = new VerticalAlignContext();
            InlineBoxMeasurements measurements = getInitialMeasurements(c, container, strutM);
            vaContext.setInitialMeasurements(measurements);

            List<TextDecoration> lBDecorations = calculateTextDecorations(
                    container, measurements.getBaseline(), strutM);
            if (lBDecorations != null) {
                current.setTextDecorations(lBDecorations);
            }

            for (int i = 0; i < current.getChildCount(); i++) {
                Box child = current.getChild(i);
                positionInlineContentVertically(c, vaContext, child);
            }

            vaContext.alignChildren();

            current.setHeight(vaContext.getLineBoxHeight());

            int paintingTop = vaContext.getPaintingTop();
            int paintingBottom = vaContext.getPaintingBottom();

            if (vaContext.getInlineTop() < 0) {
                moveLineContents(current, -vaContext.getInlineTop());
                if (lBDecorations != null) {
                    for (TextDecoration lBDecoration : lBDecorations) {
                        lBDecoration.setOffset(lBDecoration.getOffset() - vaContext.getInlineTop());
                    }
                }
                paintingTop -= vaContext.getInlineTop();
                paintingBottom -= vaContext.getInlineTop();
            }

            if (markerData != null) {
                StrutMetrics strutMetrics = markerData.getStructMetrics();
                strutMetrics.setBaseline(measurements.getBaseline() - vaContext.getInlineTop());
                markerData.setReferenceLine(current);
                current.setMarkerData(markerData);
            }

            current.setBaseline(measurements.getBaseline() - vaContext.getInlineTop());

            current.setPaintingTop(paintingTop);
            current.setPaintingHeight(paintingBottom - paintingTop);
        }
    }

    private static void positionInlineVertically(LayoutContext c,
            VerticalAlignContext vaContext, InlineLayoutBox iB) {
        InlineBoxMeasurements iBMeasurements = calculateInlineMeasurements(c, iB, vaContext);
        vaContext.pushMeasurements(iBMeasurements);
        positionInlineChildrenVertically(c, iB, vaContext);
        vaContext.popMeasurements();
    }

    private static void positionInlineBlockVertically(
            LayoutContext c, VerticalAlignContext vaContext, BlockBox inlineBlock) {
        int baseline = inlineBlock.calcInlineBaseline(c);
        int ascent = baseline;
        int descent = inlineBlock.getHeight() - baseline;
        alignInlineContent(c, inlineBlock, ascent, descent, vaContext);

        vaContext.updateInlineTop(inlineBlock.getY());
        vaContext.updatePaintingTop(inlineBlock.getY());

        vaContext.updateInlineBottom(inlineBlock.getY() + inlineBlock.getHeight());
        vaContext.updatePaintingBottom(inlineBlock.getY() + inlineBlock.getHeight());
    }

    private static void moveLineContents(LineBox current, int ty) {
        for (int i = 0; i < current.getChildCount(); i++) {
            Box child = current.getChild(i);
            child.setY(child.getY() + ty);
            if (child instanceof InlineLayoutBox) {
                moveInlineContents((InlineLayoutBox) child, ty);
            }
        }
    }

    private static void moveInlineContents(InlineLayoutBox box, int ty) {
        for (int i = 0; i < box.getInlineChildCount(); i++) {
            Object obj = box.getInlineChild(i);
            if (obj instanceof Box) {
                ((Box) obj).setY(((Box) obj).getY() + ty);

                if (obj instanceof InlineLayoutBox) {
                    moveInlineContents((InlineLayoutBox) obj, ty);
                }
            }
        }
    }

    private static InlineBoxMeasurements calculateInlineMeasurements(LayoutContext c, InlineLayoutBox iB,
                                                                     VerticalAlignContext vaContext) {
        FSFontMetrics fm = iB.getStyle().getFSFontMetrics(c);

        CalculatedStyle style = iB.getStyle();
        float lineHeight = style.getLineHeight(c);

        int halfLeading = Math.round((lineHeight - iB.getStyle().getFont(c).size) / 2);
        if (halfLeading > 0) {
            halfLeading = Math.round((lineHeight -
                    (fm.getDescent() + fm.getAscent())) / 2);
        }

        iB.setBaseline(Math.round(fm.getAscent()));

        alignInlineContent(c, iB, fm.getAscent(), fm.getDescent(), vaContext);
        List<TextDecoration> decorations = calculateTextDecorations(iB, iB.getBaseline(), fm);
        if (decorations != null) {
            iB.setTextDecorations(decorations);
        }

        InlineBoxMeasurements result = new InlineBoxMeasurements();
        result.setBaseline(iB.getY() + iB.getBaseline());
        result.setInlineTop(iB.getY() - halfLeading);
        result.setInlineBottom(Math.round(result.getInlineTop() + lineHeight));
        result.setTextTop(iB.getY());
        result.setTextBottom((int) (result.getBaseline() + fm.getDescent()));

        RectPropertySet padding = iB.getPadding(c);
        BorderPropertySet border = iB.getBorder(c);

        result.setPaintingTop((int)Math.floor(iB.getY() - border.top() - padding.top()));
        result.setPaintingBottom((int)Math.ceil(iB.getY() +
                fm.getAscent() + fm.getDescent() +
                border.bottom() + padding.bottom()));

        return result;
    }

    public static List<TextDecoration> calculateTextDecorations(Box box, int baseline,
            FSFontMetrics fm) {
        List<TextDecoration> result = null;
        CalculatedStyle style = box.getStyle();

        List<IdentValue> idents = style.getTextDecorations();
        if (idents != null) {
            result = new ArrayList<>(idents.size());
            if (idents.contains(IdentValue.UNDERLINE)) {
                TextDecoration decoration = new TextDecoration(IdentValue.UNDERLINE);
                // JDK returns zero so create additional space equal to one
                // "underlineThickness"
                if (fm.getUnderlineOffset() == 0) {
                    decoration.setOffset(Math.round((baseline + fm.getUnderlineThickness())));
                } else {
                    decoration.setOffset(Math.round((baseline + fm.getUnderlineOffset())));
                }
                decoration.setThickness(Math.round(fm.getUnderlineThickness()));

                // JDK on Linux returns some goofy values for
                // LineMetrics.getUnderlineOffset(). Compensate by always
                // making sure underline fits inside the descender
                if (fm.getUnderlineOffset() == 0) {  // HACK, are we running under the JDK
                    int maxOffset =
                        baseline + (int)fm.getDescent() - decoration.getThickness();
                    if (decoration.getOffset() > maxOffset) {
                        decoration.setOffset(maxOffset);
                    }
                }
                result.add(decoration);
            }

            if (idents.contains(IdentValue.LINE_THROUGH)) {
                TextDecoration decoration = new TextDecoration(IdentValue.LINE_THROUGH);
                decoration.setOffset(Math.round(baseline + fm.getStrikethroughOffset()));
                decoration.setThickness(Math.round(fm.getStrikethroughThickness()));
                result.add(decoration);
            }

            if (idents.contains(IdentValue.OVERLINE)) {
                TextDecoration decoration = new TextDecoration(IdentValue.OVERLINE);
                decoration.setOffset(0);
                decoration.setThickness(Math.round(fm.getUnderlineThickness()));
                result.add(decoration);
            }
        }

        return result;
    }

    // XXX vertical-align: super/middle/sub could be improved (in particular,
    // super and sub should be sized by the measurements of our inline parent
    // not us)
    private static void alignInlineContent(LayoutContext c, Box box,
                                           float ascent, float descent, VerticalAlignContext vaContext) {
        InlineBoxMeasurements measurements = vaContext.getParentMeasurements();

        CalculatedStyle style = box.getStyle();

        if (style.isLength(CSSName.VERTICAL_ALIGN)) {
            box.setY((int) (measurements.getBaseline() - ascent -
                    style.getFloatPropertyProportionalTo(CSSName.VERTICAL_ALIGN, style.getLineHeight(c), c)));
        } else {
            IdentValue vAlign = style.getIdent(CSSName.VERTICAL_ALIGN);

            if (vAlign == IdentValue.BASELINE) {
                box.setY(Math.round(measurements.getBaseline() - ascent));
            } else if (vAlign == IdentValue.TEXT_TOP) {
                box.setY(measurements.getTextTop());
            } else if (vAlign == IdentValue.TEXT_BOTTOM) {
                box.setY(Math.round(measurements.getTextBottom() - descent - ascent));
            } else if (vAlign == IdentValue.MIDDLE) {
                // FIXME: findbugs, loss of precision, try / (float)2
                box.setY(Math.round((measurements.getBaseline() - measurements.getTextTop()) / 2
                        - (ascent + descent) / 2));
            } else if (vAlign == IdentValue.SUPER) {
                box.setY(Math.round(measurements.getBaseline() - (3*ascent/2)));
            } else if (vAlign == IdentValue.SUB) {
                box.setY(Math.round(measurements.getBaseline() - ascent / 2));
            } else {
                box.setY(Math.round(measurements.getBaseline() - ascent));
            }
        }
    }

    private static InlineBoxMeasurements getInitialMeasurements(
            LayoutContext c, Box container, FSFontMetrics strutM) {
        float lineHeight = container.getStyle().getLineHeight(c);

        int halfLeading = Math.round((lineHeight -
                container.getStyle().getFont(c).size) / 2);
        if (halfLeading > 0) {
            halfLeading = Math.round((lineHeight -
                    (strutM.getDescent() + strutM.getAscent())) / 2);
        }

        InlineBoxMeasurements measurements = new InlineBoxMeasurements();
        measurements.setBaseline((int) (halfLeading + strutM.getAscent()));
        measurements.setTextTop(halfLeading);
        measurements.setTextBottom((int) (measurements.getBaseline() + strutM.getDescent()));
        measurements.setInlineTop(halfLeading);
        measurements.setInlineBottom((int) (halfLeading + lineHeight));

        return measurements;
    }

    private static void positionInlineChildrenVertically(LayoutContext c, InlineLayoutBox current,
                                               VerticalAlignContext vaContext) {
        for (int i = 0; i < current.getInlineChildCount(); i++) {
            Object child = current.getInlineChild(i);
            if (child instanceof Box) {
                positionInlineContentVertically(c, vaContext, (Box)child);
            }
        }
    }

    private static void positionInlineContentVertically(LayoutContext c,
            VerticalAlignContext vaContext, Box child) {
        VerticalAlignContext vaTarget = vaContext;
        if (! child.getStyle().isLength(CSSName.VERTICAL_ALIGN)) {
            IdentValue vAlign = child.getStyle().getIdent(
                    CSSName.VERTICAL_ALIGN);
            if (vAlign == IdentValue.TOP || vAlign == IdentValue.BOTTOM) {
                vaTarget = vaContext.createChild(child);
            }
        }
        if (child instanceof InlineLayoutBox) {
            InlineLayoutBox iB = (InlineLayoutBox) child;
            positionInlineVertically(c, vaTarget, iB);
        } else { // any other Box class
            positionInlineBlockVertically(c, vaTarget, (BlockBox)child);
        }
    }

    private static void saveLine(
        LineBox current, LayoutContext c,
        BlockBox block, int minHeight,
        int maxAvailableWidth, List<FloatLayoutResult> pendingFloats,
        boolean hasFirstLinePCs, 
        MarkerData markerData, int contentStart, boolean alwaysBreak) {

        current.setContentStart(contentStart);
        current.prunePendingInlineBoxes(c);

        int totalLineWidth;
        
        if (current.isLayedOutRTL()) {
        	totalLineWidth = positionHorizontallyRTL(c, current, 0, 0);
        	positionHorizontallyRTL(c, current, totalLineWidth, totalLineWidth);
        }
        else {
            totalLineWidth = positionHorizontally(c, current, 0);
        }
        
        current.setContentWidth(totalLineWidth);

        positionVertically(c, block, current, markerData);

        // XXX Revisit this.  Do we need this when dealing with unbreakable
        // text?  Is a line required to always have a minimum height?
        if (current.getHeight() != 0 &&
                current.getHeight() < minHeight &&
                ! current.isContainsOnlyBlockLevelContent()) {
            current.setHeight(minHeight);
        }

        if (c.isPrint()) {
            current.checkPagePosition(c, alwaysBreak);
        }

        alignLine(c, current, maxAvailableWidth);

        current.calcChildLocations();

        block.addChildForLayout(c, current);

        if (hasFirstLinePCs && current.isFirstLine()) {
            c.setFirstLinesTracker(
                 StyleTracker.withNoStyles());
            block.styleText(c);
        }

        if (pendingFloats.size() > 0) {
            for (FloatLayoutResult layoutResult : pendingFloats) {
                LayoutUtil.layoutFloated(c, current, layoutResult.getBlock(), maxAvailableWidth, null);
                current.addNonFlowContent(layoutResult.getBlock());
            }
            pendingFloats.clear();
        }
    }

    private static void alignLine(final LayoutContext c, final LineBox current, final int maxAvailableWidth) {
        if (! current.isContainsDynamicFunction() && ! current.getParent().getStyle().isTextJustify()) {
            current.setFloatDistances(new FloatDistances() {
                @Override
                public int getLeftFloatDistance() {
                    return c.getBlockFormattingContext().getLeftFloatDistance(c, current, maxAvailableWidth);
                }

                @Override
                public int getRightFloatDistance() {
                    return c.getBlockFormattingContext().getRightFloatDistance(c, current, maxAvailableWidth);
                }
            });
        } else {
            FloatDistances distances = new FloatDistances();
            distances.setLeftFloatDistance(
                    c.getBlockFormattingContext().getLeftFloatDistance(
                            c, current, maxAvailableWidth));
            distances.setRightFloatDistance(
                    c.getBlockFormattingContext().getRightFloatDistance(
                            c, current, maxAvailableWidth));
            current.setFloatDistances(distances);
        }
        current.align(false, c);
        if (! current.isContainsDynamicFunction() && ! current.getParent().getStyle().isTextJustify()) {
            current.setFloatDistances(null);
        }
    }

    private static InlineText layoutText(
            LayoutContext c,
            CalculatedStyle style,
            int remainingWidth,
            LineBreakContext lbContext,
            boolean needFirstLetter,
            byte textDirection,
            boolean tryToBreakAnywhere,
            int lineWidth,
            boolean forceOutput) {

        InlineText result = new InlineText();
        String masterText = lbContext.getMaster();

        if (needFirstLetter) {
            masterText = TextUtil.transformFirstLetterText(masterText, style);
            lbContext.setMaster(masterText);
            Breaker.breakFirstLetter(c, lbContext, remainingWidth, style);
        } else {
            BreakTextResult breakResult = 
                    Breaker.breakText(c, lbContext, remainingWidth, style, tryToBreakAnywhere, lineWidth, forceOutput);
            lbContext.checkConsistency(breakResult);
        }

        result.setMasterText(masterText);
        result.setSubstring(lbContext.getStart(), lbContext.getEnd());
        result.setWidth(lbContext.getWidth());
        result.setTextDirection(textDirection);
        result.setEndsOnSoftHyphen(lbContext.isEndsOnSoftHyphen());

        return result;
    }

    private static int processOutOfFlowContent(
            LayoutContext c, LineBox current, BlockBox block,
            int available, List<FloatLayoutResult> pendingFloats) {
        int result = 0;
        CalculatedStyle style = block.getStyle();
        if (style.isAbsolute() || style.isFixed()) {
            LayoutUtil.layoutAbsolute(c, current, block);
            current.addNonFlowContent(block);
        } else if (style.isFloated()) {
            FloatLayoutResult layoutResult = LayoutUtil.layoutFloated(
                    c, current, block, available, pendingFloats);
            if (layoutResult.isPending()) {
                pendingFloats.add(layoutResult);
            } else {
                result = layoutResult.getBlock().getWidth();
                current.addNonFlowContent(layoutResult.getBlock());
            }
        } else if (style.isRunning()) {
            block.setStaticEquivalent(current);
            c.getRootLayer().addRunningBlock(block);
        }

        return result;
    }

    private static boolean hasTrimmableLeadingSpace(
            LineBox line, CalculatedStyle style, LineBreakContext lbContext,
            boolean zeroWidthInlineBlock) {
        if ((! line.isContainsContent() || zeroWidthInlineBlock) &&
                lbContext.getStartSubstring().startsWith(WhitespaceStripper.SPACE)) {
            IdentValue whitespace = style.getWhitespace();
            if (whitespace == IdentValue.NORMAL
                    || whitespace == IdentValue.NOWRAP
                    || whitespace == IdentValue.PRE_LINE
                    || (whitespace == IdentValue.PRE_WRAP
                        && lbContext.getStart() > 0
                        && (lbContext.getMaster().length() > lbContext.getStart() - 1)
                        && lbContext.getMaster().charAt(lbContext.getStart() - 1) != WhitespaceStripper.EOLC)) {
                return true;
            }
        }
        return false;
    }

    private static void trimLeadingSpace(LineBreakContext lbContext) {
        String s = lbContext.getStartSubstring();
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        int newStart = lbContext.getStart() + i;
        lbContext.setStart(newStart);
        lbContext.setEnd(Math.max(lbContext.getEnd(), newStart));
    }

    private static LineBox newLine(LayoutContext c, LineBox previousLine, Box box) {
        int y = 0;

        if (previousLine != null) {
            y = previousLine.getY() + previousLine.getHeight();
        }

        return newLine(c, y, box);
    }

    private static LineBox newLine(LayoutContext c, int y, Box box) {
        LineBox result = new LineBox();
        result.setStyle(box.getStyle().createAnonymousStyle(IdentValue.BLOCK));
        result.setParent(box);
        result.initContainingLayer(c);

        result.setY(y);

        result.calcCanvasLocation();

        return result;
    }

    /**
     * We have to convert this HTML (angle brackets replaced with square brackets):
     * <pre>[one][two]Two lines[/two][/one]</pre>
     * to (with parent child relationship specified by indentation):
     * <pre>
     *   [line-box] (LineBox)
     *     [one]    (InlineLayoutBox)
     *       [two]  (InlineLayoutBox)
     *         Two  (InlineText)
     *       [/two]
     *     [/one]
     *   [/line-box]
     *   [line-box]  (LineBox)
     *     [one]     (InlineLayoutBox)
     *       [two]   (InlineLayoutBox)
     *         lines (InlineText)
     *       [/two]
     *     [/one]
     *   [/line-box]
     * </pre>
     * In this case the openParents param would be a flat list of <code>[one][two]</code>
     * as InlineBox objects at the start of the second line.
     * 
     * @return the deepest box (so that the rest of the line's content
     * can be added to it) or null if openParents is empty.
     */
    private static InlineLayoutBox addOpenInlineBoxes(
            LayoutContext c, LineBox line, List<InlineBox> openParents, int cbWidth, Map<InlineBox, InlineLayoutBox> iBMap) {

        InlineLayoutBox currentIB = null;
        InlineLayoutBox previousIB = null;

        boolean first = true;
        for (InlineBox iB : openParents) {
            currentIB = new InlineLayoutBox(
                    c, iB.getElement(), iB.getStyle(), cbWidth);

            if (iB.getElement() != null) {
                String id = iB.getElement().getAttribute("id");

                // If the id hasn't been added to the global tracker (for link targeting)
                // we add it here. This can happen if a box was added and then removed
                // from the previous line via #prunePending because no content could fit
                // on the previous line.
                if (!id.isEmpty() &&
                    c.getSharedContext().getBoxById(id) == null) {
                    c.addBoxId(id, currentIB);
                }
            }

            // iBMap contains a map from the original InlineBox (which can 
            // contain multiple lines of text and so is divided into InlineLayoutBox objects
            // during layout) to the last created InlineLayoutBox for its content.
            InlineLayoutBox prev = iBMap.get(iB);
            if (prev != null) {
                currentIB.setPending(prev.isPending());
            }

            iBMap.put(iB, currentIB);

            if (first) {
                line.addChildForLayout(c, currentIB);
                first = false;
            } else {
                previousIB.addInlineChild(c, currentIB, false);
            }
            previousIB = currentIB;
        }

        return currentIB;
    }
}

