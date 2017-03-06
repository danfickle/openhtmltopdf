package com.openhtmltopdf.java2d.api;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.swing.EmptyReplacedElement;
import com.openhtmltopdf.java2d.Java2DRenderer;

import org.w3c.dom.Document;

import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Build a Java2D renderer for a given HTML. The renderer allows to get a
 * BufferedImage of the HTML and to render it in components (using Graphics2D).
 */
public class Java2DRendererBuilder {
	private HttpStreamFactory _httpStreamFactory;
	private FSCache _cache;
	private FSUriResolver _resolver;
	private String _html;
	private String _baseUri;
	private Document _document;
	private SVGDrawer _svgImpl;
	private String _replacementText;
	private FSTextBreaker _lineBreaker;
	private FSTextBreaker _charBreaker;
	private FSTextTransformer _unicodeToUpperTransformer;
	private FSTextTransformer _unicodeToLowerTransformer;
	private FSTextTransformer _unicodeToTitleTransformer;
    private BidiSplitterFactory _splitter;
    private BidiReorderer _reorderer;
    private boolean _textDirection = false;
    private Float _pageWidth;
    private Float _pageHeight;
    private boolean _isPageSizeInches;
    private FSPageProcessor _pageProcessor;
    private String _uri;
    private File _file;
    private boolean _testMode = false;
    private Graphics2D _layoutGraphics;
    private int _initialPageNumber;
    
    public static enum TextDirection { RTL, LTR; }
    public static enum PageSizeUnits { MM, INCHES }
    public static enum FontStyle { NORMAL, ITALIC, OBLIQUE }
    
    private static class AddedFont {
        private final FSSupplier<InputStream> supplier;
        private final Integer weight;
        private final String family;
        private final FontStyle style;
        
        private AddedFont(FSSupplier<InputStream> supplier, Integer weight, String family, FontStyle style) {
            this.supplier = supplier;
            this.weight = weight;
            this.family = family;
            this.style = style;
        }
    }
    
    private List<AddedFont> _fonts = new ArrayList<AddedFont>();

	/**
	 * Provides an HttpStreamFactory implementation if the user desires to use
	 * an external HTTP/HTTPS implementation. Uses URL::openStream by default.
	 *
	 * @param factory
	 * @return
	 */
	public Java2DRendererBuilder useHttpStreamImplementation(HttpStreamFactory factory) {
		this._httpStreamFactory = factory;
		return this;
	}

	/**
	 * Provides a uri resolver to resolve relative uris or private uri schemes.
	 *
	 * @param resolver
	 * @return
	 */
	public Java2DRendererBuilder useUriResolver(FSUriResolver resolver) {
		this._resolver = resolver;
		return this;
	}

	/**
	 * Provides an external cache which can choose to cache items between runs,
	 * such as fonts or logo images.
	 *
	 * @param cache
	 * @return
	 */
	public Java2DRendererBuilder useCache(FSCache cache) {
		this._cache = cache;
		return this;
	}

	/**
	 * Provides a string containing XHTML/XML to convert to image.
	 *
	 * @param html
	 * @param baseUri
	 * @return
	 */
	public Java2DRendererBuilder withHtmlContent(String html, String baseUri) {
		this._html = html;
		this._baseUri = baseUri;
		return this;
	}

	/**
	 * Provides a w3c DOM Document acquired from an external source.
	 *
	 * @param doc
	 * @param baseUri
	 * @return
	 */
	public Java2DRendererBuilder withW3cDocument(org.w3c.dom.Document doc, String baseUri) {
		this._document = doc;
		this._baseUri = baseUri;
		return this;
	}
	
    /**
     * Provides a URI to convert to image. The URI MUST point to a strict XHTML/XML document.
     * @param uri
     * @return
     */
    public Java2DRendererBuilder withUri(String uri) {
        this._uri = uri;
        return this;
    }

	/**
	 * Uses the specified SVG drawer implementation.
	 *
	 * @param svgImpl
	 * @return
	 */
	public Java2DRendererBuilder useSVGDrawer(SVGDrawer svgImpl) {
		this._svgImpl = svgImpl;
		return this;
	}
	
	/**
	 * Compulsory method. The layout graphics are used to measure text and should be from an image or device with
	 * the same characteristics as the output graphicsw provided by the page processor.
	 * @param g2d
	 * @return
	 */
	public Java2DRendererBuilder useLayoutGraphics(Graphics2D g2d) {
		this._layoutGraphics = g2d;
		return this;
	}
	
    /**
     * The default text direction of the document. LTR by default.
     * @param textDirection
     * @return
     */
    public Java2DRendererBuilder defaultTextDirection(TextDirection textDirection) {
        this._textDirection = textDirection == TextDirection.RTL;
        return this;
    }

    /**
     * Whether to use test mode which will output box boundaries on the result. Turned off by default.
     * @param mode
     * @return
     */
    public Java2DRendererBuilder testMode(boolean mode) {
        this._testMode = mode;
        return this;
    }
    
    /**
     * Provides a text splitter to split text into directional runs. Does nothing by default.
     * @param splitter
     * @return
     */
    public Java2DRendererBuilder useUnicodeBidiSplitter(BidiSplitterFactory splitter) {
        this._splitter = splitter;
        return this;
    }
    
    /**
     * Provides a reorderer to properly reverse RTL text. No-op by default.
     * @param reorderer
     * @return
     */
    public Java2DRendererBuilder useUnicodeBidiReorderer(BidiReorderer reorderer) {
        this._reorderer = reorderer;
        return this;
    }
	
