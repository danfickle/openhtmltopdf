package com.openhtmltopdf.visualtest;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;

import com.openhtmltopdf.java2d.api.BufferedImagePageProcessor;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.util.JDKXRLogger;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;
import com.openhtmltopdf.util.XRSimpleLogFormatter;

public class Java2DVisualTester {
    @FunctionalInterface
    public interface Java2DBuilderConfig {
        public void configure(Java2DRendererBuilder builder);
    }

    private final String resourcePath;
    private final String primaryPath;
    private final String expectedSinglePage;
    private final File outputPath;

    public Java2DVisualTester(
            String resourceHtmlPath,
            String pathExpectedPrimary,
            String expectedSinglePage,
            File pathOutput) {
        this.resourcePath = resourceHtmlPath;
        this.primaryPath = pathExpectedPrimary;
        this.expectedSinglePage = expectedSinglePage;
        this.outputPath = pathOutput;
    }

    private BufferedImage runRendererSingle(String resourcePath, String html, Java2DBuilderConfig config) {
        Java2DRendererBuilder builder = new Java2DRendererBuilder();
        builder.withHtmlContent(html, Java2DVisualTester.class.getResource(this.resourcePath).toString());
        builder.useFastMode();
        builder.testMode(true);

        BufferedImagePageProcessor bufferedImagePageProcessor = new BufferedImagePageProcessor(
                BufferedImage.TYPE_INT_RGB, 1.0);

        builder.toSinglePage(bufferedImagePageProcessor);

        TestSupport.J2D_WITH_FONT.configure(builder);

        config.configure(builder);

        try {
            builder.runFirstPage();
        } catch (Exception e) {
            System.err.println("Failed to render resource (" + resourcePath + ")");
            e.printStackTrace();
            return null;
        }

        return bufferedImagePageProcessor.getPageImages().get(0);
    }

    private List<BufferedImage> runRendererPaged(String resourcePath, String html, Java2DBuilderConfig config) {
        Java2DRendererBuilder builder = new Java2DRendererBuilder();
        builder.withHtmlContent(html, Java2DVisualTester.class.getResource(this.resourcePath).toString());
        builder.useFastMode();
        builder.testMode(true);

        BufferedImagePageProcessor bufferedImagePageProcessor = new BufferedImagePageProcessor(
                BufferedImage.TYPE_INT_RGB, 1.0);

        builder.toPageProcessor(bufferedImagePageProcessor);

        TestSupport.J2D_WITH_FONT.configure(builder);

        config.configure(builder);

        try {
            builder.runPaged();
        } catch (Exception e) {
            System.err.println("Failed to render resource (" + resourcePath + ")");
            e.printStackTrace();
            return null;
        }

        return bufferedImagePageProcessor.getPageImages();
    }
    
    public boolean runTest(String resource) throws IOException {
        return runTest(resource, builder -> {});
    }
    
    private StringBuilder logToStringBuilder() {
        final XRLogger delegate = new JDKXRLogger(false, Level.WARNING, new ConsoleHandler(), new XRSimpleLogFormatter());
        final StringBuilder sb = new StringBuilder();
        XRLog.setLoggerImpl(new TestSupport.StringBuilderLogger(sb, delegate));
        return sb;
    }
    
    private void writeOutImages(List<BufferedImage> images, File path, String prefix) throws IOException {
        int i = 0;
        for (BufferedImage image : images) {
            ImageIO.write(image, "png", new File(path, prefix + (i++) + ".png"));
        }
    }

    public boolean runSinglePageTest(String resource, Java2DBuilderConfig additionalBuilderConfiguration) throws IOException {
        String absResPath = this.resourcePath + resource + ".html";
        String absExpPath = this.expectedSinglePage + resource + ".png";
        File outputPath = new File(this.outputPath, resource + "-single-page/");

        String html = readHtml(absResPath);
        StringBuilder sb = logToStringBuilder();
        BufferedImage actual = runRendererSingle(resourcePath, html, additionalBuilderConfiguration);

        if (actual == null) {
            System.err.println("When running test (" + resource + ") on single-page mode, rendering failed, writing log to failure file.");
            File output = new File(this.outputPath, resource + ".failure.txt");
            FileUtils.writeByteArrayToFile(output, sb.toString().getBytes(StandardCharsets.UTF_8));
            return false;
        } else if (TestcaseRunner.class.getResource(absExpPath) == null) {
            System.err.println("When running test (" + resource + ") on single-page mode, nothing to compare against as resource (" + absExpPath + ") does not exist.");

            outputPath.mkdirs();
            File output = new File(outputPath, resource + ".png");
            ImageIO.write(actual, "png", output);

            return false;
        } else {
            BufferedImage expectedPng = ImageIO.read(new ByteArrayInputStream(readExpected(absExpPath)));
            BufferedImage actualPng = ImageIO.read(new ByteArrayInputStream(toPng(actual)));
            BufferedImage diffImage = compareImages(expectedPng, actualPng);

            if (diffImage != null) {
                System.err.println("When running test (" + resource + ") on single-page mode, there were differences.");

                outputPath.mkdirs();

                File output = new File(outputPath, "/diff.png");
                ImageIO.write(diffImage, "png", output);

                output = new File(outputPath, resource + ".png");
                ImageIO.write(actualPng, "png", output);

                output = new File(outputPath, "/expected.png");
                ImageIO.write(expectedPng, "png", output);

                return false;
            }
        }

        return true;
    }

    private String readHtml(String absResPath) throws IOException {
        try (InputStream htmlIs = TestcaseRunner.class.getResourceAsStream(absResPath)) {
            byte[] htmlBytes = IOUtils.toByteArray(htmlIs);
            return new String(htmlBytes, StandardCharsets.UTF_8);
        }
    }

