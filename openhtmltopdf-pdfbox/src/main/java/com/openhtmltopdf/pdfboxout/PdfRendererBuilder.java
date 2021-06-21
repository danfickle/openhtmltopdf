package com.openhtmltopdf.pdfboxout;

import java.awt.FontFormatException;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.FSDOMMutator;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.extend.impl.FSNoOpCacheStore;
import com.openhtmltopdf.outputdevice.helper.AddedFont;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontCache;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontGroup;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

public class PdfRendererBuilder extends BaseRendererBuilder<PdfRendererBuilder, PdfRendererBuilderState> {
    public PdfRendererBuilder() {
        this(new PdfRendererBuilderState());
    }

	protected PdfRendererBuilder(PdfRendererBuilderState state) {
		super(state);

		for (CacheStore cacheStore : CacheStore.values()) {
		    // Use the flyweight pattern to initialize all caches with a no-op implementation to
		    // avoid excessive null handling.
		    state._caches.put(cacheStore, FSNoOpCacheStore.INSTANCE);
		}
	}

	/**
	 * Run the XHTML/XML to PDF conversion and output to an output stream set by
	 * toStream.
	 *
	 * @throws IOException
	 */
	public void run() throws IOException {
		try (Closeable d = applyDiagnosticConsumer(); PdfBoxRenderer renderer = this.buildPdfRenderer(d)){
			renderer.layout();
			renderer.createPDF();
		}
	}

	/**
	 * Build a PdfBoxRenderer for further customization. Remember to call
	 * {@link PdfBoxRenderer#cleanup()} after use.
	 *
	 * @return
	 */
	public PdfBoxRenderer buildPdfRenderer() {
		return buildPdfRenderer(applyDiagnosticConsumer());
	}

	public PdfBoxRenderer buildPdfRenderer(Closeable diagnosticConsumer) {
	    if (state._fontCache == null) {
	        state._fontCache = new FontCache();
	    }

		UnicodeImplementation unicode = new UnicodeImplementation(state._reorderer, state._splitter, state._lineBreaker,
				state._unicodeToLowerTransformer, state._unicodeToUpperTransformer, state._unicodeToTitleTransformer, state._textDirection,
				state._charBreaker);

		PageDimensions pageSize = new PageDimensions(state._pageWidth, state._pageHeight, state._isPageSizeInches);

		BaseDocument doc = new BaseDocument(state._baseUri, state._html, state._document, state._file, state._uri);

		PdfBoxRenderer renderer = new PdfBoxRenderer(doc, unicode, pageSize, state, diagnosticConsumer);

		/*
		 * Register all Fonts
		 */
		PdfBoxFontResolver resolver = renderer.getFontResolver();

        for (AddedFont font : state._fonts) {

            if (state._svgImpl != null &&
                font.fontFile != null &&
                font.usedFor.contains(FSFontUseCase.SVG)) {
                try {
                    state._svgImpl.addFontFile(font.fontFile, font.family, font.weight, font.style);
                } catch (IOException | FontFormatException e) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.INIT_FONT_COULD_NOT_BE_LOADED, font.fontFile.getPath(), e);
                }
            }

