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

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import org.w3c.dom.Element;

public class PdfBoxReplacedElementFactory implements ReplacedElementFactory {
    private final PdfBoxOutputDevice _outputDevice;
    private final SVGDrawer _svgImpl;
    private final SVGDrawer _mathmlImpl;
    private final FSObjectDrawerFactory _objectDrawerFactory;

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
                if (srcAttr.endsWith(".svg") && _svgImpl != null) {
                    return new PdfBoxSVGReplacedElement(uac.getXMLResource(srcAttr).getDocument().getDocumentElement(), _svgImpl, cssWidth, cssHeight, box, c, c.getSharedContext());
                }

                FSImage fsImage = uac.getImageResource(srcAttr).getImage();
                if (fsImage != null) {
                    boolean hasMaxHeight = !box.getStyle().isMaxHeightNone();
                    boolean hasMaxWidth = !box.getStyle().isMaxWidthNone();
                    boolean hasMaxProperty = hasMaxWidth || hasMaxHeight;
                    if (cssWidth == -1 && cssHeight == -1) {
                        if (hasMaxProperty) {
                            long maxWidth = CalculatedStyle.getCSSMaxWidth(c, box);
                            long maxHeight = CalculatedStyle.getCSSMaxHeight(c, box);
                            int intrinsicHeight = fsImage.getHeight();
                            int intrinsicWidth = fsImage.getWidth();

                            if (hasMaxWidth && hasMaxHeight) {
                                if (intrinsicWidth > maxWidth || intrinsicHeight > maxHeight) {
                                    double rw = (double) intrinsicWidth / (double) maxWidth;
                                    double rh = (double) intrinsicHeight / (double) maxHeight;

                                    if (rw > rh) {
                                        fsImage.scale((int) maxWidth, -1);
                                    } else {
                                        fsImage.scale(-1, (int) maxHeight);
                                    }
                                }
                            } else if (hasMaxWidth && intrinsicWidth > maxWidth) {
                                fsImage.scale((int) maxWidth, -1);
                            } else if (hasMaxHeight && intrinsicHeight > maxHeight) {
                                fsImage.scale(-1, (int) maxHeight);
                            }
                        }
                    } else {
                        if (hasMaxProperty) {
                            long maxWidth = box.getStyle().asLength(c, CSSName.MAX_WIDTH).value();
                            long maxHeight = box.getStyle().asLength(c, CSSName.MAX_HEIGHT).value();
                            if (cssHeight > maxHeight && cssHeight >= cssWidth) {
                                fsImage.scale(-1, (int) maxHeight);
                            } else if (cssWidth > maxWidth) {
                                fsImage.scale((int) maxWidth, -1);
                            } else {
                                fsImage.scale(cssWidth, cssHeight);
                            }
                        } else {
                            fsImage.scale(cssWidth, cssHeight);
                        }
                    }
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
