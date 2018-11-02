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
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.util.Charsets;
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
    
    private static void remove(String fileName) {
        new File(OUT_PATH, fileName + ".pdf").delete();
    }
    
    private static double cssPixelsToPdfPoints(double cssPixels) {
        return cssPixels * 72d / 96d;
    }
    
    private static double cssPixelYToPdfPoints(double cssPixelsY, double cssPixelsPageHeight) {
        return cssPixelsToPdfPoints(cssPixelsPageHeight - cssPixelsY);
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
        
        remove("meta-information");
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
        
        remove("bookmark-head-simple");
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
        
        remove("bookmark-head-transform");
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
        
        remove("bookmark-head-on-overflow-page");
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
        
        remove("bookmark-head-after-overflow-page");
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
        
        remove("bookmark-head-nested");
    }


    // TODO:
    // + Bookmark target on/after overflow page.
    // + Link simple
    // + Link with target after generated overflow pages.
    // + Link with target on generated overflow page.
    // + Link with active area on generated overflow page.
    // + Link with active area after generated overflow pages.
    // + Form controls plus on/after overflow page.
    // + Custom meta info.
}