            if (state._mathmlImpl != null &&
                font.fontFile != null &&
                font.usedFor.contains(FSFontUseCase.MATHML)) {
                try {
                    state._mathmlImpl.addFontFile(font.fontFile, font.family, font.weight, font.style);
                } catch (IOException | FontFormatException e) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.INIT_FONT_COULD_NOT_BE_LOADED, font.fontFile.getPath(), e);
                }
            }

            if (font.usedFor.contains(FSFontUseCase.DOCUMENT) ||
                font.usedFor.contains(FSFontUseCase.FALLBACK_PRE) ||
                font.usedFor.contains(FSFontUseCase.FALLBACK_FINAL)) {
                IdentValue fontStyle = font.style != null ? font.style.toIdentValue() : null;

                FontGroup group;
                if (font.usedFor.contains(FSFontUseCase.FALLBACK_PRE)) {
                    group = FontGroup.PRE_BUILT_IN_FALLBACK;
                } else if (font.usedFor.contains(FSFontUseCase.FALLBACK_FINAL)) {
                    group = FontGroup.FINAL_FALLBACK;
                } else {
                    group = FontGroup.MAIN;
                }

                // use InputStream supplier
                if (font.supplier != null) {
                    resolver.addFont(font.supplier, font.family, font.weight, fontStyle, font.subset, group);
                }
                // use PDFont supplier
                else if (font.pdfontSupplier != null) {
                    resolver.addFont((PDFontSupplier) font.pdfontSupplier, font.family, font.weight, fontStyle, font.subset, group);
                }
                // load via font File
                else {
                    try {
                        resolver.addFont(font.fontFile, font.family, font.weight, fontStyle, font.subset, group);
                    } catch (Exception e) {
                        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.INIT_FONT_COULD_NOT_BE_LOADED, font.fontFile.getPath(), e);
                    }
                }
            }
        }

        return renderer;
    }

    public boolean dropDOMMutator(FSDOMMutator domMutator) {
        return state._domMutators.remove(domMutator);
    }

    public void dropDOMMutators() {
        state._domMutators.clear();
    }

    public boolean dropFont(String fontFamily) {
        return dropFont(fontFamily, null, null);
    }

    public boolean dropFont(String fontFamily, Integer fontWeight, FontStyle fontStyle) {
        boolean dropped = false;
        for (Iterator<AddedFont> it = state._fonts.iterator(); it.hasNext();) {
            AddedFont font = it.next();
            if (font.family.equals(fontFamily)
                    && (fontWeight == null || fontWeight.equals(font.weight))
                    && (fontStyle == null || fontStyle.equals(font.style))) {
                it.remove();
                state._fontCache.remove(font.family, font.weight,
                        font.style != null ? font.style.toIdentValue() : null);
                dropped = true;
            }
        }
        return dropped;
    }

    public List<AddedFont> getFonts() {
        return state._fonts;
    }

	/**
	 * An output stream to output the resulting PDF. The caller is required to close
	 * the output stream after calling run.
	 *
	 * @param out
	 * @return
	 */
	public PdfRendererBuilder toStream(OutputStream out) {
		state._os = out;
		return this;
	}

	/**
	 * Set the PDF version, typically we use 1.7. If you set a lower version, it is
	 * your responsibility to make sure no more recent PDF features are used.
	 *
	 * @param version
	 * @return
	 */
	public PdfRendererBuilder usePdfVersion(float version) {
		state._pdfVersion = version;
		return this;
	}

	/**
	 * Set the PDF/A conformance, typically we use PDF/A-1
	 * 
	 * Note: PDF/A documents require fonts to be embedded. So if this is not set to NONE,
	 * the built-in fonts will not be available and currently any text without a
	 * specified and embedded font will cause the renderer to crash with an exception.
	 *
	 * @param pdfAConformance
	 * @return
	 */
	public PdfRendererBuilder usePdfAConformance(PdfAConformance pdfAConformance) {
		this.state._pdfAConformance = pdfAConformance;
        if (pdfAConformance.getPdfVersion() != 0f) {
            this.state._pdfVersion = pdfAConformance.getPdfVersion();
        }
		return this;
	}
	
	/**
	 * Whether to conform to PDF/UA or Accessible PDF. False by default.
	 * @param pdfUaAccessibility
	 * @return this for method chaining
	 */
	public PdfRendererBuilder usePdfUaAccessbility(boolean pdfUaAccessibility) {
	    this.state._pdfUaConform = pdfUaAccessibility;
	    return this;
	}

	/**
	 * Sets the color profile, needed for PDF/A conformance.
	 *
	 * You can use the sRGB.icc from https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/resources/org/apache/pdfbox/resources/pdfa/
	 *
	 * @param colorProfile
	 * @return
	 */
	public PdfRendererBuilder useColorProfile(byte[] colorProfile) {
		this.state._colorProfile = colorProfile;
		return this;
	}
	
	/**
	 * By default, this project creates an entirely in-memory <code>PDDocument</code>.
	 * The user can use this method to create a document either entirely on-disk
	 * or a mix of in-memory and on-disk using the <code>PDDocument</code> constructor
	 * that takes a <code>MemoryUsageSetting</code>.
	 * @param doc a (usually empty) PDDocument
	 * @return this for method chaining
	 */
	public PdfRendererBuilder usePDDocument(PDDocument doc) {
	    state.pddocument = doc;
	    return this;
	}

	/**
	 * Like {@link BaseRendererBuilder#useFont(FSSupplier, String, Integer, FontStyle, boolean)} but
	 * allows to supply a PDFont directly. Subclass {@link PDFontSupplier} if you need
	 * special font-loading rules (like using a font-cache).
	 */
	public PdfRendererBuilder useFont(PDFontSupplier supplier, String fontFamily, Integer fontWeight,
			FontStyle fontStyle, boolean subset) {
		state._fonts.add(new AddedFont(supplier, fontWeight, fontFamily, subset, fontStyle, EnumSet.of(FSFontUseCase.DOCUMENT)));
		return this;
	}
	
	/**
	 * Simpler overload for 
	 * {@link #useFont(PDFontSupplier, String, Integer, FontStyle, boolean)}
	 */
	public PdfRendererBuilder useFont(PDFontSupplier supplier, String fontFamily) {
		return this.useFont(supplier, fontFamily, 400, FontStyle.NORMAL, true);
	}

	/**
	 * Set a producer on the output document
	 *
	 * @param producer
	 *            the name of the producer to set defaults to openhtmltopdf.com
	 * @return this for method chaining
	 */
	public PdfRendererBuilder withProducer(String producer) {
		state._producer = producer;
		return this;
	}

	/**
	 * List of caches available.
	 */
	public enum CacheStore {
	    
	    /**
	     * Caches font metrics, based on a combined key of family name, weight and style.
	     * Using this cache avoids loading fallback fonts if the metrics are already in the cache
	     * and the previous fonts contain the needed characters.
	     */
	    PDF_FONT_METRICS;
	}
	
	/**
	 * Use a specific cache. Cache values should be thread safe, so provided your cache store itself
	 * is thread safe can be used accross threads.
	 * @return this for method chaining.
	 * @see CacheStore
	 */
	public PdfRendererBuilder useCacheStore(CacheStore which, FSCacheEx<String, FSCacheValue> cache) {
	    state._caches.put(which, cache);
	    return this;
	}

    public PdfRendererBuilder useFontCache(FontCache fontCache) {
        state._fontCache = fontCache;
        return this;
    }

	/**
	 * Set a PageSupplier that is called whenever a new page is needed.
	 * 
	 * @param pageSupplier 
	 *            {@link PageSupplier} to use
	 * @return this for method chaining.
	 */
	public PdfRendererBuilder usePageSupplier(PageSupplier pageSupplier) {
		state._pageSupplier = pageSupplier;
		return this;
	}

    /**
     * Fast mode is the default from version 1.0.5. You can call this
     * temporary method to use the slow renderer until it is removed in a
     * near future version.
     * 
     * This method is marked deprecated to prompt you to upgrade to the fast
     * renderer.
     * 
     * @return this for method chaining.
     */
    @Deprecated
    public PdfRendererBuilder useSlowMode() {
        state._useFastRenderer = false;
        return this;
    }

	/**
	 * Various level of PDF/A conformance:
	 *
	 * PDF/A-1, PDF/A-2 and PDF/A-3
	 */
	public enum PdfAConformance {
		NONE(-1, "", 0f),
		PDFA_1_A(1, "A", 1.4f), PDFA_1_B(1, "B", 1.4f),
		PDFA_2_A(2, "A", 1.7f), PDFA_2_B(2, "B", 1.7f), PDFA_2_U(2, "U", 1.7f),
		PDFA_3_A(3, "A", 1.7f), PDFA_3_B(3, "B", 1.7f), PDFA_3_U(3, "U", 1.7f);

		PdfAConformance(int part, String value, float pdfVersion) {
			this.part = part;
			this.value = value;
			this.pdfVersion = pdfVersion;
		}

		private final int part;
		private final String value;
		private final float pdfVersion;
		
		public String getConformanceValue() {
		    return this.value;
		}

		public int getPart() {
			return this.part;
		}

		public float getPdfVersion() {
			return this.pdfVersion;
		}
	}
}

