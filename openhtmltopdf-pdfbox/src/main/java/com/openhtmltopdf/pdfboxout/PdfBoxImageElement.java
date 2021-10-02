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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxLinkManager.IPdfBoxElementWithShapedLinks;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.swing.ImageMapParser;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.Map;

public class PdfBoxImageElement implements PdfBoxReplacedElement, IPdfBoxElementWithShapedLinks {
    private final FSImage _image;
    private final boolean interpolate;

    private Point _location = new Point(0, 0);
    private final Map<Shape, String> imageMap;

    public PdfBoxImageElement(Element e, FSImage image, SharedContext c, boolean interpolate) {
        _image = image;
        this.interpolate = interpolate;
        imageMap = ImageMapParser.findAndParseMap(e, c);
    }

    @Override
    public int getIntrinsicWidth() {
        return _image.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return _image.getHeight();
    }

    @Override
    public Point getLocation() {
        return _location;
    }

    @Override
    public void setLocation(int x, int y) {
        _location = new Point(x, y);
    }

    public FSImage getImage() {
        return _image;
    }

    @Override
    public Map<Shape, String> getLinkMap() {
        return imageMap;
    }

    @Override
    public void detach(LayoutContext c) {
    }

    @Override
    public boolean isRequiresInteractivePaint() {
        // N/A
        return false;
    }

    @Override
    public void paint(RenderingContext c, PdfBoxOutputDevice outputDevice, BlockBox box) {
        Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
        ReplacedElement element = box.getReplacedElement();
        
        FSImage img = ((PdfBoxImageElement) element).getImage();
        img.scale(contentBounds.width, contentBounds.height);
        
        outputDevice.drawImage(img,
                contentBounds.x, contentBounds.y, interpolate);
    }

    @Override
    public int getBaseline() {
        return 0;
    }

    @Override
    public boolean hasBaseline() {
        return false;
    }
}
