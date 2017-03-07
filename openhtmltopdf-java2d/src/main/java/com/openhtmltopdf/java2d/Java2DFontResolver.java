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
package com.openhtmltopdf.java2d;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.extend.FontResolver;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.FontFaceFontSupplier;
import com.openhtmltopdf.outputdevice.helper.FontFamily;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.outputdevice.helper.MinimalFontDescription;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.util.XRLog;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * REsolves an AWT font instance from a list of CSS font families and characteristics.
 *
 * @author Joshua Marinacci
 */
public class Java2DFontResolver implements FontResolver {
	
	private static class FontDescription implements MinimalFontDescription {
		private FSSupplier<InputStream> _supplier;
		private final int _weight;
		private final IdentValue _style;
		private Font _font;
		
		private FontDescription(FSSupplier<InputStream> supplier, int weight, IdentValue style) {
			this._supplier = supplier;
			this._weight = weight;
			this._style = style;
		}
		
		@Override
		public int getWeight() {
			return _weight;
		}

		@Override
		public IdentValue getStyle() {
			return _style;
		}
		
		private Font getBaseFont() {
			return _font;
		}
		
		private boolean realizeFont() {
            if (_font == null && _supplier != null) {
                InputStream is = _supplier.supply();
                _supplier = null; // We only try once.
                
                if (is == null) {
                    return false;
                }
                
                try {
                    _font = Font.createFont(Font.TRUETYPE_FONT, is);
                } catch (IOException e) {
                    XRLog.exception("Couldn't load font. Please check that it is a valid truetype font.");
                    return false;
                } catch (FontFormatException e) {
                	XRLog.exception("Couldn't load font. Please check that it is a valid truetype font.");
                    return false;
				} finally {
                    try {
                        is.close();
                    } catch (IOException e) { }
                }
            }
            
            return _font != null;
        }
	}

	/**
     * Map of concrete instances of fonts including size, weight, etc.
     */
    private final HashMap<String, Font> instanceHash = new HashMap<String, Font>();

    /**
     * Map of base fonts, from which we can derive a concrete instance at the correct size, weight, etc.
     * Note: The value is initially null until we need the given base font.
     */
    private final HashMap<String, Font> availableFontsHash = new HashMap<String, Font>();
    
    private final SharedContext _sharedContext;

	private final HashMap<String, FontFamily<FontDescription>> _fontFamilies = new HashMap<String, FontFamily<FontDescription>>();
    
    public Java2DFontResolver(SharedContext sharedCtx) {
        _sharedContext = sharedCtx;
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
    	_fontFamilies.clear();
        init();
    }
    
    public void importFontFaces(List<FontFaceRule> fontFaces) {
        for (FontFaceRule rule : fontFaces) {
            CalculatedStyle style = rule.getCalculatedStyle();

            FSDerivedValue src = style.valueByName(CSSName.SRC);
            if (src == IdentValue.NONE) {
                continue;
            }

            String fontFamily = null;
            IdentValue fontWeight = null;
            IdentValue fontStyle = null;

            if (rule.hasFontFamily()) {
                fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
            } else {
                XRLog.cssParse(Level.WARNING, "Must provide at least a font-family and src in @font-face rule");
                continue;
            }

            if (rule.hasFontWeight()) {
                fontWeight = style.getIdent(CSSName.FONT_WEIGHT);
            }

            if (rule.hasFontStyle()) {
                fontStyle = style.getIdent(CSSName.FONT_STYLE);
            }

            addFontFaceFont(fontFamily, fontWeight, fontStyle, src.asString());
        }
    }
    
    private void addFontFaceFont(
            String fontFamilyNameOverride, IdentValue fontWeightOverride, IdentValue fontStyleOverride,
            String uri) {
        
        FSSupplier<InputStream> fontSupplier = new FontFaceFontSupplier(_sharedContext, uri);
        FontFamily<FontDescription> fontFamily = getFontFamily(fontFamilyNameOverride);
        FontDescription descr = new FontDescription(
                 fontSupplier,
                 fontWeightOverride != null ? FontResolverHelper.convertWeightToInt(fontWeightOverride) : 400,
                 fontStyleOverride != null ? fontStyleOverride : IdentValue.NORMAL); 

        fontFamily.addFontDescription(descr);
    }

