package com.openhtmltopdf.layout;

import static com.openhtmltopdf.layout.BreakerTestSupport.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import com.openhtmltopdf.layout.LineBreakContext.LineBreakResult;

import static com.openhtmltopdf.layout.BreakerTestSupport.ContextIs.*;

public class WordBreakerTest {
    @Test
    public void testSingleCharFits() {
        String whole = "A";
        int avail = 1;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(1));
    }

    @Test
    public void testSingleCharDoesNotFitWithWidthZero() {
        String whole = "A";
        int avail = 0;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_UNBREAKABLE));
        assertContextIs(context, UNBREAKABLE, FINISHED, NEEDS_NEW_LINE);

        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(1));
    }

    @Test
    public void testMultiWordDoesNotFitWithWidthZero() {
        String whole = "A b c";
        int avail = 0;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_UNBREAKABLE));
        assertContextIs(context, UNBREAKABLE, NEEDS_NEW_LINE);

        assertThat(context.getWidth(), equalTo(2));
        assertThat(context.getEnd(), equalTo(2));
    }

    @Test
    public void testSingleCharFitsWithLetterSpacing() {
        String whole = "A";
        int avail = 2;
        float letterSpacing = 1;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(2));
        assertThat(context.getEnd(), equalTo(1));
    }

    @Test
    public void testMultiCharFitsWithLetterSpacing() {
        String whole = "Abc";
        int avail = 6;
        float letterSpacing = 1;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(6));
        assertThat(context.getEnd(), equalTo(3));
    }

    @Test
    public void testMultiWordFitsWithLetterSpacing() {
        String whole = "Abc def";
        int avail = 14;
        float letterSpacing = 1;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(14));
        assertThat(context.getEnd(), equalTo(7));
    }

    @Test
    public void testTrailingSoftHyphenWithWidthDoesNotFit() {
        String whole = "\u00ad\u00ad";
        int avail = 1;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        // Returns soft hyphen with real hyphen which in unbreakable
        // in given width.
        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_UNBREAKABLE));
        assertContextIs(context, UNBREAKABLE, NEEDS_NEW_LINE);

        assertThat(context.getWidth(), equalTo(2));
        assertThat(context.getEnd(), equalTo(1));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad"));
        assertThat(context.getNextWidth(), equalTo(0));

        context.reset();
        context.setStart(context.getEnd());
        String current = context.getStartSubstring();

        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER);

        // A trailing soft hyphen does not trigger an inserted real hyphen.
        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(2));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad"));
        assertThat(context.getNextWidth(), equalTo(0));
    }

    @Test
    public void testSingleSoftHyphenWithWidthFits() {
        String whole = "\u00ad";
        int avail = 2;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(1));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad"));
    }

    @Test
    public void testTwoSoftHyphenWithWidthFits() {
        String whole = "\u00ad\u00ad";
        int avail = 3;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(2));
        assertThat(context.getEnd(), equalTo(2));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad\u00ad"));
    }

    @Test
    public void testMultilineSoftHyphensWithWidth() {
        String whole = "\u00ad\u00ad";
        int avail = 2;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        // Inserts a real hyphen at the end of the line after the soft hyphen.
        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(2));
        assertThat(context.getEnd(), equalTo(1));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad"));
        assertThat(context.getNextWidth(), equalTo(1));

        context.reset();
        context.setStart(context.getEnd());

        String current = context.getStartSubstring();
        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER);

        // A trailing hyphen at end of string does not trigger inserted real hyphen.
        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(2));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad"));
    }

    @Test
    public void testMultilineSoftHyphensWithWidthInMiddle() {
        String whole = "abc\u00ad\u00adghi";
        int avail = 5;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        // Result includes first soft hyphen plus inserted real hyphen.
        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(5));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(context.getCalculatedSubstring(), equalTo("abc\u00ad"));
        assertThat(context.getNextWidth(), equalTo(4));

        context.reset();
        context.setStart(context.getEnd());

        String current = context.getStartSubstring();
        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(8));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00adghi"));
    }

    @Test
    public void testMultilineSoftHyphensWithWidthLeading() {
        String whole = "\u00ad\u00adabc";
        int avail = 3;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(3));
        assertThat(context.getEnd(), equalTo(2));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad\u00ad"));
        assertThat(context.getNextWidth(), equalTo(3));

        context.reset();
        context.setStart(context.getEnd());

        String current = context.getStartSubstring();
        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(3));
        assertThat(context.getEnd(), equalTo(5));
        assertThat(context.getCalculatedSubstring(), equalTo("abc"));
    }

    @Test
    public void testMultilineSoftHyphensWithOutWidthInMiddle() {
        String whole = "abc\u00ad\u00adghi";
        int avail = 5;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(5));
        assertThat(context.getCalculatedSubstring(), equalTo("abc\u00ad\u00ad"));
        assertThat(context.getNextWidth(), equalTo(3));

        context.reset();
        context.setStart(context.getEnd());

        String current = context.getStartSubstring();
        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(3));
        assertThat(context.getEnd(), equalTo(8));
        assertThat(context.getCalculatedSubstring(), equalTo("ghi"));
    }

    @Test
    public void testMultilineSoftHyphensWithOutWidthLeading() {
        String whole = "\u00ad\u00adabc";
        int avail = 2;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(2));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad\u00ad"));
        assertThat(context.getNextWidth(), equalTo(3));

        context.reset();
        context.setStart(context.getEnd());

        String current = context.getStartSubstring();
        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_UNBREAKABLE));
        assertContextIs(context, FINISHED, UNBREAKABLE, NEEDS_NEW_LINE);

        assertThat(context.getWidth(), equalTo(3));
        assertThat(context.getEnd(), equalTo(5));
        assertThat(context.getCalculatedSubstring(), equalTo("abc"));
    }

    @Test
    public void testSingleSoftHyphenWithOutWidthFits() {
        String whole = "" + Breaker.SOFT_HYPHEN;
        int avail = 1;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(0));
        assertThat(context.getEnd(), equalTo(1));
        assertThat(context.getCalculatedSubstring(), equalTo("\u00ad"));
    }

    @Test
    public void testTrailingSoftHyphenWithOutWidthFits() {
        String whole = "abc\u00ad";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(3));
        assertThat(context.getEnd(), equalTo(4));
    }

    @Test
    public void testTwoTrailingSoftHyphensWithOutWidthFits() {
        String whole = "abc\u00ad\u00ad";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(5));
    }

    @Test
    public void testThreeTrailingSoftHyphensWithOutWidthFits() {
        String whole = "abc\u00ad\u00ad\u00ad";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(6));
    }

    @Test
    public void testMiddleSoftHyphenWithOutWidthFits() {
        String whole = "abc" + Breaker.SOFT_HYPHEN + "def";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(context.getNextWidth(), equalTo(3));
    }

    @Test
    public void testTwoMiddleSoftHyphensWithOutWidthFits() {
        String whole = "abc" + Breaker.SOFT_HYPHEN +
                  Breaker.SOFT_HYPHEN + "def";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(context.getNextWidth(), equalTo(3));
    }

    @Test
    public void testThreeMiddleSoftHyphensWithOutWidthFits() {
        String whole = "abc" + Breaker.SOFT_HYPHEN +
                  Breaker.SOFT_HYPHEN + Breaker.SOFT_HYPHEN + "def";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing,
                MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE, ENDS_ON_SOFT_HYPHEN);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(context.getNextWidth(), equalTo(3));
        assertThat(context.getCalculatedSubstring(), equalTo("abc\u00ad"));
    }

    @Test
    public void testMultipleSpaces() {
        String whole = "  ";
        int avail = 2;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(2));
        assertThat(context.getEnd(), equalTo(2));
        assertThat(context.getCalculatedSubstring(), equalTo("  "));
    }

    @Test
    public void testMultilinMultiSpacesInMiddle() {
        String whole = "abc  ghi";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);

        LineBreakResult res = Breaker.doBreakTextWords(whole, context, avail, createLine(whole), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_NEED_NEW_LINE));
        assertContextIs(context, NEEDS_NEW_LINE);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(context.getCalculatedSubstring(), equalTo("abc "));
        assertThat(context.getNextWidth(), equalTo(1));

        context.reset();
        context.setStart(context.getEnd());

        String current = context.getStartSubstring();
        res = Breaker.doBreakTextWords(current, context, avail, createLine(current), letterSpacing, MEASURER);

        assertThat(res, equalTo(LineBreakResult.WORD_BREAKING_FINISHED));
        assertContextIs(context, FINISHED);

        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(8));
        assertThat(context.getCalculatedSubstring(), equalTo(" ghi"));
    }
}
