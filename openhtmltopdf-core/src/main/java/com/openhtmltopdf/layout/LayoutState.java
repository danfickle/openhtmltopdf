/*
 * {{{ header & license
 * Copyright (c) 2005 Wisconsin Court System
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

import java.util.LinkedList;
import java.util.Objects;

import com.openhtmltopdf.render.MarkerData;

/**
 * A bean which captures all state necessary to lay out an arbitrary box.
 * Mutable objects must be copied when provided to this class.  It is far too
 * expensive to maintain a bean of this class for each box.
 * It is only created as needed.
 * <br><br>
 * IMPORTANT: Immutable after construction.
 */
public class LayoutState {
    private final StyleTracker _firstLines;
    private final StyleTracker _firstLetters;

    private final MarkerData _currentMarkerData;

    private final LinkedList<BlockFormattingContext> _BFCs;

    private final String _pageName;
    private final int _extraSpaceTop;
    private final int _extraSpaceBottom;
    private final int _noPageBreak;

    public LayoutState(
            LinkedList<BlockFormattingContext> bfcs,
            MarkerData markerData,
            StyleTracker firstLetters,
            StyleTracker firstLines,
            String pageName,
            int extraSpaceTop,
            int extraSpaceBottom,
            int noPageBreak) {
        this._BFCs = bfcs;
        this._currentMarkerData = markerData;
        this._firstLetters = firstLetters;
        this._firstLines = firstLines;
        this._pageName = pageName;
        this._extraSpaceTop = extraSpaceTop;
        this._extraSpaceBottom = extraSpaceBottom;
        this._noPageBreak = noPageBreak;
    }

    public LayoutState(
            LinkedList<BlockFormattingContext> bfcs,
            MarkerData currentMarkerData,
            StyleTracker firstLetters,
            StyleTracker firstLines) {
        this(bfcs, currentMarkerData, firstLetters, firstLines, null, 0, 0, 0);
    }

    public LayoutState(
            StyleTracker firstLetters,
            StyleTracker firstLines,
            MarkerData currentMarkerData,
            String pageName) {
        this(null, currentMarkerData, firstLetters, firstLines, pageName, 0, 0, 0);
    }

    public boolean equal(
            LinkedList<BlockFormattingContext> bfcs,
            MarkerData markerData,
            StyleTracker firstLetters,
            StyleTracker firstLines,
            String pageName,
            int extraSpaceTop,
            int extraSpaceBottom,
            int noPageBreak) {

        return bfcs == _BFCs &&
               markerData == _currentMarkerData &&
               Objects.equals(firstLetters, _firstLetters) &&
               Objects.equals(firstLines, _firstLines) &&
               Objects.equals(pageName, _pageName) &&
               extraSpaceTop == _extraSpaceTop &&
               extraSpaceBottom == _extraSpaceBottom &&
               noPageBreak == _noPageBreak;
    }

    public boolean equal(
            MarkerData currentMarkerData,
            StyleTracker firstLetters,
            StyleTracker firstLines,
            String pageName) {

        return currentMarkerData == _currentMarkerData &&
               Objects.equals(firstLetters, _firstLetters) &&
               Objects.equals(firstLines, _firstLines) &&
               Objects.equals(pageName, _pageName);
    }


    public LinkedList<BlockFormattingContext> getBFCs() {
        return _BFCs;
    }

    public MarkerData getCurrentMarkerData() {
        return _currentMarkerData;
    }

    public StyleTracker getFirstLetters() {
        return _firstLetters;
    }

    public StyleTracker getFirstLines() {
        return _firstLines;
    }

    public String getPageName() {
        return _pageName;
    }

    public int getExtraSpaceTop() {
        return _extraSpaceTop;
    }

    public int getExtraSpaceBottom() {
        return _extraSpaceBottom;
    }

    public int getNoPageBreak() {
        return _noPageBreak;
    }

}
