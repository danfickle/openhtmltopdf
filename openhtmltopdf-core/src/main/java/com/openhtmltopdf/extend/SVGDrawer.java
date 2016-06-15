package com.openhtmltopdf.extend;

import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.RenderingContext;

public interface SVGDrawer {
	public void drawSVG(Element svgElement, OutputDevice outputDevice, RenderingContext ctx, double x, double y, SharedContext shared);

	public void importFontFaceRules(List<FontFaceRule> fontFaces);
}
