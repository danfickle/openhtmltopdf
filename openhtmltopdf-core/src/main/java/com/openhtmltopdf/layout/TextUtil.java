/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package com.openhtmltopdf.layout;

import java.text.BreakIterator;
import java.util.Locale;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.extend.FSTextTransformer;
import com.openhtmltopdf.util.ThreadCtx;

public class TextUtil {
	public static class DefaultCharacterBreaker implements FSTextBreaker {
		private final BreakIterator iter;
		
		public DefaultCharacterBreaker(BreakIterator iter) {
			this.iter = iter;
		}
		
		@Override
		public int next() {
			return iter.next();
		}

		@Override
		public void setText(String newText) {
			iter.setText(newText);
		}
	}

	public static class DefaultToUpperTransformer implements FSTextTransformer {
		private final Locale lc;
		
		public DefaultToUpperTransformer(Locale lc) {
			this.lc = lc;
		}
		
		@Override
		public String transform(String in) {
			return in.toUpperCase(lc);
		}
	}
	
	public static class DefaultToLowerTransformer implements FSTextTransformer {
		private final Locale lc;
		
		public DefaultToLowerTransformer(Locale lc) {
			this.lc = lc;
		}
		
		@Override
		public String transform(String in) {
			return in.toLowerCase(lc);
		}
	}

	/**
	 * A best effort implementation of title casing. Use the implementation in the rtl-support
	 * module for better results.
	 */
	public static class DefaultToTitleTransformer implements FSTextTransformer {
		public DefaultToTitleTransformer() { }
		
		@Override
		public String transform(String in) {
			StringBuilder out = new StringBuilder(in.length());
			boolean makeTitle = true;
			
			for (int i = 0; i < in.length(); ) {
				int cp = in.codePointAt(i);
				
				if (Character.isLetter(cp) && makeTitle) {
					out.appendCodePoint(Character.toTitleCase(cp));
					makeTitle = false;
				} else if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
					out.appendCodePoint(cp);
					makeTitle = true;
				} else {
					out.appendCodePoint(cp);
				}
				
				i += Character.charCount(cp);
			}
			
			return out.toString();
		}
	}

    public static String transformText( String text, CalculatedStyle style ) {
        IdentValue transform = style.getIdent( CSSName.TEXT_TRANSFORM );
        IdentValue fontVariant = style.getIdent( CSSName.FONT_VARIANT );
        
        SharedContext ctx = ThreadCtx.get().sharedContext();
        
        if ( transform == IdentValue.LOWERCASE ) {
            text = ctx.getUnicodeToLowerTransformer().transform(text);
        }
        if ( transform == IdentValue.UPPERCASE || 
        	 fontVariant == IdentValue.SMALL_CAPS ) {
            text = ctx.getUnicodeToUpperTransformer().transform(text);
        }
        if ( transform == IdentValue.CAPITALIZE ) {
            text = ctx.getUnicodeToTitleTransformer().transform(text);
        }

        return text;
    }

    public static String transformFirstLetterText( String text, CalculatedStyle style ) {
    	return transformText(text, style);
    }

    /**
     * According to the CSS spec the first letter includes certain punctuation immediately
     * preceding or following the actual first letter.
     * @param currentChar
     * @return
     */
    public static boolean isFirstLetterSeparatorChar( int currentChar ) {
        switch (Character.getType(currentChar)) {
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.SPACE_SEPARATOR:
                return true;
            default:
                return false;
        }
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.11  2007/02/07 16:33:33  peterbrant
 * Initial commit of rewritten table support and associated refactorings
 *
 * Revision 1.10  2005/01/29 20:18:41  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.9  2005/01/24 22:46:43  pdoubleya
 * Added support for ident-checks using IdentValue instead of string comparisons.
 *
 * Revision 1.8  2004/12/12 03:32:59  tobega
 * Renamed x and u to avoid confusing IDE. But that got cvs in a twist. See if this does it
 *
 * Revision 1.7  2004/12/06 02:55:43  tobega
 * More cleaning of use of Node, more preparation for Content-based inline generation.
 *
 * Revision 1.6  2004/12/05 00:48:58  tobega
 * Cleaned up so that now all property-lookups use the CalculatedStyle. Also added support for relative values of top, left, width, etc.
 *
 * Revision 1.5  2004/11/22 21:34:03  joshy
 * created new whitespace handler.
 * new whitespace routines only work if you set a special property. it's
 * off by default.
 *
 * turned off fractional font metrics
 *
 * fixed some bugs in Uu and Xx
 *
 * - j
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.4  2004/11/08 21:18:21  joshy
 * preliminary small-caps implementation
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/23 13:46:48  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 *
 */

