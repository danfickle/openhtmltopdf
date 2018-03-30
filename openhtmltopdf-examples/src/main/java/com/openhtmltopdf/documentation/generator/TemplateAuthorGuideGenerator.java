package com.openhtmltopdf.documentation.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.util.Charsets;

import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

public class TemplateAuthorGuideGenerator {
	public static void main(String[] args) throws Exception {
		
		byte[] markdownBytes = IOUtils
				.toByteArray(TemplateAuthorGuideGenerator.class.getResourceAsStream("/documentation/documentation.md"));
		String md = new String(markdownBytes, Charsets.UTF_8);
		
		String html = markdown(md);
		
		byte[] hdrBytes = IOUtils
				.toByteArray(TemplateAuthorGuideGenerator.class.getResourceAsStream("/documentation/documentation-header.htm"));
		String hdr = new String(hdrBytes, Charsets.UTF_8);
		
		// FileUtils.writeStringToFile(new File("./docs-xxxx.htm"), hdr + html + "</body></html>\n");
		
		renderPDF(hdr + html + "</body></html>", new FileOutputStream("./template-guide.pdf"));
	}

    private static String markdown(String md) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
        		TocExtension.create(),
        		AnchorLinkExtension.create()
        ));
        options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false);

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        Node document = parser.parse(md);
        return renderer.render(document);
    }
	
	private static void renderPDF(String html, OutputStream outputStream) throws Exception {
		try {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useSVGDrawer(new BatikSVGDrawer());
			builder.useMathMLDrawer(new MathMLDrawer());

			builder.withHtmlContent(html, TemplateAuthorGuideGenerator.class.getResource("/documentation/").toString());
			builder.toStream(outputStream);
			builder.run();
		} finally {
			outputStream.close();
		}
	}
}
