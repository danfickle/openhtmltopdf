/*
 * {{{ header & license
 * ValueConstants.java
 * Copyright (c) 2004, 2005 Patrick Wright
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
package com.openhtmltopdf.css.constants;

import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRRuntimeException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;


/**
 * Utility class for working with <code>CSSValue</code> instances.
 *
 * @author empty
 */
public final class ValueConstants {
    /**
     * Type descriptions--a crude approximation taken by scanning CSSValue
     * statics
     */
    private final static List<String> TYPE_DESCRIPTIONS;
    /**
     * Description of the Field
     */
    private final static Map<Short, String> sacTypesStrings;

    /**
     * Description of the Method
     *
     * @param type PARAM
     * @return Returns
     */
    public static String stringForSACPrimitiveType(short type) {
        return sacTypesStrings.get(new Short(type));
    }

    /**
     * Returns true if the specified type absolute (even if we have a computed
     * value for it), meaning that either the value can be used directly (e.g.
     * pixels) or there is a fixed context-independent conversion for it (e.g.
     * inches). Proportional types (e.g. %) return false.
     *
     * @param type The CSSValue type to check.
     * @return See desc.
     */
    //TODO: method may be unnecessary (tobe)
    public static boolean isAbsoluteUnit(short type) {
        // TODO: check this list...

        // note, all types are included here to make sure none are missed
        switch (type) {
            // proportional length or size
            case CSSPrimitiveValue.CSS_PERCENTAGE:
                return false;
                // refer to values known to the DerivedValue instance (tobe)
            case CSSPrimitiveValue.CSS_EMS:
            case CSSPrimitiveValue.CSS_REMS:
            case CSSPrimitiveValue.CSS_EXS:
                // length
            case CSSPrimitiveValue.CSS_IN:
            case CSSPrimitiveValue.CSS_CM:
            case CSSPrimitiveValue.CSS_MM:
            case CSSPrimitiveValue.CSS_PT:
            case CSSPrimitiveValue.CSS_PC:
            case CSSPrimitiveValue.CSS_PX:

                // color
            case CSSPrimitiveValue.CSS_RGBCOLOR:

                // ?
            case CSSPrimitiveValue.CSS_ATTR:
            case CSSPrimitiveValue.CSS_DIMENSION:
            case CSSPrimitiveValue.CSS_NUMBER:
            case CSSPrimitiveValue.CSS_RECT:

                // counters
            case CSSPrimitiveValue.CSS_COUNTER:

                // angles
            case CSSPrimitiveValue.CSS_DEG:
            case CSSPrimitiveValue.CSS_GRAD:
            case CSSPrimitiveValue.CSS_RAD:

                // aural - freq
            case CSSPrimitiveValue.CSS_HZ:
            case CSSPrimitiveValue.CSS_KHZ:

                // time
            case CSSPrimitiveValue.CSS_S:
            case CSSPrimitiveValue.CSS_MS:

                // URI
            case CSSPrimitiveValue.CSS_URI:

            case CSSPrimitiveValue.CSS_IDENT:
            case CSSPrimitiveValue.CSS_STRING:
                return true;
            case CSSPrimitiveValue.CSS_UNKNOWN:
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.CASCADE_IS_ABSOLUTE_CSS_UNKNOWN_GIVEN, new Exception());
                // fall-through
            default:
                return false;
        }
    }

    /**
     * Returns true if the SAC primitive value type is a number unit--a unit
     * that can only contain a numeric value. This is a shorthand way of saying,
     * did the user declare this as a number unit (like px)?
     *
     * @param cssPrimitiveType PARAM
     * @return See desc.
     */
    public static boolean isNumber(short cssPrimitiveType) {
        switch (cssPrimitiveType) {
            // fall thru on all these
            // relative length or size
            case CSSPrimitiveValue.CSS_EMS:
            case CSSPrimitiveValue.CSS_EXS:
            case CSSPrimitiveValue.CSS_PERCENTAGE:
                // relatives will be treated separately from lengths;
                return false;
                // length
            case CSSPrimitiveValue.CSS_PX:
            case CSSPrimitiveValue.CSS_IN:
            case CSSPrimitiveValue.CSS_CM:
            case CSSPrimitiveValue.CSS_MM:
            case CSSPrimitiveValue.CSS_PT:
            case CSSPrimitiveValue.CSS_PC:
                return true;
            default:
                return false;
        }
    }

    static {
        SortedMap<Short, String> map = new TreeMap<>();
        TYPE_DESCRIPTIONS = new ArrayList<>();
        try {
            Field[] fields = CSSPrimitiveValue.class.getFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                int mod = f.getModifiers();
                if (Modifier.isFinal(mod) &&
                        Modifier.isStatic(mod) &&
                        Modifier.isPublic(mod)) {

                    Short val = (Short) f.get(null);
                    String name = f.getName();
                    if (name.startsWith("CSS_")) {
                        if (!name.equals("CSS_INHERIT") &&
                                !name.equals("CSS_PRIMITIVE_VALUE") &&
                                !name.equals("CSS_VALUE_LIST") &&
                                !name.equals("CSS_CUSTOM")) {

                            map.put(val, name.substring("CSS_".length()));
                        }
                    }
                }
            }
            // now sort by the key--the short constant for the public fields
            List<Short> keys = new ArrayList<>(map.keySet());
            Collections.sort(keys);

            // then add to our static list, in the order the keys appear. this means
            // list.get(index) will return the item at index, which should be the description
            // for that constant
            Iterator<Short> iter = keys.iterator();
            while (iter.hasNext()) {
                TYPE_DESCRIPTIONS.add(map.get(iter.next()));
            }
        } catch (Exception ex) {
            throw new XRRuntimeException("Could not build static list of CSS type descriptions.", ex);
        }

        // HACK: this is a quick way to perform the lookup, but dumb if the short assigned are > 100; but the compiler will tell us that (PWW 21-01-05)
        sacTypesStrings = new HashMap<>(25);
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_EMS), "em");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_EXS), "ex");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_PX), "px");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_PERCENTAGE), "%");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_IN), "in");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_CM), "cm");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_MM), "mm");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_PT), "pt");
        sacTypesStrings.put(new Short(CSSPrimitiveValue.CSS_PC), "pc");
    }

}// end class

/*
 * $Id$
 *
 * $Log$
 * Revision 1.10  2005/10/25 16:06:49  pdoubleya
 * For guessing type, with no type code, check last char, not first.
 *
 * Revision 1.9  2005/10/25 15:38:27  pdoubleya
 * Moved guessType() to ValueConstants, applied fix to method suggested by Chris Oliver, to avoid exception-based catch.
 *
 * Revision 1.8  2005/09/11 20:43:15  tobega
 * Fixed table-css interaction bug, colspan now works again
 *
 * Revision 1.7  2005/06/01 00:47:01  tobega
 * Partly confused hack trying to get width and height working properly for replaced elements.
 *
 * Revision 1.6  2005/01/29 20:18:40  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.5  2005/01/24 14:52:20  pdoubleya
 * Fixed accidental access modifier change to private--isAbsoluteUnit() is used in tests.
 *
 * Revision 1.4  2005/01/24 14:36:32  pdoubleya
 * Mass commit, includes: updated for changes to property declaration instantiation, and new use of DerivedValue. Removed any references to older XR... classes (e.g. XRProperty). Cleaned imports.
 *
 * Revision 1.3  2004/11/16 10:38:21  pdoubleya
 * Use XRR exception, added comments.
 *
 * Revision 1.2  2004/10/23 13:09:13  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards
 * except for common packages
 * (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 *
 */

