package com.openhtmltopdf.testcases;

import static com.openhtmltopdf.testcases.TestcaseRunner.buildObjectDrawerFactory;

import java.io.File;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.Charsets;
import org.junit.Test;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.latexsupport.LaTeXDOMMutator;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

public class ConcateOutputTest {

	@Test
	public void testConcateOutput() throws Exception {
		File targetFile = new File("target/test/concatoutput/concated.pdf");
		targetFile.getParentFile().mkdirs();
		PDDocument doc = new PDDocument(MemoryUsageSetting.setupMixed(1000000));

		for (String testCaseFile : new String[] { "color", "background-color",
				"FSPageBreakMinHeightSample", "math-ml", "multi-column-layout", "simplerotate",
				"svg-inline", "svg-sizes", "transform", "RepeatedTableSample",
				"RepeatedTableTransformSample" }) {
			renderPDF(testCaseFile, doc);
		}

		doc.save(targetFile);
		doc.close();

	}

	private static void renderPDF(String testCaseFile, PDDocument document) throws Exception {
		byte[] htmlBytes = IOUtils
				.toByteArray(TestcaseRunner.class.getResourceAsStream("/testcases/" + testCaseFile + ".html"));
		String html = new String(htmlBytes, Charsets.UTF_8);
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
		builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
		builder.defaultTextDirection(BaseRendererBuilder.TextDirection.LTR);
		builder.useSVGDrawer(new BatikSVGDrawer());
		builder.useMathMLDrawer(new MathMLDrawer());
		builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
		builder.useObjectDrawerFactory(buildObjectDrawerFactory());
		builder.withHtmlContent(html, TestcaseRunner.class.getResource("/testcases/").toString());
		builder.usePDDocument(document);
		PdfBoxRenderer pdfBoxRenderer = builder.buildPdfRenderer();
		pdfBoxRenderer.createPDFWithoutClosing();
	}
}
