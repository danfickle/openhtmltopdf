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
package com.openhtmltopdf.css.style.derived;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.CounterData;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.DerivedValue;
import com.openhtmltopdf.util.Constants;

import java.util.List;

public class CountersValue extends DerivedValue {
    private final List<CounterData> _values;
    
    public CountersValue(CSSName name, PropertyValue value) {
        super(name, value.getPrimitiveType(), value.getCssText(), value.getCssText());
        
        _values = value.getCounters();
    }
    
    public List<CounterData> getValues() {
        return _values;
    }
    
    @Override
    public String[] asStringArray() {
        if (_values == null || _values.isEmpty()) {
            return Constants.EMPTY_STR_ARR;
        }
        
        return _values.stream().map(Object::toString).toArray(String[]::new);
    }
}
