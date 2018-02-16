package com.openhtmltopdf.java2d.api;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.java2d.Java2DRenderer;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.swing.EmptyReplacedElement;

/**
 * Build a Java2D renderer for a given HTML. The renderer allows to get a
 * BufferedImage of the HTML and to render it in components (using Graphics2D).
 */
public class Java2DRendererBuilder extends BaseRendererBuilder<Java2DRendererBuilder> {
	protected Graphics2D _layoutGraphics;
	protected FSPageProcessor _pageProcessor;
	private List<AddedFont> _fonts = new ArrayList<AddedFont>();

	/**
	 * Compulsory method. The layout graphics are used to measure text and should be
	 * from an image or device with the same characteristics as the output graphicsw
	 * provided by the page processor.
	 *
	 * @param g2d
	 * @return
	 */
	public Java2DRendererBuilder useLayoutGraphics(Graphics2D g2d) {
		this._layoutGraphics = g2d;
		return this;
	}

	/**
	 * Add a font programmatically. The font will only be downloaded if needed.
	 *
	 * The InputStream returned by the supplier will be closed by the caller.
	 * FSSupplier is a lambda compatible interface.
	 *
	 * Fonts can also be added using a font-face at-rule in the CSS.
	 *
	 * @param supplier
	 * @param fontFamily
	 * @param fontWeight
	 * @param fontStyle
	 * @return
	 */
	public Java2DRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily, Integer fontWeight,
			FontStyle fontStyle) {
		this._fonts.add(new AddedFont(supplier, fontWeight, fontFamily, fontStyle));
		return this;
	}

	/**
	 * Simpler overload for {@link #useFont(FSSupplier, String, Integer, FontStyle)}
	 *
	 * @param supplier
	 * @param fontFamily
	 * @return
	 */
	public Java2DRendererBuilder useFont(FSSupplier<InputStream> supplier, String fontFamily) {
		return this.useFont(supplier, fontFamily, 400, FontStyle.NORMAL);
	}

	/**
	 * Used to set an initial page number for use with page counters, etc.
	 *
	 * @param pageNumberInitial
	 * @return
	 */
	public Java2DRendererBuilder useInitialPageNumber(int pageNumberInitial) {
		this._initialPageNumber = pageNumberInitial;
		return this;
	}

	/**
	 * Render everything to a single page. I.e. only one big page is genereated, no
	 * pagebreak will be done. The page is only as height as needed.
	 */
	public Java2DRendererBuilder toSinglePage(FSPageProcessor pageProcessor) {
		this._pagingMode = Layer.PAGED_MODE_SCREEN;
		this._pageProcessor = pageProcessor;
		return this;
	}

	/**
	 * Output the document in paged format. The user can use the
	 * DefaultPageProcessor or use its source as a reference to code their own page
	 * processor for advanced usage.
	 *
	 * @param pageProcessor
	 * @return
	 */
	public Java2DRendererBuilder toPageProcessor(FSPageProcessor pageProcessor) {
		this._pagingMode = Layer.PAGED_MODE_PRINT;
		this._pageProcessor = pageProcessor;
		return this;
	}

	/**
	 * <code>useLayoutGraphics</code> and <code>toPageProcessor</code> MUST have
	 * been called. Also a document MUST have been set with one of the with*
	 * methods. This will build the renderer and output each page of the document to
	 * the specified page processor.
	 *
	 * @throws Exception
	 */
	public void runPaged() throws Exception {
		Java2DRenderer renderer = this.buildJava2DRenderer();
		renderer.layout();
		if (_pagingMode == Layer.PAGED_MODE_PRINT)
			renderer.writePages();
		else
			renderer.writeSinglePage();
	}

	/**
	 * <code>useLayoutGraphics</code> and <code>toPageProcessor</code> MUST have
	 * been called. Also a document MUST have been set with one of the with*
	 * methods. This will build the renderer and output the first page of the
	 * document to the specified page processor.
	 *
	 * @throws Exception
	 */
	public void runFirstPage() throws Exception {
		Java2DRenderer renderer = this.buildJava2DRenderer();
		renderer.layout();
		if (_pagingMode == Layer.PAGED_MODE_PRINT)
			renderer.writePage(0);
		else
			renderer.writeSinglePage();
	}

	public Java2DRenderer buildJava2DRenderer() {
		UnicodeImplementation unicode = new UnicodeImplementation(_reorderer, _splitter, _lineBreaker,
				_unicodeToLowerTransformer, _unicodeToUpperTransformer, _unicodeToTitleTransformer, _textDirection,
				_charBreaker);

		PageDimensions pageSize = new PageDimensions(_pageWidth, _pageHeight, _isPageSizeInches);

		BaseDocument doc = new BaseDocument(_baseUri, _html, _document, _file, _uri);

		/*
		 * If no layout graphics is provied, just use a sane default
		 */
		if (_layoutGraphics == null) {
			BufferedImage bf = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
			_layoutGraphics = bf.createGraphics();
		}

		return new Java2DRenderer(doc, unicode, _httpStreamFactory, _resolver, _cache, _svgImpl, _mathmlImpl, pageSize,
				_replacementText, _testMode, _pageProcessor, _layoutGraphics, _initialPageNumber, _pagingMode,
				_objectDrawerFactory, _preferredTransformerFactoryImplementationClass, _domMutators);
	}

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

	public static abstract class Graphics2DPaintingReplacedElement extends EmptyReplacedElement {
		protected Graphics2DPaintingReplacedElement(int width, int height) {
			super(width, height);
		}

		public abstract void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
				double height);

		public static double DOTS_PER_INCH = 72.0;
	}
}
