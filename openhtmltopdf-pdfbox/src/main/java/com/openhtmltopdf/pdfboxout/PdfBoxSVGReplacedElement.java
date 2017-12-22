package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.pdfboxout.PdfBoxLinkManager.IPdfBoxElementWithShapedLinks;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.swing.ImageMapParser;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.Map;

public class PdfBoxSVGReplacedElement implements PdfBoxReplacedElement, IPdfBoxElementWithShapedLinks {
    private final SVGImage svgImage;
    private final Map<Shape, String> imageMap;
    private Point point = new Point(0, 0);
    
    public PdfBoxSVGReplacedElement(Element e, SVGDrawer svgImpl, int cssWidth, int cssHeight, int cssMaxWidth, int cssMaxHeight, int dotsPerPixel) {       
        this.svgImage = svgImpl.buildSVGImage(e, cssWidth, cssHeight, cssMaxWidth, cssMaxHeight, dotsPerPixel);
        imageMap = ImageMapParser.findAndParseMap(e);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.svgImage.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.svgImage.getIntrinsicHeight();
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
        svgImage.drawSVG(outputDevice, c, point.getX(), point.getY());
    }

    @Override
    public Map<Shape, String> getLinkMap() {
        return imageMap;
    }
}
