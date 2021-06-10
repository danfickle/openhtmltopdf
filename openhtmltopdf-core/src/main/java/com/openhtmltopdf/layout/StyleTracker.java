/*
 * {{{ header & license
 * Copyright (c) 2005 Wisconsin Court System
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
package com.openhtmltopdf.layout;

import java.util.ArrayList;
import java.util.List;

import com.openhtmltopdf.css.newmatch.CascadedStyle;
import com.openhtmltopdf.css.style.CalculatedStyle;

/**
 * A managed list of {@link CalculatedStyle} objects.  It is used when keeping
 * track of the styles which apply to a :first-line or :first-letter pseudo 
 * element.
 * <br><br>
 * IMPORTANT: Immutable after constructor.
 */
public class StyleTracker {
    private final List<CascadedStyle> _styles;

    private static final StyleTracker EMPTY_INSTANCE = new StyleTracker(0);

    public StyleTracker(int size) {
        this._styles = new ArrayList<>(size);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        return ((StyleTracker) obj).getStyles().equals(this.getStyles());
    }

    public StyleTracker withStyle(CascadedStyle style) {
        StyleTracker tracker = new StyleTracker(getStyles().size() + 1);
        tracker._styles.addAll(getStyles());
        tracker._styles.add(style);
        return tracker;
    }

    public StyleTracker withOutLast() {
        if (_styles.isEmpty()) {
            return this;
        } else if (_styles.size() == 1) {
            return EMPTY_INSTANCE;
        }

        StyleTracker tracker = new StyleTracker(getStyles().size() - 1);
        tracker._styles.addAll(getStyles().subList(0, getStyles().size() - 1));
        return tracker;
    }

    public boolean hasStyles() {
        return !_styles.isEmpty();
    }

    public static StyleTracker withNoStyles() {
        return EMPTY_INSTANCE;
    }

    public CalculatedStyle deriveAll(CalculatedStyle start) {
        CalculatedStyle result = start;
        for (CascadedStyle style : getStyles()) {
            result = result.deriveStyle(style);
        }
        return result;
    }

    private List<CascadedStyle> getStyles() {
        return _styles;
    }
}
