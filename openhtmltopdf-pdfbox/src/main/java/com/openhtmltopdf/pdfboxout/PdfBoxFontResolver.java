/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FontResolver;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.util.XRLog;

import java.io.*;
import java.util.*;

public class PdfBoxFontResolver implements FontResolver {
    private Map<String, FontFamily> _fontFamilies = createInitialFontMap();
    private Map<String, FontDescription> _fontCache = new HashMap<String, FontDescription>();
    private final PDDocument _doc;
    private final SharedContext _sharedContext;

    public PdfBoxFontResolver(SharedContext sharedContext, PDDocument doc) {
        _sharedContext = sharedContext;
        _doc = doc;
    }

    public FSFont resolveFont(SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }

    public void flushCache() {
        _fontFamilies = createInitialFontMap();
        _fontCache = new HashMap();
    }

    public void flushFontFaceFonts() {
        _fontCache = new HashMap();

        for (Iterator i = _fontFamilies.values().iterator(); i.hasNext(); ) {
            FontFamily family = (FontFamily)i.next();
            for (Iterator j = family.getFontDescriptions().iterator(); j.hasNext(); ) {
                FontDescription d = (FontDescription)j.next();
                if (d.isFromFontFace()) {
                    j.remove();
                }
            }
            if (family.getFontDescriptions().size() == 0) {
                i.remove();
            }
        }
    }

    public void importFontFaces(List<FontFaceRule> fontFaces) {
        for (Iterator<FontFaceRule> i = fontFaces.iterator(); i.hasNext(); ) {
            FontFaceRule rule = i.next();
            CalculatedStyle style = rule.getCalculatedStyle();

            FSDerivedValue src = style.valueByName(CSSName.SRC);
            if (src == IdentValue.NONE) {
                continue;
            }

            byte[] font1 = _sharedContext.getUserAgentCallback().getBinaryResource(src.asString());
            if (font1 == null) {
                XRLog.exception("Could not load font " + src.asString());
                continue;
            }

            boolean noSubset = style.isIdent(CSSName.FS_FONT_SUBSET, IdentValue.COMPLETE_FONT);
            boolean embedded = style.isIdent(CSSName.FS_PDF_FONT_EMBED, IdentValue.EMBED);
            String encoding = style.getStringProperty(CSSName.FS_PDF_FONT_ENCODING);

            String fontFamily = null;
            IdentValue fontWeight = null;
            IdentValue fontStyle = null;

            if (rule.hasFontFamily()) {
                fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
            }

            if (rule.hasFontWeight()) {
                fontWeight = style.getIdent(CSSName.FONT_WEIGHT);
            }

            if (rule.hasFontStyle()) {
                fontStyle = style.getIdent(CSSName.FONT_STYLE);
            }

            try {
                addFontFaceFont(fontFamily, fontWeight, fontStyle, src.asString(), font1, !noSubset);
            } catch (IOException e) {
                XRLog.exception("Could not load font " + src.asString(), e);
            }
        }
    }

