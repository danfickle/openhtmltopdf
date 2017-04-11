package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import org.w3c.dom.Element;

import java.awt.*;

public class PdfBoxSVGReplacedElement implements PdfBoxReplacedElement {
    private final Element e;
    private final SVGDrawer svg;
    private Point point = new Point(0, 0);
    private final int width;
    private final int height;
    private final int dotsPerPixel;
    
    public PdfBoxSVGReplacedElement(Element e, SVGDrawer svgImpl, int cssWidth, int cssHeight, int dotsPerPixel) {
        this.e = e;
        this.svg = svgImpl;
        this.width = cssWidth;
        this.height = cssHeight;
        this.dotsPerPixel = dotsPerPixel;
    }

    @Override
    public int getIntrinsicWidth() {
        if (this.width >= 0) {
            // CSS takes precedence over width and height defined on element.
            return this.width;
        }
        else {
            // Seems to need dots rather than pixels.
            return this.svg.getSVGWidth(e) * this.dotsPerPixel;
        }
    }

    @Override
    public int getIntrinsicHeight() {
        if (this.height >= 0) {
            // CSS takes precedence over width and height defined on element.
            return this.height;
        }
        else {
            // Seems to need dots rather than pixels.
            return this.svg.getSVGHeight(e) * this.dotsPerPixel;
        }
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
        svg.drawSVG(e, outputDevice, c, point.getX(), point.getY(), getIntrinsicWidth(), getIntrinsicHeight(), dotsPerPixel);
    }
}
