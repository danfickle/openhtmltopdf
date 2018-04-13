package com.openhtmltopdf.svgsupport;

import java.awt.Point;
import java.util.logging.Level;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;
import com.openhtmltopdf.util.XRLog;

public class BatikSVGImage implements SVGImage {
    private final static Point DEFAULT_DIMENSIONS = new Point(400, 400);

    private final Element svgElement;
    private final double dotsPerPixel;
    private OpenHtmlFontResolver fontResolver;

    private PDFTranscoder pdfTranscoder;

    public BatikSVGImage(Element svgElement, double cssWidth, double cssHeight,
            double cssMaxWidth, double cssMaxHeight, double dotsPerPixel) {
        this.svgElement = svgElement;
        this.dotsPerPixel = dotsPerPixel;

        this.pdfTranscoder = new PDFTranscoder(cssWidth, cssHeight);
        if (cssWidth >= 0) {
            this.pdfTranscoder.addTranscodingHint(
                    SVGAbstractTranscoder.KEY_WIDTH,
                    (float) (cssWidth / dotsPerPixel));
        }
        if (cssHeight >= 0) {
            this.pdfTranscoder.addTranscodingHint(
                    SVGAbstractTranscoder.KEY_HEIGHT,
                    (float) (cssHeight / dotsPerPixel));
        }
        if (cssMaxWidth >= 0) {
            this.pdfTranscoder.addTranscodingHint(
                    SVGAbstractTranscoder.KEY_MAX_WIDTH,
                    (float) (cssMaxWidth / dotsPerPixel));
        }
        if (cssMaxHeight >= 0) {
            this.pdfTranscoder.addTranscodingHint(
                    SVGAbstractTranscoder.KEY_MAX_HEIGHT,
                    (float) (cssMaxHeight / dotsPerPixel));
        }

        Point dimensions = parseDimensions(svgElement);
        
        if (dimensions == DEFAULT_DIMENSIONS && 
        	cssWidth >= 0 && cssHeight >= 0) {
        	svgElement.setAttribute("width", Integer.toString((int) (cssWidth / dotsPerPixel)));
        	svgElement.setAttribute("height", Integer.toString((int) (cssHeight / dotsPerPixel)));
        	this.pdfTranscoder.setImageSize((float) (cssWidth / dotsPerPixel), (float) (cssHeight / dotsPerPixel));
        } else {
        	svgElement.setAttribute("width", Integer.toString(dimensions.x));
        	svgElement.setAttribute("height", Integer.toString(dimensions.y));
        	this.pdfTranscoder.setImageSize((float) dimensions.x,
        			(float) dimensions.y);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (this.pdfTranscoder.getWidth() * this.dotsPerPixel);
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (this.pdfTranscoder.getHeight() * this.dotsPerPixel);
    }

    public void setFontResolver(OpenHtmlFontResolver fontResolver) {
        this.fontResolver = fontResolver;
    }

    public Integer parseLength(String attrValue) {
        // TODO read length with units and convert to dots.
        // length ::= number (~"em" | ~"ex" | ~"px" | ~"in" | ~"cm" | ~"mm" |
        // ~"pt" | ~"pc")?
        try {
            return Integer.valueOf(attrValue);
        } catch (NumberFormatException e) {
            XRLog.general(Level.WARNING,
                    "Invalid integer passed as dimension for SVG: "
                            + attrValue);
            return null;
        }
    }

    public Point parseDimensions(Element e) {
        String widthAttr = e.getAttribute("width");
        Integer width = widthAttr.isEmpty() ? null : parseLength(widthAttr);

        String heightAttr = e.getAttribute("height");
        Integer height = heightAttr.isEmpty() ? null : parseLength(heightAttr);

        if (width != null && height != null) {
            return new Point(width, height);
        }

        String viewBoxAttr = e.getAttribute("viewBox");
        String[] splitViewBox = viewBoxAttr.split("\\s+");
        if (splitViewBox.length != 4) {
            return DEFAULT_DIMENSIONS;
        }
        try {
            int viewBoxWidth = Integer.parseInt(splitViewBox[2]);
            int viewBoxHeight = Integer.parseInt(splitViewBox[3]);

            if (width == null && height == null) {
                width = viewBoxWidth;
                height = viewBoxHeight;
            } else if (width == null) {
                width = (int) Math.round(((double) height)
                        * ((double) viewBoxWidth) / ((double) viewBoxHeight));
            } else if (height == null) {
                height = (int) Math.round(((double) width)
                        * ((double) viewBoxHeight) / ((double) viewBoxWidth));
            }
            return new Point(width, height);
        } catch (NumberFormatException ex) {
            return DEFAULT_DIMENSIONS;
        }
    }

    @Override
    public void drawSVG(OutputDevice outputDevice, RenderingContext ctx,
            double x, double y) {

        OpenHtmlFontResolver fontResolver = this.fontResolver;
        if (fontResolver == null) {
            XRLog.general(Level.INFO,
                    "importFontFaceRules has not been called for this pdf transcoder");
            fontResolver = new OpenHtmlFontResolver();
        }

        pdfTranscoder.setRenderingParameters(outputDevice, ctx, x, y,
                fontResolver);

        try {
            DOMImplementation impl = SVGDOMImplementation
                    .getDOMImplementation();
            Document newDocument = impl.createDocument(
                    SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);

            for (int i = 0; i < svgElement.getChildNodes().getLength(); i++) {
                Node importedNode = newDocument
                        .importNode(svgElement.getChildNodes().item(i), true);
                newDocument.getDocumentElement().appendChild(importedNode);
            }

            // Copy attributes such as viewBox to the new SVG document.
            for (int i = 0; i < svgElement.getAttributes().getLength(); i++) {
                Node importedAttr = svgElement.getAttributes().item(i);
                newDocument.getDocumentElement().setAttribute(
                        importedAttr.getNodeName(),
                        importedAttr.getNodeValue());
            }

            TranscoderInput in = new TranscoderInput(newDocument);
            pdfTranscoder.transcode(in, null);
        } catch (TranscoderException e) {
            XRLog.exception("Couldn't draw SVG.", e);
        }
    }
}
