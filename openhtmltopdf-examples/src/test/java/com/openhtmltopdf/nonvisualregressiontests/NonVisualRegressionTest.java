package com.openhtmltopdf.nonvisualregressiontests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.util.Charsets;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class NonVisualRegressionTest {
    private static final String RES_PATH = "/visualtest/html/";
    private static final String OUT_PATH = "target/test/visual-tests/test-output/";
    
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
        
        FileUtils.writeByteArrayToFile(new File(OUT_PATH, fileName + ".pdf"), actual.toByteArray());
    }
    
    private static PDDocument run(String fileName, BuilderConfig config) throws IOException {
        String absResPath = RES_PATH + fileName + ".html";
        
        byte[] htmlBytes = IOUtils
                .toByteArray(TestcaseRunner.class.getResourceAsStream(absResPath));
        
        String html = new String(htmlBytes, Charsets.UTF_8);

        render(fileName, html, config);
        
        return load(fileName);
    }
    
    private static PDDocument run(String filename) throws IOException {
        return run(filename, new BuilderConfig() {
            @Override
            public void configure(PdfRendererBuilder builder) {
            }
        });
    }
    
    private static PDDocument load(String filename) throws InvalidPasswordException, IOException {
        return PDDocument.load(new File(OUT_PATH, filename + ".pdf"));
    }
    
    private static void remove(String fileName, PDDocument doc) throws IOException {
        doc.close();
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
        PDDocument doc = run("meta-information");
        PDDocumentInformation did = doc.getDocumentInformation();
        
        assertThat(did.getTitle(), equalTo("Test title"));
        assertThat(did.getAuthor(), equalTo("Test author"));
        assertThat(did.getSubject(), equalTo("Test subject"));
        assertThat(did.getKeywords(), equalTo("Test keywords"));
        
        remove("meta-information", doc);
    }

    /**
     * Tests that a simple head bookmark linking to top of the second page works. 
     */
    @Test
    public void testBookmarkHeadSimple() throws IOException {
        PDDocument doc = run("bookmark-head-simple");
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

    /**
     * Tests that a head bookmark linking to transformed element (by way of transform) on third page works. 
     */
    @Test
    public void testBookmarkHeadTransform() throws IOException {
        PDDocument doc = run("bookmark-head-transform");
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
    
    /**
     * Tests that a head bookmark linking to element (on overflow page). 
     */
    @Test
    public void testBookmarkHeadOnOverflowPage() throws IOException {
        PDDocument doc = run("bookmark-head-on-overflow-page");
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

    /**
     * Tests that a head bookmark linking to an inline element (on page after overflow page) works. 
     */
    @Test
    public void testBookmarkHeadAfterOverflowPage() throws IOException {
        PDDocument doc = run("bookmark-head-after-overflow-page");
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
    
    /**
     * Tests that a nested head bookmark linking to top of the third page works. 
     */
    @Test
    public void testBookmarkHeadNested() throws IOException {
        PDDocument doc = run("bookmark-head-nested");
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
    
    /**
     * Tests that a simple block link successfully links to a simple block target on second page. 
     */
    @Test
    public void testLinkSimpleBlock() throws IOException {
        PDDocument doc = run("link-simple-block");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        // LINK: Top of first page, 100px by 10px.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(0f, 0f, 100f, 10f), 200d));
        
        assertThat(link.getAction(), instanceOf(PDActionGoTo.class));
        PDActionGoTo action = (PDActionGoTo) link.getAction();
        
        assertThat(action.getDestination(), instanceOf(PDPageXYZDestination.class));
        PDPageXYZDestination dest = (PDPageXYZDestination) action.getDestination();
        
        // TARGET: Top of second page.
        assertEquals(doc.getPage(1), dest.getPage());
        assertEquals(cssPixelYToPdfPoints(0, 200), dest.getTop(), 1.0d);
        
        remove("link-simple-block", doc);
    }
    
    /**
     * Tests that a simple block link successfully links to an element that is transformed to top of third page.
     */
    @Test
    public void testLinkTransformTarget() throws IOException {
        PDDocument doc = run("link-transform-target");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        // LINK: Top of first page, 100px by 10px.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(0f, 0f, 100f, 10f), 100d));
        
        assertThat(link.getAction(), instanceOf(PDActionGoTo.class));
        PDActionGoTo action = (PDActionGoTo) link.getAction();
        
        assertThat(action.getDestination(), instanceOf(PDPageXYZDestination.class));
        PDPageXYZDestination dest = (PDPageXYZDestination) action.getDestination();
        
        // TARGET: Top of third page.
        assertEquals(doc.getPage(2), dest.getPage());
        assertEquals(cssPixelYToPdfPoints(0, 100), dest.getTop(), 1.0d);
        
        remove("link-transform-target", doc);
    }
    
    /**
     * Tests that a link can successfully target a destination comprised of an inline element. 
     */
    @Test
    public void testLinkInlineTarget() throws IOException {
        PDDocument doc = run("link-inline-target");
        
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        
        assertThat(link.getAction(), instanceOf(PDActionGoTo.class));
        PDActionGoTo action = (PDActionGoTo) link.getAction();
        
        assertThat(action.getDestination(), instanceOf(PDPageXYZDestination.class));
        PDPageXYZDestination dest = (PDPageXYZDestination) action.getDestination();
        
        // TARGET: Top of second page.
        assertEquals(doc.getPage(1), dest.getPage());
        assertEquals(cssPixelYToPdfPoints(0, 100), dest.getTop(), 1.0d);
        
        remove("link-inline-target", doc);
    }
    
    /**
     * Tests that a simple block link successfully links to an element that is after an inserted overflow page.
     */
    @Test
    public void testLinkAfterOverflowTarget() throws IOException {
        PDDocument doc = run("link-after-overflow-target");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        // LINK: Top of first page, 80px by 10px (page margin is 10px).
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(10f, 10f, 80f, 10f), 100d));
        
        assertThat(link.getAction(), instanceOf(PDActionGoTo.class));
        PDActionGoTo action = (PDActionGoTo) link.getAction();
        
        assertThat(action.getDestination(), instanceOf(PDPageXYZDestination.class));
        PDPageXYZDestination dest = (PDPageXYZDestination) action.getDestination();
        
        // TARGET: Top of third page.
        assertEquals(doc.getPage(2), dest.getPage());
        assertEquals(cssPixelYToPdfPoints(10, 100), dest.getTop(), 1.0d);
        
        remove("link-after-overflow-target", doc);
    }
    
    /**
     * Tests that a simple block link successfully links to an element on an inserted overflow page.
     */
    @Test
    public void testLinkOnOverflowTarget() throws IOException {
        PDDocument doc = run("link-on-overflow-target");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        // LINK: Top of first page, 80px by 10px (page margin is 10px).
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(10f, 10f, 80f, 10f), 100d));
        
        assertThat(link.getAction(), instanceOf(PDActionGoTo.class));
        PDActionGoTo action = (PDActionGoTo) link.getAction();
        
        assertThat(action.getDestination(), instanceOf(PDPageXYZDestination.class));
        PDPageXYZDestination dest = (PDPageXYZDestination) action.getDestination();
        
        // TARGET: Top of third page.
        assertEquals(doc.getPage(2), dest.getPage());
        assertEquals(cssPixelYToPdfPoints(11, 100), dest.getTop(), 1.0d);
        
        remove("link-on-overflow-target", doc);
    }
    
    /**
     * Tests that link annotation area is correctly translated-y.
     */
    @Test
    public void testLinkAreaTransformTranslateY() throws IOException {
        PDDocument doc = run("link-area-transform-translatey");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        // 150px by 50px, top of page + 10px pg margin + 1px border + 50px translateY = 61px.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(10f, 61f, 150f, 50f), 200d));

        remove("link-area-transform-translatey", doc);
    }
    
    /**
     * Tests that link annotation area is correctly rotated.
     */
    @Test
    public void testLinkAreaTransformRotate() throws IOException {
        PDDocument doc = run("link-area-transform-rotate");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        // Confirmed by looking at the resulting PDF.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertEquals(11.4375, link.getRectangle().getLowerLeftX(), 1.0d);
        assertEquals(69.975, link.getRectangle().getLowerLeftY(), 1.0d);
        assertEquals(117.4875, link.getRectangle().getUpperRightX(), 1.0d);
        assertEquals(142.5, link.getRectangle().getUpperRightY(), 1.0d);

        remove("link-area-transform-rotate", doc);
    }
    
    /**
     * Tests link area on overflow page is correctly placed.
     */
    @Test
    public void testLinkAreaOverflowPage() throws IOException {
        PDDocument doc = run("link-area-overflow-page");
        
        assertEquals(0, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(1).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));

        // On second page (which is the first overflow page).
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(1).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(30f, 11f, 40f, 10f), 100d));
        
        remove("link-area-overflow-page", doc);
    }

    /**
     * Tests link area after overflow page is correctly placed.
     */
    @Test
    public void testLinkAreaAfterOverflowPage() throws IOException {
        PDDocument doc = run("link-area-after-overflow-page");
        
        assertEquals(0, doc.getPage(0).getAnnotations().size());
        assertEquals(0, doc.getPage(1).getAnnotations().size());

        assertEquals(1, doc.getPage(2).getAnnotations().size());
        assertThat(doc.getPage(2).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));

        // On third page (after the first overflow page).
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(2).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(10f, 10f, 80f, 10f), 100d));
        
        remove("link-area-after-overflow-page", doc);
    }

    /**
     * Tests that a link to an external url works correclty.
     */
    @Test
    public void testLinkExternalUrl() throws IOException {
        PDDocument doc = run("link-external-url");

        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));

        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        
        assertThat(link.getAction(), instanceOf(PDActionURI.class));
        PDActionURI action = (PDActionURI) link.getAction();
        
        assertEquals("https://openhtmltopdf.com", action.getURI());
        
        remove("link-external-url", doc);
    }
    
    /**
     * Tests a link area in the page margin.
     */
    @Test
    public void testLinkAreaPageMargin() throws IOException {
        PDDocument doc = run("link-area-page-margin");
        
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertEquals(1, doc.getPage(1).getAnnotations().size());
        
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(170f, 80f, 30f, 10f), 100d));

        // Should be repeated on page 2.
        link = (PDAnnotationLink) doc.getPage(1).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(170f, 80f, 30f, 10f), 100d));
        
        remove("link-area-page-margin", doc);
    }
    
    /**
     * Tests a link area inside a transformed element in the page margin.
     */
    @Ignore // Link annotation rectangle is not respecting the transform of its parent element.
    @Test
    public void testLinkAreaPageMarginTransform() throws IOException {
        PDDocument doc = run("link-area-page-margin-transform");
        
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertEquals(1, doc.getPage(1).getAnnotations().size());
        
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(170f, 70f, 10f, 30f), 100d));

        // Should be repeated on page 2.
        link = (PDAnnotationLink) doc.getPage(1).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(170f, 70f, 10f, 30f), 100d));
        
        remove("link-area-page-margin-transform", doc);
    }
    
    /**
     * Tests a link element inside a transformed element in the page content.
     */
    @Test
    public void testLinkAreaTransformNested() throws IOException {
        PDDocument doc = run("link-area-transform-nested");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertThat(doc.getPage(0).getAnnotations().get(0), instanceOf(PDAnnotationLink.class));
        
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(60f, 62f, 50f, 100f), 200d));

        remove("link-area-transform-nested", doc);
    }
    
    /**
     * Tests that a link area is created for each page (normal and overflow) that the link appears on.
     */
    @Test
    public void testLinkAreaMultiplePage() throws IOException {
        PDDocument doc = run("link-area-multiple-page");
        assertEquals(1, doc.getPage(0).getAnnotations().size());
        assertEquals(1, doc.getPage(1).getAnnotations().size());
        assertEquals(1, doc.getPage(2).getAnnotations().size());
        assertEquals(1, doc.getPage(3).getAnnotations().size());
        
        // First page.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(11f, 11f, 79f, 79f), 100d));

        // Overflow page for first page.
        link = (PDAnnotationLink) doc.getPage(1).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(10f, 11f, 61f, 79f), 100d));

        // Second page.
        link = (PDAnnotationLink) doc.getPage(2).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(11f, 10f, 79f, 71f), 100d));

        // Overflow page for second page.
        link = (PDAnnotationLink) doc.getPage(3).getAnnotations().get(0);
        assertThat(link.getRectangle(), rectEquals(new PDRectangle(10f, 10f, 61f, 71f), 100d));
        
        remove("link-area-multiple-page", doc);
    }
    
    /**
     * Tests that an inline link over multiple lines generates at least one link annotation for each line.
     */
    @Test
    public void testLinkAreaMultipleLine() throws IOException {
        PDDocument doc = run("link-area-multiple-line");
        
        // One link annotation for each line.
        assertEquals(2, doc.getPage(0).getAnnotations().size());
        
        // First line. Confirmed by looking at the resulting PDF.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertEquals(6.0, link.getRectangle().getLowerLeftX(), 1.0d);
        assertEquals(130.012, link.getRectangle().getLowerLeftY(), 1.0d);
        assertEquals(138.6, link.getRectangle().getUpperRightX(), 1.0d);
        assertEquals(144.0, link.getRectangle().getUpperRightY(), 1.0d);
        
        // Second line runs out of text before right side of page.
        link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(1);
        assertEquals(6.0, link.getRectangle().getLowerLeftX(), 1.0d);
        assertEquals(116.02, link.getRectangle().getLowerLeftY(), 1.0d);
        assertEquals(101.13, link.getRectangle().getUpperRightX(), 1.0d);
        assertEquals(130.01, link.getRectangle().getUpperRightY(), 1.0d);
        
        remove("link-area-multiple-line", doc);
    }
    
    /**
     * Tests that an inline link with multiple inline boxes generates one link annotation for each line.
     * ie. Multiple inline boxes are concatenated into one rect for the purposes of creating a link area.
     */
    @Test
    public void testLinkAreaMultipleBoxes() throws IOException {
        PDDocument doc = run("link-area-multiple-boxes");
        
        // One link annotation for each line.
        assertEquals(2, doc.getPage(0).getAnnotations().size());
        
        // First line. Confirmed by looking at the resulting PDF.
        PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
        assertEquals(6.0, link.getRectangle().getLowerLeftX(), 1.0d);
        assertEquals(130.012, link.getRectangle().getLowerLeftY(), 1.0d);
        assertEquals(138.6, link.getRectangle().getUpperRightX(), 1.0d);
        assertEquals(144.0, link.getRectangle().getUpperRightY(), 1.0d);
        
        // Second line runs out of text before right side of page.
        link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(1);
        assertEquals(6.0, link.getRectangle().getLowerLeftX(), 1.0d);
        assertEquals(113.28, link.getRectangle().getLowerLeftY(), 1.0d);
        assertEquals(112.57, link.getRectangle().getUpperRightX(), 1.0d);
        assertEquals(127.27, link.getRectangle().getUpperRightY(), 1.0d);
        
        remove("link-area-multiple-boxes", doc);
    }
    
    /**
     * Tests the positioning, size, name and value of a text type form control.
     */
    @Test
    public void testFormControlText() throws IOException {
        PDDocument doc = run("form-control-text");
        
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
    
    /**
     * Tests the positioning, size, name and value of a form control on an overflow page.
     */
    @Test
    public void testFormControlOverflowPage() throws IOException {
        PDDocument doc = run("form-control-overflow-page");
        
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
    
    // TODO:
    // + Form controls plus on/after overflow page.
    // + Custom meta info.
}