    private FontFamily<FontDescription> getFontFamily(String fontFamilyName) {
        FontFamily<FontDescription> fontFamily = _fontFamilies.get(fontFamilyName);
        if (fontFamily == null) {
            fontFamily = new FontFamily<FontDescription>();
            _fontFamilies.put(fontFamilyName, fontFamily);
        }
        return fontFamily;
    }

    private String normalizeFontFamily(String fontFamily) {
        String result = fontFamily;
        // strip off the "s if they are there
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }

        // normalize the font name
        if (result.equalsIgnoreCase("serif")) {
            result = "Serif";
        }
        else if (result.equalsIgnoreCase("sans-serif")) {
            result = "SansSerif";
        }
        else if (result.equalsIgnoreCase("monospace")) {
            result = "Monospaced";
        }

        return result;
    }

    private Font resolveFontFaceBaseFont(String normalizedFontFamily, float size, IdentValue weight, IdentValue style) {
        FontFamily<FontDescription> fontFamily = _fontFamilies.get(normalizedFontFamily);

        if (fontFamily != null) {
            FontDescription result = fontFamily.match(FontResolverHelper.convertWeightToInt(weight), style);

            if (result != null) {
               if (result.realizeFont()) {
                    return result.getBaseFont();
               }
            }
        }
        
        return null;
    }
    
    /**
     * Resolves a list of font families.
     * Search order for each family is:
     * 1. Concrete fonts with correct size that have already been used.
     * 2. Font face fonts.
     * 3. System fonts.
     */
    public FSFont resolveFont(SharedContext ctx, String[] families, float size, IdentValue weight, IdentValue style, IdentValue variant) {
    	List<Font> fonts = new ArrayList<Font>(3);

        if (families != null) {
        	for (int i = 0; i < families.length; i++) {
        		String normal = normalizeFontFamily(families[i]);
        		
        		String fontInstanceName = getFontInstanceHashName(ctx, normal, size, weight, style, variant);
        		
        		// check if the font instance exists in the hash table
                if (instanceHash.containsKey(fontInstanceName)) {
                    // if so then add it and continue to next family.
                    fonts.add(instanceHash.get(fontInstanceName));
                    continue;
                }
        		
                // Next we search the list of font-face rule fonts.
        		Font baseFont = resolveFontFaceBaseFont(normal, size, weight, style);

        		if (baseFont != null) {
        			// scale vs font scale value too
        	        size *= ctx.getTextRenderer().getFontScale();

        	        // We always use Font.PLAIN here as the provided font is already in the specifed weight and style.
        	        Font derivedFont = baseFont.deriveFont(Font.PLAIN, size);
        			
        	        // add the font to the hash so we don't have to do this again
                    instanceHash.put(fontInstanceName, derivedFont);

                    // add it to the list of concrete fonts to be returned and continue with the next family.
        			fonts.add(derivedFont);
        			continue;
        		}
        		
        		// Finally we search the system fonts.
        		if (availableFontsHash.containsKey(normal)) {
                    Font possiblyNullFont = availableFontsHash.get(normal);
                    // have we actually allocated the root font object yet?
                    Font rootFont = null;
                    if (possiblyNullFont != null) {
                        rootFont = possiblyNullFont;
                    } else {
                        rootFont = new Font(normal, Font.PLAIN, 1);
                        availableFontsHash.put(normal, rootFont);
                    }

                    // now that we have a root font, we need to create the correct version of it
                    Font fnt = createFont(ctx, rootFont, size, weight, style, variant);

                    // add the font to the hash so we don't have to do this again
                    instanceHash.put(fontInstanceName, fnt);
                    
                    fonts.add(fnt);
                    continue;
                }
        	}
        }

        // We add the default serif as last fallback font.
        Font fnt = createFont(ctx, availableFontsHash.get("Serif"), size, weight, style, variant);
        instanceHash.put(getFontInstanceHashName(ctx, "Serif", size, weight, style, variant), fnt);
        fonts.add(fnt);

        return new Java2DFont(fonts, size);
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
