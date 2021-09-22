package com.openhtmltopdf.pdfboxout.visualtester;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfVisualTester {
    private static final int LEFT_MARGIN_PX = 45;
    private static final int LINE_HEIGHT_PX = 17;
    private static final BufferedImage ONE_PX_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    
    public static class TestImage {
        /**
         * @return the test name passed into {@link PdfVisualTester#comparePdfDocuments(byte[], byte[], String, boolean)}
         */
        public String getTestName() {
            return testName;
        }

        /**
         * @return the page number.
         */
        public int getPageNumber() {
            return pageNumber;
        }

        /**
         * Get the expected rendered image.
         * @return the expected image, should never be null.
         */
        public BufferedImage getExpected() {
            return expected;
        }

        /**
         * Get the actual rendered image.
         * @return the actual image, may be null if expected has more pages than actual
         */
        public BufferedImage getActual() {
            return actual;
        }

        /**
         * Creates a diff image to quickly spot differences between expected and actual.
         * NOTE: May be expensive for large images.
         * @return the diff image.
         * See {@link #hasDifferences}
         */
        public BufferedImage createDiff() {
            if (actual == null) {
                return createDiffImage(expected, PdfVisualTester.ONE_PX_IMAGE);
            }

            return createDiffImage(expected, actual);
        }
        
        /**
         * Should usually be called before {@link #createDiff()}.
         * @return true if the images are different. Precaulculated.
         */
        public boolean hasDifferences() {
            return differences;
        }

        private final String testName;
        private final int pageNumber;
        private final BufferedImage expected;
        private final BufferedImage actual;
        private final boolean differences;
        
        private TestImage(String test, int pageNo, BufferedImage exp, BufferedImage act, boolean hasDifferences) {
            this.expected = exp;
            this.actual = act;
            this.testName = test;
            this.pageNumber = pageNo;
            this.differences = hasDifferences;
        }
    }
    
    public enum ProblemType {
        PAGE_GOOD,
        NO_EXPECTED_DOCUMENT_PROVIDED,
        NO_ACTUAL_DOCUMENT_PROVIDED,
        EXTRA_EXPECTED_PAGE,
        PAGE_COUNT_DIFFERENT,
        PAGE_SIZE_DIFFERENT,
        PAGE_VISUALLY_DIFFERENT;
    }
    
    public static class PdfCompareResult {
        public final ProblemType type;
        public final String logMessage;
        public final int pageNumber;
        public final TestImage testImages;
        public static final int INALID_PAGE_NO = -1;
        
        private PdfCompareResult(ProblemType type, String logMessage, int pageNumber, TestImage testImage) {
            this.type = type;
            this.logMessage = type.toString() + ": " + logMessage;
            this.pageNumber = pageNumber;
            this.testImages = testImage;
        }
    }
    
    /**
     * Compares two PDF documents by rendering each page to an image and comparing pixel by pixel.
     * @param expected
     * @param actual
     * @param testName
     * @param keepSameImages Whether to return the images in the case they are good (ie. the same).
     * @return A list of {@link PdfCompareResult} instances describing differences.
     */
    public static List<PdfCompareResult> comparePdfDocuments(byte[] expected, byte[] actual, String testName, boolean keepSameImages) throws IOException {
        List<PdfCompareResult> problems = new ArrayList<>();
        
        if (expected == null ||
            expected.length == 0) {
            problems.add(
                    new PdfCompareResult(
                       ProblemType.NO_EXPECTED_DOCUMENT_PROVIDED,
                       String.format("Test name='%s'", testName),
                       PdfCompareResult.INALID_PAGE_NO,
                       null
                    ));
        }
        
        if (actual == null ||
            actual.length == 0) {
            problems.add(
                    new PdfCompareResult(
                       ProblemType.NO_ACTUAL_DOCUMENT_PROVIDED,
                       String.format("Test name='%s'", testName),
                       PdfCompareResult.INALID_PAGE_NO,
                       null
                    ));
        }
        
        if (!problems.isEmpty()) {
            return problems;
        }
        
        try (PDDocument docActual = PDDocument.load(actual);
             PDDocument docExpected = PDDocument.load(expected)) {

            PDFRenderer rendActual = new PDFRenderer(docActual);
            PDFRenderer rendExpected = new PDFRenderer(docExpected);

            if (docActual.getNumberOfPages() != docExpected.getNumberOfPages()) {
                problems.add(
                     new PdfCompareResult(
                        ProblemType.PAGE_COUNT_DIFFERENT,
                        String.format(
                                "Test name='%s', expected page count='%d', actual page count='%d'",
                                testName, docExpected.getNumberOfPages(), docActual.getNumberOfPages()),
                        PdfCompareResult.INALID_PAGE_NO,
                        null
                     ));
            }

            for (int i = 0; i < docExpected.getNumberOfPages(); i++) {
                BufferedImage imgActual = i >= docActual.getNumberOfPages()
                        ? null : rendActual.renderImageWithDPI(i, 96f, ImageType.RGB);
                BufferedImage imgExpected = i >= docExpected.getNumberOfPages()
                        ? null : rendExpected.renderImageWithDPI(i, 96f, ImageType.RGB);

                if (i >= docActual.getNumberOfPages()) {
                    
                    problems.add(
                            new PdfCompareResult(
                              ProblemType.EXTRA_EXPECTED_PAGE,
                              String.format("Test name='%s', page number='%d'", testName, i),
                              i,
                              new TestImage(testName, i, imgExpected, null, true)
                            ));
                    
                } else if (imgActual.getWidth() != imgExpected.getWidth() ||
                           imgActual.getHeight() != imgExpected.getHeight()) {

                    problems.add(
                            new PdfCompareResult(
                              ProblemType.PAGE_SIZE_DIFFERENT,
                              String.format(
                                    "Test name='%s', page number='%d', expected size='%d x %d', actual size='%d x %d'",
                                    testName, i, imgExpected.getWidth(),
                                    imgExpected.getHeight(),
                                    imgActual.getWidth(),
                                    imgActual.getHeight()),
                              i,
                              new TestImage(testName, i, imgExpected, imgActual, true)
                            ));
                    
                } else if (isImageDifferent(imgExpected, imgActual)) {

                    problems.add(
                            new PdfCompareResult(
                              ProblemType.PAGE_VISUALLY_DIFFERENT,
                              String.format("Test name='%s', page number='%d'", testName, i),
                              i,
                              new TestImage(testName, i, imgExpected, imgActual, true)
                            ));

                } else if (keepSameImages) {
                    
                    problems.add(
                            new PdfCompareResult(
                              ProblemType.PAGE_GOOD,
                              String.format("Test name='%s', page number='%d'", testName, i),
                              i,
                              new TestImage(testName, i, imgExpected, imgActual, false)
                            ));
                    
                }
            }
            
            return problems;
        }
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
    
    public static BufferedImage createDiffImage(BufferedImage img1, BufferedImage img2) {
        int maxW = Math.max(img1.getWidth(), img2.getWidth());
        int maxH = Math.max(img1.getHeight(), img2.getHeight());
        
        BufferedImage diff = new BufferedImage(
                        maxW + LEFT_MARGIN_PX, maxH, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g2d = diff.createGraphics();
        g2d.setPaint(Color.WHITE);
        g2d.fillRect(0, 0, diff.getWidth(), diff.getHeight());
        g2d.dispose();

        DataBuffer db = diff.getRaster().getDataBuffer();
        boolean hasDifferences = false;
        List<Boolean> lines = new ArrayList<>(maxH / 10);
        
        for (int y = 0; y < maxH; y++) {
                
                int diffLineOffset = y * (maxW + LEFT_MARGIN_PX);
                
                if (y % LINE_HEIGHT_PX == 0 && y != 0) {
                        lines.add(hasDifferences);
                        hasDifferences = false;
                }
                
                for (int x = 0; x < maxW; x++) {
                        int actualPixel = getActualPixel(img1, x, y);
                        int expectedPixel = getExpectedPixel(img2, x, y);
                        
                        if (actualPixel != expectedPixel) {
                                hasDifferences = true;
                                db.setElem(diffLineOffset + x + LEFT_MARGIN_PX, getElement(expectedPixel, actualPixel));
                        } else {
                                db.setElem(diffLineOffset + x + LEFT_MARGIN_PX, getElement(expectedPixel, actualPixel));
                        }
                }
        }
        
        g2d = diff.createGraphics();
        g2d.setFont(new Font("Monospaced", Font.PLAIN, LINE_HEIGHT_PX - 2));
        g2d.setPaint(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ascent = g2d.getFontMetrics().getAscent();
        
        for (int i = 0; i < lines.size(); i++) {
                boolean differs = lines.get(i);
                
                if (differs) {
                        g2d.setPaint(Color.PINK);
                        g2d.fillRect(0, i * LINE_HEIGHT_PX, LEFT_MARGIN_PX, LINE_HEIGHT_PX);
                        g2d.setPaint(Color.BLACK);
                }
                
                g2d.drawString(String.format("%03d", i), /* left-padding */ 2, (i * LINE_HEIGHT_PX) + 1 + ascent);
        }
        
        g2d.dispose();

        return diff;
    }
    
    private static int getExpectedPixel(BufferedImage img, int x, int y) {
        if (x >= img.getWidth() ||
            y >= img.getHeight()) {
            return Color.PINK.getRGB();
        }
        return img.getRGB(x, y);
    }

    private static int getActualPixel(BufferedImage img, int x, int y) {
        if (x >= img.getWidth() ||
            y >= img.getHeight()) {
            return Color.CYAN.getRGB();
        }
        return img.getRGB(x, y);
    }

    // The following code is by Github user red6, from the excellent PDFCompare library

    private static int getElement(final int expectedElement, final int actualElement) {
        if (expectedElement != actualElement) {
            int expectedDarkness = calcCombinedIntensity(expectedElement);
            int actualDarkness = calcCombinedIntensity(actualElement);
            if (expectedDarkness > actualDarkness) {
                return color(levelIntensity(expectedDarkness, 210), 0, 0);
            } else {
                return color(0, levelIntensity(actualDarkness, 180), 0);
            }
        } else {
            return fadeElement(expectedElement);
        }
    }

    /**
    * Levels the color intensity to at least 50 and at most maxIntensity.
    *
    * @param darkness     color component to level
    * @param maxIntensity highest possible intensity cut off
    * @return A value that is at least 50 and at most maxIntensity
    */
    private static int levelIntensity(final int darkness, final int maxIntensity) {
        return Math.min(maxIntensity, Math.max(50, darkness));
    }

    /**
    * Calculate the combined intensity of a pixel and normalizes it to a value of at most 255.
    */
    private static int calcCombinedIntensity(final int element) {
        final Color color = new Color(element);
        return Math.min(255, (color.getRed() + color.getGreen() + color.getRed()) / 3);
    }

    private static int color(final int r, final int g, final int b) {
        return new Color(r, g, b).getRGB();
    }

    private static int fadeElement(final int i) {
        final Color color = new Color(i);
        return new Color(fade(color.getRed()), fade(color.getGreen()), fade(color.getBlue())).getRGB();
    }

    private static int fade(final int i) {
        return i + ((255 - i) * 3 / 5);
    }
}
