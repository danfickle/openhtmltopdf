package com.openhtmltopdf.java2d;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder.Graphics2DPaintingReplacedElement;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;

public class Java2DSVGReplacedElement extends Graphics2DPaintingReplacedElement /* TODO */ {

	private final SVGImage _svgImage;

	public Java2DSVGReplacedElement(Element e, SVGDrawer svgImpl, int width, int height, Box box, CssContext c) {
		super(width, height);
        this._svgImage = svgImpl.buildSVGImage(e, box, c, width, height, /* dots-per-pixel */ 1);
	}

	@Override
	public void paint(OutputDevice outputDevice, RenderingContext ctx, double x, double y, double width,
			double height) {
	    this._svgImage.drawSVG(outputDevice, ctx, x, y);
	}

	@Override
	public int getIntrinsicWidth() {
	        return this._svgImage.getIntrinsicWidth();
	}

	@Override
	public int getIntrinsicHeight() {
            return this._svgImage.getIntrinsicHeight();
	}

}
