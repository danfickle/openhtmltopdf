package com.openhtmltopdf.extend;

import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.RenderingContext;

public interface SVGDrawer {
	public void drawSVG(Element svgElement, OutputDevice outputDevice, RenderingContext ctx, double x, double y, float dotsPerInch);

	public void importFontFaceRules(List<FontFaceRule> fontFaces, SharedContext shared);

	/**
	 * @param e the SVG element
	 * @return the width of the SVG in pixels.
	 */
	public int getSVGWidth(Element e);

	/**
	 * @param e the SVG element
	 * @return the height of the SVG in pixels.
	 */
	public int getSVGHeight(Element e);
}