    public boolean runTest(String resource, Java2DBuilderConfig additionalBuilderConfiguration) throws IOException {
        String absResPath = this.resourcePath + resource + ".html";
        String absExpPath = this.primaryPath + resource + "/";
        File outputPath = new File(this.outputPath, resource + "/");

        String html = readHtml(absResPath);
        StringBuilder sb = logToStringBuilder();

        List<BufferedImage> actualPages = runRendererPaged(resourcePath, html, additionalBuilderConfiguration);

        boolean failed = false;

        if (actualPages == null) {
            System.err.println("When running test (" + resource + "), rendering failed, writing log to failure file.");
            File output = new File(this.outputPath, resource + ".failure.txt");
            FileUtils.writeByteArrayToFile(output, sb.toString().getBytes(StandardCharsets.UTF_8));
            return false;
        }

        List<BufferedImage> expectedPages = new ArrayList<>();
        int expectedCnt = 0;

        // We go a few over to check the size of the expected document is not larger than the actual document.
        while (expectedCnt < actualPages.size() + 10) {
            String expectedPath = absExpPath + expectedCnt + ".png";

            if (TestcaseRunner.class.getResource(expectedPath) == null && expectedCnt < actualPages.size()) {
                System.err.println("When running test (" + resource + ") on page(" + expectedCnt + "), nothing to compare against as resource (" + expectedPath + ") does not exist.");
                failed = true;
            } else if (TestcaseRunner.class.getResource(expectedPath) == null) {
                // Do nothing, we must be after the actual page count.
            } else {
                byte[] expectedBytes = readExpected(expectedPath);
                expectedPages.add(ImageIO.read(new ByteArrayInputStream(expectedBytes)));
            }

            expectedCnt++;
        }

        if (expectedPages.size() != actualPages.size()) {
            System.err.println("When running test (" + resource + "), page count did not match (actual=" + actualPages.size() + ", expected=" + expectedPages.size() + ")");
        }

        List<byte[]> actualPngBytes = actualPages.stream()
                .map(Java2DVisualTester::toPng)
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(actualPngBytes.size(), expectedPages.size()); i++) {
            BufferedImage expected = expectedPages.get(i);
            BufferedImage actualPng = ImageIO.read(new ByteArrayInputStream(actualPngBytes.get(i)));
            BufferedImage diffImage = compareImages(expected, actualPng);

            if (diffImage != null) {
                System.err.println("Page " + i + " for (" + resource + ") was different to expected");
                failed = true;
                outputPath.mkdirs();
                File output = new File(outputPath, "/diff-" + i + ".png");
                ImageIO.write(diffImage, "png", output);
            }
        }

        if (failed) {
            outputPath.mkdirs();
            System.err.println("When running test (" + resource + "), there were differences.");
            writeOutImages(actualPages, outputPath, "");
            writeOutImages(expectedPages, outputPath, "expected-");
        }

        return !failed;
    }

    private static byte[] toPng(BufferedImage actual) {
        // We need the saved PDF image data as ImageIO can change the color depth
        // meaning that out isImageDifferent function will not work with unsaved vs
        // saved data.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(actual, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    private byte[] readExpected(String expectedPath) throws IOException {
        try (InputStream expectedIs = TestcaseRunner.class.getResourceAsStream(expectedPath)) {
            return IOUtils.toByteArray(expectedIs);
        }
    }

    private BufferedImage compareImages(BufferedImage expected, BufferedImage actual) {
        if (!isImageDifferent(expected, actual)) {
            return null;
        }
        
        return PdfVisualTester.createDiffImage(expected, actual);
    }
    
    /**
     * Gets the data buffer of each image and compares.
     * NOTE: May be an expensive (memory and CPU) operation for large images.
     * @param imgExpected
     * @param imgActual
     * @return whether imgExpected is different image compared to imgActual
     */
    private static boolean isImageDifferent(BufferedImage imgExpected, BufferedImage imgActual) {
        DataBuffer dbExpected = imgExpected.getData().getDataBuffer();
        DataBuffer dbActual = imgActual.getData().getDataBuffer();
        
        if (dbExpected.getDataType() != dbActual.getDataType()) {
            System.out.println("Different image types (actual=" + dbActual.getDataType() + ", expected=" + dbExpected.getDataType());
            return true;
        }
        
        switch (dbExpected.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            return !Arrays.equals(
                        ((DataBufferByte) dbExpected).getData(), ((DataBufferByte) dbActual).getData());
        case DataBuffer.TYPE_SHORT:
            return !Arrays.equals(
                    ((DataBufferShort) dbExpected).getData(), ((DataBufferShort) dbActual).getData());
        case DataBuffer.TYPE_USHORT:
            return !Arrays.equals(
                    ((DataBufferUShort) dbExpected).getData(), ((DataBufferUShort) dbActual).getData());
        case DataBuffer.TYPE_INT:
            return !Arrays.equals(
                    ((DataBufferInt) dbExpected).getData(), ((DataBufferInt) dbActual).getData());
        case DataBuffer.TYPE_FLOAT:
            return !Arrays.equals(
                    ((DataBufferFloat) dbExpected).getData(), ((DataBufferFloat) dbActual).getData());
        case DataBuffer.TYPE_DOUBLE:
            return !Arrays.equals(
                    ((DataBufferDouble) dbExpected).getData(), ((DataBufferDouble) dbActual).getData());
        case DataBuffer.TYPE_UNDEFINED:
            return true;
        }
        
        return true;
    }
}
