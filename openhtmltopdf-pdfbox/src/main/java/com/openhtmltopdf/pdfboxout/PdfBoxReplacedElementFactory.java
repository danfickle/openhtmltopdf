/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.swing.NaiveUserAgent;

import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;

public class PdfBoxReplacedElementFactory implements ReplacedElementFactory {
    private final SVGDrawer _svgImpl;
    private final SVGDrawer _mathmlImpl;
    private final FSObjectDrawerFactory _objectDrawerFactory;
    private final PdfBoxOutputDevice _outputDevice;

    public PdfBoxReplacedElementFactory(PdfBoxOutputDevice outputDevice, SVGDrawer svgImpl, FSObjectDrawerFactory objectDrawerFactory, SVGDrawer mathmlImpl) {
        _outputDevice = outputDevice;
        _svgImpl = svgImpl;
        _objectDrawerFactory = objectDrawerFactory;
        _mathmlImpl = mathmlImpl;
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
                                                 UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.getNodeName();

        if (nodeName.equals("math") && _mathmlImpl != null) {
            return new PdfBoxSVGReplacedElement(e, _mathmlImpl, cssWidth, cssHeight, box, c, c.getSharedContext());
        } else if (nodeName.equals("svg") && _svgImpl != null) {
            return new PdfBoxSVGReplacedElement(e, _svgImpl, cssWidth, cssHeight, box, c, c.getSharedContext());
        } else if (nodeName.equals("img")) {
            String srcAttr = e.getAttribute("src");
            if (srcAttr != null && srcAttr.length() > 0) {
                //handle the case of linked svg from img tag
                boolean isDataImageSvg = false;
                if (_svgImpl != null && (srcAttr.endsWith(".svg") || (isDataImageSvg = srcAttr.startsWith("data:image/svg+xml;base64,")))) {
                    XMLResource xml = isDataImageSvg ?
                         XMLResource.load(new ByteArrayInputStream(NaiveUserAgent.getEmbeddedBase64Image(srcAttr))) : 
                         uac.getXMLResource(srcAttr, ExternalResourceType.XML_SVG);

                    if (xml != null) {
                        Element svg = xml.getDocument().getDocumentElement();

                        // Copy across the class attribute so it can be targetted with CSS.
                        if (!e.getAttribute("class").isEmpty()) {
                            svg.setAttribute("class", e.getAttribute("class"));
                        }

                        return new PdfBoxSVGReplacedElement(svg, _svgImpl, cssWidth, cssHeight, box, c, c.getSharedContext());    
                    }

                    return null;
                } else if (srcAttr.endsWith(".pdf")) {
                    byte[] pdfBytes = uac.getBinaryResource(srcAttr, ExternalResourceType.PDF);
                    
                    if (pdfBytes != null) {
                        return PdfBoxPDFReplacedElement.create(_outputDevice.getWriter(), pdfBytes, e, box, c, c.getSharedContext());
                    }
                    
                    return null;
                }

                FSImage fsImage = uac.getImageResource(srcAttr).getImage();
                if (fsImage != null) {
                    return new PdfBoxImageElement(e,fsImage,c.getSharedContext(), box.getStyle().isImageRenderingInterpolate());
                }
            }
        } else if (nodeName.equals("input")) {
            /* We do nothing here. Form Elements are handled special in PdfBoxOutputDevice.paintBackground() */
        } else if (nodeName.equals("bookmark")) {
            // HACK Add box as named anchor and return placeholder
            BookmarkElement result = new BookmarkElement();
            if (e.hasAttribute("name")) {
                String name = e.getAttribute("name");
                c.addBoxId(name, box);
                result.setAnchorName(name);
            }
            return result;
        } else if (nodeName.equals("object") && _objectDrawerFactory != null) {
			FSObjectDrawer drawer = _objectDrawerFactory.createDrawer(e);
			if (drawer != null)
				return new PdfBoxObjectDrawerReplacedElement(e, drawer, cssWidth, cssHeight,
						c.getSharedContext());
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
        } else if (nodeName.equals("math") && _mathmlImpl != null) {
            return true;
        } else if (nodeName.equals("svg") && _svgImpl != null) {
            return true;
        } else if (nodeName.equals("bookmark")) {
            return true;
        } else if (nodeName.equals("object") && _objectDrawerFactory != null) {
            return _objectDrawerFactory.isReplacedObject(e);
        }
        
        return false;
    }
}
