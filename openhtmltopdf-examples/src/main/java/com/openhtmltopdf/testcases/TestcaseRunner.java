package com.openhtmltopdf.testcases;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.openhtmltopdf.latexsupport.LaTeXDOMMutator;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.util.Diagnostic;
import org.apache.pdfbox.io.IOUtils;
import org.w3c.dom.Element;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.java2d.api.BufferedImagePageProcessor;
import com.openhtmltopdf.java2d.api.DefaultPageProcessor;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.objects.StandardObjectDrawerFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.render.DefaultObjectDrawerFactory;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.JDKXRLogger;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;

/**
 * NOTE: This is mostly obsolete.
 * New test cases should go in VisualRegressionTest or NonVisualRegressionTest, etc.
 */
public class TestcaseRunner {


	/**
	 * Runs our set of manual test cases. You can specify an output directory with
	 * -DOUT_DIRECTORY=./output for example. Otherwise, the current working
	 * directory is used. Test cases must be placed in src/main/resources/testcases/
	 * 
	 * If you only want to run one spefic test, you can specify
	 * -DONLY_TEST=&lt;testname&gt;. I.e. -DONLY_TEST=adobe-borderstyle-bugs
	 * 
	 * @deprecated Use <code>TestcaseRunnerTest</code> to run testcases instead.
	 */
    @Deprecated
	public static void main(String[] args) throws Exception {

		runTestCase("soft-hypen-oom");

		/*
		 * Note: The RepeatedTableSample optionally requires the font file
		 * NotoSans-Regular.ttf to be placed in the resources directory.
		 * 
		 * This sample demonstrates the failing repeated table header on each page.
		 */
		runTestCase("RepeatedTableSample");

		/*
		 * This sample demonstrates the -fs-pagebreak-min-height css property
		 */
		runTestCase("FSPageBreakMinHeightSample");

		runTestCase("color");
		runTestCase("text-decoration");
		runTestCase("background-color");
		runTestCase("background-image");
		runTestCase("invalid-url-background-image");
		runTestCase("text-align");
		runTestCase("font-family-built-in");
		runTestCase("form-controls");

		/*
		 * SVG samples
		 */
		runTestCase("svg-inline");
		runTestCase("svg-sizes");

		/*
		 * Graphics2D Texterror Case
		 */
		runTestCase("moonbase");

		/*
		 * Custom Objects
		 */
		runTestCase("custom-objects");

		/*
		 * CSS3 multi-column layout
		 */
		runTestCase("multi-column-layout");

		/*
		 * Adobe Borderyle Problem
		 */
		runTestCase("adobe-borderstyle-bugs");

		/*
		 * CSS Transform Test
		 */
		runTestCase("transform");

		runTestCase("simplerotate");

		runTestCase("quoting");

		runTestCase("math-ml");

		runTestCase("latex-sample");

		/*
		 * Broken rotate() on the second page
		 */
		runTestCase("RepeatedTableTransformSample");

		/* Add additional test cases here. */
	}

	/**
	 * Will throw an exception if a SEVERE or WARNING message is logged.
	 */
	public static void runTestWithoutOutput(String testCaseFile) throws Exception {
		runTestWithoutOutput(testCaseFile, PdfAConformance.NONE, false);
	}
        
        /**
	 * Will throw an exception if a SEVERE or WARNING message is logged.
	 */
	public static void runTestWithoutOutput(String testCaseFile, PdfAConformance pdfaConformance) throws Exception {
		runTestWithoutOutput(testCaseFile, pdfaConformance, false);
	}

	/**
	 * Will silently let ALL log messages through.
	 */
	public static void runTestWithoutOutputAndAllowWarnings(String testCaseFile) throws Exception {
		runTestWithoutOutput(testCaseFile, PdfAConformance.NONE, true);
	}

