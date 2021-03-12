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
package com.openhtmltopdf.pdfboxout;

public class PagePosition<T> {
    private final String _id;
    private final T _element;
    private final int _pageNo;
    private final float _x;
    private final float _y;
    private final float _width;
    private final float _height;

    public PagePosition(String id, T element, int pageNo, float x, float y, float width, float height) {
        this._id = id;
        this._element = element;
        this._pageNo = pageNo;
        this._x = x;
        this._y = y;
        this._width = width;
        this._height = height;
    }

    public int getPageNo() {
        return _pageNo;
    }

    public float getX() {
        return _x;
    }

    public float getY() {
        return _y;
    }

    public float getWidth() {
        return _width;
    }

    public float getHeight() {
        return _height;
    }

    public String getId() {
        return _id;
    }

    public T getElement() {
        return _element;
    }

    @Override
    public String toString() {
        return String.format(
                "PagePosition [_id=%s, _element=%s, _pageNo=%s, _x=%s, _y=%s, _width=%s, _height=%s]",
                _id, _element, _pageNo, _x, _y, _width, _height);
    }
}
