package com.openhtmltopdf.mathmlsupport;

import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.SharedContext;

public class MathMLDrawer implements SVGDrawer {

	@Override
	public void importFontFaceRules(List<FontFaceRule> fontFaces,
			SharedContext shared) {
		// TODO Auto-generated method stub

	}

	@Override
	public SVGImage buildSVGImage(Element mathMlElement, double cssWidth,
			double cssHeight, double cssMaxWidth, double cssMaxHeight,
			double dotsPerPixel) {

		MathMLImage img = new MathMLImage(mathMlElement, cssWidth, cssHeight, cssMaxWidth, cssMaxHeight, dotsPerPixel);
		return img;
	}
}
