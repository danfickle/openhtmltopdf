package com.openhtmltopdf.pdfboxout;

import java.io.File;
import java.io.OutputStream;

import org.w3c.dom.Document;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.extend.FSCache;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.extend.FSUriResolver;
import com.openhtmltopdf.extend.HttpStreamFactory;
import com.openhtmltopdf.extend.SVGDrawer;

public class PdfRendererBuilder
{
    public static enum TextDirection { RTL, LTR; }
    public static enum PageSizeUnits { MM, INCHES };
    
    public static final float PAGE_SIZE_LETTER_WIDTH = 8.5f;
    public static final float PAGE_SIZE_LETTER_HEIGHT = 11.0f;
    public static final PageSizeUnits PAGE_SIZE_LETTER_UNITS = PageSizeUnits.INCHES;

    private boolean _textDirection = false;
    private boolean _testMode = false;
    private boolean _useSubsets = true;
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
    private FSTextBreaker _lineBreaker;
    
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
        return new PdfBoxRenderer(_textDirection, _testMode, _useSubsets, _httpStreamFactory, _splitter, _reorderer,
                _html, _document, _baseUri, _uri, _file, _os, _resolver, _cache, _svgImpl,
                _pageWidth, _pageHeight, _isPageSizeInches, _pdfVersion, _replacementText,
                _lineBreaker);
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
     * Whether to subset fonts, resulting in a smaller PDF. Turned on by default.
     * @param subset
     * @return
     */
    public PdfRendererBuilder subsetFonts(boolean subset) {
        this._useSubsets = subset;
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
    public PdfRendererBuilder useBidiSplitter(BidiSplitterFactory splitter) {
        this._splitter = splitter;
        return this;
    }
    
    /**
     * Provides a reorderer to properly reverse RTL text. No-op by default.
     * @param reorderer
     * @return
     */
    public PdfRendererBuilder useBidiReorderer(BidiReorderer reorderer) {
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
    public PdfRendererBuilder useLineBreaker(FSTextBreaker breaker) {
        this._lineBreaker = breaker;
        return this;
    }
}