    /**
     * Provides a file to convert to PDF. The file MUST contain XHTML/XML in UTF-8 encoding.
     * @param file
     * @return
     */
    public Java2DRendererBuilder withFile(File file) {
        this._file = file;
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
    public Java2DRendererBuilder useReplacementText(String replacement) {
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
    public Java2DRendererBuilder useUnicodeLineBreaker(FSTextBreaker breaker) {
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
    public Java2DRendererBuilder useUnicodeCharacterBreaker(FSTextBreaker breaker) {
        this._charBreaker = breaker;
        return this;
    }
    
    /**
     * Specify a transformer to use to upper case strings.
     * By default <code>String::toUpperCase(Locale.US)</code> is used.
     * @param tr
     * @return
     */
    public Java2DRendererBuilder useUnicodeToUpperTransformer(FSTextTransformer tr) {
        this._unicodeToUpperTransformer = tr;
        return this;
    }

    /**
     * Specify a transformer to use to lower case strings.
     * By default <code>String::toLowerCase(Locale.US)</code> is used.
     * @param tr
     * @return
     */
    public Java2DRendererBuilder useUnicodeToLowerTransformer(FSTextTransformer tr) {
        this._unicodeToLowerTransformer = tr;
        return this;
    }
    
    /**
     * Specify a transformer to title case strings.
     * By default a best effort implementation (non locale aware) is used.
     * @param tr
     * @return
     */
    public Java2DRendererBuilder useUnicodeToTitleTransformer(FSTextTransformer tr) {
        this._unicodeToTitleTransformer = tr;
        return this;
    }
    
    /**
     * Add a font programmatically. The font will only be downloaded if needed. 
     * 
     * The InputStream returned by the supplier will be closed by the caller. 
     * FSSupplier is a lambda compatible interface.
     * 
     * Fonts can also be added using a font-face at-rule in the CSS.
     * @param supplier
     * @param fontFamily
     * @param fontWeight
     * @param fontStyle
     * @param subset
     * @return
     */
    public Java2DRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily, Integer fontWeight, FontStyle fontStyle) {
        this._fonts.add(new AddedFont(supplier, fontWeight, fontFamily, fontStyle));
        return this;
    }
    
    /**
     * Simpler overload for {@link #useFont(FSSupplier, String, Integer, FontStyle)}
     * @param supplier
     * @param fontFamily
     * @return
     */
    public Java2DRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily) {
        return this.useFont(supplier, fontFamily, 400, FontStyle.NORMAL);
    }
    
    /**
     * Specifies the default page size to use if none is specified in CSS.
     * @param pageWidth
     * @param pageHeight
     * @param units either mm or inches.
     * @see {@link #PAGE_SIZE_LETTER_WIDTH}, {@link #PAGE_SIZE_LETTER_HEIGHT} and {@link #PAGE_SIZE_LETTER_UNITS}
     * @return
     */
    public Java2DRendererBuilder useDefaultPageSize(float pageWidth, float pageHeight, PageSizeUnits units) {
        this._pageWidth = pageWidth;
        this._pageHeight = pageHeight;
        this._isPageSizeInches = (units == PageSizeUnits.INCHES);
        return this;
    }
    
    /**
     * Used to set an initial page number for use with page counters, etc.
     * @param pageNumberInitial
     * @return
     */
    public Java2DRendererBuilder useInitialPageNumber(int pageNumberInitial) {
    	this._initialPageNumber = pageNumberInitial;
    	return this;
    }

	/**
	 * Output the document in paged format. The user can use the DefaultPageProcessor or use its source
	 * as a reference to code their own page processor for advanced usage.
	 * @param pageProcessor
	 * @return
	 */
	public Java2DRendererBuilder toPageProcessor(FSPageProcessor pageProcessor) {
		this._pageProcessor = pageProcessor;
		return this;
	}
	
	/**
	 * <code>useLayoutGraphics</code> and <code>toPageProcessor</code> MUST have been called.
	 * Also a document MUST have been set with one of the with* methods.
	 * This will build the renderer and output each page of the document to the specified page 
	 * processor.
	 * @throws Exception
	 */
	public void runPaged() throws Exception {
		Java2DRenderer renderer = this.buildJava2DRenderer();
		renderer.layout();
		renderer.writePages();
	}

	/**
	 * <code>useLayoutGraphics</code> and <code>toPageProcessor</code> MUST have been called.
	 * Also a document MUST have been set with one of the with* methods.
	 * This will build the renderer and output the first page of the document to the specified page 
	 * processor.
	 * @throws Exception
	 */
	public void runFirstPage() throws Exception {
		Java2DRenderer renderer = this.buildJava2DRenderer();
		renderer.layout();
		renderer.writePage(0);
	}
	
	public Java2DRenderer buildJava2DRenderer() {
        UnicodeImplementation unicode = new UnicodeImplementation(_reorderer, _splitter, _lineBreaker, 
                _unicodeToLowerTransformer, _unicodeToUpperTransformer, _unicodeToTitleTransformer, _textDirection, _charBreaker);

        PageDimensions pageSize = new PageDimensions(_pageWidth, _pageHeight, _isPageSizeInches);
        
        BaseDocument doc = new BaseDocument(_baseUri, _html, _document, _file, _uri);
        
        return new Java2DRenderer(doc, unicode, _httpStreamFactory, _resolver, _cache, _svgImpl, pageSize, _replacementText, _testMode, _pageProcessor, _layoutGraphics, _initialPageNumber);
    }

	public static abstract class Graphics2DPaintingReplacedElement extends EmptyReplacedElement {
		protected Graphics2DPaintingReplacedElement(int width, int height) {
			super(width, height);
		}

		public abstract void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
				double height);

		public static double DOTS_PER_INCH = 72.0;
	}
}
