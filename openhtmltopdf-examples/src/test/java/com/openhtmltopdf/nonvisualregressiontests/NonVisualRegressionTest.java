package com.openhtmltopdf.nonvisualregressiontests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.hasItem;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PagePosition;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.visualregressiontests.VisualRegressionTest;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

@RunWith(PrintingRunner.class)
public class NonVisualRegressionTest {
    private static final String RES_PATH = "/visualtest/html/";
    private static final String OUT_PATH = "target/test/visual-tests/test-output/";

    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    private static void render(String fileName, String html, BuilderConfig config) throws IOException {
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, NonVisualRegressionTest.class.getResource(RES_PATH).toString());
        builder.toStream(actual);
        builder.useFastMode();
        builder.testMode(true);
        config.configure(builder);
        
        try {
            builder.run();
        } catch (Exception e) {
            System.err.println("Failed to render resource (" + fileName + ")");
            e.printStackTrace();
        }

        writePdfToFile(fileName, actual);
    }

    private static void writePdfToFile(String fileName, ByteArrayOutputStream actual) throws IOException {
        FileUtils.writeByteArrayToFile(new File(OUT_PATH, fileName + ".pdf"), actual.toByteArray());
    }

    private static String loadHtml(String fileName) throws IOException {
        String absResPath = RES_PATH + fileName + ".html";

        try (InputStream is = TestcaseRunner.class.getResourceAsStream(absResPath)) {
            byte[] htmlBytes = IOUtils
                      .toByteArray(is);

            return new String(htmlBytes, StandardCharsets.UTF_8);
        }
    }

    private static PDDocument run(String fileName, BuilderConfig config) throws IOException {
        String html = loadHtml(fileName);

        render(fileName, html, config);

        return load(fileName);
    }

    private static PDDocument run(String filename) throws IOException {
        return run(filename, b -> {});
    }

    private static PDDocument load(String filename) throws IOException {
        return PDDocument.load(new File(OUT_PATH, filename + ".pdf"));
    }

    private static void remove(String fileName, PDDocument doc) throws IOException {
        OpenUtil.closeQuietly(doc);
        new File(OUT_PATH, fileName + ".pdf").delete();
    }

    private static double cssPixelsToPdfPoints(double cssPixels) {
        return cssPixels * 72d / 96d;
    }
    
    private static double cssPixelYToPdfPoints(double cssPixelsY, double cssPixelsPageHeight) {
        return cssPixelsToPdfPoints(cssPixelsPageHeight - cssPixelsY);
    }

    private static class RectangleCompare extends CustomTypeSafeMatcher<PDRectangle> {
        private final PDRectangle expec;
        private final double pageHeight;
        
        private RectangleCompare(PDRectangle expected, double pageHeight) {
            super("Compare Rectangles");
            this.expec = expected;
            this.pageHeight = pageHeight;
        }
        
        @Override
        protected boolean matchesSafely(PDRectangle item) {
            assertEquals(cssPixelsToPdfPoints(this.expec.getLowerLeftX()), item.getLowerLeftX(), 1.0d);
            assertEquals(cssPixelsToPdfPoints(this.expec.getUpperRightX()), item.getUpperRightX(), 1.0d);
            
            // Note: We swap the Ys here because PDFBOX returns a rect in bottom up units while expected is in topdown units.
            assertEquals(cssPixelYToPdfPoints(this.expec.getUpperRightY(), pageHeight), item.getLowerLeftY(), 1.0d);
            assertEquals(cssPixelYToPdfPoints(this.expec.getLowerLeftY(), pageHeight), item.getUpperRightY(), 1.0d);
            
            return true;
        }
    }

    /**
     * Expected rect is in top down CSS pixel units. Actual rect is in bottom up PDF points.
     */
    private CustomTypeSafeMatcher<PDRectangle> rectEquals(PDRectangle expected, double pageHeight) {
        return new RectangleCompare(expected, pageHeight);
    }

    /**
     * Tests meta info: title, author, subject, keywords.
     */
    @Test
    public void testMetaInformation() throws IOException {
        try (PDDocument doc = run("meta-information")) {
            PDDocumentInformation did = doc.getDocumentInformation();

            assertThat(did.getTitle(), equalTo("Test title"));
            assertThat(did.getAuthor(), equalTo("Test author"));
            assertThat(did.getSubject(), equalTo("Test subject"));
            assertThat(did.getKeywords(), equalTo("Test keywords"));

            remove("meta-information", doc);
        }
    }

    /**
     * Tests that a simple head bookmark linking to top of the second page works. 
     */
    @Test
    public void testBookmarkHeadSimple() throws IOException {
        try (PDDocument doc = run("bookmark-head-simple")) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

            PDOutlineItem bm = outline.getFirstChild();
            assertThat(bm.getTitle(), equalTo("Test bookmark"));
            assertThat(bm.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest = (PDPageXYZDestination) bm.getDestination();

            // At top of second page.
            assertEquals(dest.getPage(), doc.getPage(1));
            assertEquals(doc.getPage(1).getMediaBox().getUpperRightY(), dest.getTop(), 1.0d);

            remove("bookmark-head-simple", doc);
        }
    }

    /**
     * Tests that a simple body bookmark linking to top of the second page works.
     */
    @Test
    public void testBookmarkBodySimple() throws IOException {
        try (PDDocument doc = run("bookmark-body-simple")) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

            PDOutlineItem bm = outline.getFirstChild();
            assertThat(bm.getTitle(), equalTo("Test bookmark"));
            assertThat(bm.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest = (PDPageXYZDestination) bm.getDestination();

            // At top of second page.
            assertEquals(dest.getPage(), doc.getPage(1));
            assertEquals(doc.getPage(1).getMediaBox().getUpperRightY(), dest.getTop(), 1.0d);

            remove("bookmark-body-simple", doc);
        }
    }

    /**
     * Tests that a head bookmark linking to transformed element (by way of transform) on third page works. 
     */
    @Test
    public void testBookmarkHeadTransform() throws IOException {
        try (PDDocument doc = run("bookmark-head-transform")) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

            PDOutlineItem bm = outline.getFirstChild();
            assertThat(bm.getTitle(), equalTo("Test bookmark"));
            assertThat(bm.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest = (PDPageXYZDestination) bm.getDestination();

            // At top of third page.
            assertEquals(dest.getPage(), doc.getPage(2));
            assertEquals(doc.getPage(2).getMediaBox().getUpperRightY(), dest.getTop(), 1.0d);

            remove("bookmark-head-transform", doc);
        }
    }
    
    /**
     * Tests that a head bookmark linking to element (on overflow page). 
     */
    @Test
    public void testBookmarkHeadOnOverflowPage() throws IOException {
        try (PDDocument doc = run("bookmark-head-on-overflow-page")) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

            PDOutlineItem bm = outline.getFirstChild();
            assertThat(bm.getTitle(), equalTo("Test bookmark"));
            assertThat(bm.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest = (PDPageXYZDestination) bm.getDestination();

            assertEquals(dest.getPage(), doc.getPage(2));
            // Should be 11px down (10px margin, 1px outer border).
            assertEquals(cssPixelYToPdfPoints(11, 50), dest.getTop(), 1.0d);

            remove("bookmark-head-on-overflow-page", doc);
        }
    }

    /**
     * Tests that a head bookmark linking to an inline element (on page after overflow page) works. 
     */
    @Test
    public void testBookmarkHeadAfterOverflowPage() throws IOException {
        try (PDDocument doc = run("bookmark-head-after-overflow-page")) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

            PDOutlineItem bm = outline.getFirstChild();
            assertThat(bm.getTitle(), equalTo("Test bookmark"));
            assertThat(bm.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest = (PDPageXYZDestination) bm.getDestination();

            assertEquals(dest.getPage(), doc.getPage(3));
            // Should be 10px down (10px page margin).
            assertEquals(cssPixelYToPdfPoints(10, 50), dest.getTop(), 1.0d);

            remove("bookmark-head-after-overflow-page", doc);
        }
    }
    
    /**
     * Tests that a nested head bookmark linking to top of the third page works. 
     */
    @Test
    public void testBookmarkHeadNested() throws IOException {
        try (PDDocument doc = run("bookmark-head-nested")) {
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

            PDOutlineItem bm1 = outline.getFirstChild();
            assertThat(bm1.getTitle(), equalTo("Outer"));
            assertThat(bm1.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest1 = (PDPageXYZDestination) bm1.getDestination();

            // At top of second page.
            assertEquals(dest1.getPage(), doc.getPage(1));
            assertEquals(doc.getPage(1).getMediaBox().getUpperRightY(), dest1.getTop(), 1.0d);

            PDOutlineItem bm2 = bm1.getFirstChild();
            assertThat(bm2.getTitle(), equalTo("Inner"));
            assertThat(bm2.getDestination(), instanceOf(PDPageXYZDestination.class));
            PDPageXYZDestination dest2 = (PDPageXYZDestination) bm2.getDestination();

            // At top of third page.
            assertEquals(dest2.getPage(), doc.getPage(2));
            assertEquals(doc.getPage(2).getMediaBox().getUpperRightY(), dest2.getTop(), 1.0d);

            remove("bookmark-head-nested", doc);
        }
    }

    /**
     * Tests bad footnote related content such as:
     * + Paginated table inside footnotes.
     * Primarily to check that these scenarios do not cause infinite loop
     * or out-of-memory and ideally don't throw exceptions.
     * Bad footnote content is not supported and will not produce expected results.
     */
    @Test
    public void testIssue364InvalidFootnoteContent() throws IOException {
        try (PDDocument doc = run("issue-364-invalid-footnote-content")) {
            remove("issue-364-invalid-footnote-content", doc);
        }

    }

    /**
     * Tests bad footnote related content such as:
     * + Pseudos (::footnote-call, ::footnote-marker, ::before, ::after) with float: footnote.
     * + Pseudos in footnotes with position: fixed.
     * + Invalid styles in the footnote at-rule such as position: fixed.
     * 
     * Primarily to check that these scenarios do not cause infinite loop
     * or out-of-memory and ideally don't throw exceptions.
     * Bad footnote content is not supported and will not produce expected results.
     */
    @Test
    public void testIssue364InvalidFootnotePseudos() throws IOException {
        TestSupport.withLog((log, builder) -> {
            try (PDDocument doc = run("issue-364-invalid-footnote-pseudos", builder)) {
            }

            assertThat(log, hasItem(LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_PSEUDO_INVALID));
            assertThat(log, hasItem(LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_CAN_NOT_BE_PSEUDO));
            assertThat(log, hasItem(LogMessageId.LogMessageId2Param.GENERAL_FOOTNOTE_AREA_INVALID_STYLE));
        });

        remove("issue-364-invalid-footnote-pseudos", null);
    }

    /**
     * Tests the positioning, size, name and value of a text type form control.
     */
    @Test
    public void testFormControlText() throws IOException {
        try (PDDocument doc = run("form-control-text")) {

            assertEquals(1, doc.getPage(0).getAnnotations().size());
            assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationWidget.class));

            PDAnnotationWidget widget = (PDAnnotationWidget) doc.getPage(0).getAnnotations().get(0);
            assertThat(widget.getRectangle(), rectEquals(new PDRectangle(23f, 23f, 100f, 20f), 200));

            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertEquals(1, form.getFields().size());
            assertThat(form.getFields().get(0), instanceOf(PDTextField.class));

            PDTextField field = (PDTextField) form.getFields().get(0);
            assertEquals("text-input", field.getFullyQualifiedName());
            assertEquals("Hello World!", field.getValue());

            remove("form-control-text", doc);
        }
    }
    
    /**
     * Tests the positioning, size, name and value of a form control on an overflow page.
     */
    @Test
    public void testFormControlOverflowPage() throws IOException {
        try (PDDocument doc = run("form-control-overflow-page")) {

            assertEquals(0, doc.getPage(0).getAnnotations().size());
            assertEquals(1, doc.getPage(1).getAnnotations().size());
            assertThat(doc.getPage(1).getAnnotations().get(0), instanceOf(PDAnnotationWidget.class));

            PDAnnotationWidget widget = (PDAnnotationWidget) doc.getPage(1).getAnnotations().get(0);
            assertThat(widget.getRectangle(), rectEquals(new PDRectangle(33f, 14f, 40f, 20f), 100));

            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertEquals(1, form.getFields().size());
            assertThat(form.getFields().get(0), instanceOf(PDTextField.class));

            PDTextField field = (PDTextField) form.getFields().get(0);
            assertEquals("text-input", field.getFullyQualifiedName());
            assertEquals("Hello World!", field.getValue());

            remove("form-control-overflow-page", doc);
        }
    }
    
    /**
     * Tests the positioning, size, name and value of a form control appearing after an overflow page.
     */
    @Test
    public void testFormControlAfterOverflowPage() throws IOException {
        try (PDDocument doc = run("form-control-after-overflow-page")) {

            assertEquals(0, doc.getPage(0).getAnnotations().size());
            assertEquals(0, doc.getPage(1).getAnnotations().size());
            assertEquals(1, doc.getPage(2).getAnnotations().size());
            assertThat(doc.getPage(2).getAnnotations().get(0), instanceOf(PDAnnotationWidget.class));

            PDAnnotationWidget widget = (PDAnnotationWidget) doc.getPage(2).getAnnotations().get(0);
            assertThat(widget.getRectangle(), rectEquals(new PDRectangle(13f, 13f, 60f, 30f), 100));

            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertEquals(1, form.getFields().size());
            assertThat(form.getFields().get(0), instanceOf(PDTextField.class));

            PDTextField field = (PDTextField) form.getFields().get(0);
            assertEquals("text-input", field.getFullyQualifiedName());
            assertEquals("Hello World!", field.getValue());

            remove("form-control-after-overflow-page", doc);
        }
    }

    /**
     * Check that an input without name attribute does not launch a NPE.
     * Will now log a warning message.
     * See issue: https://github.com/danfickle/openhtmltopdf/issues/151
     *
     * Additionally, check that a select element without options will not launch a NPE too.
     */
    @Test
    public void testInputWithoutNameAttribute() throws IOException {
        try (PDDocument doc = run("input-without-name-attribute")) {
            // Note: As of PDFBOX 2.0.22 we have the option of recreating
            // the acro form from the widgets. We pass null to avoid this behavior.
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm(null);
            assertEquals(0, form.getFields().size());
            remove("input-without-name-attribute", doc);
        }
    }

    @Test
    public void testIssue338RadioReadOnly() throws IOException {
        try (PDDocument doc = run("issue-338-radio-read-only")) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm(null);

            PDRadioButton radio = (PDRadioButton) form.getFields().get(0);
            assertTrue("radio should be readonly", radio.isReadOnly());

            remove("issue-338-radio-read-only", doc);
        }
    }

    private static float[] getQuadPoints(PDDocument doc, int pg, int linkIndex) throws IOException {
        return ((PDAnnotationLink) doc.getPage(pg).getAnnotations().get(linkIndex)).getQuadPoints();
    }

    private static String print(float[] floats) {
        StringBuilder sb = new StringBuilder();

        sb.append("new float[] { ");
        for (float floater : floats) {
            sb.append(floater);
            sb.append("f, ");
        }

        sb.deleteCharAt(sb.length() - 2);
        sb.append("}");

        return sb.toString();
    }

    private static final float QUAD_DELTA = 0.5f;
    private static boolean qAssert(List<float[]> expectedList, float[] actual, StringBuilder sb, int pg, int linkIndex) {
        sb.append("PAGE: " + pg + ", LINK: " + linkIndex + "\n");
        sb.append("   ACT(" + actual.length + "): " + print(actual) + "\n");

        // NOTE: The shapes are returned as a map and are therefore placed on
        // the page in a non-determined order. So we just searh the expected
        // list for a match.
        ALL_EXPECTED:
        for (float[] expected : expectedList) {
            if (expected.length != actual.length) {
                continue;
            }

            for (int i = 0; i < expected.length; i++) {
                float diff = Math.abs(expected[i] - actual[i]);

                if (diff > QUAD_DELTA) {
                    continue ALL_EXPECTED;
                }
            }

            return false;
        }

        sb.append("   !FAILED!");
        sb.append("\n\n");
        return true;
    }

    /**
     * Tests that there is no repeated text in the page margin area as
     * reported in issue 458.
     */
    @Test
    public void testIssue458PageContentRepeatedInMargin() throws IOException {
        try (PDDocument doc = run("issue-458-content-repeated")) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            String expected = 
               IntStream.rangeClosed(1, 9)
                        .mapToObj(i -> "Line " + i + "\r\n")
                        .collect(Collectors.joining()) +
            "This is \r\n" + 
            "some \r\n" + 
            "flowing \r\n" + 
            "text that \r\n" + 
            "should not \r\n" + 
            "repeat in \r\n" + 
            "page \r\n" + 
            "margins.\r\n" +
            "1.  \r\n" + 
            "2.  \r\n" + 
            "3.  \r\n" + 
            "One\r\n" + 
            "Two\r\n" + 
            "Three";

            String normalizedExpected = expected.replaceAll("(\\r|\\n)", "");
            String normalizedActual = text.replaceAll("(\\r|\\n)", "");

            assertEquals(normalizedExpected.trim(), normalizedActual.trim());
        }
    }

    /**
     * Table row repeating on two pages. See issue 594.
     */
    @Test
    public void testIssue594RepeatingContentTableRow() throws IOException {
        try (PDDocument doc = run("issue-594-content-repeated")) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc).replaceAll("(\\r|\\n)", "");
            String expected = "One 1" + "Abcdefghij2 2";

            assertEquals(expected, text);
        }
    }

    /**
     * Tests the shaped links support for custom object drawers
     * in the main document area and in the page margin on multiple
     * pages.
     */
    @Test
    public void testPR480LinkShapes() throws IOException {
        try (PDDocument doc = run("pr-480-link-shapes", TestSupport.WITH_SHAPES_DRAWER)) {
            StringBuilder sb = new StringBuilder();
            List<float[]> page0 = new ArrayList<>();
            List<float[]> page1 = new ArrayList<>();
            boolean failure = false;

            page0.add(new float[] { 486.75f, 251.25f, 468.0f, 213.75f, 486.75f, 213.75f, 505.5f, 213.75f, 486.75f, 251.25f, 505.5f, 213.75f, 496.125f, 232.5f, 486.75f, 251.25f });
            page0.add(new float[] { 449.25f, 270.0f, 449.25f, 251.25f, 458.625f, 251.25f, 468.0f, 251.25f, 449.25f, 270.0f, 468.0f, 251.25f, 468.0f, 260.625f, 468.0f, 270.0f });
            page0.add(new float[] { 505.5f, 213.75f, 505.5f, 195.0f, 514.875f, 195.0f, 524.25f, 195.0f, 505.5f, 213.75f, 524.25f, 195.0f, 524.25f, 204.375f, 524.25f, 213.75f });
            page0.add(new float[] { 243.0f, 203.25f, 243.0f, 128.25f, 280.5f, 128.25f, 318.0f, 128.25f, 243.0f, 203.25f, 318.0f, 128.25f, 318.0f, 165.75f, 318.0f, 203.25f });
            page0.add(new float[] { 168.0f, 353.25f, 93.0f, 203.25f, 168.0f, 203.25f, 243.0f, 203.25f, 168.0f, 353.25f, 243.0f, 203.25f, 205.5f, 278.25f, 168.0f, 353.25f });
            page0.add(new float[] { 18.0f, 428.25f, 18.0f, 353.25f, 55.5f, 353.25f, 93.0f, 353.25f, 18.0f, 428.25f, 93.0f, 353.25f, 93.0f, 390.75f, 93.0f, 428.25f });

            failure |= qAssert(page0, getQuadPoints(doc, 0, 0), sb, 0, 0);
            failure |= qAssert(page0, getQuadPoints(doc, 0, 1), sb, 0, 1);
            failure |= qAssert(page0, getQuadPoints(doc, 0, 2), sb, 0, 2);
            failure |= qAssert(page0, getQuadPoints(doc, 0, 3), sb, 0, 3);
            failure |= qAssert(page0, getQuadPoints(doc, 0, 4), sb, 0, 4);
            failure |= qAssert(page0, getQuadPoints(doc, 0, 5), sb, 0, 5);

            page1.add(new float[] { 486.75f, 251.25f, 468.0f, 213.75f, 486.75f, 213.75f, 505.5f, 213.75f, 486.75f, 251.25f, 505.5f, 213.75f, 496.125f, 232.5f, 486.75f, 251.25f });
            page1.add(new float[] { 449.25f, 270.0f, 449.25f, 251.25f, 458.625f, 251.25f, 468.0f, 251.25f, 449.25f, 270.0f, 468.0f, 251.25f, 468.0f, 260.625f, 468.0f, 270.0f });
            page1.add(new float[] { 505.5f, 213.75f, 505.5f, 195.0f, 514.875f, 195.0f, 524.25f, 195.0f, 505.5f, 213.75f, 524.25f, 195.0f, 524.25f, 204.375f, 524.25f, 213.75f });
            page1.add(new float[] { 243.0f, 209.25f, 243.0f, 134.25f, 280.5f, 134.25f, 318.0f, 134.25f, 243.0f, 209.25f, 318.0f, 134.25f, 318.0f, 171.75f, 318.0f, 209.25f });
            page1.add(new float[] { 168.0f, 359.25f, 93.0f, 209.25f, 168.0f, 209.25f, 243.0f, 209.25f, 168.0f, 359.25f, 243.0f, 209.25f, 205.5f, 284.25f, 168.0f, 359.25f });
            page1.add(new float[] { 18.0f, 434.25f, 18.0f, 359.25f, 55.5f, 359.25f, 93.0f, 359.25f, 18.0f, 434.25f, 93.0f, 359.25f, 93.0f, 396.75f, 93.0f, 434.25f });

            failure |= qAssert(page1, getQuadPoints(doc, 1, 0), sb, 1, 0);
            failure |= qAssert(page1, getQuadPoints(doc, 1, 1), sb, 1, 1);
            failure |= qAssert(page1, getQuadPoints(doc, 1, 2), sb, 1, 2);
            failure |= qAssert(page1, getQuadPoints(doc, 1, 3), sb, 1, 3);
            failure |= qAssert(page1, getQuadPoints(doc, 1, 4), sb, 1, 4);
            failure |= qAssert(page1, getQuadPoints(doc, 1, 5), sb, 1, 5);

            if (failure) {
                System.out.print(sb.toString());
                Assert.fail("Quad points were not correct");
            }

            remove("pr-480-link-shapes", doc);
        }
    }

    /**
     * Tests that many footnotes do not take too long.
     */
    @Test
    public void testIssue364ManyFootnotes() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 200; i++) {
            sb.append("Normal <div style=\"float: footnote;\">Footnote</div>");
        }

        runFuzzTest(sb.toString(), false);
    }

    /**
     * Tests performance of footnotes in many lines of text.
     */
    @Test
    public void testIssue364MuchText() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < 50; j++) {
            for (int i = 0; i < 200; i++) {
                sb.append("Hello World!<br/>");
            }
            sb.append("<div style=\"float: footnote; color: green;\">Footnote</div>");
        }

        runFuzzTest(sb.toString(), false);
    }

    /**
     * Tests that footnotes nested very deeply do not take too long.
     */
    @Test
    public void testIssue364FootnotesDeepNesting() throws IOException {
        Function<String, String> deeper = (tag) ->
          IntStream.range(0, 50)
            .mapToObj(u -> tag)
            .collect(Collectors.joining());

        String[][] tags = new String[][] {
            { "<div>", "</div>" },
            { "<span>", "</span>" },
            { "<div style=\"position: absolute;\">", "</div>" },
            { "<td>", "</td>" },
            { "<div style=\"float: left;\">", "</div>" },
        };

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tags.length; i++) {
            sb.append(deeper.apply(tags[i][0]));
            sb.append("Normal <div style=\"float: footnote;\">Footnote</div>");
            sb.append(deeper.apply(tags[i][1]));
        }

        runFuzzTest(sb.toString(), false);
    }

    /**
     * Runs a fuzz test, optionally with PDFBOX included font with non-zero-width
     * soft hyphen.
     */
    private static void runFuzzTest(String html, boolean useFont) throws IOException {
        final String header = useFont ?
                "<html><body style=\"font-family: 'Liberation Sans'\">" :
                "<html><body>";
        final String footer = "</body></html>";

        System.out.println("The test is " + html.length() + " chars long.");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.toStream(os);
            builder.withHtmlContent(header + html + footer, null);
            if (useFont) {
                builder.useFont(() -> VisualRegressionTest.class.getClassLoader().getResourceAsStream("org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"),
                        "Liberation Sans");
            }
            builder.run();

            // Files.write(Paths.get("./target/html.txt"), html.getBytes(StandardCharsets.UTF_8));
            // java.nio.file.Files.write(java.nio.file.Paths.get("./target/pdf.pdf"), os.toByteArray());

            System.out.println("The result is " + os.size() + " bytes long.");
        }
    }

    /**
     * Creates a fuzz test with random characters given the styles provided in
     * arguments.
     */
    private static void createCombinationTest(
            StringBuilder sb, int widthPx, String whiteSpace, String wordWrap, List<char[]> all, Random rndm, int testCharCount) {
        String start = String.format(Locale.US, "<div style=\"white-space: %s; word-wrap: %s; width: %dpx;\">", 
                whiteSpace, wordWrap, widthPx);
        String end = "</div>";

        sb.append(start);

        int len = 0;
        while (true) {
            char[] charCombi = all.get(rndm.nextInt(all.size()));

            if (len + charCombi.length > testCharCount) {
                String combi = String.valueOf(charCombi);
                sb.append(combi.substring(0, testCharCount - len));
                break;
            }

            sb.append(charCombi);
            len += charCombi.length;
        }

        sb.append(end);
    }

    /**
     * Creates all 5 character combinations from a list of characters
     * which have special meaning to the line breaking algorithms.
     */
    private static List<char[]> createAllCombinations() {
        char[] chars = new char[] { 'x', '\u00ad', '\n', '\r', ' ' };
        int[] loopIndices = new int[chars.length];
        int totalCombinations = (int) Math.pow(loopIndices.length, loopIndices.length);

        List<char[]> ret = new ArrayList<>(totalCombinations);

        for (int i = 0; i < totalCombinations; i++) {
                char[] result = new char[loopIndices.length];

                for (int k = 0; k < loopIndices.length; k++) {
                    char ch = chars[loopIndices[k]];
                    result[k] = ch;
                }

                ret.add(result);

                boolean carry = true;
                for (int j = loopIndices.length - 1; j >= 0; j--) {
                    if (carry) {
                        loopIndices[j]++;
                        carry = false;
                    }

                    if (loopIndices[j] >= chars.length) {
                        loopIndices[j] = 0;
                        carry = true;
                    }
                }
        }

        return ret;
    }

    /**
     * Tests the line breaking algorithms against infinite loop bugs
     * by using many combinations of styles and random character sequences.
     */
    @Test
    public void testPr492InfiniteLoopBugsInLineBreakingFuzz() throws IOException {
        final String[] whiteSpace = new String[] { "normal", "pre", "nowrap", "pre-wrap", "pre-line" };
        final String[] wordWrap = new String[] { "normal", "break-word" };
        final List<char[]> all = createAllCombinations();
        final Random rndm = new Random();
        long seed = rndm.nextLong();

        System.out.println("For NonVisualRegressionTest::testPr492InfiniteLoopBugsInLineBreakingFuzz " +
              "using a random seed of " + seed + " for Random instance.");
        rndm.setSeed(seed);

        List<Integer> lengths = new ArrayList<>();
        lengths.addAll(Arrays.asList(0, 1, 2, 3, 37, 79));

        for (int i = 0; i < 4; i++) {
            lengths.add(rndm.nextInt(150));
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < whiteSpace.length; j++) {
                for (int k = 0; k < wordWrap.length; k++) {
                    for (Integer len : lengths) {
                         createCombinationTest(sb, i, whiteSpace[j], wordWrap[k], all, rndm, len);
                    }
                }
            }
        }

        runFuzzTest(sb.toString(), false);
        runFuzzTest(sb.toString(), true);
    }

    /**
     * Tests the diagnostic consumer api added to the builder.
     */
    @Test
    public void testPr489DiagnosticConsumer() throws IOException {
        List<Diagnostic> logs = new ArrayList<>();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.withDiagnosticConsumer(logs::add);
            builder.useFastMode();
            builder.toStream(os);
            builder.withHtmlContent("<html style=\"invalid-prop: invalid-val\"><body>TEST</body></html>", null);
            builder.run();
        }

        Assert.assertTrue(
             logs.stream()
                 .noneMatch(diag -> diag.getLogMessageId() == LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI));

        Assert.assertTrue(
             logs.stream()
                 .anyMatch(diag -> diag.getLogMessageId() == LogMessageId.LogMessageId2Param.CSS_PARSE_GENERIC_MESSAGE));

        Assert.assertTrue(
              logs.stream()
                  .allMatch(diag -> !diag.getFormattedMessage().isEmpty()));
    }

    @Test
    public void testIssue508FileEmbed() throws IOException {
        try (PDDocument doc = run("issue-508-file-embed",
                builder -> {
                    // File embeds are blocked by default, allow everything.
                    builder.useExternalResourceAccessControl((uri, type) -> true, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
                    builder.useExternalResourceAccessControl((uri, type) -> true, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI);
                })) {

            // There should be multiple file attachment annotations because the link
            // is broken into two boxes on multiple lines.
            assertThat(doc.getPage(0).getAnnotations().size(), equalTo(2));

            PDAnnotationFileAttachment fileAttach1 = (PDAnnotationFileAttachment) doc.getPage(0).getAnnotations().get(0);
            assertThat(fileAttach1.getFile().getFile(), equalTo("basic.css"));

            PDAnnotationFileAttachment fileAttach2 = (PDAnnotationFileAttachment) doc.getPage(0).getAnnotations().get(1);
            assertThat(fileAttach2.getFile().getFile(), equalTo("basic.css"));

            try (COSDocument cosDoc = doc.getDocument()) {
                // Make sure the file is only embedded once.
                List<COSObject> files = cosDoc.getObjectsByType(COSName.FILESPEC);
                assertThat(files.size(), equalTo(1));
            }

            remove("issue-508-file-embed", doc);
        }
    }

    /**
     * Tests the PdfBoxRenderer::getPagePositions and
     * PdfBoxRenderer::getLastYPositionOfContent apis.
     * It does this by drawing a rect around each layer and comparing
     * with the expected document.
     */
    @SuppressWarnings("resource")
    @Test
    public void testIssue427GetBodyPagePositions() throws IOException {
        String filename = "issue-427-body-page-positions";
        String html = loadHtml(filename);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();

        builder.withHtmlContent(html, null);
        builder.useFastMode();
        builder.toStream(os);

        float lastContentLine;

        try (PdfBoxRenderer renderer = builder.buildPdfRenderer();
             PDDocument doc = renderer.createPDFKeepOpen()) {

            List<PagePosition<Layer>> posList = renderer.getLayersPositions();
            lastContentLine = renderer.getLastContentBottom();

            for (int idx = 0; idx < posList.size(); ) {
                int pageIdx = posList.get(idx).getPageNo();
                List<PagePosition<Layer>> pageLayers = new ArrayList<>();

                while (idx < posList.size() && posList.get(idx).getPageNo() == pageIdx) {
                    pageLayers.add(posList.get(idx));
                    idx++;
                }

                try (PDPageContentStream stream = new PDPageContentStream(
                        renderer.getPdfDocument(), renderer.getPdfDocument().getPage(pageIdx),
                        AppendMode.APPEND, false, false)) {

                    stream.setLineWidth(1f);
                    stream.setStrokingColor(Color.ORANGE);

                    for (PagePosition<Layer> pos : pageLayers) {
                        stream.addRect(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
                        stream.stroke();
                    }
                }
            }

            renderer.getPdfDocument().save(os);

            writePdfToFile(filename, os);
        }

        assertTrue(TestSupport.comparePdfs(os.toByteArray(), filename));
        assertEquals(111.48, lastContentLine, 0.5);
    }

    // TODO:
    // + More form controls.
    // + Custom meta info.
}
