package com.openhtmltopdf.extend;

import org.w3c.dom.Element;

import com.openhtmltopdf.render.RenderingContext;

public interface SVGDrawer {
	public void drawSVG(Element svgElement, OutputDevice outputDevice, RenderingContext ctx, double x, double y);
}
