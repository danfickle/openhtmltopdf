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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;

public class PdfBoxUtil {
	
	private PdfBoxUtil() {
	}
	
    public static class FontRun {
        String str;
        FontDescription des;
        int spaceCharacterCount;
        int otherCharacterCount;
    }
	
    /** 
     * Class for storing metadata element name/content pairs from the head
     * section of an xhtml document.
     */
    static class Metadata {
        private String _name;
        private String _content;

        Metadata(String name, String content) {
            _name = name;
            _content = content;
        }

        public String getContent() {
            return _content;
        }

        public void setContent(String content) {
            _content = content;
        }

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }
    }
    
}
