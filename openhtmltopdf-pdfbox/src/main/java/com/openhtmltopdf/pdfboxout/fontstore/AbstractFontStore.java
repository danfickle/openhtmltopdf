package com.openhtmltopdf.pdfboxout.fontstore;

import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.FontFamily;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;

public abstract class AbstractFontStore {
    public abstract FontDescription resolveFont(
            SharedContext ctx,
            String fontFamily,
            float size,
            IdentValue weight,
            IdentValue style,
            IdentValue variant);

    public static class EmptyFontStore extends AbstractFontStore {
        @Override
        public FontDescription resolveFont(
                SharedContext ctx, String fontFamily, float size, IdentValue weight,
                IdentValue style, IdentValue variant) {
            return null;
        }
    }

    public static class BuiltinFontStore extends AbstractFontStore {
        final Map<String, FontFamily<PdfBoxFontResolver.FontDescription>> _fontFamilies;

        public BuiltinFontStore(PDDocument doc) {
            this._fontFamilies = createInitialFontMap();
        }

        static Map<String, FontFamily<PdfBoxFontResolver.FontDescription>> createInitialFontMap() {
            HashMap<String, FontFamily<PdfBoxFontResolver.FontDescription>> result = new HashMap<>();
            addCourier(result);
            addTimes(result);
            addHelvetica(result);
            addSymbol(result);
            addZapfDingbats(result);

            return result;
        }

        static void addCourier(HashMap<String, FontFamily<PdfBoxFontResolver.FontDescription>> result) {
            FontFamily<PdfBoxFontResolver.FontDescription> courier = new FontFamily<>("Courier");

            courier.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.COURIER_BOLD_OBLIQUE, IdentValue.OBLIQUE, 700));
            courier.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.COURIER_OBLIQUE, IdentValue.OBLIQUE, 400));
            courier.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.COURIER_BOLD, IdentValue.NORMAL, 700));
            courier.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.COURIER, IdentValue.NORMAL, 400));

            result.put("DialogInput", courier);
            result.put("Monospaced", courier);
            result.put("Courier", courier);
        }

        static void addTimes(HashMap<String, FontFamily<PdfBoxFontResolver.FontDescription>> result) {
            FontFamily<PdfBoxFontResolver.FontDescription> times = new FontFamily<>("Times");

            times.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.TIMES_BOLD_ITALIC, IdentValue.ITALIC, 700));
            times.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.TIMES_ITALIC, IdentValue.ITALIC, 400));
            times.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.TIMES_BOLD, IdentValue.NORMAL, 700));
            times.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.TIMES_ROMAN, IdentValue.NORMAL, 400));

            result.put("Serif", times);
            result.put("TimesRoman", times);
        }

        static void addHelvetica(HashMap<String, FontFamily<PdfBoxFontResolver.FontDescription>> result) {
            FontFamily<PdfBoxFontResolver.FontDescription> helvetica = new FontFamily<>("Helvetica");

            helvetica.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.HELVETICA_BOLD_OBLIQUE, IdentValue.OBLIQUE, 700));
            helvetica.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.HELVETICA_OBLIQUE, IdentValue.OBLIQUE, 400));
            helvetica.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.HELVETICA_BOLD, IdentValue.NORMAL, 700));
            helvetica.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.HELVETICA, IdentValue.NORMAL, 400));

            result.put("Dialog", helvetica);
            result.put("SansSerif", helvetica);
            result.put("Helvetica", helvetica);
        }

        static void addSymbol(Map<String, FontFamily<PdfBoxFontResolver.FontDescription>> result) {
            FontFamily<PdfBoxFontResolver.FontDescription> fontFamily = new FontFamily<>("Symbol");

            fontFamily.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.SYMBOL, IdentValue.NORMAL, 400));

            result.put("Symbol", fontFamily);
        }

        static void addZapfDingbats(Map<String, FontFamily<PdfBoxFontResolver.FontDescription>> result) {
            FontFamily<PdfBoxFontResolver.FontDescription> fontFamily = new FontFamily<>("ZapfDingbats");

            fontFamily.addFontDescription(new PdfBoxFontResolver.FontDescription(PDType1Font.ZAPF_DINGBATS, IdentValue.NORMAL, 400));

            result.put("ZapfDingbats", fontFamily);
        }

        @Override
        public PdfBoxFontResolver.FontDescription resolveFont(
                SharedContext ctx,
                String fontFamily,
                float size,
                IdentValue weight,
                IdentValue style,
                IdentValue variant) {

            String normalizedFontFamily = FontUtil.normalizeFontFamily(fontFamily);
            FontFamily<PdfBoxFontResolver.FontDescription> family = _fontFamilies.get(normalizedFontFamily);

            if (family != null) {
                return family.match(FontResolverHelper.convertWeightToInt(weight), style);
            }

            return null;
        }
    }
}
