package com.openhtmltopdf.java2d;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;
import com.openhtmltopdf.render.RenderingContext;
import org.w3c.dom.Element;

public class Java2DObjectDrawerReplacedElement extends Java2DRendererBuilder.Graphics2DPaintingReplacedElement
		implements ReplacedElement {
	private final Element e;
	private final int dotsPerPixel;
	private final FSObjectDrawer drawer;

	public Java2DObjectDrawerReplacedElement(Element e, FSObjectDrawer drawer, int width, int height,
			int dotsPerPixel) {
		super(width, height);
		this.e = e;
		this.drawer = drawer;
		this.dotsPerPixel = dotsPerPixel;
	}

	@Override
	public void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, final double width,
			final double height) {
		drawer.drawObject(e, x, y, width, height, outputDevice, ctx, dotsPerPixel);
	}
}
