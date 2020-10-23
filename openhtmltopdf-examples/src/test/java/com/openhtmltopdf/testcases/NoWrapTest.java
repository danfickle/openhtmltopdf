package com.openhtmltopdf.testcases;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;

public class NoWrapTest {

    @Test
    public void testNoWrap() throws IOException {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.defaultTextDirection(PdfRendererBuilder.TextDirection.LTR);
        String html = IOUtils.toString(getClass().getResourceAsStream("/nowrap.html"), "UTF-8");
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(Entities.EscapeMode.xhtml);
        builder.withW3cDocument(new W3CDom().fromJsoup(doc), "/");
        try (FileOutputStream fos = new FileOutputStream("pdf-test-nowrap.pdf")) {
            builder.toStream(fos);
            builder.run();
        }
    }
}
