package com.openhtmltopdf.layout;

import java.awt.Rectangle;
import java.text.BreakIterator;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;

import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.FSGlyphVector;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.layout.Breaker.TextBreakerSupplier;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.JustificationInfo;

public class BreakerTest {
	// Mocks for Breaker:::doBreakText
	
	private static class CharacterBreaker implements FSTextBreaker {
		private String str;
		private int position = 0;
		
		@Override
		public int next() {
			return position < str.length() ? position++ : BreakIterator.DONE;
		}

		@Override
		public void setText(String newText) {
			this.str = newText;
		}
	}
	
	private static class LineBreaker implements FSTextBreaker {
		private String str;
		private int position = 0;
		
		@Override
		public int next() {
			String working = str.substring(position);
			int nextSpaceInWorking = working.indexOf(' ');
			
			if (nextSpaceInWorking >= 0) {
				position = position + nextSpaceInWorking;
				return position++;
			} else {
				position = Math.max(str.length() - 1, 0);
				return BreakIterator.DONE;
			}
		}

		@Override
		public void setText(String newText) {
			this.str = newText;
		}
	}
	
	private static class CharacterBreakerSupplier implements TextBreakerSupplier {
		@Override
		public FSTextBreaker getBreaker(String str, SharedContext sharedContext) {
			FSTextBreaker breaker = new CharacterBreaker();
			breaker.setText(str);
			return breaker;
		}
	}
	
	private static class LineBreakerSupplier implements TextBreakerSupplier {
		@Override
		public FSTextBreaker getBreaker(String str, SharedContext sharedContext) {
			FSTextBreaker breaker = new LineBreaker();
			breaker.setText(str);
			return breaker;
		}
	}
	
	private static class TextRendererMock implements TextRenderer {

		@Override
		public void setup(FontContext context) {
		}

		@Override
		public void drawString(OutputDevice outputDevice, String string,
				float x, float y) {
		}

		@Override
		public void drawString(OutputDevice outputDevice, String string,
				float x, float y, JustificationInfo info) {
		}

		@Override
		public void drawGlyphVector(OutputDevice outputDevice,
				FSGlyphVector vector, float x, float y) {
		}

		@Override
		public FSGlyphVector getGlyphVector(OutputDevice outputDevice,
				FSFont font, String string) {
			return null;
		}

		@Override
		public float[] getGlyphPositions(OutputDevice outputDevice,
				FSFont font, FSGlyphVector fsGlyphVector) {
			return null;
		}

		@Override
		public Rectangle getGlyphBounds(OutputDevice outputDevice, FSFont font,
				FSGlyphVector fsGlyphVector, int index, float x, float y) {
			return null;
		}

		@Override
		public FSFontMetrics getFSFontMetrics(FontContext context, FSFont font,
				String string) {
			return null;
		}

		@Override
		public int getWidth(FontContext context, FSFont font, String string) {
			return string.length();
		}

		@Override
		public void setFontScale(float scale) {
		}

		@Override
		public float getFontScale() {
			return 0;
		}

		@Override
		public void setSmoothingThreshold(float fontsize) {
		}

		@Override
		public int getSmoothingLevel() {
			return 0;
		}

		@Override
		public void setSmoothingLevel(int level) {
		}
	}

	private static class LayoutContextMock extends LayoutContext {
		LayoutContextMock() {
			super(null);
		}
		
		@Override
		public TextRenderer getTextRenderer() {
			return new TextRendererMock();
		}
	}
	
	private static class FontMock implements FSFont {
		@Override
		public float getSize2D() {
			return 1;
		}
	}
	
	private static class CalculatedStyleMock extends CalculatedStyle {
		@Override
		public FSFont getFSFont(CssContext cssContext) {
			return new FontMock();
		}
	}

	private final LayoutContext c = new LayoutContextMock();
	private final TextBreakerSupplier characterBreaker = new CharacterBreakerSupplier();
	private final TextBreakerSupplier lineBreaker = new LineBreakerSupplier();
	private final CalculatedStyle style = new CalculatedStyleMock();

	private LineBreakContext createContext(String str) {
		LineBreakContext ctx = new LineBreakContext();
		ctx.setMaster(str);
		return ctx;
	}
	
	@Test
	public void testEmptyString() {
		int avail = 0;
		boolean tryToBreakAnywhere = false;
		LineBreakContext context = createContext("");
		
		Breaker.doBreakText(c, context, avail, style, characterBreaker, lineBreaker, tryToBreakAnywhere);
		Assert.assertThat(context.getStart(), equalTo(0));
		Assert.assertThat(context.getEnd(), equalTo(0));
		Assert.assertThat(context.getCalculatedSubstring(), equalTo(""));
	}
	
	@Test
	public void testSecondLine() {
		int avail = 4;
		boolean tryToBreakAnywhere = false;
		LineBreakContext context = createContext("lmn opq"); 
		context.setStart(4);
		
		Breaker.doBreakText(c, context, avail, style, characterBreaker, lineBreaker, tryToBreakAnywhere);
		Assert.assertThat(context.getStart(), equalTo(4));
		Assert.assertThat(context.getEnd(), equalTo(7));
		Assert.assertThat(context.getCalculatedSubstring(), equalTo("opq")); 
	}
	
	// Currently the break loop condition is as follows:
	//
	// while (right > 0 && graphicsLength <= avail) 
	//
	// where right is ultimately derived from an instance of BreakIterator.
	// I think this is meant to be 'right >= 0' as BreakIterator.DONE is equal to -1
	// and 0 is a valid break offset (the first character).
	// I think it is only a problem with breaking characters other than space as space characters
	// are stripped from the beginning elsewhere. 
	//
	// Unfortunately, I'm too scared to change this at the moment but document it via a test instead.
	@Test
	public void testBrokenBehaviorOnFirstCharacterBeingBreaking() {
		int avail = 3;
		boolean tryToBreakAnywhere = false;
		LineBreakContext context = createContext(" opq");
		
		Breaker.doBreakText(c, context, avail, style, characterBreaker, lineBreaker, tryToBreakAnywhere);
		Assert.assertThat(context.isUnbreakable(), equalTo(true));
		Assert.assertThat(context.getCalculatedSubstring(), equalTo(" opq"));
	}



}
