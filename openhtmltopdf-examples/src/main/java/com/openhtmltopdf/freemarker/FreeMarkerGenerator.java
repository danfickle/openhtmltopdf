package com.openhtmltopdf.freemarker;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.objects.StandardObjectDrawerFactory;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.swing.NaiveUserAgent.DefaultUriResolver;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Locale;

public class FreeMarkerGenerator {
	private Configuration cfg;

	public static class FreemarkerRootObject {
		public int countChar(String s, String ch) {
			int ret = 0;
			int idx = 0;
			while (true) {
				idx = s.indexOf(ch, idx);
				if (idx == -1)
					break;
				ret++;
				idx++;
			}
			return ret;
		}
	}

	public FreeMarkerGenerator() {
		setupConfig();
	}

	private void setupConfig() {
		Version ourVersion = Configuration.VERSION_2_3_27;
		cfg = new Configuration(ourVersion);
		cfg.setObjectWrapper(new DefaultObjectWrapper(ourVersion));
		cfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
		cfg.setDefaultEncoding("UTF-8");
		cfg.setOutputEncoding("UTF-8");
		cfg.setLocale(Locale.ENGLISH);
		cfg.setTemplateLoader(new ClassTemplateLoader(this.getClass(), "/freemarker"));
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
	}

	public String generateHTML(String templateName, Locale locale, FreemarkerRootObject object)
			throws IOException, TemplateException {
		StringWriter stringWriter = new StringWriter();
		cfg.getTemplate(templateName, locale, "UTF-8").process(object, stringWriter);
		return stringWriter.toString();
	}

	public byte[] generatePDF(String html) throws IOException {
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
		builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
		builder.defaultTextDirection(PdfRendererBuilder.TextDirection.LTR);
		builder.withHtmlContent(html, "/freemarker");
		builder.useUriResolver(new DefaultUriResolver() {
			@Override
			public String resolveURI(String baseUri, String uri) {
				if (!uri.startsWith("/")) {
					// Classpath Resource
					URL resource = FreeMarkerGenerator.this.getClass().getResource(uri);
					if (resource != null)
						return resource.toString();
					resource = FreeMarkerGenerator.this.getClass().getResource(baseUri + "/" + uri);
					if (resource != null)
						return resource.toString();
				}
				return super.resolveURI(baseUri, uri);
			}
		});
		StandardObjectDrawerFactory objectDrawerFactory = new StandardObjectDrawerFactory();
		builder.useObjectDrawerFactory(objectDrawerFactory);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		builder.toStream(outputStream);
		PdfBoxRenderer pdfBoxRenderer = builder.buildPdfRenderer();
		try {
			pdfBoxRenderer.layout();
			pdfBoxRenderer.createPDF();
		} catch (Exception e) {
			pdfBoxRenderer.close();
		}
		outputStream.close();
		return outputStream.toByteArray();
	}
}
