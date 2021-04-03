package com.openhtmltopdf.pdfboxout.fontstore;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.TrueTypeCollection.TrueTypeFontProcessor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.FontFaceFontSupplier;
import com.openhtmltopdf.outputdevice.helper.FontFamily;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontCache;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;

public class MainFontStore extends AbstractFontStore implements Closeable {
    private final Map<String, FontFamily<FontDescription>> _fontFamilies = new HashMap<>();
    private final FontCache _fontCache;
    private final FSCacheEx<String, FSCacheValue> _fontMetricsCache;
    private final PDDocument _doc;
    private final SharedContext _sharedContext;
    private final List<TrueTypeCollection> _collectionsToClose = new ArrayList<>();

    public MainFontStore(
       SharedContext sharedContext,
       PDDocument doc,
       FSCacheEx<String, FSCacheValue> pdfMetricsCache,
       FontCache fontCache) {

        this._sharedContext = sharedContext;
        this._doc = doc;
        this._fontMetricsCache = pdfMetricsCache;
        this._fontCache = fontCache;
    }

    public void close() throws IOException {
        // Close all still open TrueTypeCollections
        for (TrueTypeCollection collection : _collectionsToClose) {
            FontUtil.tryClose(collection);
        }
        _collectionsToClose.clear();
    }

    /**
     * Add a font using a FontBox TrueTypeFont.
     */
    void addFont(TrueTypeFont trueTypeFont, String fontFamilyNameOverride,
                 Integer fontWeightOverride, IdentValue fontStyleOverride, boolean subset) throws IOException {

        PDFont font = PDType0Font.load(_doc, trueTypeFont, subset);

        addFontLazy(new PDFontSupplier(font), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }

    /**
     * Add a font with a lazy loaded PDFont
     */
    public void addFontLazy(FSSupplier<PDFont> font, String fontFamilyNameOverride, Integer fontWeightOverride, IdentValue fontStyleOverride, boolean subset) {
        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyNameOverride);
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

        addFontToFamily(subset, fontFamily, descr);
    }

    /**
     * Add fonts using a FontBox TrueTypeCollection.
     */
    public void addFontCollection(TrueTypeCollection collection, final String fontFamilyNameOverride,
            final Integer fontWeightOverride, final IdentValue fontStyleOverride, final boolean subset)
            throws IOException {
        collection.processAllFonts(new TrueTypeFontProcessor() {
            @Override
            public void process(TrueTypeFont ttf) throws IOException {
                addFont(ttf, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
            }
        });
        _collectionsToClose.add(collection);
    }

    public void addFontFaceFont(String fontFamilyName, IdentValue fontWeight, IdentValue fontStyle, String uri, boolean subset) {
        FSSupplier<InputStream> fontSupplier = new FontFaceFontSupplier(_sharedContext, uri);
        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyName);

        FontDescription description = new FontDescription(
                    _doc,
                    fontSupplier,
                    FontUtil.normalizeFontWeight(fontWeight),
                    FontUtil.normalizeFontStyle(fontStyle),
                    fontFamilyName,
                    true,  // isFromFontFace
                    subset,
                    _fontMetricsCache,
                    _fontCache);

        addFontToFamily(subset, fontFamily, description);
    }

    public void addFont(
            FSSupplier<InputStream> supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) {

        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyNameOverride);

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

        addFontToFamily(subset, fontFamily, descr);
    }

    public void addFont(
            PDFontSupplier supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) {

        // would have prefered to used FSSupplier<PDFont> but sadly that would give us an error
        // because the type-ereasure clashes with addFont(FSSupplier<InputStream> ...)
        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyNameOverride);

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

        addFontToFamily(subset, fontFamily, descr);
    }

    public Map<String, PDFont> getFontCache() {
        return _fontCache;
    }

    @Override
    public FontDescription resolveFont(
            SharedContext ctx, String fontFamily, float size, IdentValue weight, IdentValue style, IdentValue variant) {

        String normalizedFontFamily = FontUtil.normalizeFontFamily(fontFamily);
        FontFamily<FontDescription> family = _fontFamilies.get(normalizedFontFamily);

        if (family != null) {
            return family.match(FontResolverHelper.convertWeightToInt(weight), style);
        }

        return null;
    }

    private void addFontToFamily(boolean subset, FontFamily<FontDescription> fontFamily, FontDescription descr) {
        if (!subset) {
            if (descr.realizeFont()) {
                fontFamily.addFontDescription(descr);
            }
        } else {
            fontFamily.addFontDescription(descr);
        }
    }

    private FontFamily<FontDescription> getFontFamily(String fontFamilyName) {
        return _fontFamilies.computeIfAbsent(fontFamilyName, name -> new FontFamily<>(fontFamilyName));
    }

}
