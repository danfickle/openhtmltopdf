/*
 * Breaker.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm,
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
 *
 */
package com.openhtmltopdf.layout;

import java.util.function.ToIntFunction;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.layout.LineBreakContext.LineBreakResult;
import com.openhtmltopdf.render.FSFont;

/**
 * A utility class that scans the text of a single inline box, looking for the
 * next break point.
 * @author Torbjoern Gannholm
 */
public class Breaker {

    public static void breakFirstLetter(LayoutContext c, LineBreakContext context,
            int avail, CalculatedStyle style) {
        FSFont font = style.getFSFont(c);
        float letterSpacing = style.hasLetterSpacing() ?
                style.getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c) :
                0f;
        
        context.setEnd(getFirstLetterEnd(context.getMaster(), context.getStart()));
        context.setWidth(c.getTextRenderer().getWidth(
                c.getFontContext(), font, context.getCalculatedSubstring()) + (int) letterSpacing);

        if (context.getWidth() > avail) {
            context.setNeedsNewLine(true);
            context.setUnbreakable(true);
        }
    }

    private static int getFirstLetterEnd(String text, int start) {
        boolean letterFound = false;
        int end = text.length();
        int currentChar;
        for ( int i = start; i < end; ) {
            currentChar = text.codePointAt(i);
            if (!TextUtil.isFirstLetterSeparatorChar(currentChar)) {
                if (letterFound) {
                    return i;
                } else {
                    letterFound = true;
                }
            }
            i += Character.charCount(currentChar);
        }
        return end;
    }

    public enum BreakTextResult {
        /**
         * Has completely consumed the string.
         */
        FINISHED,

        /**
         * In char breaking mode, need a newline before continuing.
         * At least one character has been consumed.
         */
        CONTINUE_CHAR_BREAKING_ON_NL,

        /**
         * In word breaking mode, need a newline before continuing.
         * At least one word has been consumed.
         */
        CONTINUE_WORD_BREAKING_ON_NL,

        /**
         * Not a single char fitted, but we consumed one character anyway.
         */
        CHAR_UNBREAKABLE_BUT_CONSUMED,

        /**
         * Not a single word fitted, but we consumed a word anyway.
         * Only returned when word-wrap is not break-word.
         */
        WORD_UNBREAKABLE_BUT_CONSUMED,

        /**
         * DANGER: Last char did not fit and we are directing the
         * parent method to reconsume char on a newline.
         * Example, below floats.
         */
        DANGER_RECONSUME_CHAR_ON_NL,

        /**
         * DANGER: Last word did not fit and we are directing the
         * parent method to reconsume word on a newline.
         * Example, below floats.
         */
        DANGER_RECONSUME_WORD_ON_NL;
    }

    public static BreakTextResult breakText(
            LayoutContext c,
            LineBreakContext context,
            int avail,
            CalculatedStyle style,
            boolean tryToBreakAnywhere,
            int lineWidth,
            boolean forceOutput) {

        FSFont font = style.getFSFont(c);
        IdentValue whitespace = style.getWhitespace();
        float letterSpacing = style.hasLetterSpacing() ?
                style.getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c) : 0f;

        // ====== handle nowrap
        if (whitespace == IdentValue.NOWRAP) {
            int width = Breaker.getTextWidthWithLetterSpacing(c, font, context.getMaster(), letterSpacing);
            if (width <= avail || forceOutput) {
                c.setLineBreakedBecauseOfNoWrap(false);
                context.setEnd(context.getLast());
                context.setWidth(width);
                context.setNeedsNewLine(false);
                return BreakTextResult.FINISHED;
            } else if (!c.isLineBreakedBecauseOfNoWrap()) {
                c.setLineBreakedBecauseOfNoWrap(true);
                context.setEnd(context.getStart());
                context.setWidth(0);
                context.setNeedsNewLine(true);
                context.setUnbreakable(true);
                return BreakTextResult.DANGER_RECONSUME_WORD_ON_NL;
            } else {
                c.setLineBreakedBecauseOfNoWrap(false);
                context.setEnd(context.getLast());
                context.setWidth(width);
                context.setNeedsNewLine(false);
                return BreakTextResult.FINISHED;
            }
        }

        // Check if we should break on the next newline
        if (whitespace == IdentValue.PRE ||
            whitespace == IdentValue.PRE_WRAP ||
            whitespace == IdentValue.PRE_LINE) {
            int n = context.getStartSubstring().indexOf(WhitespaceStripper.EOL);
            if (n > -1) {
                context.setEnd(context.getStart() + n + 1);
                context.setWidth(Breaker.getTextWidthWithLetterSpacing(c, font, context.getCalculatedSubstring(), letterSpacing));
                context.setNeedsNewLine(true);
                context.setEndsOnNL(true);
            } else if (whitespace == IdentValue.PRE) {
                context.setEnd(context.getLast());
                context.setWidth(Breaker.getTextWidthWithLetterSpacing(c, font, context.getCalculatedSubstring(), letterSpacing));
                context.setNeedsNewLine(false);
            }
        }

        // Check if we may wrap
        if (whitespace == IdentValue.PRE ||
            (context.isNeedsNewLine() && context.getWidth() <= avail)) {
            return context.isNeedsNewLine() ?
                BreakTextResult.CONTINUE_WORD_BREAKING_ON_NL :
                BreakTextResult.FINISHED;
        }

        context.setEndsOnNL(false);

        if (style.getWordWrap() != IdentValue.BREAK_WORD) {
            // Ordinary old word wrap which will overflow too long unbreakable words.
            return toBreakTextResult(
                    doBreakText(c, context, avail, style, tryToBreakAnywhere));
        } else {
            int originalStart = context.getStart();
            int totalWidth = 0;

            // The idea is we only break a word if it will not fit on a line by itself.

            LineBreakResult result;
            BreakTextResult breakResult;

            LOOP:
            while (true) {
                int savedEnd = context.getEnd();
                result = doBreakText(c, context, avail, style, tryToBreakAnywhere);

                switch (result) {
                case WORD_BREAKING_FINISHED: /* Fallthru */
                case CHAR_BREAKING_FINISHED:
                    totalWidth += context.getWidth();
                    breakResult = BreakTextResult.FINISHED;
                    break LOOP;

                case CHAR_BREAKING_NEED_NEW_LINE:
                    totalWidth += context.getWidth();
                    breakResult = BreakTextResult.CONTINUE_CHAR_BREAKING_ON_NL;
                    break LOOP;

                case CHAR_BREAKING_UNBREAKABLE:
                    if ((totalWidth == 0 && avail == lineWidth) ||
                        forceOutput) {
                        // We are at the start of the line but could not fit a single character!
                        totalWidth += context.getWidth();
                        breakResult = BreakTextResult.CHAR_UNBREAKABLE_BUT_CONSUMED;
                        break LOOP;
                    } else {
                        // We may be at the end of the line, so pick up at next line.
                        // FIXME: This is very dangerous and has led to infinite
                        // loops. Needs review.
                        context.setEnd(savedEnd);
                        context.setNeedsNewLine(true);
                        breakResult = BreakTextResult.DANGER_RECONSUME_CHAR_ON_NL;
                        break LOOP;
                    }

                case CHAR_BREAKING_FOUND_WORD_BREAK:
                    // We found a word break so resume normal word wrapping.
                    tryToBreakAnywhere = false;
                    break;

                case WORD_BREAKING_NEED_NEW_LINE: {
                    if (context.getNextWidth() >= lineWidth) {
                        // If the next word is too great to fit on a line by itself, start wrapping
                        // here in character breaking mode.
                        tryToBreakAnywhere = true;
                        break;
                    } else {
                        // Else, finish so it can be put on a new line.
                        totalWidth += context.getWidth();
                        breakResult = BreakTextResult.CONTINUE_WORD_BREAKING_ON_NL;
                        break LOOP;
                    }
                }
                case WORD_BREAKING_UNBREAKABLE: {
                    if (context.getWidth() >= lineWidth ||
                        context.isFirstCharInLine() ||
                        forceOutput) {
                        // If the word is too long to fit on a line by itself or
                        // if we are at the start of a line,
                        // retry in character breaking mode.
                        tryToBreakAnywhere = true;
                        context.setEnd(savedEnd);
                        continue LOOP;
                    } else {
                        // Else, retry it on a new line.
                        // FIXME: This is very dangerous and has led to infinite
                        // loops. Needs review.
                        context.setEnd(savedEnd);
                        breakResult = BreakTextResult.DANGER_RECONSUME_WORD_ON_NL;
                        break LOOP;
                    }
                }
                }

                context.setStart(context.getEnd());
                avail -= context.getWidth();
                totalWidth += context.getWidth();
            }

            context.setStart(originalStart);
            context.setWidth(totalWidth);

            // We need to know this for the next line.
            context.setFinishedInCharBreakingMode(tryToBreakAnywhere);
            return breakResult;
        }
    }

    /**
     * Converts a LineBreakResult returned from doBreakText in
     * word-wrapping mode to a BreakTextResult.
     * 
     * Throws a runtime exception if unexpected result found.
     */
    private static BreakTextResult toBreakTextResult(LineBreakResult res) {
        switch (res) {
        case WORD_BREAKING_FINISHED:
            return BreakTextResult.FINISHED;
        case WORD_BREAKING_NEED_NEW_LINE:
            return BreakTextResult.CONTINUE_WORD_BREAKING_ON_NL;
        case WORD_BREAKING_UNBREAKABLE:
            return BreakTextResult.WORD_UNBREAKABLE_BUT_CONSUMED;

        case CHAR_BREAKING_FINISHED:         // Fall-thru
        case CHAR_BREAKING_FOUND_WORD_BREAK: // Fall-thru
        case CHAR_BREAKING_NEED_NEW_LINE:    // Fall-thru
        case CHAR_BREAKING_UNBREAKABLE:      // Fall-thru
        default:
            throw new RuntimeException("PROGRAMMER ERROR: Unexpected LineBreakResult from word wrap");
        }
    }

    private static LineBreakResult doBreakText(
            LayoutContext c,
            LineBreakContext context,
            int avail,
            CalculatedStyle style,
            boolean tryToBreakAnywhere) {

        if (!tryToBreakAnywhere) {
            return doBreakText(c, context, avail, style, STANDARD_LINE_BREAKER);
        } else {
            FSFont font = style.getFSFont(c);

            float letterSpacing = style.hasLetterSpacing()
                    ? style.getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c)
                    : 0f;

            ToIntFunction<String> measurer = (str) ->
                   c.getTextRenderer().getWidth(c.getFontContext(), font, str);

            String currentString = context.getStartSubstring();
            FSTextBreaker lineIterator = STANDARD_LINE_BREAKER.getBreaker(currentString, c.getSharedContext());
            FSTextBreaker charIterator = STANDARD_CHARACTER_BREAKER.getBreaker(currentString, c.getSharedContext());

            return doBreakCharacters(currentString, lineIterator, charIterator, context, avail, letterSpacing, measurer);
        }
    }

    /**
     * Breaks at most one word (until the next word break) going character by character to see
     * what will fit in.
     */
    static LineBreakResult doBreakCharacters(
            String currentString,
            FSTextBreaker lineIterator,
            FSTextBreaker charIterator,
            LineBreakContext context,
            int avail,
            float letterSpacing,
            ToIntFunction<String> measurer) {

        // The next word break opportunity. We don't want to go past this
        // because we want to resume (if possible) normal word breaking after this
        // too long word has been broken anywhere to fit.
        int nextWordBreak = lineIterator.next();
        if (nextWordBreak == 0) {
            nextWordBreak = lineIterator.next();
        }
        if (nextWordBreak < 0) {
            // No word breaking opportunity, use end of the string.
            nextWordBreak = currentString.length();
        }
        
        // Next character break opportunity. Working variable.
        int nextCharBreak = charIterator.next();
        if (nextCharBreak < 0) {
            nextCharBreak = nextWordBreak;
        }
        
        // Working vars for current graphics length which may go over the 
        // available length.
        int graphicsLength = 0;
        
        // Working var to denote the first position, after which we are looking
        // for break opportunities. This will change as we go past each break
        // opportunity that fits.
        int left = 0;
        
        // Maintain a record of the last good wrap index and the last good graphics length.
        int lastGoodWrap = 0;
        int lastGoodGraphicsLength = 0;
        
        // While we've found a another break opportunity and its in our range (ie. before
        // the next word break) and fits keep going.
        while (nextCharBreak >= 0 &&
               nextCharBreak <= nextWordBreak &&
               graphicsLength < avail) {
            String subString = currentString.substring(left, nextCharBreak);
            float extraSpacing = (nextCharBreak - left) * letterSpacing;
            
            int splitWidth = (int) (measurer.applyAsInt(subString) + extraSpacing);
            
            lastGoodWrap = left;
            left = nextCharBreak;
            
            lastGoodGraphicsLength = graphicsLength;
            
            graphicsLength += splitWidth;
            nextCharBreak = charIterator.next();
        }
        
        if (graphicsLength == avail &&
            graphicsLength > 0) {
            // Exact fit..
            boolean needNewLine = currentString.length() > left;

            context.setNeedsNewLine(needNewLine);
            context.setEnd(left + context.getStart());
            context.setWidth(graphicsLength);
            context.setUnbreakable(false);

            if (left >= currentString.length()) {
                return LineBreakResult.CHAR_BREAKING_FINISHED;
            } else if (left >= nextWordBreak) {
                return LineBreakResult.CHAR_BREAKING_FOUND_WORD_BREAK;
            } else {
                return LineBreakResult.CHAR_BREAKING_NEED_NEW_LINE;
            }
        }

        if (nextCharBreak < 0) {
            nextCharBreak = nextWordBreak;
        }
               
        if (graphicsLength < avail) {
            // Try for the last bit too!
            lastGoodWrap = nextCharBreak;
            lastGoodGraphicsLength = graphicsLength;
            
            nextCharBreak = nextWordBreak;
            
            float extraSpacing = (nextCharBreak - left) * letterSpacing;
            int splitWidth = (int) (measurer.applyAsInt(currentString.substring(left, nextCharBreak)) + extraSpacing);

            graphicsLength += splitWidth;
        }

        if (graphicsLength <= avail &&
            graphicsLength > 0) {
            // The entire word fit.
            context.setWidth(graphicsLength);
            context.setEnd(nextCharBreak + context.getStart());
            context.setEndsOnWordBreak(nextCharBreak == nextWordBreak);
            context.setUnbreakable(false);
            
            if (nextCharBreak >= currentString.length()) {
                return LineBreakResult.CHAR_BREAKING_FINISHED;
            } else if (nextCharBreak >= nextWordBreak) {
                return LineBreakResult.CHAR_BREAKING_FOUND_WORD_BREAK;
            } else {
                return LineBreakResult.CHAR_BREAKING_NEED_NEW_LINE;
            }
        }
        
        // We need a newline for this word.
        context.setNeedsNewLine(true);
        
        if (lastGoodWrap != 0) {
            // We found a wrap point in which to wrap this word.
            context.setWidth(lastGoodGraphicsLength);
            context.setEnd(lastGoodWrap + context.getStart());
            context.setEndsOnWordBreak(lastGoodWrap == nextWordBreak);
            context.setUnbreakable(false);

            if (lastGoodWrap >= currentString.length()) {
                context.setNeedsNewLine(false);
                return LineBreakResult.CHAR_BREAKING_FINISHED;
            } else if (lastGoodWrap >= nextWordBreak) {
                return LineBreakResult.CHAR_BREAKING_FOUND_WORD_BREAK;
            } else {
                return LineBreakResult.CHAR_BREAKING_NEED_NEW_LINE;
            }
        } else if (!currentString.isEmpty()) {
            // Not even one character fit!
            int end = 1;
            float extraSpacing = letterSpacing;
            int splitWidth = (int) (measurer.applyAsInt(currentString.substring(0, end)) + extraSpacing); 

            context.setUnbreakable(true);
            context.setEnd(end + context.getStart());
            context.setEndsOnWordBreak(end == nextWordBreak);
            context.setWidth(splitWidth);
            context.setNeedsNewLine(end < currentString.length());

            return LineBreakResult.CHAR_BREAKING_UNBREAKABLE;
        } else {
            // Empty string.
            context.setEnd(context.getStart());
            context.setWidth(0);
            context.setNeedsNewLine(false);
            context.setUnbreakable(false);

            return LineBreakResult.CHAR_BREAKING_FINISHED;
        }
    }
    
    public static final char SOFT_HYPHEN = '\u00ad';
    
    private static class AppBreakOpportunity {
        int left;
        int right;
        int graphicsLength;
        int withHyphenGraphicsLength;
        boolean isSoftHyphenBreak;
        
        void copyTo(AppBreakOpportunity other) {
            other.left = left;
            other.right = right;
            other.graphicsLength = graphicsLength;
            other.withHyphenGraphicsLength = withHyphenGraphicsLength;
            other.isSoftHyphenBreak = isSoftHyphenBreak;
        }
    }

    public static LineBreakResult doBreakText(
            LayoutContext c,
            LineBreakContext context,
            int avail,
            CalculatedStyle style,
            TextBreakerSupplier lineBreaker) {

        FSFont font = style.getFSFont(c);

        float letterSpacing = style.hasLetterSpacing()
                ? style.getFloatPropertyProportionalWidth(CSSName.LETTER_SPACING, 0, c)
                : 0f;

        ToIntFunction<String> measurer = (str) ->
               c.getTextRenderer().getWidth(c.getFontContext(), font, str);

        String currentString = context.getStartSubstring();
        FSTextBreaker lineIterator = lineBreaker.getBreaker(currentString, c.getSharedContext());

        return doBreakTextWords(currentString, context, avail, lineIterator, letterSpacing, measurer);
    }

    static LineBreakResult doBreakTextWords(
            String currentString,
            LineBreakContext context,
            int avail,
            FSTextBreaker iterator,
            float letterSpacing,
            ToIntFunction<String> measurer) {

        int lastWrap = 0;
        
        AppBreakOpportunity current = new AppBreakOpportunity();
        AppBreakOpportunity prev = new AppBreakOpportunity();
        
        current.right = iterator.next();
        if (current.right == 0) {
            current.right = iterator.next();
        }

        int nextUnfittableSplitWidth = 0;
        
        while (current.right > 0 && current.graphicsLength <= avail) {
            current.copyTo(prev);
            
            String subString = currentString.substring(current.left, current.right);
            float extraSpacing = (current.right - current.left) * letterSpacing;
            
            int normalSplitWidth = (int) (measurer.applyAsInt(subString) + extraSpacing);

            if (currentString.charAt(current.right - 1) == SOFT_HYPHEN) {
                current.isSoftHyphenBreak = true;
                int withTrailingHyphenSplitWidth = (int)
                     (measurer.applyAsInt(subString + '-') + 
                        extraSpacing + letterSpacing);
                current.withHyphenGraphicsLength = current.graphicsLength + withTrailingHyphenSplitWidth;
                
                if (current.withHyphenGraphicsLength >= avail &&
                    current.right != currentString.length()) {
                    current.graphicsLength = current.withHyphenGraphicsLength;
                    lastWrap = current.left;
                    current.left = current.right;
                    current.right = iterator.next();
                    break;
                }
            } else {
                current.isSoftHyphenBreak = false;
                current.withHyphenGraphicsLength += normalSplitWidth;
            }
            
            current.graphicsLength += normalSplitWidth;
            nextUnfittableSplitWidth = normalSplitWidth;
            lastWrap = current.left;
            current.left = current.right;
            current.right = iterator.next();
        }
        
        if (current.graphicsLength <= avail) {
            // Try for the last bit too!
            lastWrap = current.left;
            current.copyTo(prev);
            current.right = currentString.length();
            float extraSpacing = (current.right - current.left) * letterSpacing;
            int splitWidth = (int) (measurer.applyAsInt(
                    currentString.substring(current.left)) + extraSpacing);
            current.graphicsLength += splitWidth;
            nextUnfittableSplitWidth = splitWidth;
        }

        if (current.graphicsLength <= avail) {
            context.setWidth(current.graphicsLength);
            context.setEnd(context.getMaster().length());
            context.setNeedsNewLine(false);
            // It all fit!
            return LineBreakResult.WORD_BREAKING_FINISHED;
        }

        context.setNeedsNewLine(true);

        if (lastWrap != 0) {
            // Found a place to wrap
            if (prev.isSoftHyphenBreak) {
                context.setEndsOnSoftHyphen(true);
                context.setWidth(prev.withHyphenGraphicsLength);
            } else {
                context.setWidth(prev.graphicsLength);
            }
            
            context.setNextWidth(nextUnfittableSplitWidth);
            context.setEnd(context.getStart() + lastWrap);
            
            return LineBreakResult.WORD_BREAKING_NEED_NEW_LINE;
        } else {
            // Unbreakable string
            if (current.left == 0) {
                current.left = currentString.length();
            }

            context.setEnd(context.getStart() + current.left);
            context.setUnbreakable(true);

            if (current.isSoftHyphenBreak) {
                context.setWidth(current.withHyphenGraphicsLength);
            } else if (current.left == currentString.length()) {
                String text = context.getCalculatedSubstring();
                float extraSpacing = text.length() * letterSpacing;
                context.setWidth((int) (measurer.applyAsInt(text) + extraSpacing));
            } else {
                context.setWidth(current.graphicsLength);
            }
            return LineBreakResult.WORD_BREAKING_UNBREAKABLE;
        }
    }
    
    public interface TextBreakerSupplier {
    	public FSTextBreaker getBreaker(String str, SharedContext sharedContext);
    }
    
    private static class CharacterBreakerSupplier implements TextBreakerSupplier {
		@Override
		public FSTextBreaker getBreaker(String str, SharedContext sharedContext) {
			return getCharacterBreakStream(str, sharedContext);
		}
    }

    private static class LineBreakerSupplier implements TextBreakerSupplier {
		@Override
		public FSTextBreaker getBreaker(String str, SharedContext sharedContext) {
			return getLineBreakStream(str, sharedContext);
		}
    }
    
    public static final TextBreakerSupplier STANDARD_CHARACTER_BREAKER = new CharacterBreakerSupplier();
    public static final TextBreakerSupplier STANDARD_LINE_BREAKER = new LineBreakerSupplier();
    
	public static FSTextBreaker getCharacterBreakStream(String currentString, SharedContext sharedContext) {
		FSTextBreaker i = sharedContext.getCharacterBreaker();
		i.setText(currentString);
		return i;
	}

	public static FSTextBreaker getLineBreakStream(String s, SharedContext shared) {
		FSTextBreaker i = shared.getLineBreaker();
		i.setText(s);
		return i;
	}

	/**
	 * Gets the width of a string with letter spacing factored in.
	 * Favor this method over using the text renderer directly.
	 */
    public static int getTextWidthWithLetterSpacing(CssContext c, FSFont font, String text, float letterSpacing) {
        float extraSpace = text.length() * letterSpacing;
        return (int) (c.getTextRenderer().getWidth(c.getFontContext(), font, text) + extraSpace);
    }
}
