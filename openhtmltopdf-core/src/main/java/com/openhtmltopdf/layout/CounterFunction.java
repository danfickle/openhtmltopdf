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
package com.openhtmltopdf.layout;

import java.util.Iterator;
import java.util.List;

import com.openhtmltopdf.css.constants.IdentValue;

public class CounterFunction {
    private IdentValue _listStyleType;
    private int _counterValue;
    private List<Integer> _counterValues;
    private String _separator;

    public CounterFunction(int counterValue, IdentValue listStyleType) {
        _counterValue = counterValue;
        _listStyleType = listStyleType;
    }

    public CounterFunction(List<Integer> counterValues, String separator, IdentValue listStyleType) {
        _counterValues = counterValues;
        _separator = separator;
        _listStyleType = listStyleType;
    }

    public static String createCounterText(IdentValue listStyle, int listCounter) {
        String text;
        if (listStyle == IdentValue.LOWER_LATIN || listStyle == IdentValue.LOWER_ALPHA) {
            text = CounterLanguage.toLatin(listCounter).toLowerCase();
        } else if (listStyle == IdentValue.UPPER_LATIN || listStyle == IdentValue.UPPER_ALPHA) {
            text = CounterLanguage.toLatin(listCounter).toUpperCase();
        } else if (listStyle == IdentValue.LOWER_ROMAN) {
            text = CounterLanguage.toRoman(listCounter).toLowerCase();
        } else if (listStyle == IdentValue.UPPER_ROMAN) {
            text = CounterLanguage.toRoman(listCounter).toUpperCase();
        } else if (listStyle == IdentValue.DECIMAL_LEADING_ZERO) {
            text = (listCounter >= 10 ? "" : "0") + listCounter;
        } else { // listStyle == IdentValue.DECIMAL or anything else
            text = Integer.toString(listCounter);
        }
        return text;
    }


    public String evaluate() {
        if (_counterValues == null) {
            return createCounterText(_listStyleType, _counterValue);
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<Integer> i = _counterValues.iterator(); i.hasNext();) {
            Integer value = i.next();
            sb.append(createCounterText(_listStyleType, value.intValue()));
            if (i.hasNext()) sb.append(_separator);
        }
        return sb.toString();
    }
}
