package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import org.w3c.dom.Element;

import java.awt.*;

/**
 * FSObjectDrawer Element for PDFBox
 */
public class PdfBoxObjectDrawerReplacedElement implements PdfBoxReplacedElement {
	private final Element e;
	private Point point = new Point(0, 0);
	private final FSObjectDrawer drawer;
	private final int width;
	private final int height;
	private final int dotsPerPixel;

	public PdfBoxObjectDrawerReplacedElement(Element e, FSObjectDrawer drawer, int cssWidth, int cssHeight,
			int dotsPerPixel) {
		this.e = e;
		this.drawer = drawer;
		this.width = cssWidth;
		this.height = cssHeight;
		this.dotsPerPixel = dotsPerPixel;
	}

	@Override
	public int getIntrinsicWidth() {
		return this.width;
	}

	@Override
	public int getIntrinsicHeight() {
		return this.height;
	}

	@Override
	public Point getLocation() {
		return point;
	}

	@Override
	public void setLocation(int x, int y) {
		point.setLocation(x, y);
	}

	@Override
	public void detach(LayoutContext c) {
	}

	@Override
	public boolean isRequiresInteractivePaint() {
		return false;
	}

	@Override
	public boolean hasBaseline() {
		return false;
	}

	@Override
	public int getBaseline() {
		return 0;
	}

	@Override
	public void paint(RenderingContext c, PdfBoxOutputDevice outputDevice, BlockBox box) {
		drawer.drawObject(e, point.getX(), point.getY(), getIntrinsicWidth(), getIntrinsicHeight(), outputDevice, c, dotsPerPixel);
	}
}
