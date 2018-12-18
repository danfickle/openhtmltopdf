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
import com.openhtmltopdf.outputdevice.helper.FontFaceFontSupplier;
import com.openhtmltopdf.outputdevice.helper.FontFamily;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.outputdevice.helper.MinimalFontDescription;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.util.XRLog;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeCollection.TrueTypeFontProcessor;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

/**
 * This class handles all font resolving for the PDF generation. Please note that at the moment only subsetting/embedding
 * of fonts work. So you should always set embedded/subset=true for now.
 */
public class PdfBoxFontResolver implements FontResolver {
    private Map<String, FontFamily<FontDescription>> _fontFamilies;
    private Map<String, FontDescription> _fontCache = new HashMap<String, FontDescription>();
    private final PDDocument _doc;
    private final SharedContext _sharedContext;
    private final List<TrueTypeCollection> _collectionsToClose = new ArrayList<TrueTypeCollection>();
    private final FSCacheEx<String, FSCacheValue> _fontMetricsCache;
    private final PdfAConformance _pdfAConformance;

    public PdfBoxFontResolver(SharedContext sharedContext, PDDocument doc, FSCacheEx<String, FSCacheValue> pdfMetricsCache, PdfAConformance pdfAConformance) {
        _sharedContext = sharedContext;
        _doc = doc;
        _fontMetricsCache = pdfMetricsCache;
        _pdfAConformance = pdfAConformance;
 
        // All fonts are required to be embedded in PDF/A documents, so we don't add the built-in fonts, if conformance is required.
        _fontFamilies = (_pdfAConformance == PdfAConformance.NONE) ? createInitialFontMap() : new HashMap<String, FontFamily<FontDescription>>();
    }

    @Override
    public FSFont resolveFont(SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }

	/**
	 * Free all font resources (i.e. open files), the document should already be
	 * closed.
	 */
	public void close() {
		_fontCache.clear();

		// Close all still open TrueTypeCollections
		for (TrueTypeCollection collection : _collectionsToClose) {
			try {
				collection.close();
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
		_collectionsToClose.clear();
	}

    @Deprecated
    @Override
    public void flushCache() {
        _fontFamilies = createInitialFontMap();
        close();
        _fontCache = new HashMap<String, FontDescription>();
    }

    @Deprecated
    public void flushFontFaceFonts() {
        _fontCache = new HashMap<String, FontDescription>();

        for (Iterator<FontFamily<FontDescription>> i = _fontFamilies.values().iterator(); i.hasNext(); ) {
            FontFamily<FontDescription> family = i.next();
            for (Iterator<FontDescription> j = family.getFontDescriptions().iterator(); j.hasNext(); ) {
                FontDescription d = j.next();
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
        for (FontFaceRule rule : fontFaces) {
            CalculatedStyle style = rule.getCalculatedStyle();

            FSDerivedValue src = style.valueByName(CSSName.SRC);
            if (src == IdentValue.NONE) {
                continue;
            }

            boolean noSubset = style.isIdent(CSSName.FS_FONT_SUBSET, IdentValue.COMPLETE_FONT);
//            boolean embedded = style.isIdent(CSSName.FS_PDF_FONT_EMBED, IdentValue.EMBED);
//            String encoding = style.getStringProperty(CSSName.FS_PDF_FONT_ENCODING);

            String fontFamily = null;
            IdentValue fontWeight = null;
            IdentValue fontStyle = null;

            if (rule.hasFontFamily()) {
                fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
            } else {
                XRLog.cssParse(Level.WARNING, "Must provide at least a font-family and src in @font-face rule");
                continue;
            }

            if (rule.hasFontWeight()) {
                fontWeight = style.getIdent(CSSName.FONT_WEIGHT);
            }

            if (rule.hasFontStyle()) {
                fontStyle = style.getIdent(CSSName.FONT_STYLE);
            }

            addFontFaceFont(fontFamily, fontWeight, fontStyle, src.asString(), !noSubset);
        }
    }

    /**
     * Add all fonts in the given directory
     */
    public void addFontDirectory(String dir, boolean embedded) throws IOException {
        File f = new File(dir);
        if (f.isDirectory()) {
            File[] files = f.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    String lower = name.toLowerCase(Locale.US);
                    return lower.endsWith(".ttf") || lower.endsWith(".ttc");
                }
            });

            assert files != null;
            for (File file : files) {
                addFont(file, file.getName(), 400, IdentValue.NORMAL, embedded);
            }
        }
    }

    /**
     * Add a font using a FontBox TrueTypeFont.
     */
    private void addFont(TrueTypeFont trueTypeFont, String fontFamilyNameOverride,
                        Integer fontWeightOverride, IdentValue fontStyleOverride, boolean subset) throws IOException {


        PDFont font = PDType0Font.load(_doc, trueTypeFont, subset);

		addFontLazy(new PDFontSupplier(font), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }
    
	private static class PDFontSupplier implements FSSupplier<PDFont> {
		private final PDFont _font;

		PDFontSupplier(PDFont font) {
			_font = font;
		}

		@Override
		public PDFont supply() {
			return _font;
		}
	}

	/**
	 * Add a font with a lazy loaded PDFont
	 */
    private void addFontLazy(FSSupplier<PDFont> font, String fontFamilyNameOverride, Integer fontWeightOverride, IdentValue fontStyleOverride, boolean subset) {
        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyNameOverride);
        FontDescription descr = new FontDescription(
                _doc,
                font,
                normalizeFontStyle(fontStyleOverride),
                normalizeFontWeight(fontWeightOverride),
                fontFamilyNameOverride,
                false,   // isFromFontFace
                subset,
                _fontMetricsCache);

        if (!subset) {
            if (descr.realizeFont()) {
                fontFamily.addFontDescription(descr);
            }
        } else {
            fontFamily.addFontDescription(descr);
        }
    }

    /**
	 * Add fonts using a FontBox TrueTypeCollection.
	 */
	private void addFontCollection(TrueTypeCollection collection, final String fontFamilyNameOverride,
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

	/**
	 * Add fonts using a .ttc TrueTypeCollection
	 */
	public void addFontCollection(FSSupplier<InputStream> supplier, final String fontFamilyNameOverride,
			final Integer fontWeightOverride, final IdentValue fontStyleOverride, final boolean subset)
			throws IOException {
		InputStream inputStream = supplier.supply();
		try {
			TrueTypeCollection collection = new TrueTypeCollection(inputStream);
			addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
		} finally {
			inputStream.close();
		}
    }

    /**
     * Add fonts using a .ttc TrueTypeCollection
     */
    public void addFontCollection(File file, final String fontFamilyNameOverride,
                                  final Integer fontWeightOverride, final IdentValue fontStyleOverride, final boolean subset)
            throws IOException {
		TrueTypeCollection collection = new TrueTypeCollection(file);
		addFontCollection(collection, fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
    }

	/**
	 * Add a font using a existing file. If the file is a TrueTypeCollection, it
	 * will be handled as such.
	 */
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
		addFontLazy(new FilePDFontSupplier(fontFile, _doc), fontFamilyNameOverride, fontWeightOverride, fontStyleOverride, subset);
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
				return null;
			}
		}
	}


