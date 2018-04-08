package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PdfRendererBuilder extends BaseRendererBuilder<PdfRendererBuilder> {
	private final List<AddedFont> _fonts = new ArrayList<AddedFont>();
	private OutputStream _os;
	private float _pdfVersion = 1.7f;
	private String _producer;
	private PDDocument pddocument;

	/**
	 * Run the XHTML/XML to PDF conversion and output to an output stream set by
	 * toStream.
	 *
	 * @throws Exception
	 */
	public void run() throws Exception {
		PdfBoxRenderer renderer = null;
		try {
			renderer = this.buildPdfRenderer();
			renderer.layout();
			renderer.createPDF();
		} finally {
			if (renderer != null)
				renderer.close();
		}
	}

	/**
	 * Returns an instance of PdfBoxUserAgentFactory in case you<br/>
	 * need to use custom implementation of {@link PdfBoxUserAgent} class
	 * @return PdfBoxUserAgentFactory instance
	 */
	public PdfBoxUserAgentFactory getPdfBoxUserAgentFactory() {
		return PdfBoxUserAgentFactory.instance();
	}

	/**
	 * Build a PdfBoxRenderer for further customization. Remember to call
	 * {@link PdfBoxRenderer#cleanup()} after use.
	 *
	 * @return
	 */
	public PdfBoxRenderer buildPdfRenderer() {
		UnicodeImplementation unicode = new UnicodeImplementation(_reorderer, _splitter, _lineBreaker,
				_unicodeToLowerTransformer, _unicodeToUpperTransformer, _unicodeToTitleTransformer, _textDirection,
				_charBreaker);

		PageDimensions pageSize = new PageDimensions(_pageWidth, _pageHeight, _isPageSizeInches);

		BaseDocument doc = new BaseDocument(_baseUri, _html, _document, _file, _uri);

		PdfBoxRenderer renderer = new PdfBoxRenderer(doc, unicode, _httpStreamFactory, _os, _resolver, _cache, _svgImpl,
				pageSize, _pdfVersion, _replacementText, _testMode, _objectDrawerFactory,
				_preferredTransformerFactoryImplementationClass,  _preferredDocumentBuilderFactoryImplementationClass,
				_producer, _mathmlImpl, _domMutators, pddocument);

		/*
		 * Register all Fonts
		 */
		PdfBoxFontResolver resolver = renderer.getFontResolver();
		for (AddedFont font : _fonts) {
			IdentValue fontStyle = null;

			if (font.style == FontStyle.NORMAL) {
				fontStyle = IdentValue.NORMAL;
			} else if (font.style == FontStyle.ITALIC) {
				fontStyle = IdentValue.ITALIC;
			} else if (font.style == FontStyle.OBLIQUE) {
				fontStyle = IdentValue.OBLIQUE;
			}

			if (font.supplier != null) {
				resolver.addFont(font.supplier, font.family, font.weight, fontStyle, font.subset);
			} else {
				try {
					resolver.addFont(font.fontFile, font.family, font.weight, fontStyle, font.subset);
				} catch (Exception e) {
					XRLog.init(Level.WARNING, "Font " + font.fontFile + " could not be loaded", e);
				}
			}
		}

		return renderer;
	}

	/**
	 * An output stream to output the resulting PDF. The caller is required to close
	 * the output stream after calling run.
	 *
	 * @param out
	 * @return
	 */
	public PdfRendererBuilder toStream(OutputStream out) {
		this._os = out;
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
		this._pdfVersion = version;
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
	    this.pddocument = doc;
	    return this;
	}

	/**
	 * Add a font programmatically. If the font is NOT subset, it will be downloaded
	 * when the renderer is run, otherwise the font will only be downloaded if
	 * needed. Therefore, the user could add many fonts, confidant that only those
	 * that are used will be downloaded and processed.
	 *
	 * The InputStream returned by the supplier will be closed by the caller. Fonts
	 * should generally be subset, except when used in form controls. FSSupplier is
	 * a lambda compatible interface.
	 *
	 * Fonts can also be added using a font-face at-rule in the CSS.
	 *
	 * @param supplier
	 * @param fontFamily
	 * @param fontWeight
	 * @param fontStyle
	 * @param subset
	 * @return
	 */
	public PdfRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily, Integer fontWeight,
			FontStyle fontStyle, boolean subset) {
		this._fonts.add(new AddedFont(supplier, null, fontWeight, fontFamily, subset, fontStyle));
		return this;
	}

	/**
	 * Simpler overload for
	 * {@link #useFont(FSSupplier, String, Integer, FontStyle, boolean)}
	 *
	 * @param supplier
	 * @param fontFamily
	 * @return
	 */
	public PdfRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily) {
		return this.useFont(supplier, fontFamily, 400, FontStyle.NORMAL, true);
	}

	/**
	 * Like {@link #useFont(FSSupplier, String, Integer, FontStyle, boolean)}, but
	 * allows to supply a font file. If the font file is a .ttc file it is handled
	 * as TrueTypeCollection. If you have the font in file form you should use this
	 * API.
	 */
	public PdfRendererBuilder useFont(File fontFile, String fontFamily, Integer fontWeight, FontStyle fontStyle,
			boolean subset) {
		this._fonts.add(new AddedFont(null, fontFile, fontWeight, fontFamily, subset, fontStyle));
		return this;
	}

	/**
	 * Simpler overload for
	 * {@link #useFont(File, String, Integer, FontStyle, boolean)}
	 *
	 * @param fontFile
	 * @param fontFamily
	 * @return
	 */
	public PdfRendererBuilder useFont(File fontFile, String fontFamily) {
		return this.useFont(fontFile, fontFamily, 400, FontStyle.NORMAL, true);
	}


	/**
	 * Set a producer on the output document
	 *
	 * @param producer
	 *            the name of the producer to set defaults to openhtmltopdf.com
	 * @return this for method chaining
	 */
	public PdfRendererBuilder withProducer(String producer) {
		this._producer = producer;
		return this;
	}


	private static class AddedFont {
		private final FSSupplier<InputStream> supplier;
		private final File fontFile;
		private final Integer weight;
		private final String family;
		private final boolean subset;
		private final FontStyle style;

		private AddedFont(FSSupplier<InputStream> supplier, File fontFile, Integer weight, String family,
				boolean subset, FontStyle style) {
			this.supplier = supplier;
			this.fontFile = fontFile;
			this.weight = weight;
			this.family = family;
			this.subset = subset;
			this.style = style;
		}
	}

}
