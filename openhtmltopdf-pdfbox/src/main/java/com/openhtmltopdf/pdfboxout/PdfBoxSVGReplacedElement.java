package com.openhtmltopdf.pdfboxout;

import java.awt.Point;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;

public class PdfBoxSVGReplacedElement implements PdfBoxReplacedElement {
    private final Element e;
    private final SVGDrawer svg;
    private Point point = new Point(0, 0);
    private final int width;
    private final int height;
    
    public PdfBoxSVGReplacedElement(Element e, SVGDrawer svgImpl, int cssWidth, int cssHeight) {
        this.e = e;
        this.svg = svgImpl;
        this.width = cssWidth;
        this.height = cssHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        // TODO Auto-generated method stub
        return 10000; // TOTAL GUESS.
    }

    @Override
    public int getIntrinsicHeight() {
        // TODO Auto-generated method stub
        return 10000; // TOTAL GUESS.
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
        // TODO Auto-generated method stub
        
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
        svg.drawSVG(e, outputDevice, null, point.getX(), point.getY());
    }

}
