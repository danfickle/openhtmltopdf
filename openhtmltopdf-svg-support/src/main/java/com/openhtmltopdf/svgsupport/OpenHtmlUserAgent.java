package com.openhtmltopdf.svgsupport;

import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.bridge.UserAgentAdapter;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;

public class OpenHtmlUserAgent extends UserAgentAdapter {

	private final OpenHtmlFontResolver resolver;

	public OpenHtmlUserAgent(OpenHtmlFontResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public FontFamilyResolver getFontFamilyResolver() {
		return this.resolver;
	}
}
