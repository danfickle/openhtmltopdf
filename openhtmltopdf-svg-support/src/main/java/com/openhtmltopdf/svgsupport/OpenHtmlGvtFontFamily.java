package com.openhtmltopdf.svgsupport;

import java.awt.FontFormatException;
import java.awt.font.TextAttribute;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

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
	}
	
	private final Map<FontDescriptor, OpenHtmlGvtFont> fonts = new HashMap<FontDescriptor, OpenHtmlGvtFont>(1);
	private final String fontFamily;
	
	public OpenHtmlGvtFontFamily(String family) {
		this.fontFamily = family;
	}
	
	public void addFont(byte[] bytes, float size, Float fontWeight, Float fontStyle) throws FontFormatException {
		FontDescriptor des = new FontDescriptor();
		des.size = size;
		des.style = fontStyle;
		des.weight = fontWeight;
		
		fonts.put(des, new OpenHtmlGvtFont(bytes, this, size, fontWeight, fontStyle));
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
	    
	    if (fonts.containsKey(des)) {
	    	return fonts.get(des);
	    }

	    return fonts.values().iterator().next().deriveFont(sz);
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
