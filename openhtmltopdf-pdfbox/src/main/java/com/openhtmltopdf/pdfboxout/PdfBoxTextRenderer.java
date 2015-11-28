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

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.xhtmlrenderer.extend.FSGlyphVector;
import org.xhtmlrenderer.extend.FontContext;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.JustificationInfo;
import org.xhtmlrenderer.util.Configuration;

import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;

public class PdfBoxTextRenderer implements TextRenderer {
    private static float TEXT_MEASURING_DELTA = 0.01f;
    
    public void setup(FontContext context) {
    }

    public void drawString(OutputDevice outputDevice, String string, float x, float y) {
        ((PdfBoxOutputDevice)outputDevice).drawString(string, x, y, null);
    }
    
    public void drawString(
            OutputDevice outputDevice, String string, float x, float y, JustificationInfo info) {
        ((PdfBoxOutputDevice)outputDevice).drawString(string, x, y, info);
    }

    public FSFontMetrics getFSFontMetrics(FontContext context, FSFont font, String string) {
        FontDescription descr = ((PdfBoxFSFont)font).getFontDescription();
        PDFont bf = descr.getFont();
        float size = font.getSize2D();
        PdfBoxFSFontMetrics result = new PdfBoxFSFontMetrics();
        
        try {
            result.setAscent(bf.getBoundingBox().getUpperRightY() / 1000f * size);
            result.setDescent(bf.getBoundingBox().getLowerLeftY() / 1000f * size);
        } catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("getFSFontMetrics", e);
        }
                
        result.setStrikethroughOffset(-descr.getYStrikeoutPosition() / 1000f * size);
        if (descr.getYStrikeoutSize() != 0) {
            result.setStrikethroughThickness(descr.getYStrikeoutSize() / 1000f * size);
        } else {
            result.setStrikethroughThickness(size / 12.0f);
        }
        
        result.setUnderlineOffset(-descr.getUnderlinePosition() / 1000f * size);
        result.setUnderlineThickness(descr.getUnderlineThickness() / 1000f * size);
        
        return result;
    }

    private float getStringWidthSlow(PDFont bf, String str) {
        
        float res = 0;
        float space = 0;
        try {
            char replacement = Configuration.valueAsChar("xr.renderer.missing-character-replacement", ' ');
            space = bf.getStringWidth(String.valueOf(replacement));
        } catch (IOException e1) {
            throw new PdfContentStreamAdapter.PdfException("getStringWidthSlow", e1);
        }
        
        for (int i = 0; i < str.length(); ) {
            int unicode = str.codePointAt(i);
            i += Character.charCount(unicode);
            String ch = String.valueOf(Character.toChars(unicode));
            
            try {
                res += bf.getStringWidth(ch);
            }
            catch (IllegalArgumentException e2) {
                // Font does not support this character. Bad luck. Use replacement character width.
                // TODO: Log missing characters.
                res += space;
            } catch (IOException e) {
                throw new PdfContentStreamAdapter.PdfException("getStringWidthSlow", e);
            }
        }
        
        return res;
    }
    
    public int getWidth(FontContext context, FSFont font, String string) {
        PDFont bf = ((PdfBoxFSFont)font).getFontDescription().getFont();

        float result = 0f;
        try {
            result = bf.getStringWidth(string) / 1000f * font.getSize2D();
        } catch (IllegalArgumentException e2) {
            // PDFont::getStringWidth throws an IllegalArgumentException if the character doesn't exist in the font.
            // So we do it one character by character instead.
            result = getStringWidthSlow(bf, string) / 1000f * font.getSize2D();
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
}