	/**
	 * Add a font using a InputStream. The given file must be a TrueType Font
	 * (.ttf). If you know the underlying stream is a .ttc file you should use
	 * {@link #addFontCollection(FSSupplier, String, Integer, IdentValue, boolean)}
	 */
	public void addFont(FSSupplier<InputStream> supplier, String fontFamilyNameOverride, Integer fontWeightOverride,
			IdentValue fontStyleOverride, boolean subset) {
		FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyNameOverride);

		FontDescription descr = new FontDescription(
		        _doc,
		        supplier,
		        normalizeFontWeight(fontWeightOverride),
		        normalizeFontStyle(fontStyleOverride),
		        fontFamilyNameOverride,
		        false, // isFromFontFace
		        subset,
		        _fontMetricsCache);

		if (!subset) {
			if (descr.realizeFont()) {
				fontFamily.addFontDescription(descr);
			}
		} else {
			fontFamily.addFontDescription(descr);
		}
	}
	
    private int normalizeFontWeight(IdentValue fontWeight) {
        return fontWeight != null ? FontResolverHelper.convertWeightToInt(fontWeight) : 400;
    }
    
    private int normalizeFontWeight(Integer fontWeight) {
        return fontWeight != null ? fontWeight : 400;
    }
    
    private IdentValue normalizeFontStyle(IdentValue fontStyle) {
        return fontStyle != null ? fontStyle : IdentValue.NORMAL;
    }
    
    private void addFontFaceFont(String fontFamilyName, IdentValue fontWeight, IdentValue fontStyle, String uri, boolean subset) {
        FSSupplier<InputStream> fontSupplier = new FontFaceFontSupplier(_sharedContext, uri);
        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyName);
        
        FontDescription description = new FontDescription(
                    _doc,
                    fontSupplier,
                    normalizeFontWeight(fontWeight),
                    normalizeFontStyle(fontStyle),
                    fontFamilyName,
                    true,  // isFromFontFace
                    subset,
                    _fontMetricsCache);
        
        if (!subset) {
            if (description.realizeFont()) {
                fontFamily.addFontDescription(description);
            }
        } else {
            fontFamily.addFontDescription(description);
        }
    }

    private FontFamily<FontDescription> getFontFamily(String fontFamilyName) {
        FontFamily<FontDescription> fontFamily = _fontFamilies.get(fontFamilyName);
        if (fontFamily == null) {
            fontFamily = new FontFamily<FontDescription>();
            _fontFamilies.put(fontFamilyName, fontFamily);
        }
        return fontFamily;
    }

    private FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        if (!(style == IdentValue.NORMAL || style == IdentValue.OBLIQUE || style == IdentValue.ITALIC)) {
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

        if (_pdfAConformance == PdfAConformance.NONE) {
            // We don't have a final fallback font for PDF/A documents as serif may not be available
            // unless the user has explicitly embedded it.
            
            // For now, we end up with "Serif" built-in font.
            // Q: Should this change?
            // Q: Should we have a final automatically added font?
            fonts.add(resolveFont(ctx, "Serif", size, weight, style, variant));
        }
        
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

        FontFamily<FontDescription> family = _fontFamilies.get(normalizedFontFamily);

        if (family != null) {
            result = family.match(FontResolverHelper.convertWeightToInt(weight), style);

            if (result != null) {
                _fontCache.put(cacheKey, result);
                return result;
            }
        }

        return null;
    }

    protected static String getHashName(
            String name, IdentValue weight, IdentValue style) {
        return name + "-" + weight + "-" + style;
    }

    private static Map<String, FontFamily<FontDescription>> createInitialFontMap() {
        HashMap<String, FontFamily<FontDescription>> result = new HashMap<String, FontFamily<FontDescription>>();

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

    private static void addCourier(HashMap<String, FontFamily<FontDescription>> result) throws IOException {
        FontFamily<FontDescription> courier = new FontFamily<FontDescription>();
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

    private static void addTimes(HashMap<String, FontFamily<FontDescription>> result) throws IOException {
        FontFamily<FontDescription> times = new FontFamily<FontDescription>();
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

    private static void addHelvetica(HashMap<String, FontFamily<FontDescription>> result) throws IOException {
        FontFamily<FontDescription> helvetica = new FontFamily<FontDescription>();
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

    private static void addSymbol(Map<String, FontFamily<FontDescription>> result) throws IOException {
        FontFamily<FontDescription> fontFamily = new FontFamily<FontDescription>();
        fontFamily.setName("Symbol");

        fontFamily.addFontDescription(new FontDescription(createFont(PDType1Font.SYMBOL), IdentValue.NORMAL, 400));

        result.put("Symbol", fontFamily);
    }

    private static void addZapfDingbats(Map<String, FontFamily<FontDescription>> result) throws IOException {
        FontFamily<FontDescription> fontFamily = new FontFamily<FontDescription>();
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

        /**
         * Create a font description from one of the PDF built-in fonts.
         */
        private FontDescription(PDFont font, IdentValue style, int weight) {
            this(null, font, style, weight);
        }
        
        /**
         * Create a font description from an input stream supplier.
         * The input stream will only be accessed if {@link #getFont()} or 
         * {@link #getFontMetrics()} (and the font metrics were not available from cache) are called.
         */
        private FontDescription(
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
                XRLog.exception("Couldn't load font metrics.", e);
            }
        }

        /**
         * Creates a font description from a PDFont supplier. The supplier will only be called upon
         * if {@link #getFont()} or {@link #getFontMetrics()} (and the font metrics were not available from cache) are called.
         */
        private FontDescription(
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

        private String createFontMetricsCacheKey(String family, int weight, IdentValue style) {
            return "font-metrics:" + family + ":" + weight + ":" + style.toString();
        }
        
        private PdfBoxRawPDFontMetrics getFontMetricsFromCache(String family, int weight, IdentValue style) {
            return (PdfBoxRawPDFontMetrics) _metricsCache.get(createFontMetricsCacheKey(family, weight, style));
        }
        
        private void putFontMetricsInCache(String family, int weight, IdentValue style, PdfBoxRawPDFontMetrics metrics) {
            _metricsCache.put(createFontMetricsCacheKey(family, weight, style), metrics);
        }

        private boolean realizeFont() {
            if (_font == null && _fontSupplier != null) {
                XRLog.load(Level.INFO, "Loading font(" + _family + ") from PDFont supplier now.");
                
                _font = _fontSupplier.supply();
		_fontSupplier = null;
	    }
            
            if (_font == null && _supplier != null) {
                XRLog.load(Level.INFO, "Loading font(" + _family + ") from InputStream supplier now.");
                
                InputStream is = _supplier.supply();
                _supplier = null; // We only try once.
                
                if (is == null) {
                    return false;
                }
                
                try {
                    _font = PDType0Font.load(_doc, is, _isSubset);
                    
                    if (!isMetricsAvailable()) {
                        // If we already have metrics, they must have come from the cache.
                        PDFontDescriptor descriptor = _font.getFontDescriptor();
                        _metrics = PdfBoxRawPDFontMetrics.fromPdfBox(_font, descriptor);
                        putFontMetricsInCache(_family, _weight, _style, _metrics);
                    }
                } catch (IOException e) {
                    XRLog.exception("Couldn't load font. Please check that it is a valid truetype font.");
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
