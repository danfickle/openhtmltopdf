package com.openhtmltopdf.pdfboxout.fontstore;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeCollection.TrueTypeFontProcessor;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontCache;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;

public class FallbackFontStore implements Closeable {
    private final List<FontDescription> fonts = new ArrayList<>();
    private final List<TrueTypeCollection> _collectionsToClose = new ArrayList<>();
    private final PDDocument _doc;
    private final FontCache _fontCache;
    private final FSCacheEx<String, FSCacheValue> _fontMetricsCache;

    public FallbackFontStore(
            SharedContext sharedContext,
            PDDocument doc,
            FSCacheEx<String, FSCacheValue> pdfMetricsCache,
            FontCache fontCache) {
        this._doc = doc;
        this._fontMetricsCache = pdfMetricsCache;
        this._fontCache = fontCache;
    }

    private int getFontPriority(FontDescription font, String[] families, IdentValue weight, IdentValue desiredStyle, IdentValue variant) {
        String fontFamily = font.getFamily();
        int fontWeight = font.getWeight();
        IdentValue fontStyle = font.getStyle();

        List<String> desiredFamilies = families != null ?
                Arrays.asList(families) : Collections.emptyList();
        int desiredWeight = FontResolverHelper.convertWeightToInt(weight);

        if (fontWeight == desiredWeight &&
            fontStyle == desiredStyle) {
            // Exact match for weight and style.
            return getFamilyPriority(fontFamily, desiredFamilies);
        } else if (Math.abs(fontWeight - desiredWeight) < 200 &&
                   fontStyle == desiredStyle) {
            // Near enough weight match, exact style match.
            return 3 + getFamilyPriority(fontFamily, desiredFamilies);
        } else if (fontStyle == desiredStyle) {
            // No weight match, but style matches.
            return 6 + getFamilyPriority(fontFamily, desiredFamilies);
        } else {
            // Neither weight nor style matches.
            return 9 + getFamilyPriority(fontFamily, desiredFamilies);
        }
    }

    private int getFamilyPriority(String fontFamily, List<String> desiredFamilies) {
        if (!desiredFamilies.isEmpty() &&
            desiredFamilies.get(0).equals(fontFamily)) {
            return 1;
        } else if (desiredFamilies.contains(fontFamily)) {
            return 2;
        } else {
            return 3;
        }
    }

    public List<FontDescription> resolveFonts(
            SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {

        if (fonts.size() <= 1) {
            // No need to make a copy to sort.
            return fonts;
        }

        List<FontDescription> ret = new ArrayList<>(fonts);

        Collections.sort(ret, Comparator.comparing(font -> getFontPriority(font, families, weight, style, variant)));

        return ret;
    }

    public void addFont(
            FSSupplier<InputStream> supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) {

        FontDescription descr = new FontDescription(
                _doc,
                supplier,
                FontUtil.normalizeFontWeight(fontWeightOverride),
                FontUtil.normalizeFontStyle(fontStyleOverride),
                fontFamilyNameOverride,
                false, // isFromFontFace
                subset,
                _fontMetricsCache,
                _fontCache);

        addFont(subset, descr);
    }

    public void addFont(
            PDFontSupplier supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) {

        FontDescription descr = new FontDescription(
                _doc,
                supplier,
                FontUtil.normalizeFontStyle(fontStyleOverride),
                FontUtil.normalizeFontWeight(fontWeightOverride),
                fontFamilyNameOverride,
                false, // isFromFontFace
                subset,
                _fontMetricsCache,
                _fontCache);

        addFont(subset, descr);
    }

    /**
     * Add a font with a lazy loaded PDFont
     */
    public void addFontLazy(FSSupplier<PDFont> font, String fontFamilyNameOverride, Integer fontWeightOverride, IdentValue fontStyleOverride, boolean subset) {
        FontDescription descr = new FontDescription(
                _doc,
                font,
                FontUtil.normalizeFontStyle(fontStyleOverride),
                FontUtil.normalizeFontWeight(fontWeightOverride),
                fontFamilyNameOverride,
                false,   // isFromFontFace
                subset,
                _fontMetricsCache,
                _fontCache);

        addFont(subset, descr);
    }

    private void addFont(boolean subset, FontDescription descr) {
        if (!subset) {
            if (descr.realizeFont()) {
                fonts.add(descr);
            }
        } else {
            fonts.add(descr);
        }
    }

    public void close() throws IOException {
        for (TrueTypeCollection collection : _collectionsToClose) {
            FontUtil.tryClose(collection);
        }
        _collectionsToClose.clear();
    }

    /**
     * Add a font using a FontBox TrueTypeFont.
     */
    void addFont(
            TrueTypeFont trueTypeFont,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) throws IOException {

        PDFont font = PDType0Font.load(_doc, trueTypeFont, subset);

        addFontLazy(new PDFontSupplier(font), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }

    public void addFontCollection(
            TrueTypeCollection collection,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) throws IOException {

        collection.processAllFonts(new TrueTypeFontProcessor() {
            @Override
            public void process(TrueTypeFont ttf) throws IOException {
                addFont(ttf, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
            }
        });
        _collectionsToClose.add(collection);
    }
}
