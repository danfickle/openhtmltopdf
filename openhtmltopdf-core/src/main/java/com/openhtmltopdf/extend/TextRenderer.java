/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package com.openhtmltopdf.extend;

import com.openhtmltopdf.layout.Breaker;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.JustificationInfo;
import com.openhtmltopdf.util.OpenUtil;

import static com.openhtmltopdf.util.OpenUtil.areAllCharactersPrintable;

public interface TextRenderer {

    /**
     * Returns a string containing printable characters only.
     *
     * @param input The string can be null
     * @return The cleaned string or <code>null</code> if the input is null
     * @see com.openhtmltopdf.util.OpenUtil#isSafeFontCodePointToPrint(int)
     */
    public static String getEffectivePrintableString(String input) {
        if (input == null || input.isEmpty() || areAllCharactersPrintable(input)) {
            return input;
        }

        StringBuilder effective = new StringBuilder(input.length());
        input.codePoints().filter(OpenUtil::isSafeFontCodePointToPrint).forEach(effective::appendCodePoint);

        return effective.toString();
    }

    void setup(FontContext context);

    void drawString(OutputDevice outputDevice, String string, float x, float y);
    void drawString(
            OutputDevice outputDevice, String string, float x, float y, JustificationInfo info);

    FSFontMetrics getFSFontMetrics(
            FontContext context, FSFont font, String string );

    /**
     * Rarely need to use this method directly.
     * Instead favor {@link Breaker} static method instead.
     */
    int getWidth(FontContext context, FSFont font, String string);
}

