package com.openhtmltopdf.pdfboxout;

import org.apache.pdfbox.pdmodel.font.PDFont;

import com.openhtmltopdf.extend.FSSupplier;

/**
 * Implementation of {@link FSSupplier} that supplies a {@link PDFont}.<br>
 * Subclass this if you need special font loading rules (like using a font-cache, ...).
 */
public class PDFontSupplier implements FSSupplier<PDFont> { 

	// this used to be a private static class in PdfBoxFontResolver
	private PDFont _font;
	
	public PDFontSupplier(PDFont font) {
		this._font = font;
	}
	
	@Override
	public PDFont supply() {
		return _font;
	}
	
}
