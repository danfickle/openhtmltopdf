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

public class FSCMYKColor implements FSColor {
    private final float _cyan;
    private final float _magenta;
    private final float _yellow;
    private final float _black;
    
    public FSCMYKColor(float c, float m, float y, float k) {
        if (c < 0 || c > 1) {
            throw new IllegalArgumentException();
        }
        if (m < 0 || m > 1) {
            throw new IllegalArgumentException();
        }
        if (y < 0 || y > 1) {
            throw new IllegalArgumentException();
        }
        if (k < 0 || k > 1) {
            throw new IllegalArgumentException();
        }
        _cyan = c;
        _magenta = m;
        _yellow = y;
        _black = k;
    }

    public float getCyan() {
        return _cyan;
    }

    public float getMagenta() {
        return _magenta;
    }

    public float getYellow() {
        return _yellow;
    }

    public float getBlack() {
        return _black;
    }

    public String toString() {
        return "cmyk(" + _cyan + ", " + _magenta + ", " + _yellow + ", " + _black + ")";
    }
    
    public FSColor lightenColor() {
        return new FSCMYKColor(_cyan * 0.8f, _magenta * 0.8f, _yellow * 0.8f, _black);
    }
    
    public FSColor darkenColor() {
        return new FSCMYKColor(
                Math.min(1.0f, _cyan / 0.8f), Math.min(1.0f, _magenta / 0.8f), 
                Math.min(1.0f, _yellow / 0.8f), _black);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(_black);
		result = prime * result + Float.floatToIntBits(_cyan);
		result = prime * result + Float.floatToIntBits(_magenta);
		result = prime * result + Float.floatToIntBits(_yellow);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FSCMYKColor other = (FSCMYKColor) obj;
		if (Float.floatToIntBits(_black) != Float.floatToIntBits(other._black))
			return false;
		if (Float.floatToIntBits(_cyan) != Float.floatToIntBits(other._cyan))
			return false;
		if (Float.floatToIntBits(_magenta) != Float
				.floatToIntBits(other._magenta))
			return false;
		if (Float.floatToIntBits(_yellow) != Float
				.floatToIntBits(other._yellow))
			return false;
		return true;
	}
    
    
}
