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

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.extend.FontResolver;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.MinimalFontDescription;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.pdfboxout.fontstore.AbstractFontStore;
import com.openhtmltopdf.pdfboxout.fontstore.FallbackFontStore;
import com.openhtmltopdf.pdfboxout.fontstore.FontUtil;
import com.openhtmltopdf.pdfboxout.fontstore.MainFontStore;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

/**
 * This class handles all font resolving for the PDF generation.
 */
public class PdfBoxFontResolver implements FontResolver, Closeable {
    public enum FontGroup {
        MAIN,
        PRE_BUILT_IN_FALLBACK,
        FINAL_FALLBACK;
    }

    private final PDDocument _doc;
    private final MainFontStore _suppliedFonts;
    private final FallbackFontStore _preBuiltinFallbackFonts;
    private final AbstractFontStore _builtinFonts;
    private final FallbackFontStore _finalFallbackFonts;

    public PdfBoxFontResolver(SharedContext sharedContext, PDDocument doc, FSCacheEx<String, FSCacheValue> pdfMetricsCache, PdfAConformance pdfAConformance, boolean pdfUaConform) {
        this._doc = doc;

        this._suppliedFonts = new MainFontStore(sharedContext, doc, pdfMetricsCache);

        this._preBuiltinFallbackFonts = new FallbackFontStore(sharedContext, doc, pdfMetricsCache);

        // All fonts are required to be embedded in PDF/A documents, so we don't add
        // the built-in fonts, if conformance is required.
        this._builtinFonts = (pdfAConformance == PdfAConformance.NONE && !pdfUaConform) ? 
                new AbstractFontStore.BuiltinFontStore(doc) :
                new AbstractFontStore.EmptyFontStore();

        this._finalFallbackFonts = new FallbackFontStore(sharedContext, doc, pdfMetricsCache);
    }

    @Override
    public FSFont resolveFont(SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }

    /**
     * Free all font resources (i.e. open files), the document should already be
     * closed.
     */
    @Override
    public void close() {
        FontUtil.tryClose(this._suppliedFonts);
        FontUtil.tryClose(this._preBuiltinFallbackFonts);
        FontUtil.tryClose(this._finalFallbackFonts);
    }

