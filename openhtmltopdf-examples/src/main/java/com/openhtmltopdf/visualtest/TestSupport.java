package com.openhtmltopdf.visualtest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.io.IOUtils;
import org.w3c.dom.Element;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.bidi.support.ICUBreakers;
import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.FSObjectDrawerFactory;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.util.XRLogger;
import com.openhtmltopdf.visualtest.Java2DVisualTester.Java2DBuilderConfig;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class TestSupport {
    /**
     * Output the font file as a regular file so we don't have to use streams.
     * @throws IOException
     */
    public static void makeFontFile(String resource) throws IOException {
        File outputDirectory = new File("target/test/visual-tests/test-output/j2d/");
        outputDirectory.mkdirs();

        File fontFile = new File("target/test/visual-tests/" + resource);

        if (!fontFile.exists()) {
            try (InputStream in = TestSupport.class.getResourceAsStream("/visualtest/html/fonts/" + resource)) {
                Files.write(fontFile.toPath(), IOUtils.toByteArray(in));
            }
        }
    }

    /**
     * Output the test fonts from classpath to files in target so we can use them 
     * without streams.
     */
    public static void makeFontFiles() throws IOException {
        makeFontFile("Karla-Bold.ttf");
        makeFontFile("NotoNaskhArabic-Regular.ttf");
        makeFontFile("SourceSansPro-Regular.ttf");
    }

    public static class StringBuilderLogger implements XRLogger {
        private final StringBuilder sb;
        private final XRLogger delegate;
        
        public StringBuilderLogger(StringBuilder sb, XRLogger delegate) {
            this.delegate = delegate;
            this.sb = sb;
        }

        @Override
        public boolean isLogLevelEnabled(Diagnostic diagnostic) {
            return true;
        }

        @Override
        public void setLevel(String logger, Level level) {
        }

        @Override
        public void log(String where, Level level, String msg, Throwable th) {
            if (th == null) {
                log(where, level, msg);
                return;
            }
            StringWriter sw = new StringWriter();
            th.printStackTrace(new PrintWriter(sw, true));
            sb.append(where + ": " + level + ":\n" + msg + sw.toString() + "\n");
            delegate.log(where, level, msg, th);
        }

        @Override
        public void log(String where, Level level, String msg) {
            sb.append(where + ": " + level + ": " + msg + "\n");

            if (!level.equals(Level.INFO)) {
              delegate.log(where, level, msg);
            }
        }
    }
    
    /**
     * A simple line breaker so that our tests are not reliant on the external Java API.
     */
    public static class SimpleTextBreaker implements FSTextBreaker {
        private String text;
        private int position;
        
        @Override
        public int next() {
            int ret = text.indexOf(' ', this.position);
            this.position = ret + 1;
            return ret;
        }

        @Override
        public void setText(String newText) {
            this.text = newText;
            this.position = 0;
        }
    }
    
    /**
     * A simple line breaker that produces similar results to the JRE standard line breaker.
     * So we can test line breaking/justification with conditions more like real world.
     */
    public static class CollapsedSpaceTextBreaker implements FSTextBreaker {
        private final static Pattern SPACES = Pattern.compile("[\\s\u00AD]");
        private Matcher matcher;
        
        @Override
        public int next() {
            if (!matcher.find()) {
                return -1;
            }
        
            return matcher.end();
        }

        @Override
        public void setText(String newText) {
            this.matcher = SPACES.matcher(newText);
        }
    }
    
    public static final BuilderConfig WITH_FONT = (builder) -> {
        builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
        builder.useUnicodeLineBreaker(new SimpleTextBreaker());
    };
    
    public static final Java2DBuilderConfig J2D_WITH_FONT = (builder) -> {
        builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
        builder.useUnicodeLineBreaker(new SimpleTextBreaker());
    };
    
    public static final BuilderConfig WITH_EXTRA_FONT = (builder) -> {
        WITH_FONT.configure(builder);
        builder.useFont(new File("target/test/visual-tests/SourceSansPro-Regular.ttf"), "ExtraFont");
    };
    
    public static final BuilderConfig WITH_ARABIC = (builder) -> {
        WITH_FONT.configure(builder);
        builder.useFont(new File("target/test/visual-tests/NotoNaskhArabic-Regular.ttf"), "arabic");
        builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
        builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
        builder.useUnicodeLineBreaker(new ICUBreakers.ICULineBreaker(Locale.US)); // Overrides WITH_FONT
        builder.defaultTextDirection(TextDirection.LTR);
    };
    
    public static final BuilderConfig WITH_COLLAPSED_LINE_BREAKER = (builder) -> {
        WITH_FONT.configure(builder);
        builder.useUnicodeLineBreaker(new CollapsedSpaceTextBreaker());
    };
    
    /**
     * Configures the builder to use SVG drawer but not font.
     */
    public static final BuilderConfig WITH_SVG = (builder) -> builder.useSVGDrawer(new BatikSVGDrawer());

    public static class ShapesObjectDrawer implements FSObjectDrawer {
        public Map<Shape, String> drawObject(Element e, double x, double y, double width, double height,
                OutputDevice outputDevice, RenderingContext ctx, int dotsPerPixel) {

            Map<Shape, String> shapes = new HashMap<>();

            outputDevice.drawWithGraphics((float) x, (float) y, (float) width / dotsPerPixel,
                (float) height / dotsPerPixel, (Graphics2D g2d) -> {

                double realWidth = width / dotsPerPixel;
                double realHeight = height / dotsPerPixel;

                Rectangle2D rectUpperLeft = new Rectangle2D.Double(0, 0, realWidth / 4d, realHeight / 4d);
                Rectangle2D rectLowerRight = new Rectangle2D.Double(realWidth * (3d/4d), realHeight * (3d/4d), realWidth / 4d, realHeight / 4d);

                int[] xpoints = new int[] { (int) (realWidth / 2d), (int) (realWidth * (1d/4d)), (int) (realWidth * (3d/4d)), (int) (realWidth / 2d) }; 
                int[] ypoints = new int[] { (int) (realHeight * (1d/4d)), (int) (realHeight * (3d/4d)), (int) (realHeight * (3d/4d)), (int) (realHeight * (1d/4d)) }; 
                Polygon centreTriangle = new Polygon(xpoints, ypoints, xpoints.length);

                g2d.setColor(Color.CYAN);

                g2d.draw(rectUpperLeft);
                g2d.draw(rectLowerRight);
                g2d.draw(centreTriangle);

                AffineTransform scale = AffineTransform.getScaleInstance(dotsPerPixel, dotsPerPixel);

                shapes.put(scale.createTransformedShape(rectUpperLeft), "http://example.com/1");
                shapes.put(scale.createTransformedShape(rectLowerRight), "http://example.com/2");
                shapes.put(scale.createTransformedShape(centreTriangle), "http://example.com/3");
            });

            return shapes;
        }
    }

    public static class ShapesObjectDrawerFactory implements FSObjectDrawerFactory {
        public FSObjectDrawer createDrawer(Element e) {
            if (!isReplacedObject(e)) {
                return null;
            }

            return new ShapesObjectDrawer();
        }

        public boolean isReplacedObject(Element e) {
            return e.getAttribute("type").equals("shapes");
        }
    }

    public static final BuilderConfig WITH_SHAPES_DRAWER = (builder) -> { builder.useObjectDrawerFactory(new ShapesObjectDrawerFactory()); };

    public static boolean comparePdfs(byte[] actualPdfBytes, String resource) throws IOException {
        File outputPath = new File("target/test/visual-tests/test-output/");

        byte[] expectedPdfBytes;
        try (InputStream expectedIs = TestcaseRunner.class.getResourceAsStream("/visualtest/expected/" + resource + ".pdf")) {
            expectedPdfBytes = IOUtils.toByteArray(expectedIs);
        }

        List<PdfCompareResult> problems = PdfVisualTester.comparePdfDocuments(expectedPdfBytes, actualPdfBytes, resource, false);

        if (!problems.isEmpty()) {
            System.err.println("Found problems with test case (" + resource + "):");
            System.err.println(problems.stream().map(p -> p.logMessage).collect(Collectors.joining("\n    ", "[\n    ", "\n]")));

            File outPdf = new File(outputPath, resource + "---actual.pdf");
            Files.write(outPdf.toPath(), actualPdfBytes);
        }

        if (problems.stream().anyMatch(p -> p.testImages != null)) {
            System.err.println("For test case (" + resource + ") writing diff images to '" + outputPath + "'");
        }

        for (PdfCompareResult result : problems) {
            if (result.testImages != null) {
                File output = new File(outputPath, resource + "---" + result.pageNumber + "---diff.png");
                ImageIO.write(result.testImages.createDiff(), "png", output);

                output = new File(outputPath, resource + "---" + result.pageNumber + "---actual.png");
                ImageIO.write(result.testImages.getActual(), "png", output);

                output = new File(outputPath, resource + "---" + result.pageNumber + "---expected.png");
                ImageIO.write(result.testImages.getExpected(), "png", output);
            }
        }

        return problems.isEmpty();
    }

    public static void quietLogs() {
        XRLog.listRegisteredLoggers().forEach(log -> XRLog.setLevel(log, Level.WARNING));
    }

    @FunctionalInterface
    public interface WithLog {
        void accept(List<LogMessageId> log, Consumer<Diagnostic> con) throws IOException;
    }

    @FunctionalInterface
    public interface WithLogBuilder {
        void accept(List<LogMessageId> log, BuilderConfig config) throws IOException;
    }

    public static void withLogConsumer(WithLog consumer) throws IOException {
        List<LogMessageId> log = new ArrayList<>();
        Consumer<Diagnostic> diagCon = (d) -> log.add(d.getLogMessageId());

        consumer.accept(log, diagCon);
    }

    public static void withLog(WithLogBuilder consumer) throws IOException {
        withLogConsumer((log, con) -> consumer.accept(log, (builder) -> builder.withDiagnosticConsumer(con)));
    }
}
