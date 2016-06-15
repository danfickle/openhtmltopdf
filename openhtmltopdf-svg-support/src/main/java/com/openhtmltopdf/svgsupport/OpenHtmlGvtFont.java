package com.openhtmltopdf.svgsupport;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.CharacterIterator;

import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.gvt.font.GVTGlyphVector;
import org.apache.batik.gvt.font.GVTLineMetrics;

/**
 * An adapter around awt.Font to GVTFont.
 * Code from: http://svn.apache.org/viewvc/xmlgraphics/fop/trunk/fop-core/src/main/java/org/apache/fop/svg/font/FOPGVTFont.java
 *
 */
public class OpenHtmlGvtFont implements GVTFont {

	private final Font baseFont;
	private final GVTFontFamily fontFamily;
	private final float size;
	
    private static int toFontWeight(Float weight) {
        if (weight == null) {
            return Font.PLAIN;
        }
        else if (weight <= TextAttribute.WEIGHT_BOLD) {
           	return Font.PLAIN;
        }
        else {
        	return Font.BOLD;
        }
    }

    private static int toStyle(Float posture) {
    	return ((posture != null) && (posture.floatValue() > 0.0))
    			? Font.ITALIC
                : Font.PLAIN;
    }
	
	
	public OpenHtmlGvtFont(byte[] fontBytes, GVTFontFamily family, float size, Float fontWeight, Float fontStyle) throws FontFormatException {
		Font font;
		
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes)).deriveFont(toFontWeight(fontWeight) | toStyle(fontStyle) , size);
		} catch (IOException e) {
			// Shouldn't happen
			e.printStackTrace();
			font = null;
		}

		this.baseFont = font;
		this.fontFamily = family;
		this.size = size;
	}
	
	private OpenHtmlGvtFont(Font font, GVTFontFamily family, float size) {
		this.baseFont = font;
		this.fontFamily = family;
		this.size = size;
	}
	
	@Override
	public boolean canDisplay(char c) {
		return this.baseFont.canDisplay(c);
	}

	@Override
	public int canDisplayUpTo(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (!this.baseFont.canDisplay(str.charAt(i)))
				return i;
		}
	
		return -1;
	}

	@Override
	public int canDisplayUpTo(char[] str, int start, int limit) {
		for (int i = start; i < limit; i++) {
			if (!this.baseFont.canDisplay(str[i]))
				return i;
		}
	
		return -1;
	}

	@Override
	public int canDisplayUpTo(CharacterIterator iter, int start, int limit) {
        for (char c = iter.setIndex(start); iter.getIndex() < limit; c = iter.next()) {
	            if (!canDisplay(c)) {
	                return iter.getIndex();
	            }
        }
        
        return -1;
	}

	@Override
	public GVTGlyphVector createGlyphVector(FontRenderContext frc, char[] arg1) {
		return createGlyphVector(frc, new String(arg1));
	}

	@Override
	public GVTGlyphVector createGlyphVector(FontRenderContext frc,
			CharacterIterator arg1) {
		return new OpenHtmlGvtGlyphVector(this.baseFont.createGlyphVector(frc, arg1), this, frc);
	}

	@Override
	public GVTGlyphVector createGlyphVector(FontRenderContext frc, String arg1) {
		return new OpenHtmlGvtGlyphVector(this.baseFont.createGlyphVector(frc, arg1), this, frc);
	}

	@Override
	public GVTGlyphVector createGlyphVector(FontRenderContext frc, int[] arg1,
			CharacterIterator arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GVTFont deriveFont(float arg0) {
		Font newFont = this.baseFont.deriveFont(arg0);
		return new OpenHtmlGvtFont(newFont, this.fontFamily, arg0);
	}

	@Override
	public String getFamilyName() {
		return this.fontFamily.getFamilyName();
	}

	@Override
	public float getHKern(int arg0, int arg1) {
		return 0;
	}

	@Override
	public GVTLineMetrics getLineMetrics(String arg0, FontRenderContext arg1) {
		return new GVTLineMetrics(this.baseFont.getLineMetrics(arg0, arg1));
	}

	@Override
	public GVTLineMetrics getLineMetrics(char[] arg0, int arg1, int arg2,
			FontRenderContext arg3) {
		return new GVTLineMetrics(this.baseFont.getLineMetrics(arg0, arg1, arg2, arg3));
	}

	@Override
	public GVTLineMetrics getLineMetrics(CharacterIterator arg0, int arg1,
			int arg2, FontRenderContext arg3) {
		return new GVTLineMetrics(this.baseFont.getLineMetrics(arg0, arg1, arg2, arg3));
	}

	@Override
	public GVTLineMetrics getLineMetrics(String arg0, int arg1, int arg2,
			FontRenderContext arg3) {
		return new GVTLineMetrics(this.baseFont.getLineMetrics(arg0, arg1, arg2, arg3));
	}

	@Override
	public float getSize() {
		return this.baseFont.getSize() / 1000f;
	}

	@Override
	public float getVKern(int arg0, int arg1) {
		return 0;
	}
}
