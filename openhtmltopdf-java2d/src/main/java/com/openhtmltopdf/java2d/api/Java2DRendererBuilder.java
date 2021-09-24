package com.openhtmltopdf.java2d.api;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

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
public class Java2DRendererBuilder extends BaseRendererBuilder<Java2DRendererBuilder, Java2DRendererBuilderState> {
	public Java2DRendererBuilder() {
		super(new Java2DRendererBuilderState());
	}

	/**
	 * Compulsory method. The layout graphics are used to measure text and should be
	 * from an image or device with the same characteristics as the output graphicsw
	 * provided by the page processor.
	 *
	 * @param g2d
	 * @return this for method chaining
	 */
	public Java2DRendererBuilder useLayoutGraphics(Graphics2D g2d) {
		state._layoutGraphics = g2d;
		return this;
	}

    /**
     * Whether to use fonts available in the environment. Enabling environment fonts may mean different text
     * rendering behavior across different environments. The default is not to use environment fonts.
     */
    public Java2DRendererBuilder useEnvironmentFonts(boolean useEnvironmentFonts) {
        state._useEnvironmentFonts = useEnvironmentFonts;
        return this;
    }

	/**
	 * Render everything to a single page. I.e. only one big page is genereated, no
	 * pagebreak will be done. The page is only as height as needed.
	 */
	public Java2DRendererBuilder toSinglePage(FSPageProcessor pageProcessor) {
		state._pagingMode = Layer.PAGED_MODE_SCREEN;
		state._pageProcessor = pageProcessor;
		return this;
	}

	/**
	 * Output the document in paged format. The user can use the
	 * DefaultPageProcessor or use its source as a reference to code their own page
	 * processor for advanced usage.
	 *
	 * @param pageProcessor
	 * @return this for method chaining
	 */
	public Java2DRendererBuilder toPageProcessor(FSPageProcessor pageProcessor) {
		state._pagingMode = Layer.PAGED_MODE_PRINT;
		state._pageProcessor = pageProcessor;
		return this;
	}

	/**
	 * <code>useLayoutGraphics</code> and <code>toPageProcessor</code> MUST have
	 * been called. Also a document MUST have been set with one of the with*
	 * methods. This will build the renderer and output each page of the document to
	 * the specified page processor.
	 */
	public void runPaged() throws IOException {
		try (Closeable d = this.applyDiagnosticConsumer(); Java2DRenderer renderer = this.buildJava2DRenderer(d)) {
			renderer.layout();
			if (state._pagingMode == Layer.PAGED_MODE_PRINT)
				renderer.writePages();
			else
				renderer.writeSinglePage();
		}
	}

	/**
	 * <code>useLayoutGraphics</code> and <code>toPageProcessor</code> MUST have
	 * been called. Also a document MUST have been set with one of the with*
	 * methods. This will build the renderer and output the first page of the
	 * document to the specified page processor.
	 */
	public void runFirstPage() throws IOException {
		try (Closeable d = this.applyDiagnosticConsumer();
		     Java2DRenderer renderer = this.buildJava2DRenderer(d)) {
			renderer.layout();
			if (state._pagingMode == Layer.PAGED_MODE_PRINT)
				renderer.writePage(0);
			else
				renderer.writeSinglePage();
		}
	}

	public Java2DRenderer buildJava2DRenderer() {
		return buildJava2DRenderer(this.applyDiagnosticConsumer());
	}

	public Java2DRenderer buildJava2DRenderer(Closeable diagnosticConsumer) {

		UnicodeImplementation unicode = new UnicodeImplementation(state._reorderer, state._splitter, state._lineBreaker,
				state._unicodeToLowerTransformer, state._unicodeToUpperTransformer, state._unicodeToTitleTransformer, state._textDirection,
				state._charBreaker);

		PageDimensions pageSize = new PageDimensions(state._pageWidth, state._pageHeight, state._isPageSizeInches);

		BaseDocument doc = new BaseDocument(state._baseUri, state._html, state._document, state._file, state._uri);

		/*
		 * If no layout graphics is provied, just use a sane default
		 */
		if (state._layoutGraphics == null) {
			BufferedImage bf = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
			state._layoutGraphics = bf.createGraphics();
		}

		return new Java2DRenderer(doc, unicode,  pageSize, state, diagnosticConsumer);
	}

	/**
	 * This class is internal to this library, please do not use or override it!
	 */
	public static abstract class Graphics2DPaintingReplacedElement extends EmptyReplacedElement {
		protected Graphics2DPaintingReplacedElement(int width, int height) {
			super(width, height);
		}

		public abstract void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
				double height);

		public static double DOTS_PER_INCH = 72.0;
	}
}

