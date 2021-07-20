package com.openhtmltopdf.nonvisualregressiontests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

@RunWith(PrintingRunner.class)
public class JsoupNonVisualRegressionTest {
    private static final String RES_PATH = "/visualtest/html/";
    private static final String OUT_PATH = "target/test/visual-tests/test-output/";

    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    private static void render(String fileName, BuilderConfig config) throws IOException {
         String resource = RES_PATH + fileName + ".html";

         try (InputStream is = NonVisualRegressionTest.class.getResourceAsStream(resource)) {
             org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(is, "UTF-8", NonVisualRegressionTest.class.getResource(resource).toString());
             Document doc = new W3CDom().fromJsoup(jsoupDoc);

             ByteArrayOutputStream actual = new ByteArrayOutputStream();

             PdfRendererBuilder builder = new PdfRendererBuilder();
             builder.withW3cDocument(doc, NonVisualRegressionTest.class.getResource(RES_PATH).toString());
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
    }

    private static PDDocument load(String filename) throws IOException {
        return PDDocument.load(new File(OUT_PATH, filename + ".pdf"));
    }
    
    private PDDocument run(String fileName, BuilderConfig config) throws IOException {
        render(fileName, config);

        return load(fileName);
    }

    /**
     * Tests that the example provided by swarl in issue 401 does not cause an
     * NPE.
     * 
     * https://github.com/danfickle/openhtmltopdf/issues/401
     */
    @Test
    @Ignore // Causes an NPE!
    public void testIssue401PdfANpe() throws IOException {
        try (PDDocument doc = run("issue-401-pdf-a-npe", 
             (builder) -> { 
                 builder.usePdfAConformance(PdfAConformance.PDFA_3_A);
                 builder.useFont(() -> PDDocument.class.getClassLoader().getResourceAsStream("org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"),
                         "Liberation Sans");
             })) {
            // Do nothing, we're just testing that we don't throw.
        }
    }

    /**
     * Tests a now-fixed near-infinite loop when page-break-inside: avoid
     * is used on heavily nested content.
     */
    @Test
    public void testIssue551PageBreakAvoidStuck() throws IOException {
        try (PDDocument doc = run("issue-551-page-break-avoid-stuck", TestSupport.WITH_FONT)) {
            assertEquals(3, doc.getNumberOfPages());
        }
    }
}
