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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.render;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.List;


/**
 * A note on this class: What we really want is a FontMetrics2D object (i.e.
 * font metrics with float precision).  Unfortunately, it doesn't seem
 * the JDK provides this.  However, looking at the JDK code, it appears the
 * metrics contained in the LineMetrics are actually the metrics of the font, not
 * the metrics of the line (and empirically strings of "X" and "j" return the same 
 * value for getAscent()).  So... for now we use LineMetrics for font metrics.
 */
public class LineMetricsAdapter implements FSFontMetrics {
    private final List<LineMetrics> _lineMetrics;
    
    public LineMetricsAdapter(List<Font> fonts, String str, FontRenderContext ctx) {
        _lineMetrics = new ArrayList<LineMetrics>(fonts.size());
    	for (Font fnt : fonts) {
        	_lineMetrics.add(fnt.getLineMetrics(str, ctx));
        }
    }

    public float getAscent() {
    	float maxAscent = -Float.MAX_VALUE;
    	
    	for (LineMetrics met : _lineMetrics) {
    		maxAscent = Math.max(maxAscent, met.getAscent());
    	}
    	
        return maxAscent;
    }

    public float getDescent() {
    	float maxDescent = -Float.MAX_VALUE;
    	
    	for (LineMetrics met : _lineMetrics) {
    		maxDescent = Math.max(maxDescent, met.getDescent());
    	}

    	return maxDescent;
    }

    public float getStrikethroughOffset() {
    	float max = -Float.MAX_VALUE;
    	
    	for (LineMetrics met : _lineMetrics) {
    		max = Math.max(max, met.getStrikethroughOffset());
    	}
    	
    	return max;
    }

    public float getStrikethroughThickness() {
    	float max = -Float.MAX_VALUE;
    	
    	for (LineMetrics met : _lineMetrics) {
    		max = Math.max(max, met.getStrikethroughThickness());
    	}
    	
    	return max;
    }

    public float getUnderlineOffset() {
    	float max = -Float.MAX_VALUE;
    	
    	for (LineMetrics met : _lineMetrics) {
    		max = Math.max(max, met.getUnderlineOffset());
    	}
    	
    	return max;
    }

    public float getUnderlineThickness() {
    	float max = -Float.MAX_VALUE;
    	
    	for (LineMetrics met : _lineMetrics) {
    		max = Math.max(max, met.getUnderlineThickness());
    	}
    	
    	return max;
    }
}
