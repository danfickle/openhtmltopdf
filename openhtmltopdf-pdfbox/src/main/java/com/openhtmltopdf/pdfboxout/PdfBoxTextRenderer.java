/*
 * {{{ header & license
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
package com.openhtmltopdf.pdfboxout;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.font.PDFont;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.extend.FSGlyphVector;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.JustificationInfo;
import com.openhtmltopdf.util.Configuration;

public class PdfBoxTextRenderer implements TextRenderer {
    private static float TEXT_MEASURING_DELTA = 0.01f;
    
    private BidiReorderer _reorderer;
    
    public void setup(FontContext context, BidiReorderer reorderer) {
        this._reorderer = reorderer;
    }

    public void drawString(OutputDevice outputDevice, String string, float x, float y) {
        ((PdfBoxOutputDevice)outputDevice).drawString(string, x, y, null);
    }
    
    public void drawString(
            OutputDevice outputDevice, String string, float x, float y, JustificationInfo info) {
        ((PdfBoxOutputDevice)outputDevice).drawString(string, x, y, info);
    }

    public FSFontMetrics getFSFontMetrics(FontContext context, FSFont font, String string) {
        List<FontDescription> descrs = ((PdfBoxFSFont) font).getFontDescription();
        float size = font.getSize2D();
        PdfBoxFSFontMetrics result = new PdfBoxFSFontMetrics();
        
        try {
            float largestAscent = Float.MIN_VALUE;
            float largestDescent = Float.MIN_VALUE;
            float largestStrikethroughOffset = Float.MIN_VALUE;
            float largestStrikethroughThickness = Float.MIN_VALUE;
            float largestUnderlinePosition = Float.MIN_VALUE;
            float largestUnderlineThickness = Float.MIN_VALUE;
            
            for (FontDescription des : descrs) {
                float loopAscent = des.getFont().getBoundingBox().getUpperRightY();
                float loopDescent = -des.getFont().getBoundingBox().getLowerLeftY();
                float loopStrikethroughOffset = -des.getYStrikeoutPosition();
                float loopStrikethroughThickness = des.getYStrikeoutSize();
                float loopUnderlinePosition = -des.getUnderlinePosition();
                float loopUnderlineThickness = des.getUnderlineThickness();
                
                if (loopAscent > largestAscent) {
                    largestAscent = loopAscent;
                }
                
                if (loopDescent > largestDescent) {
                    largestDescent = loopDescent; 
                }
                
                if (loopStrikethroughOffset > largestStrikethroughOffset) {
                    largestStrikethroughOffset = loopStrikethroughOffset;
                }
                
                if (loopStrikethroughThickness > largestStrikethroughThickness) {
                    largestStrikethroughThickness = loopStrikethroughThickness;
                }
                
                if (loopUnderlinePosition > largestUnderlinePosition) {
                    largestUnderlinePosition = loopUnderlinePosition;
                }
                
                if (loopUnderlineThickness > largestUnderlineThickness) {
                    largestUnderlineThickness = loopUnderlineThickness;
                }
            }
            
            result.setAscent(largestAscent / 1000f * size);
            result.setDescent(largestDescent / 1000f * size);
            result.setStrikethroughOffset(largestStrikethroughOffset / 1000f * size);
            
            if (largestStrikethroughThickness > 0) {
                result.setStrikethroughThickness(largestStrikethroughThickness / 1000f * size);
            } else {
                result.setStrikethroughThickness(size / 12.0f);
            }
            
            result.setUnderlineOffset(largestUnderlinePosition / 1000f * size);
            result.setUnderlineThickness(largestUnderlineThickness / 1000f * size);
        } catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("getFSFontMetrics", e);
        }

        return result;
    }

    static class ReplacementChar {
        char replacement;
        FontDescription fontDescription;
        float width;
    }
    
    
    static ReplacementChar getReplacementChar(FSFont font) {
        char replacement = Configuration.valueAsChar("xr.renderer.missing-character-replacement", ' ');
        String replaceStr = String.valueOf(replacement);
        List<FontDescription> descriptions = ((PdfBoxFSFont) font).getFontDescription();
        
        for (FontDescription des : descriptions) {
            try {
                float width = des.getFont().getStringWidth(replaceStr);

                // Got here without throwing, so the char exists in font.
                ReplacementChar replace = new ReplacementChar();
                replace.replacement = replacement;
                replace.fontDescription = des;
                replace.width = width;
                return replace;
            } catch (Exception e)
            {
                // Could not use replacement character in this font.
            }
        }

        // Still haven't found a font supporting our replacement character, try space character.
        replaceStr = " ";
        for (FontDescription des : descriptions) {
            try {
                float width = des.getFont().getStringWidth(replaceStr);

                // Got here without throwing, so the char exists in font.
                ReplacementChar replace = new ReplacementChar();
                replace.replacement = ' ';
                replace.fontDescription = des;
                replace.width = width;
                return replace;
            } catch (Exception e)
            {
                // Could not use replacement character in this font.
            }
        }
    
        // Really?, no font support for either replacement character or space!
        ReplacementChar replace = new ReplacementChar();
        replace.replacement = ' ';
        replace.fontDescription = descriptions.get(0);
        replace.width = 0;
        return replace;
    }
    
    
    private float getStringWidthSlow(FSFont bf, String str) {
        ReplacementChar replace = getReplacementChar(bf);
        List<FontDescription> fonts = ((PdfBoxFSFont) bf).getFontDescription();
        float strWidthResult = 0;

        for (int i = 0; i < str.length(); ) {
            int unicode = str.codePointAt(i);
            i += Character.charCount(unicode);
            String ch = String.valueOf(Character.toChars(unicode));
            boolean gotWidth = false;
            
            FONT_LOOP:
            for (FontDescription des : fonts) {
                try {
                    strWidthResult += des.getFont().getStringWidth(ch);
                    gotWidth = true;
                    break;
                }
                catch (Exception e1) {
                    if (_reorderer.isLiveImplementation()) {
                        // Character is not in font! Next, we try deshaping.
                        String deshaped = _reorderer.deshapeText(ch);
                        try {
                            strWidthResult += des.getFont().getStringWidth(deshaped);
                            gotWidth = true;
                            break FONT_LOOP;
                        }
                        catch (Exception e2) {
                            // Keep trying with next font.
                        }
                    }
                }
            }
            
            if (!gotWidth) {
                // We still don't have the character after all that. So use replacement character.
                strWidthResult += replace.width;
            }
        }
        
        return strWidthResult;
    }
    
    public int getWidth(FontContext context, FSFont font, String string) {
        float result = 0f;

        try {
            // First try using the first given font in the list.
            result = ((PdfBoxFSFont) font).getFontDescription().get(0).getFont().getStringWidth(string) / 1000f * font.getSize2D();
        } catch (IllegalArgumentException e2) {
            // PDFont::getStringWidth throws an IllegalArgumentException if the character doesn't exist in the font.
            // So we do it one character by character instead.
            result = getStringWidthSlow(font, string) / 1000f * font.getSize2D();
        }
        catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("getWidth", e);
        }

        if (result - Math.floor(result) < TEXT_MEASURING_DELTA) {
            return (int)result;
        } else {
            return (int)Math.ceil(result); 
        }
    }

    public void setFontScale(float scale) {
        // TODO: Implement.
        throw new UnsupportedOperationException();
    }

    public float getFontScale() {
        return 1.0f;
    }

    public void setSmoothingThreshold(float fontsize) {
    }

    public int getSmoothingLevel() {
        return 0;
    }

    public void setSmoothingLevel(int level) {
    }

    public Rectangle getGlyphBounds(OutputDevice outputDevice, FSFont font, FSGlyphVector fsGlyphVector, int index, float x, float y) {
        throw new UnsupportedOperationException();
    }

    public float[] getGlyphPositions(OutputDevice outputDevice, FSFont font, FSGlyphVector fsGlyphVector) {
        throw new UnsupportedOperationException();
    }

    public FSGlyphVector getGlyphVector(OutputDevice outputDevice, FSFont font, String string) {
        throw new UnsupportedOperationException();
    }

    public void drawGlyphVector(OutputDevice outputDevice, FSGlyphVector vector, float x, float y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setup(FontContext context) {
        
    }
}
