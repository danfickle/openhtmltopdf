package com.openhtmltopdf.testcases;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.pdfbox.io.IOUtils;
import org.junit.Test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

/**
 * Tests for PDF accessiblility (PDF/UA, WCAG, Section 508).
 * These tests only test that the PDF/UA implementation doesn't crash.
 * They additionally need confirming manually with the PDF Accessibility Checker (PAC).
 * This free (but closed source) Windows software is available at:
 *   https://www.access-for-all.ch/en/pdf-lab/pdf-accessibility-checker-pac.html
 *
 *  If you don't want to confirm all, please at least confirm the all-in-one testcase!
 */
public class PdfUaTestcaseRunnerTest {
    private static void run(String testCase) throws Exception {

        byte[] htmlBytes = null;
        try (InputStream is = PdfUaTestcaseRunnerTest.class.getResourceAsStream("/testcases/pdfua/" + testCase + ".html")) {
            htmlBytes = IOUtils.toByteArray(is);
        }
        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        
        Files.createDirectories(Paths.get("target/test/visual-tests/"));
        if (!Files.exists(Paths.get("target/test/visual-tests/Karla-Bold.ttf"))) {
            try (InputStream in = PdfUaTestcaseRunnerTest.class.getResourceAsStream("/visualtest/html/fonts/Karla-Bold.ttf")) {
                Files.copy(in, Paths.get("target/test/visual-tests/Karla-Bold.ttf"));
            }
        }
        
        Files.createDirectories(Paths.get("./target/pdfua-test-cases/"));
        try (FileOutputStream os = new FileOutputStream("./target/pdfua-test-cases/" + testCase + ".pdf")) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.testMode(true);
            builder.usePdfUaAccessbility(true);
            builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
            builder.withHtmlContent(html, PdfUaTestcaseRunnerTest.class.getResource("/testcases/pdfua/").toString());
            builder.toStream(os);
            builder.run();
        }
    }
    
    @Test
    public void testAllInOne() throws Exception {
        run("all-in-one");
    }

    @Test
    public void testSimplest() throws Exception {
        run("simplest");
    }
    
    @Test
    public void testSimple() throws Exception {
        run("simple");
    }
    
    @Test
    public void testLayersZIndex() throws Exception {
        run("layers-z-index");
    }
    
    @Test
    public void testTextOverTwoPages() throws Exception {
        run("text-over-two-pages");
    }
    
    @Test
    public void testImage() throws Exception {
        run("image");
    }
    
    @Test
    public void testImageOverTwoPages() throws Exception {
        run("image-over-two-pages");
    }
    
    @Test
    public void testRunning() throws Exception {
        run("running");
    }
    
    @Test
    public void testLists() throws Exception {
        run("lists");
    }
    
    @Test
    public void testBookmarks() throws Exception {
        run("bookmarks");
    }
    
    @Test
    public void testTables() throws Exception {
        run("tables");
    }
    
    @Test
    public void testOrdering() throws Exception {
        run("ordering");
    }
    
    @Test
    public void testLinks() throws Exception {
        run("links");
    }
}
