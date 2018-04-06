package com.openhtmltopdf.freemarker;

import java.io.*;
import java.net.URL;
import java.util.Locale;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.latexsupport.LaTeXDOMMutator;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.objects.StandardObjectDrawerFactory;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.swing.NaiveUserAgent.DefaultUriResolver;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.*;

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

	public void generateHTMLToFile(String templateName, Locale locale, FreemarkerRootObject object, File htmlFile)
			throws IOException, TemplateException {
		FileOutputStream output = new FileOutputStream(htmlFile);
		Writer fw = new OutputStreamWriter(output, "UTF-8");
		cfg.getTemplate(templateName, locale, "UTF-8").process(object, fw);
		fw.close();
		output.close();
	}

	public byte[] generatePDF(final String html) throws IOException {
		return generatePDF(new ICallableWithPdfBuilder() {
			@Override
			public void apply(PdfRendererBuilder builder) {
				builder.withHtmlContent(html, "/freemarker");
			}
		});
	}

	public byte[] generatePDF(final File htmlFile) throws IOException {
		return generatePDF(new ICallableWithPdfBuilder() {
			@Override
			public void apply(PdfRendererBuilder builder) {
				builder.withFile(htmlFile);
			}
		});
	}

	private byte[] generatePDF(ICallableWithPdfBuilder callWithBuilder) throws IOException {
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
		builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
		builder.defaultTextDirection(PdfRendererBuilder.TextDirection.LTR);
		builder.useSVGDrawer(new BatikSVGDrawer());
		builder.useMathMLDrawer(new MathMLDrawer());
		builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
		builder.usePDDocument(new PDDocument(MemoryUsageSetting.setupMixed(1000000)));
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
		callWithBuilder.apply(builder);

		PdfBoxRenderer pdfBoxRenderer = builder.buildPdfRenderer();
		try {
			pdfBoxRenderer.layout();
			pdfBoxRenderer.createPDF();
		} finally {
			pdfBoxRenderer.close();
		}
		outputStream.close();
		return outputStream.toByteArray();
	}

	interface ICallableWithPdfBuilder {
		void apply(PdfRendererBuilder builder);
	}
}
