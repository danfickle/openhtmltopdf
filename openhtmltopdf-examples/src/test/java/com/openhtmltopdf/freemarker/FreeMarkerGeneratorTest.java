package com.openhtmltopdf.freemarker;

import com.openhtmltopdf.freemarker.FreeMarkerGenerator.FreemarkerRootObject;
import com.openhtmltopdf.util.XRLog;
import freemarker.template.TemplateException;
import org.apache.pdfbox.util.Charsets;
import org.junit.Test;

import java.io.*;
import java.util.Locale;

public class FreeMarkerGeneratorTest {

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Test
	public void testFreeMarkerGenerator() throws IOException, TemplateException {
		File targetDir = new File("target/test/freemarker");
		targetDir.mkdirs();
		FreeMarkerGenerator freeMarkerGenerator = new FreeMarkerGenerator();
		FreemarkerRootObject object = new FreemarkerRootObject();
		String html = freeMarkerGenerator.generateHTML("featuredocumentation.ftl", Locale.GERMAN, object);
		byte[] pdf = freeMarkerGenerator.generatePDF(html);
		FileOutputStream fileOutputStream = new FileOutputStream(new File(targetDir, "featuredocumentation.html"));
		fileOutputStream.write(html.getBytes(Charsets.UTF_8));
		fileOutputStream.close();
		fileOutputStream = new FileOutputStream(new File(targetDir, "featuredocumentation.pdf"));
		fileOutputStream.write(pdf);
		fileOutputStream.close();
	}

	@Test
	public void testFreeMarkerWithManyPages() throws IOException, TemplateException {
		/*
		 * We really should disable logging here, as it takes ages anyway to generate
		 * the report...
		 */
		XRLog.setLoggingEnabled(false);
		File targetDir = new File("target/test/freemarker");
		targetDir.mkdirs();
		FreeMarkerGenerator freeMarkerGenerator = new FreeMarkerGenerator();
		FreemarkerRootObject object = new FreemarkerRootObject();
		File htmlFile = new File(targetDir, "many_pages.html");
		freeMarkerGenerator.generateHTMLToFile("many_pages.ftl", Locale.GERMAN, object, htmlFile);
		byte[] pdf = freeMarkerGenerator.generatePDF(htmlFile);
		FileOutputStream fileOutputStream = new FileOutputStream(new File(targetDir, "many_pages.pdf"));
		fileOutputStream.write(pdf);
		fileOutputStream.close();
		XRLog.setLoggingEnabled(true);
	}
}