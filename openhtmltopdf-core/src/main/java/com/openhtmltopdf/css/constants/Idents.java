/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Patrick Wright
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
package com.openhtmltopdf.css.constants;

import java.util.*;
import java.util.regex.Pattern;


/**
 * Booch utility class for working with ident values in CSS.
 *
 * @author Patrick Wright
 */

// TODO: idents are also defined in Ident, but then need to decide whether lookup is useful or not; here we use strings (PWW 28-01-05)
// TODO: check idents list against CSS 2.1 spec (not 2.0 spec) (PWW 28-01-05)
public final class Idents {
    /*
     * Useful regexes to remember for later, from http://www.javapractices.com/Topic151.cjp
     * text
     * "^(\\S)(.){1,75}(\\S)$";
     * non-negative ints, incl
     * "(\\d){1,9}";
     * ints
     * "(-)?" + <non-negative ints>
     * non-negative floats, incl 0.0
     * "(\\d){1,10}\\.(\\d){1,10}";
     * floats
     * "(-)?" + <non-negative floats>;
     */
    /**
     * Regex pattern, a CSS number--either integer or float
     */
    private final static String RCSS_NUMBER = "(-)?((\\d){1,10}((\\.)(\\d){1,10})?)";
    /**
     * Regex pattern, CSS lengths, a length must have a unit, unless it is zero
     */
    private final static String RCSS_LENGTH = "((0$)|((" + RCSS_NUMBER + ")+" + "((em)|(ex)|(px)|(cm)|(mm)|(in)|(pt)|(pc)|(%))))";

    /**
     * Pattern instance, for CSS lengths
     */
    private final static Pattern CSS_LENGTH_PATTERN = Pattern.compile(RCSS_LENGTH);

    /**
     * Description of the Field
     */
    private final static Set<String> BACKGROUND_POSITIONS_IDENTS = new HashSet<>(Arrays.asList("top", "center", "bottom", "right", "left"));


    /**
     * Description of the Method
     *
     * @param val PARAM
     * @return Returns
     */
    public static boolean looksLikeALength(String val) {
        return CSS_LENGTH_PATTERN.matcher(val).matches();
    }


    /**
     * Description of the Method
     *
     * @param val PARAM
     * @return Returns
     */
    public static boolean looksLikeABGPosition(String val) {
        return BACKGROUND_POSITIONS_IDENTS.contains(val) || looksLikeALength(val);
    }

}// end class

/*
 * $Id$
 *
 * $Log$
 * Revision 1.17  2007/02/19 14:53:36  peterbrant
 * Integrate new CSS parser
 *
 * Revision 1.16  2007/02/07 16:33:36  peterbrant
 * Initial commit of rewritten table support and associated refactorings
 *
 * Revision 1.15  2006/07/28 10:08:55  pdoubleya
 * Additional work for support of parsing content and quotes.
 *
 * Revision 1.14  2006/04/03 00:01:59  peterbrant
 * Fix color: inherit
 *
 * Revision 1.13  2006/04/02 22:22:35  peterbrant
 * Add function interface for generated content / Implement page counters in terms of this, removing previous hack / Add custom page numbering functions
 *
 * Revision 1.12  2005/11/12 21:55:25  tobega
 * Inline enhancements: block box text decorations, correct line-height when it is a number, better first-letter handling
 *
 * Revision 1.11  2005/11/08 22:53:44  tobega
 * added getLineHeight method to CalculatedStyle and hacked in some list-item support
 *
 * Revision 1.10  2005/10/31 16:19:58  pdoubleya
 * Orange is a CSS 2.1 color; double-checked list of color constants.
 *
 * Revision 1.9  2005/10/20 20:48:03  pdoubleya
 * Updates for refactoring to style classes. CalculatedStyle now has lookup methods to cover all general cases, so propertyByName() is private, which means the backing classes for styling were able to be replaced.
 *
 * Revision 1.8  2005/07/04 00:12:11  tobega
 * text-align now works for table-cells too (is done in render, not in layout)
 *
 * Revision 1.7  2005/06/04 12:45:14  tobega
 * Added support for rgb-triples. Added fallback to default for non-css color idents.
 * Fixed some stuff with eeze.
 *
 * Revision 1.6  2005/04/07 16:21:34  pdoubleya
 * Formatting.
 *
 * Revision 1.5  2005/03/17 20:22:32  pdoubleya
 * Added orange (Kevin).
 *
 * Revision 1.4  2005/01/29 20:21:09  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.3  2005/01/29 12:17:18  pdoubleya
 * .
 *
 * Revision 1.2  2005/01/24 19:01:07  pdoubleya
 * Mass checkin. Changed to use references to CSSName, which now has a Singleton instance for each property, everywhere property names were being used before. Removed commented code. Cascaded and Calculated style now store properties in arrays rather than maps, for optimization.
 *
 * Revision 1.1  2005/01/24 14:27:51  pdoubleya
 * Added to CVS.
 *
 *
 */

