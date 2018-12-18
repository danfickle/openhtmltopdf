/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

/**
 * Callback listener for PDF creation. To use this, call {@link PdfBoxRenderer#setListener(PDFCreationListener)}.
 */
public interface PDFCreationListener {
    /**
     * Called immediately after the PDF Document instance is created before the content is written.
     *
     * @param pdfBoxRenderer the renderer preparing the document
     */
    void preOpen(PdfBoxRenderer pdfBoxRenderer);

    /**
     * Called immediately before the pages of the PDF file are about to be written out.
     * This is an opportunity to modify any document metadata that will be used to generate
     * the PDF header fields (the document information dictionary).
     *
     * @param pdfBoxRenderer the renderer preparing the document
     * @param pageCount the number of pages that will be written to the PDF document
     */
    void preWrite(PdfBoxRenderer pdfBoxRenderer, int pageCount);

    /**
     * Called immediately before the Pdf Document instance is closed
     *
     * @param renderer the iTextRenderer preparing the document
     */
    void onClose(PdfBoxRenderer renderer);
}
