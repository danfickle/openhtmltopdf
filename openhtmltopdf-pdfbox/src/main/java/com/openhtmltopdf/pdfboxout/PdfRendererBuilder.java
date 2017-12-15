package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.util.XRLog;
import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PdfRendererBuilder
{

    public static enum TextDirection { RTL, LTR; }
    public static enum PageSizeUnits { MM, INCHES }
    public static enum FontStyle { NORMAL, ITALIC, OBLIQUE }
    
    public static final float PAGE_SIZE_LETTER_WIDTH = 8.5f;
    public static final float PAGE_SIZE_LETTER_HEIGHT = 11.0f;
    public static final PageSizeUnits PAGE_SIZE_LETTER_UNITS = PageSizeUnits.INCHES;

    private boolean _textDirection = false;
    private boolean _testMode = false;
    private HttpStreamFactory _httpStreamFactory;
    private BidiSplitterFactory _splitter;
    private BidiReorderer _reorderer;
    private String _html;
    private Document _document;
    private String _baseUri;
    private String _uri;
    private File _file;
    private OutputStream _os;
    private FSUriResolver _resolver;
    private FSCache _cache;
    private SVGDrawer _svgImpl;
    private Float _pageWidth;
    private Float _pageHeight;
    private boolean _isPageSizeInches;
    private float _pdfVersion = 1.7f;
    private String _replacementText;
    private String _producer;
    private FSTextBreaker _lineBreaker;
    private FSTextBreaker _charBreaker;
    private FSTextTransformer _unicodeToUpperTransformer;
    private FSTextTransformer _unicodeToLowerTransformer;
    private FSTextTransformer _unicodeToTitleTransformer;
    private FSObjectDrawerFactory _objectDrawerFactory;
    private String _preferredTransformerFactoryImplementationClass = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
    
    private static class AddedFont {
        private final FSSupplier<InputStream> supplier;
        private final File fontFile;
        private final Integer weight;
        private final String family;
        private final boolean subset;
        private final FontStyle style;
        
        private AddedFont(FSSupplier<InputStream> supplier, File fontFile, Integer weight, String family, boolean subset, FontStyle style) {
            this.supplier = supplier;
            this.fontFile = fontFile;
            this.weight = weight;
            this.family = family;
            this.subset = subset;
            this.style = style;
        }
    }
    
    private final List<AddedFont> _fonts = new ArrayList<AddedFont>();

    /**
     * Run the XHTML/XML to PDF conversion and output to an output stream set by toStream.
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
                renderer.cleanup();
        }
    }
    
    /**
     * Build a PdfBoxRenderer for further customization.
     * Remember to call {@link PdfBoxRenderer#cleanup()} after use.
     * @return
     */
    public PdfBoxRenderer buildPdfRenderer() {
        UnicodeImplementation unicode = new UnicodeImplementation(_reorderer, _splitter, _lineBreaker, 
                _unicodeToLowerTransformer, _unicodeToUpperTransformer, _unicodeToTitleTransformer, _textDirection, _charBreaker);

        PageDimensions pageSize = new PageDimensions(_pageWidth, _pageHeight, _isPageSizeInches);
        
        BaseDocument doc = new BaseDocument(_baseUri, _html, _document, _file, _uri);

        PdfBoxRenderer renderer = new PdfBoxRenderer(
                doc, unicode, _httpStreamFactory, _os, _resolver,
                _cache, _svgImpl, pageSize, _pdfVersion, _replacementText,
                _testMode, _objectDrawerFactory, _preferredTransformerFactoryImplementationClass,
                _producer);

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

			if( font.supplier != null) {
				resolver.addFont(font.supplier, font.family, font.weight, fontStyle, font.subset);
			}
			else {
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
     * The default text direction of the document. LTR by default.
     * @param textDirection
     * @return
     */
    public PdfRendererBuilder defaultTextDirection(TextDirection textDirection) {
        this._textDirection = textDirection == TextDirection.RTL;
        return this;
    }

    /**
     * Whether to use test mode and output the PDF uncompressed. Turned off by default.
     * @param mode
     * @return
     */
    public PdfRendererBuilder testMode(boolean mode) {
        this._testMode = mode;
        return this;
    }
    
    /**
     * Provides an HttpStreamFactory implementation if the user desires to use an external
     * HTTP/HTTPS implementation. Uses URL::openStream by default.
     * @param factory
     * @return
     */
    public PdfRendererBuilder useHttpStreamImplementation(HttpStreamFactory factory) {
        this._httpStreamFactory = factory;
        return this;
    }
    
    /**
     * Provides a uri resolver to resolve relative uris or private uri schemes.
     * @param resolver
     * @return
     */
    public PdfRendererBuilder useUriResolver(FSUriResolver resolver) {
        this._resolver = resolver;
        return this;
    }
    
    /**
     * Provides an external cache which can choose to cache items between runs,
     * such as fonts or logo images.
     * @param cache
     * @return
     */
    public PdfRendererBuilder useCache(FSCache cache) {
        this._cache = cache;
        return this;
    }
    
    /**
     * Provides a text splitter to split text into directional runs. Does nothing by default.
     * @param splitter
     * @return
     */
    public PdfRendererBuilder useUnicodeBidiSplitter(BidiSplitterFactory splitter) {
        this._splitter = splitter;
        return this;
    }
    
    /**
     * Provides a reorderer to properly reverse RTL text. No-op by default.
     * @param reorderer
     * @return
     */
    public PdfRendererBuilder useUnicodeBidiReorderer(BidiReorderer reorderer) {
        this._reorderer = reorderer;
        return this;
    }
    
    /**
     * Provides a string containing XHTML/XML to convert to PDF.
     * @param html
     * @param baseUri
     * @return
     */
    public PdfRendererBuilder withHtmlContent(String html, String baseUri) {
        this._html = html;
        this._baseUri = baseUri;
        return this;
    }

    /**
     * Provides a w3c DOM Document acquired from an external source.
     * @param doc
     * @param baseUri
     * @return
     */
    public PdfRendererBuilder withW3cDocument(org.w3c.dom.Document doc, String baseUri) {
        this._document = doc;
        this._baseUri = baseUri;
        return this;
    }

    /**
     * Provides a URI to convert to PDF. The URI MUST point to a strict XHTML/XML document.
     * @param uri
     * @return
     */
    public PdfRendererBuilder withUri(String uri) {
        this._uri = uri;
        return this;
    }
    
    /**
     * Provides a file to convert to PDF. The file MUST contain XHTML/XML in UTF-8 encoding.
     * @param file
     * @return
     */
    public PdfRendererBuilder withFile(File file) {
        this._file = file;
        return this;
    }

    /**
     * An output stream to output the resulting PDF. The caller is required to close the output stream after calling
     * run.
     * @param out
     * @return
     */
    public PdfRendererBuilder toStream(OutputStream out) {
        this._os = out;
        return this;
    }
    
    /**
     * Uses the specified SVG drawer implementation.
     * @param svgImpl
     * @return
     */
    public PdfRendererBuilder useSVGDrawer(SVGDrawer svgImpl) {
        this._svgImpl = svgImpl;
        return this;
    }

    /**
     * Specifies the default page size to use if none is specified in CSS.
     * @param pageWidth
     * @param pageHeight
     * @param units either mm or inches.
     * @see {@link #PAGE_SIZE_LETTER_WIDTH}, {@link #PAGE_SIZE_LETTER_HEIGHT} and {@link #PAGE_SIZE_LETTER_UNITS}
     * @return
     */
    public PdfRendererBuilder useDefaultPageSize(float pageWidth, float pageHeight, PageSizeUnits units) {
        this._pageWidth = pageWidth;
        this._pageHeight = pageHeight;
        this._isPageSizeInches = (units == PageSizeUnits.INCHES);
        return this;
    }
    
    /**
     * Set the PDF version, typically we use 1.7.
     * If you set a lower version, it is your responsibility to make sure
     * no more recent PDF features are used.
     * @param version
     * @return
     */
    public PdfRendererBuilder usePdfVersion(float version) {
        this._pdfVersion = version;
        return this;
    }
    
    /**
     * The replacement text to use if a character is cannot be renderered by any of the specified fonts.
     * This is not broken across lines so should be one or zero characters for best results.
     * Also, make sure it can be rendered by at least one of your specified fonts!
     * The default is the # character.
     * @param replacement
     * @return
     */
    public PdfRendererBuilder useReplacementText(String replacement) {
        this._replacementText = replacement;
        return this;
    }
    
    /**
     * Specify the line breaker. By default a Java default BreakIterator line instance is used
     * with US locale. Additionally, this is wrapped with UrlAwareLineBreakIterator to also
     * break before the forward slash (/) character so that long URIs can be broken on to multiple lines.
     * 
     * You may want to use a BreakIterator with a different locale (wrapped by UrlAwareLineBreakIterator or not)
     * or a more advanced BreakIterator from icu4j (see the rtl-support module for an example).
     * @param breaker
     * @return
     */
    public PdfRendererBuilder useUnicodeLineBreaker(FSTextBreaker breaker) {
        this._lineBreaker = breaker;
        return this;
    }
    
    /**
     * Specify the character breaker. By default a break iterator character instance is used with 
     * US locale. Currently this is used when <code>word-wrap: break-word</code> is in
     * effect.
     * @param breaker
     * @return
     */
    public PdfRendererBuilder useUnicodeCharacterBreaker(FSTextBreaker breaker) {
        this._charBreaker = breaker;
        return this;
    }
    
    /**
     * Specify a transformer to use to upper case strings.
     * By default <code>String::toUpperCase(Locale.US)</code> is used.
     * @param tr
     * @return
     */
    public PdfRendererBuilder useUnicodeToUpperTransformer(FSTextTransformer tr) {
        this._unicodeToUpperTransformer = tr;
        return this;
    }

    /**
     * Specify a transformer to use to lower case strings.
     * By default <code>String::toLowerCase(Locale.US)</code> is used.
     * @param tr
     * @return
     */
    public PdfRendererBuilder useUnicodeToLowerTransformer(FSTextTransformer tr) {
        this._unicodeToLowerTransformer = tr;
        return this;
    }

    /**
     * Specify a transformer to title case strings.
     * By default a best effort implementation (non locale aware) is used.
     * @param tr
     * @return
     */
    public PdfRendererBuilder useUnicodeToTitleTransformer(FSTextTransformer tr) {
        this._unicodeToTitleTransformer = tr;
        return this;
    }
    
    /**
     * Add a font programmatically. If the font is NOT subset, it will be downloaded when the renderer is run, otherwise
     * the font will only be downloaded if needed. Therefore, the user could add many fonts, confidant that only those
     * that are used will be downloaded and processed. 
     * 
     * The InputStream returned by the supplier will be closed by the caller. Fonts should generally be subset, except
     * when used in form controls. FSSupplier is a lambda compatible interface.
     * 
     * Fonts can also be added using a font-face at-rule in the CSS.
     * @param supplier
     * @param fontFamily
     * @param fontWeight
     * @param fontStyle
     * @param subset
     * @return
     */
    public PdfRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily, Integer fontWeight, FontStyle fontStyle, boolean subset) {
        this._fonts.add(new AddedFont(supplier, null, fontWeight, fontFamily, subset, fontStyle));
        return this;
    }
    
    /**
     * Simpler overload for {@link #useFont(FSSupplier, String, Integer, FontStyle, boolean)}
     * @param supplier
     * @param fontFamily
     * @return
     */
    public PdfRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily) {
        return this.useFont(supplier, fontFamily, 400, FontStyle.NORMAL, true);
    }

    /**
     * Like {@link #useFont(FSSupplier, String, Integer, FontStyle, boolean)}, but allows to supply a font file. If the font file
     * is a .ttc file it is handled as TrueTypeCollection. If you have the font in file form you should use this API.
     */
    public PdfRendererBuilder useFont(File fontFile, String fontFamily, Integer fontWeight, FontStyle fontStyle, boolean subset) {
        this._fonts.add(new AddedFont(null, fontFile, fontWeight, fontFamily, subset, fontStyle));
        return this;
    }

    /**
     * Simpler overload for {@link #useFont(File, String, Integer, FontStyle, boolean)}
     * @param fontFile
     * @param fontFamily
     * @return
     */
    public PdfRendererBuilder useFont(File fontFile, String fontFamily) {
        return this.useFont(fontFile, fontFamily, 400, FontStyle.NORMAL, true);
    }

    /**
     * Set a factory for &lt;object&gt; drawers
     * @param objectDrawerFactory Object Drawer Factory
     * @return this for method chaining
     */
    public PdfRendererBuilder useObjectDrawerFactory(FSObjectDrawerFactory objectDrawerFactory) {
        this._objectDrawerFactory = objectDrawerFactory;
        return this;
    }
    
    /**
     * This method should be considered advanced and is not required for most setups.
     * Set a preferred implementation class for use as javax.xml.transform.TransformerFactory. Use null to let 
     * a default implementation class be used. The default is "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl".
     * This seems to work with most systems but not JBoss Wildfly and related setups. In this case you can use null to let
     * the container use whatever TransformerFactory it has available. 
     * @param transformerFactoryClass
     * @return this for method chaining
     */
    public PdfRendererBuilder useTransformerFactoryImplementationClass(String transformerFactoryClass) {
        this._preferredTransformerFactoryImplementationClass = transformerFactoryClass;
        return this;
    }

    /**
     * Set a producer on the output document
     *
     * @param producer the name of the producer to set defaults to openhtmltopdf.com
     * @return this for method chaining
     */
    public PdfRendererBuilder withProducer(String producer) {
        this._producer = producer;
        return this;
    }

}
