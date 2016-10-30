package com.openhtmltopdf.testcases;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.TextDirection;
import com.openhtmltopdf.util.JDKXRLogger;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;

public class TestcaseRunner {

	/**
	 * Runs our set of manual test cases. You can specify an output directory with
	 * -DOUT_DIRECTORY=./output
	 * for example. Otherwise, the current working directory is used.
	 * Test cases must be placed in src/main/resources/testcases/
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		/*
		 * Note: The RepeatedTableSample optionally requires the font file NotoSans-Regular.ttf
		 * to be placed in the resources directory. 
		 * 
		 * This sample demonstrates the failing repeated table header on each page.
		 */
		runTestCase("RepeatedTableSample");

		/*
		 * This sample demonstrates the -fs-pagebreak-min-height css property
		 */
		runTestCase("FSPageBreakMinHeightSample");
		
		runTestCase("color");
		runTestCase("background-color");
		runTestCase("background-image");
		runTestCase("invalid-url-background-image");
		runTestCase("text-align");
		runTestCase("font-family-built-in");
		runTestCase("form-controls");

		/* Add additional test cases here. */
	}
	
	/**
	 * Will throw an exception if a SEVERE or WARNING message is logged.
	 * @param testCaseFile
	 * @throws Exception
	 */
	public static void runTestWithoutOutput(String testCaseFile) throws Exception {
		runTestWithoutOutput(testCaseFile, false);
	}
	
	/**
	 * Will silently let ALL log messages through.
	 * @param testCaseFile
	 * @throws Exception
	 */
	public static void runTestWithoutOutputAndAllowWarnings(String testCaseFile) throws Exception {
		runTestWithoutOutput(testCaseFile, true);
	}

	private static void runTestWithoutOutput(String testCaseFile, boolean allowWarnings) throws Exception {
		System.out.println("Trying to run: " + testCaseFile);
		
		byte[] htmlBytes = IOUtils.toByteArray(TestcaseRunner.class
				.getResourceAsStream("/testcases/" + testCaseFile + ".html"));
		String html = new String(htmlBytes, Charsets.UTF_8);
		OutputStream outputStream = new ByteArrayOutputStream(4096);
		
		// We wan't to throw if we get a warning or severe log message.
		final XRLogger delegate = new JDKXRLogger();
		final java.util.List<RuntimeException> warnings = new ArrayList<RuntimeException>();
		XRLog.setLoggerImpl(new XRLogger() {
			@Override
			public void setLevel(String logger, Level level) {
			}
			
			@Override
			public void log(String where, Level level, String msg, Throwable th) {
				if (level.equals(Level.WARNING) ||
					level.equals(Level.SEVERE)) {
					warnings.add(new RuntimeException(where + ": " + msg, th));
				}
				delegate.log(where, level, msg, th);
			}
			
			@Override
			public void log(String where, Level level, String msg) {
				if (level.equals(Level.WARNING) ||
					level.equals(Level.SEVERE)) {
					warnings.add(new RuntimeException(where + ": " + msg));
				}
				delegate.log(where, level, msg);
			}
		});
		
		try {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
			builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
			builder.defaultTextDirection(TextDirection.LTR);
			builder.withHtmlContent(html, TestcaseRunner.class.getResource("/testcases/").toString());
			builder.toStream(outputStream);
			builder.run();
		} finally {
			outputStream.close();
		}
		
		if (!warnings.isEmpty() && !allowWarnings) {
			throw warnings.get(0);
		}
	}
	
	public static void runTestCase(String testCaseFile) throws Exception {
		byte[] htmlBytes = IOUtils.toByteArray(TestcaseRunner.class
				.getResourceAsStream("/testcases/" + testCaseFile + ".html"));
		String html = new String(htmlBytes, Charsets.UTF_8);
		String outDir = System.getProperty("OUT_DIRECTORY", ".");
		String testCaseOutputFile = outDir + "/" + testCaseFile + ".pdf";
		FileOutputStream outputStream = new FileOutputStream(testCaseOutputFile);
		
		try {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
			builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
			builder.defaultTextDirection(TextDirection.LTR);
			builder.withHtmlContent(html, TestcaseRunner.class.getResource("/testcases/").toString());
			builder.toStream(outputStream);
			builder.run();
		} finally {
			outputStream.close();
		}
		System.out.println("Wrote " + testCaseOutputFile);
	}
}
