package com.openhtmltopdf.svgsupport;

import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.openhtmltopdf.extend.UserAgentCallback;
import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;

public class BatikSVGDrawer implements SVGDrawer {
    private final Set<String> allowedProtocols;
    public OpenHtmlFontResolver fontResolver;
    private final boolean allowScripts;
    private final boolean allowExternalResources;
    private UserAgentCallback userAgentCallback;
    
    public enum SvgScriptMode {
        SECURE,
        INSECURE_ALLOW_SCRIPTS;
    }
    
    public enum SvgExternalResourceMode {
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
        this.allowedProtocols = null;
    }

    /**
     * Creates a <code>SVGDrawer</code> that can allow arbitary scripts to run and allow the loading of
     * external resources with the specified protocols.
     *
     * @param scriptMode
     * @param allowedProtocols
     */
    public BatikSVGDrawer(SvgScriptMode scriptMode, Set<String> allowedProtocols) {
        this.allowScripts = scriptMode == SvgScriptMode.INSECURE_ALLOW_SCRIPTS;
        this.allowExternalResources = false;
        this.allowedProtocols = Collections.unmodifiableSet(allowedProtocols);
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
    public void withUserAgent(UserAgentCallback userAgentCallback) {
        this.userAgentCallback = userAgentCallback;
    }

    @Override
    public SVGImage buildSVGImage(Element svgElement, Box box, CssContext c,
    		double cssWidth, double cssHeight, double dotsPerPixel) {
    	
    	double cssMaxWidth = CalculatedStyle.getCSSMaxWidth(c, box);
    	double cssMaxHeight = CalculatedStyle.getCSSMaxHeight(c, box);

        BatikSVGImage img = new BatikSVGImage(
                svgElement, box, cssWidth, cssHeight,
                cssMaxWidth, cssMaxHeight, dotsPerPixel, c);
        img.setFontResolver(fontResolver);
        img.setUserAgentCallback(userAgentCallback);
        img.setSecurityOptions(allowScripts, allowExternalResources, allowedProtocols);
        return img;
    }
    
    @Override
    public void close() {
    }

    @Override
    public void addFontFile(File fontFile, String family, Integer weight, FontStyle style) throws IOException, FontFormatException {
        this.fontResolver.addFontFile(fontFile, family, weight, style);
    }
}
