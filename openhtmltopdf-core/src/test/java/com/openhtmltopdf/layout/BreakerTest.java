package com.openhtmltopdf.layout;

import java.util.function.ToIntFunction;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import com.openhtmltopdf.extend.FSTextBreaker;

public class BreakerTest {
    private static class SimpleCharBreaker implements FSTextBreaker {
        private String text;
        private int pos;
        
        @Override
        public int next() {
            return pos > text.length() ? -1 : pos++;
        }

        @Override
        public void setText(String newText) {
            this.text = newText;
            this.pos = 0;
        }
    }
    
    private static class SimpleLineBreaker implements FSTextBreaker {
        private String text;
        private int position;
        
        @Override
        public int next() {
            int ret = text.indexOf(' ', this.position);
            this.position = ret < 0 ? -1 : ret + 1;
            return ret;
        }

        @Override
        public void setText(String newText) {
            this.text = newText;
            this.position = 0;
        }
    }

    private FSTextBreaker createLine(String line) {
        SimpleLineBreaker breaker = new SimpleLineBreaker();
        breaker.setText(line);
        return breaker;
    }

    private FSTextBreaker createChar(String line) {
        FSTextBreaker breaker = new SimpleCharBreaker();
        breaker.setText(line);
        return breaker;
    }

    private final ToIntFunction<String> MEASURER = (str) -> str.length();
    private final ToIntFunction<String> MEASURER3 = (str) -> str.length() * 3;
    
    private LineBreakContext createContext(String str) {
        LineBreakContext ctx = new LineBreakContext();
        ctx.setMaster(str);
        return ctx;
    }
    
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
        assertThat(context.getWidth(), equalTo(4));
        assertThat(context.getEnd(), equalTo(4));
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
        
        assertTrue(context.isUnbreakable());
        assertTrue(context.isNeedsNewLine());
        assertThat(context.getWidth(), equalTo(18));
        assertThat(context.getEnd(), equalTo(6));
        assertThat(whole.substring(context.getEnd()), equalTo(""));
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
