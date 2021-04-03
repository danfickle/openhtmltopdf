package com.openhtmltopdf.visualtest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.util.JDKXRLogger;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;


/**
 * Based on library PDFCompare by Github user red6.
 * https://github.com/red6/pdfcompare
 * 
 * Given a resource, this will use the project renderer to convert it from html to PDF.
 * It will then visual compare the generated PDF against an expected version (of the same name)
 * in the override or expected path provided.
 * 
 * If they differ, a diff image will be output to the output path along with the generated pdf.
 * Hopefully, this will allow the user to easily see where the PDF has changed compared to the expected
 * version.
 */

public class VisualTester {
    @FunctionalInterface
    public interface BuilderConfig {
        public void configure(PdfRendererBuilder builder);
    }
    
    private final String resourcePath;
    private final String primaryPath;
    private final File outputPath;

    public VisualTester(String resourceHtmlPath, String pathExpectedPrimary, File pathOutput) {
        this.resourcePath = resourceHtmlPath;
        this.primaryPath = pathExpectedPrimary;
        this.outputPath = pathOutput;
    }
    
    private byte[] runRenderer(String resourcePath, String html, BuilderConfig config) {
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, VisualTester.class.getResource(this.resourcePath).toString());
        builder.toStream(actual);
        builder.useFastMode();
        builder.testMode(true);
        config.configure(builder);
        try {
            builder.run();
        } catch (Exception e) {
            System.err.println("Failed to render resource (" + resourcePath + ")");
            e.printStackTrace();
            return null;
        }
        
        return actual.toByteArray();
    }
    
    public boolean runTest(String resource) throws IOException {
        return runTest(resource, builder -> {});
    }
    
    private StringBuilder logToStringBuilder() {
        final XRLogger delegate = new JDKXRLogger();
        final StringBuilder sb = new StringBuilder();
        XRLog.setLoggerImpl(new TestSupport.StringBuilderLogger(sb, delegate));
        return sb;
    }

    public boolean runTest(String resource, BuilderConfig additionalBuilderConfiguration) throws IOException {
        String absResPath = this.resourcePath + resource + ".html";
        String absExpPath = this.primaryPath + resource + ".pdf";
        
        String html;
        
        try (InputStream htmlIs = TestcaseRunner.class.getResourceAsStream(absResPath)) {
            byte[] htmlBytes = IOUtils.toByteArray(htmlIs);
            html = new String(htmlBytes, StandardCharsets.UTF_8);
        }

        StringBuilder sb = logToStringBuilder();
        byte[] actualPdfBytes = runRenderer(resourcePath, html, additionalBuilderConfiguration);
        
        if (actualPdfBytes == null) {
            System.err.println("When running test (" + resource + "), rendering failed, writing log to failure file.");
            File output = new File(this.outputPath, resource + ".failure.txt");
            FileUtils.writeByteArrayToFile(output, sb.toString().getBytes(StandardCharsets.UTF_8));
            return false;
        }

        if (TestcaseRunner.class.getResource(absExpPath) == null) {
            System.err.println("When running test (" + resource + "), nothing to compare against as resource (" + absResPath + ") does not exist.");
            System.err.println("Writing generated PDF to file instead in output directory (" + this.outputPath +    ")");
            File output = new File(this.outputPath, resource + ".pdf");
            Files.write(output.toPath(), actualPdfBytes);
            return false;
        }
        
        byte[] expectedPdfBytes;
        
        try (InputStream expectedIs = TestcaseRunner.class.getResourceAsStream(absExpPath)) {
            expectedPdfBytes = IOUtils.toByteArray(expectedIs);
        }
        
        List<PdfCompareResult> problems = PdfVisualTester.comparePdfDocuments(expectedPdfBytes, actualPdfBytes, resource, false);
        
        if (!problems.isEmpty()) {
            System.err.println("Found problems with test case (" + resource + "):");
            System.err.println(problems.stream().map(p -> p.logMessage).collect(Collectors.joining("\n    ", "[\n    ", "\n]")));
            
            File outPdf = new File(this.outputPath, resource + "---actual.pdf");
            Files.write(outPdf.toPath(), actualPdfBytes);
        }
        
        if (problems.stream().anyMatch(p -> p.testImages != null)) {
            System.err.println("For test case (" + resource + ") writing diff images to '" + this.outputPath + "'");
        }
        
        for (PdfCompareResult result : problems) {
            if (result.testImages != null) {
                File output = new File(this.outputPath, resource + "---" + result.pageNumber + "---diff.png");
                ImageIO.write(result.testImages.createDiff(), "png", output);
                
                output = new File(this.outputPath, resource + "---" + result.pageNumber + "---actual.png");
                ImageIO.write(result.testImages.getActual(), "png", output);
                
                output = new File(this.outputPath, resource + "---" + result.pageNumber + "---expected.png");
                ImageIO.write(result.testImages.getExpected(), "png", output);
            }
        }
        
        return problems.isEmpty();
    }
}
