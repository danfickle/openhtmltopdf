package com.openhtmltopdf.svgsupport;

import java.awt.FontFormatException;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTFontFace;
import org.apache.batik.gvt.font.GVTFontFamily;

public class OpenHtmlGvtFontFamily implements GVTFontFamily {

    private static class FontDescriptor {
        Float style;
        Float weight;

        @Override
        public boolean equals(Object obj) {
            return (obj != null && obj.getClass() == this.getClass() &&
                    Objects.equals(((FontDescriptor) obj).style, this.style) &&
                    Objects.equals(((FontDescriptor) obj).weight, this.weight));
        }

        @Override
        public int hashCode() {
            return Objects.hash(style, weight);
        }
    }

    private static class FontEntry {
        GVTFont font;
        float size;
    }

	private final Map<FontDescriptor, FontEntry> fonts = new HashMap<>(1);
	private final String fontFamily;
	
	public OpenHtmlGvtFontFamily(String family) {
		this.fontFamily = family;
	}

    public void addFont(File file, float size, Float fontWeight, Float fontStyle) throws IOException, FontFormatException {
        FontDescriptor des = new FontDescriptor();
        des.style = fontStyle;
        des.weight = fontWeight;

        FontEntry entry = new FontEntry();
        entry.size = size;
        entry.font = new OpenHtmlGvtFont(file, this, size, fontWeight, fontStyle);

        fonts.put(des, entry);
    }

    public void addFont(byte[] bytes, float size, Float fontWeight, Float fontStyle) throws FontFormatException {
        FontDescriptor des = new FontDescriptor();
        des.style = fontStyle;
        des.weight = fontWeight;

        FontEntry entry = new FontEntry();
        entry.size = size;
        entry.font = new OpenHtmlGvtFont(bytes, this, size, fontWeight, fontStyle);

        fonts.put(des, entry);
    }

	@Override
	public GVTFont deriveFont(float sz, AttributedCharacterIterator arg1) {
		return deriveFont(sz, arg1.getAttributes());
	}

    @Override
    public GVTFont deriveFont(float size, @SuppressWarnings("rawtypes") Map attrs) {
        Float fontWeight = (Float) attrs.get(TextAttribute.WEIGHT);
        Float fontStyle = (Float) attrs.get(TextAttribute.POSTURE);

        FontDescriptor des = new FontDescriptor();
        des.weight = fontWeight;
        des.style = fontStyle;

        FontEntry fnt = fonts.get(des);

        if (fnt != null) {
            if (fnt.size == size) {
                // We have the exact size.
                return fnt.font;
            } else {
                // Derive a font with the correct size.
                return fnt.font.deriveFont(size);
            }
        } else {
            // We don't have any matches for this style, weight so
            // just use the first font in the family.
            return fonts.values().iterator().next().font.deriveFont(size);
        }
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
