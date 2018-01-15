package com.openhtmltopdf.svgsupport;

import java.io.IOException;
import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.Box;
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
    public SVGImage buildSVGImage(Element svgElement, Box box, CssContext c,
    		double cssWidth, double cssHeight, double dotsPerPixel) {
    	
    	double cssMaxWidth = CalculatedStyle.getCSSMaxWidth(c, box);
    	double cssMaxHeight = CalculatedStyle.getCSSMaxHeight(c, box);
    	
        BatikSVGImage img = new BatikSVGImage(svgElement, cssWidth, cssHeight,
                cssMaxWidth, cssMaxHeight, dotsPerPixel);
        img.setFontResolver(fontResolver);
        return img;
    }
    
    @Override
    public void close() throws IOException {
    }
}
