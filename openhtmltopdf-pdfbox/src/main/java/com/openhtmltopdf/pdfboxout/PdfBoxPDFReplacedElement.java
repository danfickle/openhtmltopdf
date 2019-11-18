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

import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxLinkManager.IPdfBoxElementWithShapedLinks;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.swing.ImageMapParser;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.w3c.dom.Element;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class PdfBoxPDFReplacedElement implements PdfBoxReplacedElement, IPdfBoxElementWithShapedLinks {
    private final PDFormXObject _srcFormObject;
    private final float _width;
    private final float _height;
    private final Map<Shape, String> _imageMap;
    private Point _location = new Point(0, 0);

    private PdfBoxPDFReplacedElement(PDFormXObject srcForm, Element e, Box box, CssContext ctx, SharedContext shared, float w, float h) {
        this._srcFormObject = srcForm;
        this._width = w;
        this._height = h;
        this._imageMap = ImageMapParser.findAndParseMap(e, shared);
    }
    
    private static int parsePage(Element e) {
        if (e.getAttribute("page").isEmpty()) {
            return 0;
        }
        
        try {
            return Integer.parseInt(e.getAttribute("page")) - 1;
        } catch (NumberFormatException e1) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_UNABLE_TO_PARSE_PAGE_OF_IMG_TAG_WITH_PDF, e1);
        }

        return 0;
    }

    public static PdfBoxPDFReplacedElement create(PDDocument target, byte[] pdfBytes, Element e, Box box, CssContext ctx, SharedContext shared) {
        try (PDDocument srcDocument = PDDocument.load(pdfBytes)){
            int pageNo = parsePage(e);
            if (pageNo >= srcDocument.getNumberOfPages()) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.LOAD_PAGE_DOES_NOT_EXIST_FOR_PDF_IN_IMG_TAG);
                return null;
            }
            
            PDPage page = srcDocument.getPage(pageNo);
            float conversion = 96f / 72f;
            float width = page.getMediaBox().getWidth() * shared.getDotsPerPixel() * conversion;
            float height = page.getMediaBox().getHeight() * shared.getDotsPerPixel() * conversion;
            
            LayerUtility util = new LayerUtility(target);
            PDFormXObject formXObject = util.importPageAsForm(srcDocument, page);
            
            return new PdfBoxPDFReplacedElement(formXObject, e, box, ctx, shared, width, height);
        } catch (InvalidPasswordException e1) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_TRIED_TO_OPEN_A_PASSWORD_PROTECTED_DOCUMENT_AS_SRC_FOR_IMG, e1);
        } catch (IOException e1) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_COULD_NOT_READ_PDF_AS_SRC_FOR_IMG, e1);
        }
        
        return null;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) _width;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) _height;
    }

    @Override
    public Point getLocation() {
        return _location;
    }

    @Override
    public void setLocation(int x, int y) {
        _location = new Point(x, y);
    }

    @Override
    public Map<Shape, String> getLinkMap() {
        return _imageMap;
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
        outputDevice.drawPdfAsImage(_srcFormObject, contentBounds, getIntrinsicWidth(), getIntrinsicHeight());
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
