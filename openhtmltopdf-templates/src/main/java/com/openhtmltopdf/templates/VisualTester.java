package com.openhtmltopdf.templates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult;

public class VisualTester {
    private final File outputPath;

    public VisualTester(File outputPath) {
        this.outputPath = outputPath;
        this.outputPath.mkdirs();
    }

    public boolean testPdfEquality(byte[] expectedPdfBytes, byte[] actualPdfBytes, String resource) throws IOException {
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
