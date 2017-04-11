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
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.simple.extend.FormSubmissionListener;
import org.w3c.dom.Element;

public class PdfBoxReplacedElementFactory implements ReplacedElementFactory {
    private final PdfBoxOutputDevice _outputDevice;
    private final SVGDrawer _svgImpl;
    private final FSObjectDrawerFactory _objectDrawerFactory;

    public PdfBoxReplacedElementFactory(PdfBoxOutputDevice outputDevice, SVGDrawer svgImpl, FSObjectDrawerFactory objectDrawerFactory) {
        _outputDevice = outputDevice;
        _svgImpl = svgImpl;
        _objectDrawerFactory = objectDrawerFactory;
    }

    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
                                                 UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.getNodeName();

        if (nodeName.equals("svg") && _svgImpl != null) {
            return new PdfBoxSVGReplacedElement(e, _svgImpl, cssWidth, cssHeight, c.getSharedContext().getDotsPerPixel());
        } else if (nodeName.equals("img")) {
            String srcAttr = e.getAttribute("src");
            if (srcAttr != null && srcAttr.length() > 0) {
                FSImage fsImage = uac.getImageResource(srcAttr).getImage();
                if (fsImage != null) {
                    boolean hasMaxHeight = !box.getStyle().isMaxHeightNone();
                    boolean hasMaxWidth = !box.getStyle().isMaxWidthNone();
                    boolean hasMaxProperty = hasMaxWidth || hasMaxHeight;
                    if (cssWidth == -1 && cssHeight == -1) {
                        if (hasMaxProperty) {
                            long maxWidth = box.getStyle().asLength(c, CSSName.MAX_WIDTH).value();
                            long maxHeight = box.getStyle().asLength(c, CSSName.MAX_HEIGHT).value();
                            int intrinsicHeight = fsImage.getHeight();
                            int intrinsicWidth = fsImage.getWidth();

                            if (hasMaxWidth && hasMaxHeight) {
                                double rw = (double) intrinsicWidth / (double) maxWidth;
                                double rh = (double) intrinsicHeight / (double) maxHeight;

                                if (rw > rh) {
                                    fsImage.scale((int) maxWidth, -1);
                                } else {
                                    fsImage.scale(-1, (int) maxHeight);
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
                    return new PdfBoxImageElement(fsImage);
                }
            }
        } else if (nodeName.equals("input")) {
            String type = e.getAttribute("type");
// TODO: Implement form fields.
//            if (type.equals("hidden")) {
//                return new EmptyReplacedElement(1, 1);
//            } else if (type.equals("checkbox")) {
//                return new CheckboxFormField(c, box, cssWidth, cssHeight);
//            } else if (type.equals("radio")) {
//                //TODO finish support for Radio button
//                //RadioButtonFormField result = new RadioButtonFormField(
//                //			this, c, box, cssWidth, cssHeight);
//                //		saveResult(e, result);
//                //return result;
//                return new EmptyReplacedElement(0, 0);
//
//            } else {
//                return new TextFormField(c, box, cssWidth, cssHeight);
//            }
//            /*
//             } else if (nodeName.equals("select")) {//TODO Support select
//             return new SelectFormField(c, box, cssWidth, cssHeight);
//             } else if (isTextarea(e)) {//TODO Review if this is needed the textarea item prints fine currently
//             return new TextAreaFormField(c, box, cssWidth, cssHeight);
//             */
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
						c.getSharedContext().getDotsPerPixel());
        }

        return null;
    }

    public void setFormSubmissionListener(FormSubmissionListener listener) {
        // nothing to do, form submission is handled by pdf readers
    }

    @Override
    public void reset() {
    }

    @Override
    public void remove(Element e) {
    }
}
