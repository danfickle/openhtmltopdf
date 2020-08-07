/*
 * {{{ header & license
 * GeneralUtil.java
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
package com.openhtmltopdf.util;

/**
 * Description of the Class
 *
 * @author Patrick Wright
 */
public class GeneralUtil {

    /**
     * Parses an integer from a string using less restrictive rules about which
     * characters we won't accept.  This scavenges the supplied string for any
     * numeric character, while dropping all others.
     *
     * @param s The string to parse
     * @return The number represented by the passed string, or 0 if the string
     *         is null, empty, white-space only, contains only non-numeric
     *         characters, or simply evaluates to 0 after parsing (e.g. "0")
     */
    public static int parseIntRelaxed(String s) {
        // An edge-case short circuit...
        if (s == null || s.length() == 0 || s.trim().length() == 0) {
            return 0;
        }

        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c)) {
                buffer.append(c);
            } else {
                // If we hit a non-numeric with numbers already in the
                // buffer, we're done.
                if (buffer.length() > 0) {
                    break;
                }
            }
        }

        if (buffer.length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(buffer.toString());
        } catch (NumberFormatException exception) {
            // The only way we get here now is if s > Integer.MAX_VALUE
            return Integer.MAX_VALUE;
        }
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.19  2009/05/09 15:16:43  pdoubleya
 * FindBugs: proper disposal of IO resources
 *
 * Revision 1.18  2009/04/25 11:03:27  pdoubleya
 * Fix some potential IO resource leaks, patches from Peter Fassev in issue #263. Clarifying docs on inputStreamToString() (we don't close the stream, the caller does), and a couple of other minor edits.
 *
 * Revision 1.17  2008/03/13 16:46:47  peterbrant
 * Comment out non-ASCII characters in escapeHTML() for now.  Will only work if the compiler assumes the source file encoding is ISO-8859-1 (or maybe Cp1252).  Does not work on Linux (with a default encoding of UTF-8).  Should be replaced with equivalent Unicode escapes.
 *
 * Revision 1.16  2008/03/01 19:27:28  pdoubleya
 * Utility method to convert certain character to HTML entity equivalents.
 *
 * Revision 1.15  2007/05/11 22:51:35  peterbrant
 * Patch from Sean Bright
 *
 * Revision 1.14  2007/04/10 20:46:38  pdoubleya
 * Fix, was not checking if resource was actually available before opening it
 *
 * Revision 1.13  2006/07/26 18:17:09  pdoubleya
 * Added convenience method, write string to file.
 *
 * Revision 1.12  2006/07/17 22:16:31  pdoubleya
 * Added utility methods, InputStream to String, and simple HTML escaping.
 *
 * Revision 1.11  2005/06/26 01:02:22  tobega
 * Now checking for SecurityException on System.getProperty
 *
 * Revision 1.10  2005/06/13 06:50:17  tobega
 * Fixed a bug in table content resolution.
 * Various "tweaks" in other stuff.
 *
 * Revision 1.9  2005/04/03 21:51:31  joshy
 * fixed code that gets the XMLReader on the mac
 * added isMacOSX() to GeneralUtil
 * added app name and single menu bar to browser
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.8  2005/02/02 11:17:18  pdoubleya
 * Added trackBack() method.
 *
 * Revision 1.7  2005/01/29 20:21:08  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.6  2005/01/24 14:33:47  pdoubleya
 * Added exception dump.
 *
 * Revision 1.5  2004/10/23 14:06:56  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 * Revision 1.4  2004/10/19 15:00:53  joshy
 * updated the build file
 * removed some extraneous files
 * update the home page to point to the new jnlp files
 * updated the resource loader to use the marker class
 * updated the text of the about box
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/18 23:43:02  joshy
 * final updates today
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.2  2004/10/18 17:10:13  pdoubleya
 * Added additional condition, and error check.
 *
 * Revision 1.1  2004/10/13 23:00:32  pdoubleya
 * Added to CVS.
 *
 */

