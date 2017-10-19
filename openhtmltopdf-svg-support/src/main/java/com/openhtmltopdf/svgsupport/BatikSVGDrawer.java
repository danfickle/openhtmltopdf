package com.openhtmltopdf.svgsupport;

import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;

public class BatikSVGDrawer implements SVGDrawer {
    public OpenHtmlFontResolver fontResolver;

    @Override
    public void importFontFaceRules(List<FontFaceRule> fontFaces,
            SharedContext shared) {
        this.fontResolver = new OpenHtmlFontResolver();
        this.fontResolver.importFontFaces(fontFaces, shared);
    }

    @Override
    public SVGImage buildSVGImage(Element svgElement, double cssWidth,
            double cssHeight, double cssMaxWidth, double cssMaxHeight,
            double dotsPerPixel) {
        BatikSVGImage img = new BatikSVGImage(svgElement, cssWidth, cssHeight,
                cssMaxWidth, cssMaxHeight, dotsPerPixel);
        img.setFontResolver(fontResolver);
        return img;
    }
}