    public void addFontDirectory(String dir, boolean embedded) throws IOException {
        File f = new File(dir);
        if (f.isDirectory()) {
            File[] files = f.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    String lower = name.toLowerCase(Locale.US);
                    return lower.endsWith(".otf") || lower.endsWith(".ttf");
                }
            });
            for (int i = 0; i < files.length; i++) {
                addFont(files[i].getAbsolutePath(), null);
            }
        }
    }

    public void addFont(String path, String fontFamilyNameOverride) throws IOException {
        InputStream is = new FileInputStream(path);
        
        try {
            addFont(is, fontFamilyNameOverride);
        } finally {
            is.close();
        }
    }
    
    public void addFont(InputStream is, String fontFamilyNameOverride) throws IOException {
        PDType0Font font = PDType0Font.load(_doc, is, true);

        String[] fontFamilyNames;
        
        if (fontFamilyNameOverride != null) {
            fontFamilyNames = new String[] { fontFamilyNameOverride };
        } else {
            fontFamilyNames = new String[] { font.getFontDescriptor().getFontFamily() };
        }

        for (int i = 0; i < fontFamilyNames.length; i++) {
            String fontFamilyName = fontFamilyNames[i];
            FontFamily fontFamily = getFontFamily(fontFamilyName);

            FontDescription descr = new FontDescription(font);
            PDFontDescriptor descriptor = font.getFontDescriptor();
            descr.setUnderlinePosition(descriptor.getDescent());
            descr.setWeight((int) descriptor.getFontWeight());
            descr.setStyle(descriptor.getItalicAngle() != 0 ? IdentValue.ITALIC : IdentValue.NORMAL);
            // TODO: Check if we can get anything better for measurements below.
            descr.setYStrikeoutPosition(descriptor.getFontBoundingBox().getUpperRightY() / 3f);
            descr.setYStrikeoutSize(100f);
            descr.setUnderlineThickness(50f);

            fontFamily.addFontDescription(descr);
        }
    }

    private void addFontFaceFont(
            String fontFamilyNameOverride, IdentValue fontWeightOverride, IdentValue fontStyleOverride, String uri, byte[] font1, boolean subset)
            throws IOException {
        String lower = uri.toLowerCase(Locale.US);
        
        if (lower.endsWith(".ttf")) {
            PDType0Font font = PDType0Font.load(_doc, new ByteArrayInputStream(font1), subset);
            
            String[] fontFamilyNames;
            if (fontFamilyNameOverride != null) {
                fontFamilyNames = new String[] { fontFamilyNameOverride };
            } else {
                fontFamilyNames = new String[] { font.getFontDescriptor().getFontFamily() };
            }

            for (int i = 0; i < fontFamilyNames.length; i++) {
                FontFamily fontFamily = getFontFamily(fontFamilyNames[i]);

                FontDescription descr = new FontDescription(font);
                PDFontDescriptor descriptor = font.getFontDescriptor();
                descr.setUnderlinePosition(descriptor.getDescent());
                descr.setWeight((int) descriptor.getFontWeight());
                descr.setStyle(descriptor.getItalicAngle() != 0 ? IdentValue.ITALIC : IdentValue.NORMAL); 
                // TODO: Check if we can get anything better for measurements below.
                descr.setYStrikeoutPosition(descriptor.getFontBoundingBox().getUpperRightY() / 3f);
                descr.setYStrikeoutSize(100f);
                descr.setUnderlineThickness(50f);
                descr.setFromFontFace(true);

                if (fontWeightOverride != null) {
                    descr.setWeight(convertWeightToInt(fontWeightOverride));
                }

                if (fontStyleOverride != null) {
                    descr.setStyle(fontStyleOverride);
                }

                fontFamily.addFontDescription(descr);
            }
        } else {
            // TODO: Logging
            throw new IOException("Unsupported font type");
        }
    }

    public FontFamily getFontFamily(String fontFamilyName) {
        FontFamily fontFamily = _fontFamilies.get(fontFamilyName);
        if (fontFamily == null) {
            fontFamily = new FontFamily();
            fontFamily.setName(fontFamilyName);
            _fontFamilies.put(fontFamilyName, fontFamily);
        }
        return fontFamily;
    }

    private FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        if (! (style == IdentValue.NORMAL || style == IdentValue.OBLIQUE
                || style == IdentValue.ITALIC)) {
            style = IdentValue.NORMAL;
        }

        List<FontDescription> fonts = new ArrayList<FontDescription>(3);
        
        if (families != null) {
            for (int i = 0; i < families.length; i++) {
                FontDescription font = resolveFont(ctx, families[i], size, weight, style, variant);
                if (font != null) {
                    fonts.add(font);
                }
            }
        }

        // For now, we end up with "Serif" built-in font.
        // Q: Should this change?
        // Q: Should we have a final automatically added font?
        fonts.add(resolveFont(ctx, "Serif", size, weight, style, variant));
        return new PdfBoxFSFont(fonts, size);
    }

    private String normalizeFontFamily(String fontFamily) {
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

    private FontDescription resolveFont(SharedContext ctx, String fontFamily, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        String normalizedFontFamily = normalizeFontFamily(fontFamily);
        String cacheKey = getHashName(normalizedFontFamily, weight, style);
        FontDescription result = _fontCache.get(cacheKey);

        if (result != null) {
            return result;
        }

        FontFamily family = (FontFamily)_fontFamilies.get(normalizedFontFamily);

        if (family != null) {
            result = family.match(convertWeightToInt(weight), style);

            if (result != null) {
                _fontCache.put(cacheKey, result);
                return result;
            }
        }

        return null;
    }

    public static int convertWeightToInt(IdentValue weight) {
        if (weight == IdentValue.NORMAL) {
            return 400;
        } else if (weight == IdentValue.BOLD) {
            return 700;
        } else if (weight == IdentValue.FONT_WEIGHT_100) {
            return 100;
        } else if (weight == IdentValue.FONT_WEIGHT_200) {
            return 200;
        } else if (weight == IdentValue.FONT_WEIGHT_300) {
            return 300;
        } else if (weight == IdentValue.FONT_WEIGHT_400) {
            return 400;
        } else if (weight == IdentValue.FONT_WEIGHT_500) {
            return 500;
        } else if (weight == IdentValue.FONT_WEIGHT_600) {
            return 600;
        } else if (weight == IdentValue.FONT_WEIGHT_700) {
            return 700;
        } else if (weight == IdentValue.FONT_WEIGHT_800) {
            return 800;
        } else if (weight == IdentValue.FONT_WEIGHT_900) {
            return 900;
        } else if (weight == IdentValue.LIGHTER) {
            // FIXME
            return 400;
        } else if (weight == IdentValue.BOLDER) {
            // FIXME
            return 700;
        }
        throw new IllegalArgumentException();
    }

    protected static String getHashName(
            String name, IdentValue weight, IdentValue style) {
        return name + "-" + weight + "-" + style;
    }

    private static Map<String, FontFamily> createInitialFontMap() {
        HashMap<String, FontFamily> result = new HashMap<String, FontFamily>();

        try {
            addCourier(result);
            addTimes(result);
            addHelvetica(result);
            addSymbol(result);
            addZapfDingbats(result);

            // Try and load the iTextAsian fonts
//            if(PdfBoxFontResolver.class.getClassLoader().getResource("com/lowagie/text/pdf/fonts/cjkfonts.properties") != null) {
//                addCJKFonts(result);
//            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return result;
    }

    private static PDFont createFont(PDFont font) throws IOException {
        return font;
    }

    private static void addCourier(HashMap result) throws IOException {
        FontFamily courier = new FontFamily();
        courier.setName("Courier");

        courier.addFontDescription(new FontDescription(
                createFont(PDType1Font.COURIER_BOLD_OBLIQUE), IdentValue.OBLIQUE, 700));
        courier.addFontDescription(new FontDescription(
                createFont(PDType1Font.COURIER_OBLIQUE), IdentValue.OBLIQUE, 400));
        courier.addFontDescription(new FontDescription(
                createFont(PDType1Font.COURIER_BOLD), IdentValue.NORMAL, 700));
        courier.addFontDescription(new FontDescription(
                createFont(PDType1Font.COURIER), IdentValue.NORMAL, 400));

        result.put("DialogInput", courier);
        result.put("Monospaced", courier);
        result.put("Courier", courier);
    }

    private static void addTimes(HashMap result) throws IOException {
        FontFamily times = new FontFamily();
        times.setName("Times");

        times.addFontDescription(new FontDescription(
                createFont(PDType1Font.TIMES_BOLD_ITALIC), IdentValue.ITALIC, 700));
        times.addFontDescription(new FontDescription(
                createFont(PDType1Font.TIMES_ITALIC), IdentValue.ITALIC, 400));
        times.addFontDescription(new FontDescription(
                createFont(PDType1Font.TIMES_BOLD), IdentValue.NORMAL, 700));
        times.addFontDescription(new FontDescription(
                createFont(PDType1Font.TIMES_ROMAN), IdentValue.NORMAL, 400));

        result.put("Serif", times);
        result.put("TimesRoman", times);
    }

    private static void addHelvetica(HashMap result) throws IOException {
        FontFamily helvetica = new FontFamily();
        helvetica.setName("Helvetica");

        helvetica.addFontDescription(new FontDescription(
                createFont(PDType1Font.HELVETICA_BOLD_OBLIQUE), IdentValue.OBLIQUE, 700));
        helvetica.addFontDescription(new FontDescription(
                createFont(PDType1Font.HELVETICA_OBLIQUE), IdentValue.OBLIQUE, 400));
        helvetica.addFontDescription(new FontDescription(
                createFont(PDType1Font.HELVETICA_BOLD), IdentValue.NORMAL, 700));
        helvetica.addFontDescription(new FontDescription(
                createFont(PDType1Font.HELVETICA), IdentValue.NORMAL, 400));

        result.put("Dialog", helvetica);
        result.put("SansSerif", helvetica);
        result.put("Helvetica", helvetica);
    }

    private static void addSymbol(Map result) throws IOException {
        FontFamily fontFamily = new FontFamily();
        fontFamily.setName("Symbol");

        fontFamily.addFontDescription(new FontDescription(createFont(PDType1Font.SYMBOL), IdentValue.NORMAL, 400));

        result.put("Symbol", fontFamily);
    }

    private static void addZapfDingbats(Map result) throws IOException {
        FontFamily fontFamily = new FontFamily();
        fontFamily.setName("ZapfDingbats");

        fontFamily.addFontDescription(new FontDescription(createFont(PDType1Font.ZAPF_DINGBATS), IdentValue.NORMAL, 400));

        result.put("ZapfDingbats", fontFamily);
    }
    
/* TODO: CJK Fonts
    // fontFamilyName, fontName, encoding
    private static final String[][] cjkFonts = {
        {"STSong-Light-H", "STSong-Light", "UniGB-UCS2-H"},
        {"STSong-Light-V", "STSong-Light", "UniGB-UCS2-V"},
        {"STSongStd-Light-H", "STSongStd-Light", "UniGB-UCS2-H"},
        {"STSongStd-Light-V", "STSongStd-Light", "UniGB-UCS2-V"},
        {"MHei-Medium-H", "MHei-Medium", "UniCNS-UCS2-H"},
        {"MHei-Medium-V", "MHei-Medium", "UniCNS-UCS2-V"},
        {"MSung-Light-H", "MSung-Light", "UniCNS-UCS2-H"},
        {"MSung-Light-V", "MSung-Light", "UniCNS-UCS2-V"},
        {"MSungStd-Light-H", "MSungStd-Light", "UniCNS-UCS2-H"},
        {"MSungStd-Light-V", "MSungStd-Light", "UniCNS-UCS2-V"},
        {"HeiseiMin-W3-H", "HeiseiMin-W3", "UniJIS-UCS2-H"},
        {"HeiseiMin-W3-V", "HeiseiMin-W3", "UniJIS-UCS2-V"},
        {"HeiseiKakuGo-W5-H", "HeiseiKakuGo-W5", "UniJIS-UCS2-H"},
        {"HeiseiKakuGo-W5-V", "HeiseiKakuGo-W5", "UniJIS-UCS2-V"},
        {"KozMinPro-Regular-H", "KozMinPro-Regular", "UniJIS-UCS2-HW-H"},
        {"KozMinPro-Regular-V", "KozMinPro-Regular", "UniJIS-UCS2-HW-V"},
        {"HYGoThic-Medium-H", "HYGoThic-Medium", "UniKS-UCS2-H"},
        {"HYGoThic-Medium-V", "HYGoThic-Medium", "UniKS-UCS2-V"},
        {"HYSMyeongJo-Medium-H", "HYSMyeongJo-Medium", "UniKS-UCS2-H"},
        {"HYSMyeongJo-Medium-V", "HYSMyeongJo-Medium", "UniKS-UCS2-V"},
        {"HYSMyeongJoStd-Medium-H", "HYSMyeongJoStd-Medium", "UniKS-UCS2-H"},
        {"HYSMyeongJoStd-Medium-V", "HYSMyeongJoStd-Medium", "UniKS-UCS2-V"}
    };

    private static void addCJKFonts(Map fontFamilyMap) throws DocumentException, IOException {
        for(int i = 0; i < cjkFonts.length; i++) {
            String fontFamilyName = cjkFonts[i][0];
            String fontName = cjkFonts[i][1];
            String encoding = cjkFonts[i][2];

            addCJKFont(fontFamilyName, fontName, encoding, fontFamilyMap);
        }
    }

    private static void addCJKFont(String fontFamilyName, String fontName, String encoding, Map fontFamilyMap) throws DocumentException, IOException {
        FontFamily fontFamily = new FontFamily();
        fontFamily.setName(fontFamilyName);

        fontFamily.addFontDescription(new FontDescription(createFont(fontName+",BoldItalic", encoding, false), IdentValue.OBLIQUE, 700));
        fontFamily.addFontDescription(new FontDescription(createFont(fontName+",Italic", encoding, false), IdentValue.OBLIQUE, 400));
        fontFamily.addFontDescription(new FontDescription(createFont(fontName+",Bold", encoding, false), IdentValue.NORMAL, 700));
        fontFamily.addFontDescription(new FontDescription(createFont(fontName, encoding, false), IdentValue.NORMAL, 400));

        fontFamilyMap.put(fontFamilyName, fontFamily);
    }
*/
    private static class FontFamily {
        private String _name;
        private List _fontDescriptions;

        public FontFamily() {
        }

        public List getFontDescriptions() {
            return _fontDescriptions;
        }

        public void addFontDescription(FontDescription descr) {
            if (_fontDescriptions == null) {
                _fontDescriptions = new ArrayList();
            }
            _fontDescriptions.add(descr);
            Collections.sort(_fontDescriptions,
                    new Comparator() {
                        public int compare(Object o1, Object o2) {
                            FontDescription f1 = (FontDescription)o1;
                            FontDescription f2 = (FontDescription)o2;
                            return f1.getWeight() - f2.getWeight();
                        }
            });
        }

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }

        public FontDescription match(int desiredWeight, IdentValue style) {
            if (_fontDescriptions == null) {
                throw new RuntimeException("fontDescriptions is null");
            }

            List candidates = new ArrayList();

            for (Iterator i = _fontDescriptions.iterator(); i.hasNext(); ) {
                FontDescription description = (FontDescription)i.next();

                if (description.getStyle() == style) {
                    candidates.add(description);
                }
            }

            if (candidates.size() == 0) {
                if (style == IdentValue.ITALIC) {
                    return match(desiredWeight, IdentValue.OBLIQUE);
                } else if (style == IdentValue.OBLIQUE) {
                    return match(desiredWeight, IdentValue.NORMAL);
                } else {
                    candidates.addAll(_fontDescriptions);
                }
            }

            FontDescription[] matches = (FontDescription[])
                candidates.toArray(new FontDescription[candidates.size()]);
            FontDescription result;

            result = findByWeight(matches, desiredWeight, SM_EXACT);

            if (result != null) {
                return result;
            } else {
                if (desiredWeight <= 500) {
                    return findByWeight(matches, desiredWeight, SM_LIGHTER_OR_DARKER);
                } else {
                    return findByWeight(matches, desiredWeight, SM_DARKER_OR_LIGHTER);
                }
            }
        }

        private static final int SM_EXACT = 1;
        private static final int SM_LIGHTER_OR_DARKER = 2;
        private static final int SM_DARKER_OR_LIGHTER = 3;

        private FontDescription findByWeight(FontDescription[] matches,
                int desiredWeight, int searchMode) {
            if (searchMode == SM_EXACT) {
                for (int i = 0; i < matches.length; i++) {
                    FontDescription descr = matches[i];
                    if (descr.getWeight() == desiredWeight) {
                        return descr;
                    }
                }
                return null;
            } else if (searchMode == SM_LIGHTER_OR_DARKER){
                int offset = 0;
                FontDescription descr = null;
                for (offset = 0; offset < matches.length; offset++) {
                    descr = matches[offset];
                    if (descr.getWeight() > desiredWeight) {
                        break;
                    }
                }

                if (offset > 0 && descr.getWeight() > desiredWeight) {
                    return matches[offset-1];
                } else {
                    return descr;
                }

            } else if (searchMode == SM_DARKER_OR_LIGHTER) {
                int offset = 0;
                FontDescription descr = null;
                for (offset = matches.length - 1; offset >= 0; offset--) {
                    descr = matches[offset];
                    if (descr.getWeight() < desiredWeight) {
                        break;
                    }
                }

                if (offset != matches.length - 1 && descr.getWeight() < desiredWeight) {
                    return matches[offset+1];
                } else {
                    return descr;
                }
            }

            return null;
        }
    }

    public static class FontDescription {
        private IdentValue _style;
        private int _weight;

        private PDFont _font;

        private float _underlinePosition;
        private float _underlineThickness;

        private float _yStrikeoutSize;
        private float _yStrikeoutPosition;

        private boolean _isFromFontFace;

        public FontDescription() {
        }

        public FontDescription(PDFont font) {
            this(font, IdentValue.NORMAL, 400);
        }

        public FontDescription(PDFont font, IdentValue style, int weight) {
            _font = font;
            _style = style;
            _weight = weight;
            setMetricDefaults();
        }

        public PDFont getFont() {
            return _font;
        }

        public void setFont(PDFont font) {
            _font = font;
        }

        public int getWeight() {
            return _weight;
        }

        public void setWeight(int weight) {
            _weight = weight;
        }

        public IdentValue getStyle() {
            return _style;
        }

        public void setStyle(IdentValue style) {
            _style = style;
        }

        /**
         * @see #getUnderlinePosition()
         */
        public float getUnderlinePosition() {
            return _underlinePosition;
        }

        /**
         * This refers to the top of the underline stroke
         */
        public void setUnderlinePosition(float underlinePosition) {
            _underlinePosition = underlinePosition;
        }

        public float getUnderlineThickness() {
            return _underlineThickness;
        }

        public void setUnderlineThickness(float underlineThickness) {
            _underlineThickness = underlineThickness;
        }

        public float getYStrikeoutPosition() {
            return _yStrikeoutPosition;
        }

        public void setYStrikeoutPosition(float strikeoutPosition) {
            _yStrikeoutPosition = strikeoutPosition;
        }

        public float getYStrikeoutSize() {
            return _yStrikeoutSize;
        }

        public void setYStrikeoutSize(float strikeoutSize) {
            _yStrikeoutSize = strikeoutSize;
        }

        private void setMetricDefaults() {
            _underlinePosition = -50;
            _underlineThickness = 50;

            float boxHeight = _font.getFontDescriptor().getXHeight();
            _yStrikeoutPosition = boxHeight / 2 + 50;
            _yStrikeoutSize = 100;
        }

        public boolean isFromFontFace() {
            return _isFromFontFace;
        }

        public void setFromFontFace(boolean isFromFontFace) {
            _isFromFontFace = isFromFontFace;
        }
    }
}
