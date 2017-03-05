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
	
	public Java2DRendererBuilder useLayoutGraphics(Graphics2D g2d) {
		this._layoutGraphics = g2d;
		return this;
	}

	public Java2DRendererBuilder usePageProcessor(FSPageProcessor pageProcessor) {
		this._pageProcessor = pageProcessor;
		return this;
	}
	
	public Java2DRenderer buildJava2DRenderer() {
        UnicodeImplementation unicode = new UnicodeImplementation(_reorderer, _splitter, _lineBreaker, 
                _unicodeToLowerTransformer, _unicodeToUpperTransformer, _unicodeToTitleTransformer, _textDirection, _charBreaker);

        PageDimensions pageSize = new PageDimensions(_pageWidth, _pageHeight, _isPageSizeInches);
        
        BaseDocument doc = new BaseDocument(_baseUri, _html, _document, _file, _uri);
        
        return new Java2DRenderer(doc, unicode, _httpStreamFactory, _resolver, _cache, _svgImpl, pageSize, _replacementText, _testMode, _pageProcessor, _layoutGraphics);
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
