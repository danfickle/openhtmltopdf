package com.openhtmltopdf.svgsupport;

import java.awt.Point;
import java.util.Set;
import java.util.logging.Level;

import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.util.LogMessageId;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.CSSParser;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;
import com.openhtmltopdf.util.XRLog;

public class BatikSVGImage implements SVGImage {
    private final static int DEFAULT_SVG_WIDTH = 400;
    private final static int DEFAULT_SVG_HEIGHT = 400;
    private final static Point DEFAULT_DIMENSIONS = new Point(DEFAULT_SVG_WIDTH, DEFAULT_SVG_HEIGHT);

    private final Element svgElement;
    private final double dotsPerPixel;
    private OpenHtmlFontResolver fontResolver;
    private final PDFTranscoder pdfTranscoder;
    private UserAgentCallback userAgentCallback;

    public BatikSVGImage(
            Element svgElement, Box box,
            double cssWidth, double cssHeight,
            double cssMaxWidth, double cssMaxHeight,
            double dotsPerPixel,
            CssContext ctx) {

        this.svgElement = svgElement;
        this.dotsPerPixel = dotsPerPixel;
        this.pdfTranscoder = new PDFTranscoder(box, dotsPerPixel, cssWidth, cssHeight);

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
        
        Point dimensions = parseDimensions(svgElement, box, ctx);
        double w;
        double h;
        
        if (dimensions == DEFAULT_DIMENSIONS) {
            if (cssWidth >= 0 && cssHeight >= 0) {
                w = (cssWidth / dotsPerPixel);
                h = (cssHeight / dotsPerPixel);
            } else if (cssWidth >= 0) {
                w = (cssWidth / dotsPerPixel);
                h = DEFAULT_SVG_HEIGHT;
            } else if (cssHeight >= 0) {
                w = DEFAULT_SVG_WIDTH;
                h = (cssHeight / dotsPerPixel);
            } else {
                w = DEFAULT_SVG_WIDTH;
                h = DEFAULT_SVG_HEIGHT;
            }
        } else {
            w = dimensions.x;
            h = dimensions.y;
        }
        
        svgElement.setAttribute("width", Integer.toString((int) w));
        svgElement.setAttribute("height", Integer.toString((int) h));
        this.pdfTranscoder.setImageSize((float) w, (float) h);
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
    
    public void setSecurityOptions(boolean allowScripts, boolean allowExternalResources, Set<String> allowedProtocols) {
        this.pdfTranscoder.setSecurityOptions(allowScripts, allowExternalResources, allowedProtocols);
        this.pdfTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_EXECUTE_ONLOAD, allowScripts);
    }

    public void setUserAgentCallback(UserAgentCallback userAgentCallback) {
        this.userAgentCallback = userAgentCallback;
    }

    private Integer parseLength(
            String attrValue,
            CSSName property,
            Box box,
            CssContext ctx) {

        try {
            return Integer.valueOf(attrValue);
        } catch (NumberFormatException e) {
            // Not a plain number, probably has a unit (px, cm, etc), so
            // try with css parser.

            CSSParser parser = new CSSParser((uri, msg) -> 
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_INVALID_INTEGER_PASSED_AS_DIMENSION_FOR_SVG, attrValue));

            PropertyValue value = parser.parsePropertyValue(property, StylesheetInfo.AUTHOR, attrValue);

            if (value == null) {
                // CSS parser couldn't deal with value either.
                return null;
            }

            LengthValue length = new LengthValue(box.getStyle(), property, value);
            float pixels = length.getFloatProportionalTo(property, box.getContainingBlock() == null ? 0 : box.getContainingBlock().getWidth(), ctx);

            return (int) Math.round(pixels / this.dotsPerPixel);
        }
    }

    private Point parseWidthHeightAttributes(Element e, Box box, CssContext ctx) {
        String widthAttr = e.getAttribute("width");
        Integer width = widthAttr.isEmpty() ? null :
            parseLength(widthAttr, CSSName.WIDTH, box, ctx);

        String heightAttr = e.getAttribute("height");
        Integer height = heightAttr.isEmpty() ? null : 
            parseLength(heightAttr, CSSName.HEIGHT, box, ctx);

        if (width != null && height != null) {
            return new Point(width, height);
        }

        return DEFAULT_DIMENSIONS;
    }

    private Point parseDimensions(Element e, Box box, CssContext ctx) {
        String viewBoxAttr = e.getAttribute("viewBox");
        String[] splitViewBox = viewBoxAttr.split("\\s+");
        if (splitViewBox.length != 4) {
            return parseWidthHeightAttributes(e, box, ctx);
        }
        try {
            int viewBoxWidth = Integer.parseInt(splitViewBox[2]);
            int viewBoxHeight = Integer.parseInt(splitViewBox[3]);

            return new Point(viewBoxWidth, viewBoxHeight);
        } catch (NumberFormatException ex) {
            return parseWidthHeightAttributes(e, box, ctx);
        }
    }

    @Override
    public void drawSVG(OutputDevice outputDevice, RenderingContext ctx,
            double x, double y) {

        OpenHtmlFontResolver fontResolver = this.fontResolver;
        if (fontResolver == null) {
            XRLog.log(Level.INFO, LogMessageId.LogMessageId0Param.GENERAL_IMPORT_FONT_FACE_RULES_HAS_NOT_BEEN_CALLED);
            fontResolver = new OpenHtmlFontResolver();
        }

        pdfTranscoder.setRenderingParameters(outputDevice, ctx, x, y,
                fontResolver, userAgentCallback);


        String styles = ctx.getCss().getCSSForAllDescendants(svgElement);

        try {
            DOMImplementation impl = SVGDOMImplementation
                    .getDOMImplementation();
            Document newDocument = impl.createDocument(
                    SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);

            if (styles != null && !styles.isEmpty()) {
                Element styleElem = newDocument.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, "style");
                Text styleText = newDocument.createTextNode(styles);
                styleElem.appendChild(styleText);
                newDocument.getDocumentElement().appendChild(styleElem);
            }

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
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_SVG_COULD_NOT_DRAW, e);
        }
    }
}
