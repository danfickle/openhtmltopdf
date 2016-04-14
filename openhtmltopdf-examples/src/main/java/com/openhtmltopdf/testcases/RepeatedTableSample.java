package com.openhtmltopdf.testcases;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.TextDirection;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

import java.io.FileOutputStream;

public class RepeatedTableSample {
	public static void main(String[] args) throws Exception {
		byte[] htmlBytes = IOUtils.toByteArray(RepeatedTableSample.class
				.getResourceAsStream("/testcases/RepeatedTableSample.html"));
		String html = new String(htmlBytes, Charsets.UTF_8);
		FileOutputStream outputStream = new FileOutputStream("RepeatedTableSample.pdf");
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
		builder.useBidiReorderer(new ICUBidiReorderer());
		builder.defaultTextDirection(TextDirection.LTR);
		builder.withHtmlContent(html, "");
		builder.toStream(outputStream);
		PdfBoxRenderer pdfBoxRenderer = builder.buildPdfRenderer();
		// Add Noto Sans Font
		// pdfBoxRenderer.getFontResolver().addFont(tempFile.getPath(), null);
		pdfBoxRenderer.layout();
		pdfBoxRenderer.createPDF();
		outputStream.close();

	}
}
