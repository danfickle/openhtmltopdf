package com.openhtmltopdf.layout;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;

import static com.openhtmltopdf.layout.BreakerTestSupport.*;

public class BreakerTest {
    @Test
    public void testCharacterBreakerSingleChar() {
        String whole = "A";
        int avail = 5;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER);
        
        assertFalse(context.isUnbreakable());
        assertFalse(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(1));
    }

    @Test
    public void testCharacterBreakerEmptyString() {
        String whole = "";
        int avail = 5;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER);
        
        assertFalse(context.isUnbreakable());
        assertFalse(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(0));
        assertThat(context.getEnd(), equalTo(0));
    }
    
    @Test
    public void testCharacterBreakerUntilWord() {
        String whole = "ABCD WORD";
        int avail = 15;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER);
        
        assertFalse(context.isUnbreakable());
        assertFalse(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(5));
        assertThat(context.getEnd(), equalTo(5));
    }

    @Test
    public void testCharacterBreakerNoFit() {
        String whole = "ABCDEF";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER);
        
        assertFalse(context.isUnbreakable());
        assertTrue(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(whole.substring(context.getEnd()), equalTo("EF"));
    }

    @Test
    public void testCharacterBreakerExactFit() {
        String whole = "ABCD";
        int avail = 4;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER);
        
        assertFalse(context.isUnbreakable());
        assertFalse(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(whole.substring(context.getEnd()), equalTo(""));
    }
    
    @Test
    public void testCharacterBreakerPartialFit() {
        String whole = "ABCDEF";
        int avail = 13;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER3);
        
        assertFalse(context.isUnbreakable());
        assertTrue(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(12));
        assertThat(context.getEnd(), equalTo(4));
        assertThat(whole.substring(context.getEnd()), equalTo("EF"));
    }
    
    @Test
    public void testCharacterBreakerNoFit2() {
        String whole = "ABCDEF";
        int avail = 2;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER3);
        
        // Breaks off minimum of one character.
        assertTrue(context.isUnbreakable());
        assertTrue(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(3));
        assertThat(context.getEnd(), equalTo(1));
        assertThat(whole.substring(context.getEnd()), equalTo("BCDEF"));
    }
    
    @Test
    public void testCharacterBreakerWordBreakAtStart() {
        String whole = "  ABCDEF";
        int avail = 20;
        float letterSpacing = 0;
        LineBreakContext context = createContext(whole);
        
        Breaker.doBreakCharacters(whole, createLine(whole), createChar(whole), context, avail, letterSpacing, MEASURER);
        
        assertFalse(context.isUnbreakable());
        assertFalse(context.isNeedsNewLine());
        
        // Should always consume one space character.
        assertThat(context.getWidth(), equalTo(1));
        assertThat(context.getEnd(), equalTo(1));
        assertThat(whole.substring(context.getEnd()), equalTo(" ABCDEF"));
    }
}
