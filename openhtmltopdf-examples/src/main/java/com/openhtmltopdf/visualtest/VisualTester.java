package com.openhtmltopdf.visualtest;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Charsets;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
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
	
	public interface BuilderConfig {
		public void configure(PdfRendererBuilder builder);
	}
	
	private final String resourcePath;
	private final File primaryPath;
	private final File overridePath;
	private final File outputPath;
	
	private static final int LEFT_MARGIN_PX = 45;
	private static final int LINE_HEIGHT_PX = 17;
	private static final BufferedImage ONE_PX_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	public VisualTester(String resourceHtmlPath, File pathExpectedPrimary, File pathExpectedOverride, File pathOutput) {
		this.resourcePath = resourceHtmlPath;
		this.primaryPath = pathExpectedPrimary;
		this.overridePath = pathExpectedOverride;
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
		return runTest(resource, new BuilderConfig() {
			@Override
			public void configure(PdfRendererBuilder builder) {
			}
		});
	}
	
	private StringBuilder logToStringBuilder() {
	    final XRLogger delegate = new JDKXRLogger();
	    final StringBuilder sb = new StringBuilder();   
	        XRLog.setLoggerImpl(new XRLogger() {
	            @Override
	            public void setLevel(String logger, Level level) {
	            }

	            @Override
	            public void log(String where, Level level, String msg, Throwable th) {
					if (th == null) {
						log(where, level, msg);
						return;
					}
	                StringWriter sw = new StringWriter();
					th.printStackTrace(new PrintWriter(sw, true));
	                sb.append(where + ": " + level + ":\n" + msg + sw.toString() + "\n");
	                delegate.log(where, level, msg, th);
	            }

	            @Override
	            public void log(String where, Level level, String msg) {
	                sb.append(where + ": " + level + ": " + msg + "\n");
	                delegate.log(where, level, msg);
	            }
	        });
	        return sb;
	}

	public boolean runTest(String resource, BuilderConfig additionalBuilderConfiguration) throws IOException {
		String absResPath = this.resourcePath + resource + ".html";
		
		File override = new File(this.overridePath, resource + ".pdf");
		File primary = new File(this.primaryPath, resource + ".pdf");

		File testFile = override.exists() ? override : primary;
		
		byte[] htmlBytes = IOUtils
				.toByteArray(TestcaseRunner.class.getResourceAsStream(absResPath));
		String html = new String(htmlBytes, Charsets.UTF_8);

		StringBuilder sb = logToStringBuilder();
		byte[] actualPdfBytes = runRenderer(resourcePath, html, additionalBuilderConfiguration);
		
		if (actualPdfBytes == null) {
		    System.err.println("When running test (" + resource + "), rendering failed, writing log to failure file.");
			File output = new File(this.outputPath, resource + ".failure.txt");
			FileUtils.writeByteArrayToFile(output, sb.toString().getBytes(Charsets.UTF_8));
			return false;
		}

		if (!testFile.exists()) {
			System.err.println("When running test (" + resource + "), nothing to compare against as file (" + testFile.getCanonicalPath() + ") does not exist.");
			System.err.println("Writing generated PDF to file instead in output directory.");
			File output = new File(this.outputPath, resource + ".pdf");
			FileUtils.writeByteArrayToFile(output, actualPdfBytes);
			return false;
		}
		
		PDDocument docActual = PDDocument.load(actualPdfBytes);
		PDDocument docExpected = PDDocument.load(testFile);
		
		PDFRenderer rendActual = new PDFRenderer(docActual);
		PDFRenderer rendExpected = new PDFRenderer(docExpected);
		
		boolean problems = false;
		
		for (int i = 0; i < docActual.getNumberOfPages(); i++) {
			BufferedImage imgActual = i >= docActual.getNumberOfPages() ? ONE_PX_IMAGE : rendActual.renderImageWithDPI(i, 96f, ImageType.RGB);
			BufferedImage imgExpected = i >= docExpected.getNumberOfPages() ? ONE_PX_IMAGE : rendExpected.renderImageWithDPI(i, 96f, ImageType.RGB);

			if (imgActual.getWidth() != imgExpected.getWidth() ||
				imgActual.getHeight() != imgExpected.getHeight()) {
				System.err.println("When running test (" + resource + "), page sizes were different. Please check diff image in output directory.");
				problems = true;
			}
		
			BufferedImage diff = compareImages(imgActual, imgExpected);
			
			if (diff != null) {
				System.err.println("When running test (" + resource + "), differences were found. Please check diff images in output directory.");
				File output = new File(this.outputPath, resource + "---" + i + "---diff.png");
				ImageIO.write(diff, "png", output);
				
				output = new File(this.outputPath, resource + "---" + i + "---actual.png");
				ImageIO.write(imgActual, "png", output);
				
				output = new File(this.outputPath, resource + "---" + i + "---expected.png");
				ImageIO.write(imgExpected, "png", output);
				problems = true;
			}
		}
		
		docActual.close();
		docExpected.close();

		if (problems) {
			File outPdf = new File(this.outputPath, resource + ".pdf");
			FileUtils.writeByteArrayToFile(outPdf, actualPdfBytes);
			return false;
		}
		
		return true;
	}
	
	public BufferedImage compareImages(BufferedImage img1, BufferedImage img2) {
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
		boolean hasGlobalDifferences = false;
		List<Boolean> lines = new ArrayList<Boolean>(maxH / 10);
		
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
					hasGlobalDifferences = true;
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

		return hasGlobalDifferences ? diff : null;
	}
    
	private int getExpectedPixel(BufferedImage img, int x, int y) {
		if (x >= img.getWidth() ||
			y >= img.getHeight()) {
			return Color.PINK.getRGB();
		}
		return img.getRGB(x, y);
	}
	
	private int getActualPixel(BufferedImage img, int x, int y) {
		if (x >= img.getWidth() ||
			y >= img.getHeight()) {
			return Color.CYAN.getRGB();
		}
		return img.getRGB(x, y);
	}
	
	// The following code is by Github user red6, from the excellent PDFCompare library
    
    private int getElement(final int expectedElement, final int actualElement) {
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
     *
     * @param element
     * @return
     */
    private static int calcCombinedIntensity(final int element) {
        final Color color = new Color(element);
        return Math.min(255, (color.getRed() + color.getGreen() + color.getRed()) / 3);
    }
    
    public static int color(final int r, final int g, final int b) {
        return new Color(r, g, b).getRGB();
    }

    public static int fadeElement(final int i) {
        final Color color = new Color(i);
        return new Color(fade(color.getRed()), fade(color.getGreen()), fade(color.getBlue())).getRGB();
    }

    private static int fade(final int i) {
        return i + ((255 - i) * 3 / 5);
    }
}
