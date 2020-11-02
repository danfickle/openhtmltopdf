package com.openhtmltopdf.svgsupport;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class BatikSVGImage implements SVGImage {
    private final static int DEFAULT_SVG_WIDTH = 400;
    private final static int DEFAULT_SVG_HEIGHT = 400;
    private final static Point DEFAULT_DIMENSIONS = new Point(DEFAULT_SVG_WIDTH, DEFAULT_SVG_HEIGHT);

    private final Element svgElement;
    private final double dotsPerPixel;
    private OpenHtmlFontResolver fontResolver;
    private final PDFTranscoder pdfTranscoder;
    private UserAgentCallback userAgentCallback;
    
    private enum AbsoluteCSSUnits {
        
        CENTIMETERS("cm", 37.7952755906),
 	MILLIMETERS("mm", 3.77952755906),
        INCHES("in", 300.0),
        PIXELS("px", 1.0),
        POINTS("pt", 4.0/3.0),
        PICAS("pc", 16.0);
        
        private String unit;
        private double pixelRatio;
        
        private AbsoluteCSSUnits(String unit, double pixelRatio) {
            this.unit = unit;
            this.pixelRatio = pixelRatio;
        }

        public String getUnit() {
            return unit;
        }
        
        public int toPixel(Number amount) {
            return amount != null ? (int) Math.round(amount.doubleValue() * pixelRatio) : null;
        }
        
        public static List<String> getUnits() {
            return Arrays.stream(AbsoluteCSSUnits.values())
                    .map(AbsoluteCSSUnits::getUnit)
                    .collect(Collectors.toList());
        }
        
        public static AbsoluteCSSUnits parseFromUnit(String unit) {
            return unit != null
                    ? Arrays.stream(AbsoluteCSSUnits.values())
                            .filter(cssUnit -> unit.toLowerCase().equals(cssUnit.unit))
                            .findFirst().orElse(null)
                    : null;
        }
    }
    
    private static class HtmlUnitConverter {
        public static Pattern UNIT_PATTERN = Pattern.compile("\\s*(\\d+(\\.\\d+)?)\\s*(" 
                + AbsoluteCSSUnits.getUnits().stream().collect(Collectors.joining("|")) 
                + ")?", Pattern.CASE_INSENSITIVE);
        
        public static Integer convert(String value) throws Exception {
            Matcher matcher = UNIT_PATTERN.matcher(value);
            if (matcher.matches()) {
                Double number = Double.valueOf(matcher.group(1));
                AbsoluteCSSUnits unit = AbsoluteCSSUnits.parseFromUnit(matcher.group(3));
                Integer dots = unit != null 
                        ? (int) Math.round(unit.toPixel(number)) 
                        : number.intValue();
                return dots;
            }
            return null;
        }
    }

    public BatikSVGImage(Element svgElement, Box box, double cssWidth, double cssHeight,
            double cssMaxWidth, double cssMaxHeight, double dotsPerPixel) {
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
        
        Point dimensions = parseDimensions(svgElement);
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
    
    public Integer parseLength(String attrValue) {
        try {
            return HtmlUnitConverter.convert(attrValue);
        } catch (Exception e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_INVALID_INTEGER_PASSED_AS_DIMENSION_FOR_SVG, attrValue);
            return null;
        }
    }
    
    public Point parseWidthHeightAttributes(Element e) {
        String widthAttr = e.getAttribute("width");
        Integer width = widthAttr.isEmpty() ? null : parseLength(widthAttr);

        String heightAttr = e.getAttribute("height");
        Integer height = heightAttr.isEmpty() ? null : parseLength(heightAttr);

        if (width != null && height != null) {
            return new Point(width, height);
        }
        
        return DEFAULT_DIMENSIONS;
    }

    public Point parseDimensions(Element e) {
        String viewBoxAttr = e.getAttribute("viewBox");
        String[] splitViewBox = viewBoxAttr.split("\\s+");
        if (splitViewBox.length != 4) {
            return parseWidthHeightAttributes(e);
        }
        try {
            int viewBoxWidth = Integer.parseInt(splitViewBox[2]);
            int viewBoxHeight = Integer.parseInt(splitViewBox[3]);

            return new Point(viewBoxWidth, viewBoxHeight);
        } catch (NumberFormatException ex) {
            return parseWidthHeightAttributes(e);
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
