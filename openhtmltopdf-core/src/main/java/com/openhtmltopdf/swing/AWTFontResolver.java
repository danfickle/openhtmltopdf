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
package com.openhtmltopdf.swing;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FontResolver;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.FSFont;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * REsolves an AWT font instance from a list of CSS font families and characteristics.
 *
 * @author Joshua Marinacci
 */
public class AWTFontResolver implements FontResolver {

	/**
     * Map of concrete instances of fonts including size, weight, etc.
     */
    private final HashMap<String, Font> instanceHash = new HashMap<String, Font>();

    /**
     * Map of base fonts, from which we can derive a concrete instance at the correct size, weight, etc.
     * Note: The value is initially null until we need the given base font.
     */
    private final HashMap<String, Font> availableFontsHash = new HashMap<String, Font>();

    /**
     * Constructor
     */
    public AWTFontResolver() {
        init();
    }
    
    private void init() {
        GraphicsEnvironment gfx = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = gfx.getAvailableFontFamilyNames();

        // preload the font map with the font names as keys
        // don't add the actual font objects because that would be a waste of memory
        // we will only add them once we need to use them
        // put empty strings in instead
        for (String fontName : availableFonts) {
            availableFontsHash.put(fontName, null);
        }

        // preload sans, serif, and monospace into the available font hash
        availableFontsHash.put("Serif", new Font("Serif", Font.PLAIN, 1));
        availableFontsHash.put("SansSerif", new Font("SansSerif", Font.PLAIN, 1));
        availableFontsHash.put("Monospaced", new Font("Monospaced", Font.PLAIN, 1));
    }
    
    public void flushCache() {
    	instanceHash.clear();
    	availableFontsHash.clear();
        init();
    }

    /**
     * Resolves a list of font families.
     */
    public FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
    	List<Font> fonts = new ArrayList<Font>(3);
    	
        // for each font family
        if (families != null) {
            for (int i = 0; i < families.length; i++) {
                Font font = resolveFont(ctx, families[i], size, weight, style, variant);
                if (font != null) {
                    fonts.add(font);
                }
            }
        }

        // We add the default sans as last fallback font.
        String family = "SansSerif";
        if (style == IdentValue.ITALIC) {
            family = "Serif";
        }

        Font fnt = createFont(ctx, availableFontsHash.get(family), size, weight, style, variant);
        instanceHash.put(getFontInstanceHashName(ctx, family, size, weight, style, variant), fnt);
        fonts.add(fnt);

