package com.openhtmltopdf.java2d;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder.Graphics2DPaintingReplacedElement;
import com.openhtmltopdf.render.RenderingContext;

public class Java2DSVGReplacedElement extends Graphics2DPaintingReplacedElement /* TODO */ {

	private final SVGDrawer _svgImpl;
	private final Element e;

	public Java2DSVGReplacedElement(Element e, SVGDrawer svgImpl, int width, int height) {
		super(width, height);
		this.e = e;
		this._svgImpl = svgImpl;
	}

	@Override
	public void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
			double height) {
		_svgImpl.drawSVG(e, outputDevice, ctx, x, y, width, height, DOTS_PER_INCH);
	}

	@Override
	public int getIntrinsicWidth() {
		if (super.getIntrinsicWidth() >= 0) {
			// CSS takes precedence over width and height defined on
			// element.
			return super.getIntrinsicWidth();
		} else {
			// Seems to need dots rather than pixels.
			return this._svgImpl.getSVGWidth(e);
		}
	}

	@Override
	public int getIntrinsicHeight() {
		if (super.getIntrinsicHeight() >= 0) {
			// CSS takes precedence over width and height defined on
			// element.
			return super.getIntrinsicHeight();
		} else {
			// Seems to need dots rather than pixels.
			return this._svgImpl.getSVGHeight(e);
		}
	}

}