    public void importFontFaces(List<FontFaceRule> fontFaces) {
        for (FontFaceRule rule : fontFaces) {
            CalculatedStyle style = rule.getCalculatedStyle();

            FSDerivedValue src = style.valueByName(CSSName.SRC);
            if (src == IdentValue.NONE) {
                continue;
            }

            boolean noSubset = style.isIdent(CSSName.FS_FONT_SUBSET, IdentValue.COMPLETE_FONT);

            String fontFamily = null;
            IdentValue fontWeight = null;
            IdentValue fontStyle = null;

            if (rule.hasFontFamily()) {
                fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
            } else {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.CSS_PARSE_MUST_PROVIDE_AT_LEAST_A_FONT_FAMILY_AND_SRC_IN_FONT_FACE_RULE);
                continue;
            }

            if (rule.hasFontWeight()) {
                fontWeight = style.getIdent(CSSName.FONT_WEIGHT);
            }

            if (rule.hasFontStyle()) {
                fontStyle = style.getIdent(CSSName.FONT_STYLE);
            }

            this._suppliedFonts.addFontFaceFont(fontFamily, fontWeight, fontStyle, src.asString(), !noSubset);
        }
    }

    /**
     * @deprecated Use {@link #addFontDirectory(String, boolean, FontGroup)}
     */
    @Deprecated
    public void addFontDirectory(String dir, boolean embedded) throws IOException {
        addFontDirectory(dir, embedded, FontGroup.MAIN);
    }

    /**
     * Add all fonts in the given directory
     */
    public void addFontDirectory(String dir, boolean embedded, FontGroup fontGroup) throws IOException {
        File f = new File(dir);
        if (f.isDirectory()) {
            File[] files = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String lower = name.toLowerCase(Locale.US);
                    return lower.endsWith(".ttf") || lower.endsWith(".ttc");
                }
            });

            assert files != null;
            for (File file : files) {
                addFont(file, file.getName(), 400, IdentValue.NORMAL, embedded, fontGroup);
            }
        }
    }

    /**
     * Adds a font collection (.ttc in an input stream) to a
     * specific font group.
     */
    public void addFontCollection(
            FSSupplier<InputStream> supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset,
            FontGroup fontGroup) throws IOException {

        try (InputStream inputStream = supplier.supply()) {
            TrueTypeCollection collection = new TrueTypeCollection(inputStream);

            if (fontGroup == FontGroup.MAIN) {
                this._suppliedFonts.addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
            } else {
                getFallbackFontStore(fontGroup).addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
            }
        }
    }

	/**
	 * Add fonts using a .ttc TrueTypeCollection
	 * @deprecated Use {@link #addFontCollection(FSSupplier, String, Integer, IdentValue, boolean, FontGroup)}
	 */
    @Deprecated
	public void addFontCollection(FSSupplier<InputStream> supplier, final String fontFamilyNameOverride,
			final Integer fontWeightOverride, final IdentValue fontStyleOverride, final boolean subset)
			throws IOException {
		try (InputStream inputStream = supplier.supply()){
			TrueTypeCollection collection = new TrueTypeCollection(inputStream);
            this._suppliedFonts.addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
		}
    }

    /**
     * Adds a font collection (.ttc file) to a specific group.
     */
    public void addFontCollection(
            File file,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset,
            FontGroup fontGroup) throws IOException {

        TrueTypeCollection collection = new TrueTypeCollection(file);

        if (fontGroup == FontGroup.MAIN) {
            this._suppliedFonts.addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        } else {
            getFallbackFontStore(fontGroup).addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        }
    }

    /**
     * Add fonts using a .ttc TrueTypeCollection
     * @deprecated Use {@link #addFontCollection(File, String, Integer, IdentValue, boolean, FontGroup)}
     */
    @Deprecated
    public void addFontCollection(File file, final String fontFamilyNameOverride,
                                  final Integer fontWeightOverride, final IdentValue fontStyleOverride, final boolean subset)
            throws IOException {
		TrueTypeCollection collection = new TrueTypeCollection(file);
        this._suppliedFonts.addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }

    /**
     * Add a font file (truetype) to a specific font group. 
     */
    public void addFont(
            File fontFile,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset,
            FontGroup fontGroup) throws IOException {

        if (fontFile.getName().toLowerCase(Locale.US).endsWith(".ttc")) {
            // Specialcase for TrueTypeCollections
            addFontCollection(fontFile, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset, fontGroup);
        } else if (fontGroup == FontGroup.MAIN) {
            this._suppliedFonts.addFontLazy(new FilePDFontSupplier(fontFile, _doc), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        } else {
            getFallbackFontStore(fontGroup).addFontLazy(new FilePDFontSupplier(fontFile, _doc), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        }
    }

    private FallbackFontStore getFallbackFontStore(FontGroup fontGroup) {
        assert fontGroup == FontGroup.PRE_BUILT_IN_FALLBACK ||
               fontGroup == FontGroup.FINAL_FALLBACK;

        return fontGroup == FontGroup.PRE_BUILT_IN_FALLBACK ?
                this._preBuiltinFallbackFonts :
                this._finalFallbackFonts;
    }

	/**
	 * Add a font using a existing file. If the file is a TrueTypeCollection, it
	 * will be handled as such.
	 * @deprecated Use {@link #addFont(File, String, Integer, IdentValue, boolean, FontGroup)}
	 */
    @Deprecated
	public void addFont(File fontFile, final String fontFamilyNameOverride, final Integer fontWeightOverride,
			final IdentValue fontStyleOverride, final boolean subset) throws IOException {
		/*
		 * Specialcase for TrueTypeCollections
		 */
		if (fontFile.getName().toLowerCase(Locale.US).endsWith(".ttc")) {
			addFontCollection(fontFile, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
			return;
		}

		/*
		 * We load the font using the file.
		 */
        this._suppliedFonts.addFontLazy(new FilePDFontSupplier(fontFile, _doc), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
	}

	/**
	 * Loads a Type0 font on demand
	 */
	private static class FilePDFontSupplier implements FSSupplier<PDFont> {
		private final File _fontFile;
		private final PDDocument _doc;

		FilePDFontSupplier(File fontFile, PDDocument doc) {
			this._fontFile = fontFile;
			this._doc = doc;
		}

		@Override
		public PDFont supply() {
			try {
				return PDType0Font.load(_doc, _fontFile);
			} catch (IOException e) {
			    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT, _fontFile.getAbsoluteFile(), e);
			    return null;
			}
		}
	}

    /**
     * Adds a font specified by an input stream (truetype) to a specific font group.
     */
    public void addFont(
            FSSupplier<InputStream> supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset,
            FontGroup fontGroup) {

        if (fontGroup == FontGroup.MAIN) {
            this._suppliedFonts.addFont(
                    supplier, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        } else {
            getFallbackFontStore(fontGroup).addFont(supplier, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        }
    }

    /**
     * Add a font using a InputStream. The given file must be a TrueType Font
     * (.ttf). If you know the underlying stream is a .ttc file you should use
     * {@link #addFontCollection(FSSupplier, String, Integer, IdentValue, boolean)}
     * 
     * @deprecated Use {@link #addFont(FSSupplier, String, Integer, IdentValue, boolean, FontGroup)}
     */
    @Deprecated
    public void addFont(
       FSSupplier<InputStream> supplier,
       String fontFamilyNameOverride,
       Integer fontWeightOverride,
       IdentValue fontStyleOverride,
       boolean subset) {

       this._suppliedFonts.addFont(
               supplier, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }

    /**
     * Add a font specified by a PDFontSupplier to a specific font group.
     */
    public void addFont(
            PDFontSupplier supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset,
            FontGroup fontGroup) {

        if (fontGroup == FontGroup.MAIN) {
            this._suppliedFonts.addFont(supplier, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        } else {
            getFallbackFontStore(fontGroup).addFont(supplier, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
        }
    }

    /**
     * Add a font using a <b>PDFontSupplier</b>. Use this method if you need special rules for font-loading (like using a font-cache) 
     * and subclass the {@link PDFontSupplier}.
     * 
     * @deprecated Use {@link #addFont(PDFontSupplier, String, Integer, IdentValue, boolean, FontGroup)}
     */
    @Deprecated
    public void addFont(
            PDFontSupplier supplier,
            String fontFamilyNameOverride,
            Integer fontWeightOverride,
            IdentValue fontStyleOverride,
            boolean subset) {

        this._suppliedFonts.addFont(supplier, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }





    private FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        if (!(style == IdentValue.NORMAL || style == IdentValue.OBLIQUE || style == IdentValue.ITALIC)) {
            style = IdentValue.NORMAL;
        }

        List<FontDescription> fonts = new ArrayList<>(3);

        // Supplied fonts
        if (families != null) {
            resolveFamilyFont(ctx, families, size, weight, style, variant, fonts, _suppliedFonts);
        }

        // Pre-builtin fallback fonts.
        fonts.addAll(_preBuiltinFallbackFonts.resolveFonts(ctx, families, size, weight, style, variant));

        // Built-in fonts.
        if (families != null) {
            resolveFamilyFont(ctx, families, size, weight, style, variant, fonts, _builtinFonts);

            FontDescription serif = _builtinFonts.resolveFont(ctx, "Serif", size, weight, style, variant);
            if (serif != null) {
                fonts.add(serif);
            }
        }

        // Post built-in fallback fonts.
        fonts.addAll(_finalFallbackFonts.resolveFonts(ctx, families, size, weight, style, variant));

        return new PdfBoxFSFont(fonts, size);
    }

    private void resolveFamilyFont(
            SharedContext ctx,
            String[] families,
            float size,
            IdentValue weight,
            IdentValue style,
            IdentValue variant,
            List<FontDescription> fonts,
            AbstractFontStore store) {

        for (int i = 0; i < families.length; i++) {
            FontDescription font = store.resolveFont(ctx, families[i], size, weight, style, variant);

            if (font != null) {
               fonts.add(font);
            }
        }
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

    /**
     * A <code>FontDescription</code> can exist in multiple states. Firstly the font may
     * or may not be realized. Fonts are automatically realized upon calling {@link #getFont()}
     * 
     * Secondly, the metrics may or may not be available. If not available, you can attempt
     * to retrieve them by realizing the font.
     */
    public static class FontDescription implements MinimalFontDescription {
        private final IdentValue _style;
        private final int _weight;
        private final String _family;
        private final PDDocument _doc;

        private FSSupplier<InputStream> _supplier;
        private FSSupplier<PDFont> _fontSupplier;
        private PDFont _font;

        private final boolean _isFromFontFace;
        private final boolean _isSubset;

        private PdfBoxRawPDFontMetrics _metrics;
        private final FSCacheEx<String, FSCacheValue> _metricsCache;

        @Override
        public String toString() {
            return String.format(
                    "FontDescription [_style=%s, _weight=%s, _family=%s, _isFromFontFace=%s, _isSubset=%s]",
                    _style, _weight, _family, _isFromFontFace, _isSubset);
        }

        /**
         * Create a font description from one of the PDF built-in fonts.
         */
        public FontDescription(PDFont font, IdentValue style, int weight) {
            this(null, font, style, weight);
        }

        /**
         * Create a font description from an input stream supplier.
         * The input stream will only be accessed if {@link #getFont()} or 
         * {@link #getFontMetrics()} (and the font metrics were not available from cache) are called.
         */
        public FontDescription(
                PDDocument doc, FSSupplier<InputStream> supplier,
                int weight, IdentValue style, String family,
                boolean isFromFontFace, boolean isSubset,
                FSCacheEx<String, FSCacheValue> metricsCache) {
            this._supplier = supplier;
            this._weight = weight;
            this._style = style;
            this._doc = doc;
            this._family = family;
            this._isFromFontFace = isFromFontFace;
            this._isSubset = isSubset;
            this._metricsCache = metricsCache;
            this._metrics = getFontMetricsFromCache(family, weight, style);
        }

        /**
         * Create a font description when a PDFont is definitely available to begin with.
         * Currently only used for PDF built-in fonts.
         */
        private FontDescription(PDDocument doc, PDFont font, IdentValue style, int weight) {
            _font = font;
            _style = style;
            _weight = weight;
            _supplier = null;
            _doc = doc;
            _metricsCache = null;
            _family = null;
            _isFromFontFace = false;
            _isSubset = false;
            PDFontDescriptor descriptor = font.getFontDescriptor();
            
            try {
                _metrics = PdfBoxRawPDFontMetrics.fromPdfBox(font, descriptor);
            } catch (IOException e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_COULD_NOT_LOAD_FONT_METRICS, e);
            }
        }

        /**
         * Creates a font description from a PDFont supplier. The supplier will only be called upon
         * if {@link #getFont()} or {@link #getFontMetrics()} (and the font metrics were not available from cache) are called.
         */
        public FontDescription(
                PDDocument doc, FSSupplier<PDFont> fontSupplier,
                IdentValue style, int weight, String family, 
                boolean isFromFontFace, boolean isSubset,
                FSCacheEx<String, FSCacheValue> metricsCache) {
            _fontSupplier = fontSupplier;
            _style = style;
            _weight = weight;
            _supplier = null;
            _doc = doc;
            _family = family;
            _isFromFontFace = isFromFontFace;
            _isSubset = isSubset;
            _metricsCache = metricsCache;
            _metrics = getFontMetricsFromCache(family, weight, style);
        }

        public String getFamily() {
            return _family;
        }

        private String createFontMetricsCacheKey(String family, int weight, IdentValue style) {
            return "font-metrics:" + family + ":" + weight + ":" + style.toString();
        }
        
        private PdfBoxRawPDFontMetrics getFontMetricsFromCache(String family, int weight, IdentValue style) {
            return (PdfBoxRawPDFontMetrics) _metricsCache.get(createFontMetricsCacheKey(family, weight, style));
        }
        
        private void putFontMetricsInCache(String family, int weight, IdentValue style, PdfBoxRawPDFontMetrics metrics) {
            _metricsCache.put(createFontMetricsCacheKey(family, weight, style), metrics);
        }
        
        private boolean loadMetrics() {
            try {
                PDFontDescriptor descriptor = _font.getFontDescriptor();
                _metrics = PdfBoxRawPDFontMetrics.fromPdfBox(_font, descriptor);
                putFontMetricsInCache(_family, _weight, _style, _metrics);
                return true;
            } catch (IOException e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT, _family);
                return false;
            }
        }

        public boolean realizeFont() {
            if (_font == null && _fontSupplier != null) {
                XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_LOADING_FONT_FROM_SUPPLIER, _family, "PDFont");

                _font = _fontSupplier.supply();
		_fontSupplier = null;
		
                if (!isMetricsAvailable()) {
                    // If we already have metrics, they must have come from the cache.
                    return loadMetrics();
                }
	    }
            
            if (_font == null && _supplier != null) {
                XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_LOADING_FONT_FROM_SUPPLIER, _family, "InputStream");

                InputStream is = _supplier.supply();
                _supplier = null; // We only try once.
                
                if (is == null) {
                    return false;
                }
                
                try {
                    _font = PDType0Font.load(_doc, is, _isSubset);
                    
                    if (!isMetricsAvailable()) {
                        return loadMetrics();
                    }
                } catch (IOException e) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT, _family);
                    return false;
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) { }
                }
            }
            
            return _font != null;
        }

        /**
         * Returns whether the font is available yet.
         * @see #getFont()
         */
        public boolean isFontAvailable() {
            return _font != null;
        }
        
        /**
         * Downloads and parses the font if required. Should only be called when the font is definitely needed.
         * @return the font or null if there was a problem.
         */
        public PDFont getFont() {
            realizeFont();
            return _font;
        }

        @Override
        public int getWeight() {
            return _weight;
        }

        @Override
        public IdentValue getStyle() {
            return _style;
        }

        public boolean isFromFontFace() {
            return _isFromFontFace;
        }
        
        /**
         * If the metrics are available yet.
         * @see #getFontMetrics()
         */
        public boolean isMetricsAvailable() {
            return _metrics != null;
        }
        
        /**
         * Downloads and parses the font if required (metrics were not available from cache).
         * Should only be called when the font metrics are definitely needed.
         * @return the font metrics or null if there was a problem.
         * @see #isMetricsAvailable()
         */
        public PdfBoxRawPDFontMetrics getFontMetrics() {
            if (!isMetricsAvailable()) {
                realizeFont();
            }
            
            return _metrics;
        }
    }
}
