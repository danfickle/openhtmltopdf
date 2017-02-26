package com.openhtmltopdf.simple;

import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.swing.EmptyReplacedElement;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.swing.SwingReplacedElementFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Build a Java2D renderer for a given
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
	 * Provides a string containing XHTML/XML to convert to PDF.
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
	 * Built renderer, which can be used to render the document as image or to a
	 * Graphics2D
	 */
	public interface IJava2DRenderer {
		/**
		 * Builds an Image
		 */
		BufferedImage renderToImage(int width, int bufferedImageType);

		/**
		 * Layout the HTML for the given width
		 */
		void layout(int width);

		/**
		 * Draw the HTML on the given Graphics2D. It will be drawn on (0,0). So
		 * if you want the rendering somewhere else you should first apply the
		 * needed transforms and or clippings on gfx before calling this method.
		 * 
		 * Note: You must call layout() at least once before calling this
		 * method!
		 * 
		 * @param gfx
		 *            graphics 2D to draw to.
		 */
		void render(Graphics2D gfx);

	}

	/**
	 * Build a renderer
	 *
	 * @return a renderer which can be used to create images or draw to a
	 *         Graphics2D
	 */
	public IJava2DRenderer build() {
		final XHTMLPanel panel = new XHTMLPanel();
		panel.setInteractive(false);

		NaiveUserAgent userAgent = new NaiveUserAgent();
		userAgent.setBaseURL(_baseUri);
		if (_httpStreamFactory != null)
			userAgent.setHttpStreamFactory(_httpStreamFactory);

		if (_resolver != null)
			userAgent.setUriResolver(_resolver);

		if (_cache != null)
			userAgent.setExternalCache(_cache);

		panel.getSharedContext().setUserAgentCallback(userAgent);
		Java2DReplacedElementFactory ref = new Java2DReplacedElementFactory();
		ref._svgImpl = _svgImpl;
		panel.getSharedContext().setReplacedElementFactory(ref);

		if (_html != null)
			panel.setDocumentFromString(_html, _baseUri, new XhtmlNamespaceHandler());
		if (_document != null)
			panel.setDocument(_document, _baseUri, new XhtmlNamespaceHandler());

		return new IJava2DRenderer() {

			@Override
			public BufferedImage renderToImage(int width, int bufferedImageType) {
				layout(width);

				// get size
				Rectangle rect;
				if (panel.getPreferredSize() != null) {
					rect = new Rectangle(0, 0, (int) panel.getPreferredSize().getWidth(),
							(int) panel.getPreferredSize().getHeight());
				} else {
					rect = new Rectangle(0, 0, panel.getWidth(), panel.getHeight());
				}

				// render into real buffer
				BufferedImage buff = new BufferedImage((int) rect.getWidth(), (int) rect.getHeight(),
						bufferedImageType);
				Graphics2D g = (Graphics2D) buff.getGraphics();
				if (buff.getColorModel().hasAlpha()) {
					g.clearRect(0, 0, (int) rect.getWidth(), (int) rect.getHeight());
				} else {
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, (int) rect.getWidth(), (int) rect.getHeight());
				}
				render(g);
				g.dispose();

				return buff;
			}

			@Override
			public void layout(int width) {
				Dimension dim = new Dimension(width, 100);

				// do layout with temp buffer
				BufferedImage buff = new BufferedImage((int) dim.getWidth(), (int) dim.getHeight(),
						BufferedImage.TYPE_3BYTE_BGR);
				Graphics2D g = (Graphics2D) buff.getGraphics();
				panel.setSize(dim);
				panel.doDocumentLayout(g);
				g.dispose();

			}

			@Override
			public void render(Graphics2D gfx) {
				panel.paintComponent(gfx);
			}
		};
	}

	public static abstract class Graphics2DPaintingReplacedElement extends EmptyReplacedElement {
		protected Graphics2DPaintingReplacedElement(int width, int height) {
			super(width, height);
		}

		public abstract void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
				double height);

		public static double DOTS_PER_INCH = 72.0;
	}

	private static class Java2DSVGReplacedElement extends Graphics2DPaintingReplacedElement {
		private final SVGDrawer _svgImpl;
		private final Element e;

		public Java2DSVGReplacedElement(Element e, SVGDrawer svgImpl, int width, int height) {
			super(width, height);
			this.e = e;
			this._svgImpl = svgImpl;
		}

		@Override
		public void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
				double height) {
			_svgImpl.drawSVG(e, outputDevice, ctx, x, y, width, height, DOTS_PER_INCH);
		}

		@Override
		public int getIntrinsicWidth() {
			if (super.getIntrinsicWidth() >= 0) {
				// CSS takes precedence over width and height defined on
				// element.
				return super.getIntrinsicWidth();
			} else {
				// Seems to need dots rather than pixels.
				return this._svgImpl.getSVGWidth(e);
			}
		}

		@Override
		public int getIntrinsicHeight() {
			if (super.getIntrinsicHeight() >= 0) {
				// CSS takes precedence over width and height defined on
				// element.
				return super.getIntrinsicHeight();
			} else {
				// Seems to need dots rather than pixels.
				return this._svgImpl.getSVGHeight(e);
			}
		}
	}


	public static class Java2DReplacedElementFactory extends SwingReplacedElementFactory {
		private SVGDrawer _svgImpl;

		@Override
		public ReplacedElement createReplacedElement(LayoutContext context, BlockBox box, UserAgentCallback uac,
				int cssWidth, int cssHeight) {
			Element e = box.getElement();
			if (e == null) {
				return null;
			}

			String nodeName = e.getNodeName();
			if (nodeName.equals("svg") && _svgImpl != null)
				return new Java2DSVGReplacedElement(e, _svgImpl, cssWidth, cssHeight);

			/*
			 * Default: Just let the base class handle everything
			 */
			return super.createReplacedElement(context, box, uac, cssWidth, cssHeight);
		}
	}

}
