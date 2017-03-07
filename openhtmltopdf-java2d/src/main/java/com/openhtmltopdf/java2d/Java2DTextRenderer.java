/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
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
package com.openhtmltopdf.java2d;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.extend.FSGlyphVector;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.JustificationInfo;
import com.openhtmltopdf.render.LineMetricsAdapter;
import com.openhtmltopdf.util.Configuration;
import com.openhtmltopdf.swing.AWTFSFont;
import com.openhtmltopdf.swing.AWTFSGlyphVector;


/**
 * Renders to a Graphics2D instance.
 *
 * @author   Joshua Marinacci
 * @author   Torbjoern Gannholm
 */
public class Java2DTextRenderer implements TextRenderer {
    protected float scale;
    protected float threshold;
    protected Object antiAliasRenderingHint;
    protected Object fractionalFontMetricsHint;

    public Java2DTextRenderer() {
        scale = Configuration.valueAsFloat("xr.text.scale", 1.0f);
        threshold = Configuration.valueAsFloat("xr.text.aa-fontsize-threshhold", 25);

        Object dummy = new Object();

        Object aaHint = Configuration.valueFromClassConstant("xr.text.aa-rendering-hint", dummy);        
        if (aaHint == dummy) {
            try {
                Map map;
                // we should be able to look up the "recommended" AA settings (that correspond to the user's
                // desktop preferences and machine capabilities
                // see: http://java.sun.com/javase/6/docs/api/java/awt/doc-files/DesktopProperties.html
                Toolkit tk = Toolkit.getDefaultToolkit();
                map = (Map) (tk.getDesktopProperty("awt.font.desktophints"));
                antiAliasRenderingHint = map.get(RenderingHints.KEY_TEXT_ANTIALIASING);
            } catch (Exception e) {
                // conceivably could get an exception in a webstart environment? not sure
                antiAliasRenderingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
            }
        } else {
            antiAliasRenderingHint = aaHint;
        }
        if("true".equals(Configuration.valueFor("xr.text.fractional-font-metrics", "false"))) {
            fractionalFontMetricsHint = RenderingHints.VALUE_FRACTIONALMETRICS_ON;
        } else {
            fractionalFontMetricsHint = RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawString(OutputDevice outputDevice, String string, float x, float y ) {
    	
    	if (string.isEmpty())
    		return;
    	
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        if ( graphics.getFont().getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        List<Font> fonts = ((Java2DOutputDevice) outputDevice).getFont().getAWTFonts();
        List<FontRun> runs = divideIntoFontRuns(fonts, string);

        AttributedString attString = new AttributedString(string);
        int offset = 0;
        
        for (FontRun run : runs) {
        	attString.addAttribute(TextAttribute.FONT, run.fnt, offset, offset + run.sb.length());
        	offset += run.sb.length();
        }

        graphics.drawString(attString.getIterator(), (int) x, (int) y);
        
        if ( graphics.getFont().getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
    }
    
    /**
     * Draws a justified string. TODO: Font fallback.
     */
    @Override
    public void drawString(
            OutputDevice outputDevice, String string, float x, float y, JustificationInfo info) {
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        if ( graphics.getFont().getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        GlyphVector vector = graphics.getFont().createGlyphVector(
                graphics.getFontRenderContext(), string);
        
        adjustGlyphPositions(string, info, vector);
        
        graphics.drawGlyphVector(vector, x, y);
        
        if ( graphics.getFont().getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
    }

    private void adjustGlyphPositions(
            String string, JustificationInfo info, GlyphVector vector) {
        float adjust = 0.0f;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (i != 0) {
                Point2D point = vector.getGlyphPosition(i);
                vector.setGlyphPosition(
                        i, new Point2D.Double(point.getX() + adjust, point.getY()));
            }
            if (c == ' ' || c == '\u00a0' || c == '\u3000') {
                adjust += info.getSpaceAdjust();
            } else {
                adjust += info.getNonSpaceAdjust();
            }
        }
    }
    
    @Override
    public void drawGlyphVector(OutputDevice outputDevice, FSGlyphVector fsGlyphVector, float x, float y ) {
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        
        if ( graphics.getFont().getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);

        GlyphVector vector = ((AWTFSGlyphVector)fsGlyphVector).getGlyphVector();
        graphics.drawGlyphVector(vector, (int)x, (int)y );
        if ( graphics.getFont().getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
    }

    /** {@inheritDoc} 
     * @param bidiReorderer */
    public void setup(FontContext fontContext, BidiReorderer bidiReorderer) {
        //Uu.p("setup graphics called");
//        ((Java2DFontContext)fontContext).getGraphics().setRenderingHint( 
//                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF );
    }

    @Override
    public void setFontScale( float scale ) {
        this.scale = scale;
    }

    @Override
    public void setSmoothingThreshold( float fontsize ) {
        threshold = fontsize;
    }

    @Override
    public void setSmoothingLevel( int level ) { /* no-op */ }

    @Override
    public FSFontMetrics getFSFontMetrics(FontContext fc, FSFont font, String string ) {
        Object fracHint = null;
        Graphics2D graphics = ((Java2DFontContext)fc).getGraphics();
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        LineMetricsAdapter adapter = new LineMetricsAdapter(((Java2DFont) font).getAWTFonts(), string, graphics.getFontRenderContext());
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        return adapter;
    }
    
    private boolean canDisplayWithFont(String str, Font fnt) {
    	return fnt.canDisplayUpTo(str) == -1;
    }
    
    private int getWidthFast(FontContext fc, Font awtFont, String string) {
        Object fracHint = null;
        Graphics2D graphics = ((Java2DFontContext) fc).getGraphics();
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        int width = 0;

        if(fractionalFontMetricsHint == RenderingHints.VALUE_FRACTIONALMETRICS_ON) {
            width = (int) Math.round(
                    graphics.getFontMetrics(awtFont).getStringBounds(string, graphics).getWidth());            
        } else {
            width = (int) Math.ceil(
                    graphics.getFontMetrics(awtFont).getStringBounds(string, graphics).getWidth());
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);

        return width;
    }
    
    private static class FontRun {
    	StringBuilder sb = new StringBuilder();
    	Font fnt;
    }
    
    private List<FontRun> divideIntoFontRuns(List<Font> fonts, String string) {
    	
    	List<FontRun> fontRuns = new ArrayList<FontRun>();
    	int length = string.length();
    	FontRun current = null;
    	
        // Divide text up into font runs.
    	for (int offset = 0; offset < length; ) {
           int codePoint = string.codePointAt(offset);
           
           for (Font fnt : fonts) {
        	   if (fnt.canDisplay(codePoint)) {
        		   if (current == null ||
        			   current.fnt != fnt)
        		   {
        			   if (current != null) {
        				   fontRuns.add(current);
        			   }
        			   
        			   current = new FontRun();
        			   current.fnt = fnt;
        		   }
        	   
        		   current.sb.append(Character.toChars(codePoint));
        		   break;
        	   }
           }
           
           offset += Character.charCount(codePoint);
        }

    	if (current != null &&
        	current.sb.length() > 0)
        {
        	fontRuns.add(current);
        }
    	
    	return fontRuns;
    }
    
    
    /**
     * This method divides the string up into font runs, then measures each font run, adding
     * it to the total. We do this, rather than get the width of each character,
     * in case kerning is enabled and it also may be faster.
     */
    private int getWidthSlow(FontContext fc, List<Font> fonts, String string) {
    	List<FontRun> runs = divideIntoFontRuns(fonts, string);

    	// Now, we have our font runs, get the width of each.
    	int width = 0;
    	for (FontRun run : runs) {
    		width += getWidthFast(fc, run.fnt, run.sb.toString());
    	}
    	
    	return width;
    }
    
    
    @Override
    public int getWidth(FontContext fc, FSFont font, String string) {
    	List<Font> fonts = ((Java2DFont) font).getAWTFonts();
    	
    	if (canDisplayWithFont(string, fonts.get(0))) {
    		return getWidthFast(fc, fonts.get(0), string);
    	}
    	
    	return getWidthSlow(fc, fonts, string);
    }
    

    @Override
    public float getFontScale() {
        return this.scale;
    }

    @Override
    public int getSmoothingLevel() {
        return 0;
    }

    /**
     * If anti-alias text is enabled, the value from RenderingHints to use for AA smoothing in Java2D. Defaults to
     * {@link java.awt.RenderingHints#VALUE_TEXT_ANTIALIAS_ON}.
     *
     * @return Current AA rendering hint
     */
    public Object getRenderingHints() {
        return antiAliasRenderingHint;
    }

    /**
     * If anti-alias text is enabled, the value from RenderingHints to use for AA smoothing in Java2D. Defaults to
     * {@link java.awt.RenderingHints#VALUE_TEXT_ANTIALIAS_ON}.
     *
     * @param renderingHints  rendering hint for AA smoothing in Java2D
     */
    public void setRenderingHints(Object renderingHints) {
        this.antiAliasRenderingHint = renderingHints;
    }

    /**
     * This method gets glyph positions for purposes of selecting text. WE are not too worried about selecting text
     * at this point so we just use the first font available.
     */
    public float[] getGlyphPositions(OutputDevice outputDevice, FSFont font, String text) {
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        Font awtFont = ((AWTFSFont)font).getAWTFonts().get(0);
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);

        
        GlyphVector vector = awtFont.createGlyphVector(
                graphics.getFontRenderContext(),
                text);
        float[] result = vector.getGlyphPositions(0, text.length() + 1, null);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return result;
    }
    
    /**
     * This method gets glyph bounds for purposes of selecting text. WE are not too worried about selecting text
     * at this point so we just use the first font available.
     */
    @Override
    public Rectangle getGlyphBounds(OutputDevice outputDevice, FSFont font, FSGlyphVector fsGlyphVector, int index, float x, float y) {
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        Font awtFont = ((AWTFSFont)font).getAWTFonts().get(0);
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        GlyphVector vector = ((AWTFSGlyphVector)fsGlyphVector).getGlyphVector();
        
        Rectangle result = vector.getGlyphPixelBounds(index, graphics.getFontRenderContext(), x, y);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return result;
    }

    /**
     * This method gets glyph positions for purposes of selecting text. WE are not too worried about selecting text
     * at this point so we just use the first font available.
     */
    @Override
    public float[] getGlyphPositions(OutputDevice outputDevice, FSFont font, FSGlyphVector fsGlyphVector) {
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        Font awtFont = ((AWTFSFont)font).getAWTFonts().get(0);
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        GlyphVector vector = ((AWTFSGlyphVector)fsGlyphVector).getGlyphVector();
        
        float[] result = vector.getGlyphPositions(0, vector.getNumGlyphs() + 1, null);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return result;
    }

    /**
     * This method gets a glyph vector for purposes of selecting text. WE are not too worried about selecting text
     * at this point so we just use the first font available.
     */
    @Override
    public FSGlyphVector getGlyphVector(OutputDevice outputDevice, FSFont font, String text) {
        Object aaHint = null;
        Object fracHint = null;
        Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        Font awtFont = ((AWTFSFont)font).getAWTFonts().get(0);
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        GlyphVector vector = awtFont.createGlyphVector(
                graphics.getFontRenderContext(),
                text);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return new AWTFSGlyphVector(vector);
    }

	@Override
	public void setup(FontContext context) {
		// TODO Auto-generated method stub
		
	}
}