        return new AWTFSFont(fonts, size);
    }

    /**
     * Sets the fontMapping attribute of the FontResolver object
     *
     * @param name The new fontMapping value
     * @param font The new fontMapping value
     */
    public void setFontMapping(String name, Font font) {
        availableFontsHash.put(name, font.deriveFont(1f));
    }

    /**
     * Creates a concrete instance of a font at specified size, weight, style and variant.
     */
    protected static Font createFont(SharedContext ctx, Font rootFont, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        //Uu.p("creating font: " + root_font + " size = " + size +
        //    " weight = " + weight + " style = " + style + " variant = " + variant);
        int fontConst = Font.PLAIN;
        if (weight != null &&
                (weight == IdentValue.BOLD ||
                weight == IdentValue.FONT_WEIGHT_700 ||
                weight == IdentValue.FONT_WEIGHT_800 ||
                weight == IdentValue.FONT_WEIGHT_900)) {

            fontConst = fontConst | Font.BOLD;
        }
        if (style != null && (style == IdentValue.ITALIC || style == IdentValue.OBLIQUE)) {
            fontConst = fontConst | Font.ITALIC;
        }

        // scale vs font scale value too
        size *= ctx.getTextRenderer().getFontScale();

        Font fnt = rootFont.deriveFont(fontConst, size);
        if (variant != null) {
            if (variant == IdentValue.SMALL_CAPS) {
                fnt = fnt.deriveFont((float) (((float) fnt.getSize()) * 0.6));
            }
        }

        return fnt;
    }

    /**
     * Resolves a single font name.
     * TODO: Make case insensitive.
     */
    protected Font resolveFont(SharedContext ctx, String font, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        // strip off the double quotes if they are present
        if (font.startsWith("\"")) {
            font = font.substring(1);
        }
        if (font.endsWith("\"")) {
            font = font.substring(0, font.length() - 1);
        }

        // normalize the font name
        if (font.equalsIgnoreCase("serif")) {
            font = "Serif";
        }
        if (font.equalsIgnoreCase("sans-serif")) {
            font = "SansSerif";
        }
        if (font.equalsIgnoreCase("monospace")) {
            font = "Monospaced";
        }

        if (font.equals("Serif") && style == IdentValue.OBLIQUE) {
        	font = "SansSerif";
        }

        if (font.equals("SansSerif") && style == IdentValue.ITALIC) {
        	font = "Serif";
        }

        // assemble a font instance hash name
        String fontInstanceName = getFontInstanceHashName(ctx, font, size, weight, style, variant);

        // check if the font instance exists in the hash table
        if (instanceHash.containsKey(fontInstanceName)) {
            // if so then return it
            return instanceHash.get(fontInstanceName);
        }

        // if not then
        //  does the font exist
        if (availableFontsHash.containsKey(font)) {
            //Uu.p("found an available font for: " + font);
            Font possiblyNullFont = availableFontsHash.get(font);
            // have we actually allocated the root font object yet?
            Font rootFont = null;
            if (possiblyNullFont != null) {
                rootFont = possiblyNullFont;
            } else {
                rootFont = new Font(font, Font.PLAIN, 1);
                availableFontsHash.put(font, rootFont);
            }

            // now that we have a root font, we need to create the correct version of it
            Font fnt = createFont(ctx, rootFont, size, weight, style, variant);

            // add the font to the hash so we don't have to do this again
            instanceHash.put(fontInstanceName, fnt);
            return fnt;
        }

        // we didn't find any possible matching font, so just return null
        return null;
    }

    /**
     * Gets the hash key for a concrete instance of a font.
     * This incorporates size, weight, etc.
     */
    protected static String getFontInstanceHashName(SharedContext ctx, String name, float size, IdentValue weight, IdentValue style, IdentValue variant) {
        return name + "-" + (size * ctx.getTextRenderer().getFontScale()) + "-" + weight + "-" + style + "-" + variant;
    }

    public FSFont resolveFont(SharedContext renderingContext, FontSpecification spec) {
        return resolveFont(renderingContext, spec.families, spec.size, spec.fontWeight, spec.fontStyle, spec.variant);
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.3  2009/05/09 14:44:18  pdoubleya
 * FindBugs: redundant call to someObject.toString() and new String("")
 *
 * Revision 1.2  2009/05/08 12:22:26  pdoubleya
 * Merge Vianney's SWT branch to trunk. Passes regress.verify and browser still works :).
 *
 * Revision 1.1.4.2  2008/05/28 09:21:15  vianney
 * updated SWT port to latest HEAD
 *
 * Revision 1.3  2008/01/22 21:25:40  pdoubleya
 * Fix: fonts not being keyed properly in font cache when a scaling factor was applied to the text renderer; scaled font size now used as part of the key.
 *
 * Revision 1.2  2007/02/07 16:33:29  peterbrant
 * Initial commit of rewritten table support and associated refactorings
 *
 * Revision 1.1  2006/02/01 01:30:15  peterbrant
 * Initial commit of PDF work
 *
 * Revision 1.2  2005/10/27 00:08:51  tobega
 * Sorted out Context into RenderingContext and LayoutContext
 *
 * Revision 1.1  2005/06/22 23:48:40  tobega
 * Refactored the css package to allow a clean separation from the core.
 *
 * Revision 1.17  2005/06/16 07:24:48  tobega
 * Fixed background image bug.
 * Caching images in browser.
 * Enhanced LinkListener.
 * Some house-cleaning, playing with Idea's code inspection utility.
 *
 * Revision 1.16  2005/06/03 22:04:10  tobega
 * Now handles oblique fonts somewhat and does a better job of italic
 *
 * Revision 1.15  2005/05/29 16:38:59  tobega
 * Handling of ex values should now be working well. Handling of em values improved. Is it correct?
 * Also started defining dividing responsibilities between Context and RenderingContext.
 *
 * Revision 1.14  2005/03/24 23:19:11  pdoubleya
 * Cleaned up DPI calculations for font size (Kevin).
 *
 * Revision 1.13  2005/02/02 11:32:29  pdoubleya
 * Fixed error in font-weight; now checks for 700, 800, 900 or BOLD.
 *
 * Revision 1.12  2005/01/29 20:21:10  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.11  2005/01/24 22:46:45  pdoubleya
 * Added support for ident-checks using IdentValue instead of string comparisons.
 *
 * Revision 1.10  2005/01/05 01:10:13  tobega
 * Went wild with code analysis tool. removed unused stuff. Lucky we have CVS...
 *
 * Revision 1.9  2004/12/29 10:39:26  tobega
 * Separated current state Context into LayoutContext and the rest into SharedContext.
 *
 * Revision 1.8  2004/12/12 03:32:55  tobega
 * Renamed x and u to avoid confusing IDE. But that got cvs in a twist. See if this does it
 *
 * Revision 1.7  2004/12/12 02:56:59  tobega
 * Making progress
 *
 * Revision 1.6  2004/11/18 02:58:06  joshy
 * collapsed the font resolver and font resolver test into one class, and removed
 * the other
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.5  2004/11/12 02:23:56  joshy
 * added new APIs for rendering context, xhtmlpanel, and graphics2drenderer.
 * initial support for font mapping additions
 *
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.4  2004/11/08 21:18:20  joshy
 * preliminary small-caps implementation
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.3  2004/10/23 13:03:45  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc)
 * Added CVS log comments at bottom.
 *
 *
 */

