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
package com.openhtmltopdf.swing;

import java.awt.Font;
import java.util.List;

import com.openhtmltopdf.render.FSFont;

public class AWTFSFont implements FSFont {
    private final List<Font> _fonts;
    private final float _size;
    
    public AWTFSFont(List<Font> fonts, float size) {
        _fonts = fonts;
        _size = size;
    }
    
    public float getSize2D() {
        return _size;
    }
    
    public List<Font> getAWTFonts() {
        return _fonts;
    }
}
