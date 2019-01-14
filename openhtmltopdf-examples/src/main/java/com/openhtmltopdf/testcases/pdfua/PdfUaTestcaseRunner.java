package com.openhtmltopdf.testcases.pdfua;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.io.IOUtils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class PdfUaTestcaseRunner {
    private static void run(String testCase) throws Exception {
        byte[] htmlBytes = IOUtils
                .toByteArray(PdfUaTestcaseRunner.class.getResourceAsStream("/testcases/pdfua/" + testCase + ".html"));
        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        
        new File("./target/pdfua-test-cases/").mkdirs();
        
        if (!(new File("target/test/visual-tests/Karla-Bold.ttf")).exists()) {
            try (InputStream in = PdfUaTestcaseRunner.class.getResourceAsStream("/visualtest/html/fonts/Karla-Bold.ttf");
                    OutputStream out = new FileOutputStream("target/test/visual-tests/Karla-Bold.ttf")) {
                    IOUtils.copy(in, out);
            }
        }
        
        try (FileOutputStream os = new FileOutputStream("./target/pdfua-test-cases/" + testCase + ".pdf")) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.testMode(true);
            builder.usePdfUaAccessbility(true);
            builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
            builder.withHtmlContent(html, PdfUaTestcaseRunner.class.getResource("/testcases/pdfua/").toString());
            builder.toStream(os);
            builder.run();
        }
        
    }
    
    public static void main(String... args) throws Exception {
        run("simple");
        run("simplest");
    }
}
