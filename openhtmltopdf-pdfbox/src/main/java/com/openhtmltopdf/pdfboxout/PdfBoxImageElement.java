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

import java.awt.Point;
import java.awt.Rectangle;

import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;

public class PdfBoxImageElement implements PdfBoxReplacedElement {
    private FSImage _image;

    private Point _location = new Point(0, 0);

    public PdfBoxImageElement(FSImage image) {
        _image = image;
    }

    public int getIntrinsicWidth() {
        return (int) _image.getWidth();
    }

    public int getIntrinsicHeight() {
        return (int) _image.getHeight();
    }

    public Point getLocation() {
        return _location;
    }

    public void setLocation(int x, int y) {
        _location = new Point(x, y);
    }

    public FSImage getImage() {
        return _image;
    }

    public void detach(LayoutContext c) {
    }

    public boolean isRequiresInteractivePaint() {
        // N/A
        return false;
    }

    public void paint(RenderingContext c, PdfBoxOutputDevice outputDevice,
            BlockBox box) {
        Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(),
                box.getAbsY(), c);
        ReplacedElement element = box.getReplacedElement();
        outputDevice.drawImage(((PdfBoxImageElement) element).getImage(),
                contentBounds.x, contentBounds.y);
    }

    public int getBaseline() {
        return 0;
    }

    public boolean hasBaseline() {
        return false;
    }
}
