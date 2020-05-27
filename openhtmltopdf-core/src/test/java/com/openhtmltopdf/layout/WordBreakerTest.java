package com.openhtmltopdf.layout;

import static com.openhtmltopdf.layout.BreakerTestSupport.*;
import static com.openhtmltopdf.layout.BreakerTestSupport.createContext;
import static com.openhtmltopdf.layout.BreakerTestSupport.createLine;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.openhtmltopdf.layout.LineBreakContext.LineBreakResult;

import static com.openhtmltopdf.layout.BreakerTestSupport.ContextIs.*;
import static com.openhtmltopdf.layout.BreakerTestSupport.assertContextIs;

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
    }
}
