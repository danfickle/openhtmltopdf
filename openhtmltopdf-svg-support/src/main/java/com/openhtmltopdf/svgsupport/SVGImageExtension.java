package com.openhtmltopdf.svgsupport;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.SVGBridgeExtension;
import org.apache.batik.bridge.SVGImageElementBridge;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.svg12.SVG12BridgeContext;
import org.apache.batik.bridge.svg12.SVG12BridgeExtension;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Document;

import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

public class SVGImageExtension {
    public static BridgeContext newBridge(String svgVersion, UserAgent userAgent, UserAgentCallback uac) {
        if ("1.2".equals(svgVersion)) {
            return new Bridge12Ctx(userAgent, uac);
        } else {
            return new BridgeCtx(userAgent, uac);
        }
    }

    public static String resolveUri(String uri, UserAgentCallback userAgentCallback) {
        try {
            // special handling of relative uri in case of file protocol, we receive something like "file:file.svg"
            // ie. Batik adds file: to the start of relative uris for some reason.
            URI parsedURI = new URI(uri);

            if ("file".equals(parsedURI.getScheme())) {
                return new URI(parsedURI.getSchemeSpecificPart()).isAbsolute() ?
                        userAgentCallback.resolveURI(parsedURI.toString()) :
                        userAgentCallback.resolveURI(parsedURI.getSchemeSpecificPart());
            } else {
                return userAgentCallback.resolveURI(uri);
            }
        } catch (URISyntaxException uriSyntaxException) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_URI_SYNTAX_WHILE_LOADING_EXTERNAL_SVG_RESOURCE, uri, uriSyntaxException);
            return null;
        }
    }

    private static class BridgeCtx extends BridgeContext {
        UserAgentCallback uac;

        public BridgeCtx(UserAgent userAgent, UserAgentCallback userAgentCallback) {
            super(userAgent, new OpenHtmlDocumentLoader(userAgent, userAgentCallback));
            this.uac = userAgentCallback;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public List getBridgeExtensions(Document doc) {
            List existing = super.getBridgeExtensions(doc);
            existing.add(new Ext(uac));
            return existing;
        }
    }

    private static class Bridge12Ctx extends SVG12BridgeContext {
        UserAgentCallback uac;

        public Bridge12Ctx(UserAgent userAgent, UserAgentCallback userAgentCallback) {
            super(userAgent, new OpenHtmlDocumentLoader(userAgent, userAgentCallback));
            this.uac = userAgentCallback;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public List getBridgeExtensions(Document doc) {
            List existing = super.getBridgeExtensions(doc);
            existing.add(new Ext12(uac));
            return existing;
        }
    }

    private static class Ext extends SVGBridgeExtension {
        final UserAgentCallback uac;

        public Ext(UserAgentCallback uac) {
            this.uac = uac;
        }

        @Override
        public void registerTags(BridgeContext ctx) {
            super.registerTags(ctx);
            ctx.putBridge(new ImageBridge(uac));
        }
    }

    private static class Ext12 extends SVG12BridgeExtension {
        final UserAgentCallback uac;

        public Ext12(UserAgentCallback uac) {
            this.uac = uac;
        }

        @Override
        public void registerTags(BridgeContext ctx) {
            super.registerTags(ctx);
            ctx.putBridge(new ImageBridge(uac));
        }
    }

    private static class ImageBridge extends SVGImageElementBridge {
        final UserAgentCallback uac;

        public ImageBridge(UserAgentCallback uac) {
            this.uac = uac;
        }

        @Override
        public org.apache.batik.bridge.Bridge getInstance() {
            return new ImageBridge(uac);
        }

        @Override
        protected org.apache.batik.gvt.GraphicsNode createImageGraphicsNode(BridgeContext ctx, org.w3c.dom.Element e, org.apache.batik.util.ParsedURL purl) {
            String uri = resolveUri(purl.toString(), uac);
            return super.createImageGraphicsNode(ctx, e, new ParsedURL(uri));
        };
    }
}
