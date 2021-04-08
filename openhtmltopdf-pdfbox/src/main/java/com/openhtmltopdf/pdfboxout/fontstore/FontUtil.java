package com.openhtmltopdf.pdfboxout.fontstore;

import java.io.Closeable;
import java.io.IOException;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;

public class FontUtil {
    public static final String QNameSeparator = "/";

    /**
     * Gets the qualified name for the given font coordinates.
     * 
     * @param fontFamily
     * @param fontWeight
     * @param fontStyle
     * @implNote The result is built according to this rule (ABNF):
     *           <pre>
     * fontQName = fontFamily {@link #QNameSeparator} fontStyle QNameSeparator fontWeight</pre>
     *           where the font coordinates are normalized.
     */
    public static String getFontQName(String fontFamily, Integer fontWeight, IdentValue fontStyle) {
        return normalizeFontFamily(fontFamily) + QNameSeparator
                + normalizeFontStyle(fontStyle) + QNameSeparator
                + normalizeFontWeight(fontWeight);
    }

    public static String normalizeFontFamily(String fontFamily) {
        String result = fontFamily;
        // strip off the "s if they are there
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }

        // normalize the font name
        if (result.equalsIgnoreCase("serif")) {
            result = "Serif";
        }
        else if (result.equalsIgnoreCase("sans-serif")) {
            result = "SansSerif";
        }
        else if (result.equalsIgnoreCase("monospace")) {
            result = "Monospaced";
        }

        return result;
    }

    public static int normalizeFontWeight(Integer fontWeight) {
        return fontWeight != null ? fontWeight : 400;
    }

    public static IdentValue normalizeFontStyle(IdentValue fontStyle) {
        return fontStyle != null ? fontStyle : IdentValue.NORMAL;
    }

    public static int normalizeFontWeight(IdentValue fontWeight) {
        return fontWeight != null ? FontResolverHelper.convertWeightToInt(fontWeight) : 400;
    }

    public static void tryClose(Closeable obj) {
        try {
            obj.close();
        } catch (IOException e) {
        }
    }
}
