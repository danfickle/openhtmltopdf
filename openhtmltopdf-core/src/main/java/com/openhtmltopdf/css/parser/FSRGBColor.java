/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
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
package com.openhtmltopdf.css.parser;

public class FSRGBColor implements FSColor {
    public static final FSRGBColor TRANSPARENT = new FSRGBColor(0, 0, 0);
    public static final FSRGBColor RED = new FSRGBColor(255, 0, 0);
    public static final FSRGBColor GREEN = new FSRGBColor(0, 255, 0);
    public static final FSRGBColor BLUE = new FSRGBColor(0, 0, 255);
    public static final FSRGBColor BLACK = new FSRGBColor(0, 0, 0);

    private final int _red;
    private final int _green;
    private final int _blue;

    public FSRGBColor(int red, int green, int blue) {
        if (red < 0 || red > 255) {
            throw new IllegalArgumentException();
        }
        if (green < 0 || green > 255) {
            throw new IllegalArgumentException();
        }
        if (blue < 0 || blue > 255) {
            throw new IllegalArgumentException();
        }
        _red = red;
        _green = green;
        _blue = blue;
    }

    public FSRGBColor(int color) {
        this(((color & 0xff0000) >> 16),((color & 0x00ff00) >> 8), color & 0xff);
    }

    public int getBlue() {
        return _blue;
    }

    public int getGreen() {
        return _green;
    }

    public int getRed() {
        return _red;
    }
    
    @Override
    public String toString() {
        return '#' + toString(_red) + toString(_green) + toString(_blue);
    }
    
    private String toString(int color) {
        String result = Integer.toHexString(color);
        if (result.length() == 1) {
            return "0" + result;
        } else {
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSRGBColor)) return false;

        FSRGBColor that = (FSRGBColor) o;

        if (_blue != that._blue) return false;
        if (_green != that._green) return false;
        if (_red != that._red) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _red;
        result = 31 * result + _green;
        result = 31 * result + _blue;
        return result;
    }

    @Override
    public FSColor lightenColor() {
        float[] hsb = RGBtoHSB(getRed(), getGreen(), getBlue(), null);
        float hBase = hsb[0];
        float sBase = hsb[1];
        float bBase = hsb[2];
        
        float hLighter = hBase;
        float sLighter = 0.35f*bBase*sBase;
        float bLighter = 0.6999f + 0.3f*bBase;
        
        int[] rgb = HSBtoRGB(hLighter, sLighter, bLighter);
        return new FSRGBColor(rgb[0], rgb[1], rgb[2]);
    }
    
    @Override
    public FSColor darkenColor() {
        float[] hsb = RGBtoHSB(getRed(), getGreen(), getBlue(), null);
        float hBase = hsb[0];
        float sBase = hsb[1];
        float bBase = hsb[2];
        
        float hDarker = hBase;
        float sDarker = sBase;
        float bDarker = 0.56f*bBase;
        
        int[] rgb = HSBtoRGB(hDarker, sDarker, bDarker);
        return new FSRGBColor(rgb[0], rgb[1], rgb[2]);
    }
    
    // Taken from java.awt.Color to avoid dependency on it
    private static float[] RGBtoHSB(int red, int green, int blue, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (red > green) ? red : green;
        if (blue > cmax)
            cmax = blue;
        int cmin = (red < green) ? red : green;
        if (blue < cmin)
            cmin = blue;

        brightness = (cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - red)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - green)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - blue)) / ((float) (cmax - cmin));
            if (red == cmax)
                hue = bluec - greenc;
            else if (green == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }
    
    // Taken from java.awt.Color to avoid dependency on it
    private static int[] HSBtoRGB(float hue, float saturation, float brightness) {
        int red = 0, green = 0, blue = 0;
        if (saturation == 0) {
            red = green = blue = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    red = (int) (brightness * 255.0f + 0.5f);
                    green = (int) (t * 255.0f + 0.5f);
                    blue = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    red = (int) (q * 255.0f + 0.5f);
                    green = (int) (brightness * 255.0f + 0.5f);
                    blue = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    red = (int) (p * 255.0f + 0.5f);
                    green = (int) (brightness * 255.0f + 0.5f);
                    blue = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    red = (int) (p * 255.0f + 0.5f);
                    green = (int) (q * 255.0f + 0.5f);
                    blue = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    red = (int) (t * 255.0f + 0.5f);
                    green = (int) (p * 255.0f + 0.5f);
                    blue = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    red = (int) (brightness * 255.0f + 0.5f);
                    green = (int) (p * 255.0f + 0.5f);
                    blue = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return new int[] { red, green, blue };
    }
}
