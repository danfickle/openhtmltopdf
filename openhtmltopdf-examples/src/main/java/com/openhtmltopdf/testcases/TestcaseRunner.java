package com.openhtmltopdf.testcases;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.TextDirection;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

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

		/* Add additional test cases here. */
	}
	
	public static void runTestWithoutOutput(String testCaseFile) throws Exception {
		System.out.println("Trying to run: " + testCaseFile);
		
		byte[] htmlBytes = IOUtils.toByteArray(TestcaseRunner.class
				.getResourceAsStream("/testcases/" + testCaseFile + ".html"));
		String html = new String(htmlBytes, Charsets.UTF_8);
		OutputStream outputStream = new ByteArrayOutputStream(4096);
		
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
