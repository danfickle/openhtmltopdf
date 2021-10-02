package com.openhtmltopdf.pdfboxout.fontstore;

import java.io.Closeable;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.util.OpenUtil;

public class FontUtil {
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
        OpenUtil.closeQuietly(obj);
    }
}
