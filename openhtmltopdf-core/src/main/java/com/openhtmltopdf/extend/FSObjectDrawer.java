package com.openhtmltopdf.extend;

import com.openhtmltopdf.render.RenderingContext;
import org.w3c.dom.Element;

/**
 * Handle the drawing of &lt;object&gt; tags
 */
public interface FSObjectDrawer {
	void drawObject(Element e, double x, double y, double width, double height, OutputDevice outputDevice,
			RenderingContext ctx, int dotsPerPixel);
}
