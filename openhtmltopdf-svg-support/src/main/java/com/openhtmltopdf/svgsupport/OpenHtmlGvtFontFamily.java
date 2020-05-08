package com.openhtmltopdf.svgsupport;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.TextAttribute;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import org.apache.batik.gvt.font.AWTGVTFont;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTFontFace;
import org.apache.batik.gvt.font.GVTFontFamily;

import com.openhtmltopdf.util.XRLog;

public class OpenHtmlGvtFontFamily implements GVTFontFamily {

	private static class FontDescriptor {
		Float size;
		Float style;
		Float weight;
		
		private boolean eq(Object obj1, Object obj2)
		{
			if (obj1 == null && obj2 == null)
				return true;
			else if (obj1 == null)
				return false;
			else
				return obj1.equals(obj2);
			
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof FontDescriptor &&
				    eq(((FontDescriptor) obj).style, this.style) &&
				    eq(((FontDescriptor) obj).weight, this.weight) &&
				    eq(((FontDescriptor) obj).size, this.size));
		}
		
		@Override
		public int hashCode() {
			return (size == null ? 0 : size.hashCode()) + (style == null ? 0 : style.hashCode()) + (weight == null ? 0 : weight.hashCode());
			
		}

        @Override
        public String toString() {
            return "[size=" + size + ", style=" + style + ", weight=" + weight + "]";
        }
	}

    private final Map<FontDescriptor, AWTGVTFont> awtFonts = new HashMap<>();
    private final String fontFamily;
    private AWTGVTFont defaultFont;

	public OpenHtmlGvtFontFamily(String family) {
		this.fontFamily = family;
	}
	
	public void addFont(byte[] bytes, float size, Float fontWeight, Float fontStyle) throws FontFormatException {
		FontDescriptor des = new FontDescriptor();
		des.size = size;
		des.style = fontStyle;
		des.weight = fontWeight;
        try {
            Font fnt = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bytes));

            // FIXME: 10 is arbitrary, need to research correct value...
            AWTGVTFont gvtFont = new AWTGVTFont(fnt, 10);

            if (defaultFont == null) {
                defaultFont = gvtFont;
            }

            awtFonts.put(des, gvtFont);
        } catch (IOException e) {
            // Shouldn't happen, reading from byte array...
            XRLog.exception("IOException reading from byte array!", e);
        }
	}
	
	@Override
	public GVTFont deriveFont(float sz, AttributedCharacterIterator arg1) {
		return deriveFont(sz, arg1.getAttributes());
	}

	@Override
	public GVTFont deriveFont(float size, @SuppressWarnings("rawtypes") Map attrs) {
        Float fontWeight = (Float) attrs.get(TextAttribute.WEIGHT);
        Float fontStyle = (Float) attrs.get(TextAttribute.POSTURE);
	    Float sz = size;
	    
	    FontDescriptor des = new FontDescriptor();
	    des.weight = fontWeight;
	    des.style = fontStyle;
	    des.size = sz;

        return awtFonts.getOrDefault(des, defaultFont);
	}
	
	@Override
	public String getFamilyName() {
		return this.fontFamily;
	}

	@Override
	public GVTFontFace getFontFace() {
		return new GVTFontFace(this.fontFamily);
	}

	@Override
	public boolean isComplex() {
		return false;
	}

}
