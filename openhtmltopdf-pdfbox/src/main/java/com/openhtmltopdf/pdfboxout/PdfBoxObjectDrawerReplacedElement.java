package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxLinkManager.IPdfBoxElementWithShapedLinks;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.swing.ImageMapParser;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.Map;

/**
 * FSObjectDrawer Element for PDFBox
 */
public class PdfBoxObjectDrawerReplacedElement implements PdfBoxReplacedElement, IPdfBoxElementWithShapedLinks {
	private final Element e;
	private Point point = new Point(0, 0);
	private final FSObjectDrawer drawer;
	private final int width;
	private final int height;
	private final int dotsPerPixel;
	private Map<Shape, String> imageMap;

	public PdfBoxObjectDrawerReplacedElement(Element e, FSObjectDrawer drawer, int cssWidth, int cssHeight,
											 SharedContext c) {
		this.e = e;
		imageMap = ImageMapParser.findAndParseMap(e, c);
		this.drawer = drawer;
		this.width = cssWidth;
		this.height = cssHeight;
		this.dotsPerPixel = c.getDotsPerPixel();
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
		Map<Shape, String> shapeStringMap = drawer.drawObject(e, point.getX(), point.getY(), getIntrinsicWidth(), getIntrinsicHeight(), outputDevice, c, dotsPerPixel);
		if(shapeStringMap != null )
			imageMap = shapeStringMap;
	}

	@Override
	public Map<Shape, String> getLinkMap() {
		return imageMap;
	}
}
