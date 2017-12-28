package com.openhtmltopdf.extend;

import com.openhtmltopdf.render.RenderingContext;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.Map;

/**
 * Handle the drawing of &lt;object&gt; tags
 */
public interface FSObjectDrawer {
	/**
	 * Perform your drawing.
	 * 
	 * @return null or a map of Shape =&gt; URL-String to annotate the drawing with
	 *         links. The shapes must be relative to (x,y), i.e. (0,0) is at the
	 *         corner (x,y). Also they should not extend (width,height). The links
	 *         are only exported into the PDF and also only respected by Acrobat
	 *         Reader.
	 */
	Map<Shape, String> drawObject(Element e, double x, double y, double width, double height, OutputDevice outputDevice,
			RenderingContext ctx, int dotsPerPixel);
}
