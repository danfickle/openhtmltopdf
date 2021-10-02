package com.openhtmltopdf.java2d;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.java2d.image.AWTFSImage;
import com.openhtmltopdf.java2d.image.ImageReplacedElement;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

public class Java2DReplacedElementFactory implements ReplacedElementFactory {
    private final SVGDrawer _svgImpl;
    private final FSObjectDrawerFactory _objectDrawerFactory;
    private final SVGDrawer _mathMLImpl;
    private final Map<SizedImageCacheKey, ReplacedElement> _sizedImageCache = new HashMap<>();

    public Java2DReplacedElementFactory(
            SVGDrawer svgImpl,
            FSObjectDrawerFactory objectDrawerFactory,
            SVGDrawer mathMLImpl) {
        this._svgImpl = svgImpl;
        this._objectDrawerFactory = objectDrawerFactory;
        this._mathMLImpl = mathMLImpl;
    }

    @Override
    public ReplacedElement createReplacedElement(
            LayoutContext context,
            BlockBox box,
            UserAgentCallback uac,
            int cssWidth,
            int cssHeight) {

        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.getNodeName();

        if (nodeName.equals("math") && _mathMLImpl != null) {
            return new Java2DSVGReplacedElement(e, _mathMLImpl, cssWidth, cssHeight, box, context);
        } else if (nodeName.equals("svg") && _svgImpl != null) {
            return new Java2DSVGReplacedElement(e, _svgImpl, cssWidth, cssHeight, box, context);
        } else if (nodeName.equals("object") && _objectDrawerFactory != null) {
            FSObjectDrawer drawer = _objectDrawerFactory.createDrawer(e);
            if (drawer != null) {
                return new Java2DObjectDrawerReplacedElement(e, drawer, cssWidth, cssHeight,
                        context.getSharedContext().getDotsPerPixel());
            }
        } else if (nodeName.equals("img")) {
            String srcAttr = e.getAttribute("src");
            if (!srcAttr.isEmpty() && srcAttr.endsWith(".svg") && _svgImpl != null) {
                return new Java2DSVGReplacedElement(uac.getXMLResource(srcAttr, ExternalResourceType.XML_SVG).getDocument().getDocumentElement(), _svgImpl, cssWidth, cssHeight, box, context);
            } else if (!srcAttr.isEmpty()) {
                return replaceImage(e, srcAttr, cssWidth, cssHeight, uac);
            }
        }

        return null;
    }

    @Override
    public boolean isReplacedElement(Element e) {
        if (e == null) {
            return false;
        }

        String nodeName = e.getNodeName();
        if (nodeName.equals("img")) {
            return true;
        } else if (nodeName.equals("math") && _mathMLImpl != null) {
            return true;
        } else if (nodeName.equals("svg") && _svgImpl != null) {
            return true;
        } else if (nodeName.equals("object") && _objectDrawerFactory != null) {
            return _objectDrawerFactory.isReplacedObject(e);
        }

        return false;
    }

    private ReplacedElement replaceImage(Element elem, String uri, int width, int height, UserAgentCallback uac) {
        ReplacedElement replaced = _sizedImageCache.get(new SizedImageCacheKey(uri, width, height));

        if (replaced != null) {
            return replaced;
        }

        XRLog.log(Level.FINE, LogMessageId.LogMessageId1Param.LOAD_LOAD_IMMEDIATE_URI, uri);

        ImageResource ir = uac.getImageResource(uri);

        if (ir == null) {
            return null;
        }

        FSImage awtfsImage = ir.getImage();
        BufferedImage newImg = ((AWTFSImage) awtfsImage).getImage();

        if (newImg == null) {
            return null;
        }

        if (width > -1 || height > -1) {
            XRLog.log(Level.FINE, LogMessageId.LogMessageId4Param.LOAD_IMAGE_LOADER_SCALING_URI_TO,
                    this, uri, width, height);

            replaced = new ImageReplacedElement(newImg, width, height);

            _sizedImageCache.put(new SizedImageCacheKey(uri, width, height), replaced);
        } else {
            replaced = new ImageReplacedElement(newImg, width, height);
        }

        return replaced;
    }

    private static class SizedImageCacheKey {
        final String uri;
        final int width;
        final int height;

        public SizedImageCacheKey(final String uri, final int width, final int height) {
            this.uri = uri;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || o.getClass() != this.getClass()) return false;

            final SizedImageCacheKey cacheKey = (SizedImageCacheKey) o;

            if (width != cacheKey.width ||
                height != cacheKey.height ||
                !Objects.equals(uri, cacheKey.uri)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, width, height);
        }
    }
}
