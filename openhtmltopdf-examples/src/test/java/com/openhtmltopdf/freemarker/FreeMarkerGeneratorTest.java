package com.openhtmltopdf.freemarker;

import com.openhtmltopdf.freemarker.FreeMarkerGenerator.FreemarkerRootObject;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.visualtest.TestSupport;

import freemarker.template.TemplateException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RunWith(PrintingRunner.class)
public class FreeMarkerGeneratorTest {
    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    private File targetDir;

    @Before
    public void makeTarget() {
        this.targetDir = new File("target/test/freemarker");
        this.targetDir.mkdirs();
    }

    @Test
    public void testFreeMarkerGenerator() throws IOException, TemplateException {
        FreeMarkerGenerator freeMarkerGenerator = new FreeMarkerGenerator();
        FreemarkerRootObject object = new FreemarkerRootObject();

        String html = freeMarkerGenerator.generateHTML("featuredocumentation.ftl", Locale.GERMAN, object);
        byte[] pdf = freeMarkerGenerator.generatePDF(html);

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(targetDir, "featuredocumentation.html"))) {
            fileOutputStream.write(html.getBytes(StandardCharsets.UTF_8));
        }

        File pdfFile = new File(targetDir, "featuredocumentation.pdf");

        try (FileOutputStream fileOutputStream = new FileOutputStream(pdfFile)) {
            fileOutputStream.write(pdf);
        }

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertEquals(6, doc.getNumberOfPages());
        }
    }

    @Test
    public void testFreeMarkerWithManyPages() throws IOException, TemplateException {
        /*
         * We really should disable logging here, as it takes ages anyway to generate
         * the report...
         */
        XRLog.setLoggingEnabled(false);

        FreeMarkerGenerator freeMarkerGenerator = new FreeMarkerGenerator();
        FreemarkerRootObject object = new FreemarkerRootObject();

        File htmlFile = new File(targetDir, "many_pages.html");
        freeMarkerGenerator.generateHTMLToFile("many_pages.ftl", Locale.GERMAN, object, htmlFile);

        byte[] pdf = freeMarkerGenerator.generatePDF(htmlFile);

        File pdfFile = new File(targetDir, "many_pages.pdf");

        try (FileOutputStream fileOutputStream = new FileOutputStream(pdfFile)) {
            fileOutputStream.write(pdf);
        }

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertEquals(31, doc.getNumberOfPages());
        }

        XRLog.setLoggingEnabled(true);
    }
}