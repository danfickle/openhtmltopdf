package com.openhtmltopdf.templates;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class PdfCreator {
    @SafeVarargs
    public final byte[] runRenderer(String resourcePath, String html, Consumer<PdfRendererBuilder>... config) {
        ByteArrayOutputStream actual = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, VisualTester.class.getResource(resourcePath).toString());
        builder.toStream(actual);
        builder.useFastMode();

        for (Consumer<PdfRendererBuilder> conf : config) {
            conf.accept(builder);
        }

        try {
            builder.run();
        } catch (Exception e) {
            System.err.println("Failed to render resource (" + resourcePath + ")");
            e.printStackTrace();
            return null;
        }

        return actual.toByteArray();
    }
}