    private static void runTestWithoutOutput(String testCaseFile, PdfAConformance pdfaConformance, boolean allowWarnings) throws Exception {
        System.out.println("Trying to run: " + testCaseFile);

        try (InputStream is = TestcaseRunner.class.getResourceAsStream("/testcases/" + testCaseFile + ".html");
             OutputStream outputStream = new ByteArrayOutputStream(4096)) {

            byte[] htmlBytes = IOUtils.toByteArray(is);
            String html = new String(htmlBytes, StandardCharsets.UTF_8);

            // We wan't to throw if we get a warning or severe log message.
            final XRLogger delegate = new JDKXRLogger();
            final java.util.List<RuntimeException> warnings = new ArrayList<>();

            XRLog.setLoggerImpl(new XRLogger() {
                @Override
                public void setLevel(String logger, Level level) {
                }

                @Override
                public boolean isLogLevelEnabled(Diagnostic diagnostic) {
                    return true;
                }

                @Override
                public void log(String where, Level level, String msg, Throwable th) {
                    if (level.equals(Level.WARNING) || level.equals(Level.SEVERE)) {
                        warnings.add(new RuntimeException(where + ": " + msg, th));
                        }
                    delegate.log(where, level, msg, th);
                }

                @Override
                public void log(String where, Level level, String msg) {
                    if (level.equals(Level.WARNING) || level.equals(Level.SEVERE)) {
                        warnings.add(new RuntimeException(where + ": " + msg));
                    }
                    if (!level.equals(Level.INFO)) {
                        delegate.log(where, level, msg);
                    }
                }
            });

            renderPDF(html, pdfaConformance, outputStream);

            if (!warnings.isEmpty() && !allowWarnings) {
                throw warnings.get(0);
            }
        }
    }

    private static void renderPDF(String html, PdfAConformance pdfaConformance, OutputStream outputStream) throws IOException {
        try (SVGDrawer svg = new BatikSVGDrawer();
             SVGDrawer mathMl = new MathMLDrawer()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
            builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
            builder.defaultTextDirection(BaseRendererBuilder.TextDirection.LTR);
            builder.useSVGDrawer(svg);
            builder.useMathMLDrawer(mathMl);
            builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
            builder.useObjectDrawerFactory(buildObjectDrawerFactory());
            builder.usePdfAConformance(pdfaConformance);

            builder.withHtmlContent(html, TestcaseRunner.class.getResource("/testcases/").toString());
            builder.toStream(outputStream);
            builder.run();
        }
    }

	static DefaultObjectDrawerFactory buildObjectDrawerFactory() {
		DefaultObjectDrawerFactory objectDrawerFactory = new StandardObjectDrawerFactory();
		objectDrawerFactory.registerDrawer("custom/binary-tree", new SampleObjectDrawerBinaryTree());
		return objectDrawerFactory;
	}

    private static void renderPNG(String html, final String filename) throws IOException {
        try (SVGDrawer svg = new BatikSVGDrawer();
             SVGDrawer mathMl = new MathMLDrawer()) {

            Java2DRendererBuilder builder = new Java2DRendererBuilder();
            builder.useSVGDrawer(svg);
            builder.useMathMLDrawer(mathMl);
            builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
            builder.useObjectDrawerFactory(buildObjectDrawerFactory());
            builder.withHtmlContent(html, TestcaseRunner.class.getResource("/testcases/").toString());

            BufferedImagePageProcessor bufferedImagePageProcessor = new BufferedImagePageProcessor(
                    BufferedImage.TYPE_INT_RGB, 2.0);

            builder.useDefaultPageSize(210, 297, Java2DRendererBuilder.PageSizeUnits.MM);
            builder.useEnvironmentFonts(true);

            /*
             * Render Single Page Image
             */
            builder.toSinglePage(bufferedImagePageProcessor).runFirstPage();
            BufferedImage image = bufferedImagePageProcessor.getPageImages().get(0);

            ImageIO.write(image, "PNG", new File(filename));

            /*
             * Render Multipage Image Files
             */
            builder.toPageProcessor(new DefaultPageProcessor(
                    zeroBasedPageNumber -> new FileOutputStream(filename.replace(".png", "_" + zeroBasedPageNumber + ".png")),
                    BufferedImage.TYPE_INT_ARGB, "PNG")).runPaged();
        }
    }

