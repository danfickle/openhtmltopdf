package com.openhtmltopdf.testcases;

import static com.openhtmltopdf.testcases.TestcaseRunner.buildObjectDrawerFactory;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.latexsupport.LaTeXDOMMutator;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;

@RunWith(PrintingRunner.class)
public class ConcateOutputTest {
    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    @Test
    public void testConcateOutput() throws Exception {
        File targetFile = new File("target/test/concatoutput/concated.pdf");
        targetFile.getParentFile().mkdirs();

        try (PDDocument doc = new PDDocument(MemoryUsageSetting.setupMixed(1_000_000))) {
            for (String testCaseFile : Arrays.asList(
                    "color", "background-color", "FSPageBreakMinHeightSample",
                    "math-ml", "multi-column-layout", "simplerotate", "svg-inline", "svg-sizes", "transform",
                    "RepeatedTableSample", "RepeatedTableTransformSample" )) {
                renderPDF(testCaseFile, doc);
            }

            assertEquals(27, doc.getNumberOfPages());

            doc.save(targetFile);
        }
    }

    private static void renderPDF(String testCaseFile, PDDocument document) throws IOException {
        String html;
        try (InputStream is = TestcaseRunner.class.getResourceAsStream("/testcases/" + testCaseFile + ".html")) {
            byte[] htmlBytes = IOUtils.toByteArray(is);
            html = new String(htmlBytes, StandardCharsets.UTF_8);
        }

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
            builder.withHtmlContent(html, TestcaseRunner.class.getResource("/testcases/").toString());
            builder.usePDDocument(document);

            try (PdfBoxRenderer pdfBoxRenderer = builder.buildPdfRenderer()) {
                pdfBoxRenderer.createPDFWithoutClosing();
            }
        }
    }
}
