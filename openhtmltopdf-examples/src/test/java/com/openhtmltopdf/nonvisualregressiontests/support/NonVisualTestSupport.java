package com.openhtmltopdf.nonvisualregressiontests.support;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.openhtmltopdf.nonvisualregressiontests.NonVisualRegressionTest;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class NonVisualTestSupport {
    public static class TestDocument implements Closeable {
        final Path filePath;
        final PDDocument doc;
        boolean delete;

        TestDocument(Path filePath, PDDocument doc, boolean delete) {
            this.filePath = filePath;
            this.doc = doc;
            this.delete = delete;
        }

        public PDDocument doc() {
            return doc;
        }

        public void close() throws IOException {
            OpenUtil.closeQuietly(doc);
            if (this.delete) {
                Files.delete(filePath);
                this.delete = false;
            }
        }
    }

    final String baseResPath;
    final String outPath;

    public NonVisualTestSupport(String baseResourcePath, String outFilePath) {
        this.baseResPath = baseResourcePath;
        this.outPath = outFilePath;
    }

    private void render(
            String fileName, String html, BuilderConfig config) throws IOException {
        ByteArrayOutputStream actual = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, NonVisualRegressionTest.class.getResource(baseResPath).toString());
        builder.toStream(actual);
        builder.useFastMode();
        builder.testMode(true);
        config.configure(builder);

        builder.run();

        writePdfToFile(fileName, actual);
    }

    private void writePdfToFile(String fileName, ByteArrayOutputStream actual) throws IOException {
        Files.write(Paths.get(outPath, fileName + ".pdf"), actual.toByteArray());
    }

    private String loadHtml(String fileName) throws IOException {
        String absResPath = baseResPath + fileName + ".html";

        try (InputStream is = TestcaseRunner.class.getResourceAsStream(absResPath)) {
            return new String(OpenUtil.readAll(is), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("resource")
    public TestDocument run(String fileName, BuilderConfig config, boolean delete) throws IOException {
        String html = loadHtml(fileName);

        render(fileName, html, config);

        return new TestDocument(Paths.get(outPath, fileName + ".pdf"), load(fileName), delete);
    }

    public TestDocument run(String fileName, BuilderConfig config) throws IOException {
        return run(fileName, config, true);
    }

    public TestDocument run(String filename, boolean delete) throws IOException {
        return run(filename, b -> {}, delete);
    }

    public TestDocument run(String filename) throws IOException {
        return run(filename, true);
    }

    private PDDocument load(String filename) throws IOException {
        return PDDocument.load(new File(outPath, filename + ".pdf"));
    }
}