    public static void runTestCase(String testCaseFile) throws IOException {
        String onlyTest = System.getProperty("ONLY_TEST", "");
        if (!onlyTest.isEmpty() && !onlyTest.equals(testCaseFile))
            return;

        try (InputStream is = TestcaseRunner.class.getResourceAsStream("/testcases/" + testCaseFile + ".html")) {
            byte[] htmlBytes = IOUtils.toByteArray(is);
            String html = new String(htmlBytes, StandardCharsets.UTF_8);
            String outDir = prepareOutDir();

            String testCaseOutputFile = outDir + "/" + testCaseFile + ".pdf";
            String testCaseOutputPNGFile = outDir + "/png/" + testCaseFile + ".png";

            try (FileOutputStream outputStream = new FileOutputStream(testCaseOutputFile)) {
                renderPDF(html, PdfAConformance.NONE, outputStream);
            }

            renderPNG(html, testCaseOutputPNGFile);
        }
    }

    private static String prepareOutDir() {
        String outDir = System.getProperty("OUT_DIRECTORY", "target/testcases");
        File outDirFile = new File(outDir);
        File pngOutDir = new File(outDirFile, "/png/");

        outDirFile.mkdirs();
        pngOutDir.mkdirs();

        return outDirFile.getAbsolutePath();
    }

	public static class SampleObjectDrawerBinaryTree implements FSObjectDrawer {
		int fanout;
		int angle;

		@Override
		public Map<Shape,String> drawObject(Element e, double x, double y, final double width, final double height,
											OutputDevice outputDevice, RenderingContext ctx, final int dotsPerPixel) {
			final int depth = Integer.parseInt(e.getAttribute("data-depth"));
			fanout = Integer.parseInt(e.getAttribute("data-fanout"));
			angle = Integer.parseInt(e.getAttribute("data-angle"));

			outputDevice.drawWithGraphics((float) x, (float) y, (float) width / dotsPerPixel,
					(float) height / dotsPerPixel, new OutputDeviceGraphicsDrawer() {
						@Override
						public void render(Graphics2D graphics2D) {
							double realWidth = width / dotsPerPixel;
							double realHeight = height / dotsPerPixel;
							double titleBottomHeight = 10;

							renderTree(graphics2D, realWidth / 2f, realHeight - titleBottomHeight, realHeight / depth,
									-90, depth);

							/*
							 * Now draw some text using different fonts to exercise all different font
							 * mappings
							 */
							Font font = Font.decode("Times New Roman").deriveFont(10f);
							if (depth == 10)
								font = Font.decode("Arial"); // Does not get mapped
							if (angle == 35)
								font = Font.decode("Courier"); // Would get mapped to Courier
							if (depth == 6)
								font = Font.decode("Dialog"); // Gets mapped to Helvetica
							graphics2D.setFont(font);
							String txt = "FanOut " + fanout + " Angle " + angle + " Depth " + depth;
							Rectangle2D textBounds = font.getStringBounds(txt, graphics2D.getFontRenderContext());
							graphics2D.setPaint(new Color(16, 133, 30));
							GradientPaint gp = new GradientPaint(10.0f, 25.0f, Color.blue,
									(float) textBounds.getWidth(), (float) textBounds.getHeight(), Color.red);
							if (angle == 35)
								graphics2D.setPaint(gp);
							graphics2D.drawString(txt, (int) ((realWidth - textBounds.getWidth()) / 2),
									(int) (realHeight - titleBottomHeight));
						}
					});
			return null;
		}

		private void renderTree(Graphics2D gfx, double x, double y, double len, double angleDeg, int depth) {
			double rad = angleDeg * Math.PI / 180f;
			double xTarget = x + Math.cos(rad) * len;
			double yTarget = y + Math.sin(rad) * len;
			gfx.setStroke(new BasicStroke(2f));
			gfx.setColor(new Color(255 / depth, 128, 128));
			gfx.draw(new Line2D.Double(x, y, xTarget, yTarget));

			if (depth > 1) {
				double childAngle = angleDeg - (((fanout - 1) * angle) / 2f);
				for (int i = 0; i < fanout; i++) {
					renderTree(gfx, xTarget, yTarget, len * 0.95, childAngle, depth - 1);
					childAngle += angle;
				}
			}
		}
	}
}
