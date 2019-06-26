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
    private final boolean allowScripts;
    private final boolean allowExternalResources;
    
    public static enum SvgScriptMode {
        SECURE,
        INSECURE_ALLOW_SCRIPTS;
    }
    
    public static enum SvgExternalResourceMode {
        SECURE,
        INSECURE_ALLOW_EXTERNAL_RESOURCE_REQUESTS;
    }
    
    /**
     * Creates a <code>SVGDrawer</code> that can allow arbitary scripts to run or allow arbitary external
     * resources to be requested.
     * 
     * IMPORTANT: External resources include the <code>file://</code> protocol and
     * may give an attacker access to all files on the system. Scripts
     * may call Javascript or Java code and take control of the system. Please be very sure you 
     * are ONLY using trusted SVGs before using this constructor!
     */
    public BatikSVGDrawer(SvgScriptMode scriptMode, SvgExternalResourceMode externalResourceMode) {
        this.allowScripts = scriptMode == SvgScriptMode.INSECURE_ALLOW_SCRIPTS;
        this.allowExternalResources = externalResourceMode == SvgExternalResourceMode.INSECURE_ALLOW_EXTERNAL_RESOURCE_REQUESTS;
    }

    /**
     * Creates a <code>SVGDrawer</code> that does NOT allow scripts to run or external resources such
     * as <code>file://</code> or <code>http://</code> protocol urls to be requested.
     * 
     * Recommended for most users.
     */
    public BatikSVGDrawer() {
        this(SvgScriptMode.SECURE, SvgExternalResourceMode.SECURE);
    }

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
        img.setSecurityOptions(allowScripts, allowExternalResources);
        return img;
    }
    
    @Override
    public void close() throws IOException {
    }
}
