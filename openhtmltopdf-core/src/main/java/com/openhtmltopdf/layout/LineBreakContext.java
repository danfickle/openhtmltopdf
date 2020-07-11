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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.layout;

/**
 * A bean which serves as a way for the layout code to pass information to the
 * line breaking code and for the line breaking code to pass instructions back
 * to the layout code.
 */
public class LineBreakContext {
    public enum LineBreakResult {
        CHAR_BREAKING_NEED_NEW_LINE,
        WORD_BREAKING_NEED_NEW_LINE,

        CHAR_BREAKING_UNBREAKABLE,
        WORD_BREAKING_UNBREAKABLE,

        CHAR_BREAKING_FOUND_WORD_BREAK,

        CHAR_BREAKING_FINISHED, 
        WORD_BREAKING_FINISHED;
    }
    
    
    private String _master;
    private int _start;
    private int _end;
    private int _savedEnd;
    private boolean _unbreakable;
    private boolean _needsNewLine;
    private int _width;
    private boolean _endsOnNL;
    private boolean _endsOnSoftHyphen;
    private int _nextWidth;
    private boolean _endsOnWordBreak;
    private boolean _finishedInCharBreakingMode;
    
    public int getLast() {
        return _master.length();
    }
    
    public void reset() {
        _width = 0;
        _unbreakable = false;
        _needsNewLine = false;
        _endsOnSoftHyphen = false;
        _nextWidth = 0;
        _endsOnWordBreak = false;
    }
    
    public int getEnd() {
        return _end;
    }
    
    public void setEnd(int end) {
        _end = end;
    }
    
    public String getMaster() {
        return _master;
    }
    
    public void setMaster(String master) {
        _master = master;
    }
    
    public int getStart() {
        return _start;
    }
    
    public void setStart(int start) {
        _start = start;
    }
    
    public String getStartSubstring() {
        return _master.substring(_start);
    }
    
    public String getCalculatedSubstring() {
        // mimic the calculation in InlineText.setSubstring to strip newlines for our width calculations
        // the original text width calculation in InlineBox.calcMaxWidthFromLineLength() excludes the newline character
        // so if we include them here we get spurious newlines
        // apparently newlines do take up some width in most fonts
        if (_end > 0 && _master.charAt(_end-1) == WhitespaceStripper.EOLC) {
            return _master.substring(_start, _end-1);
        }
        return _master.substring(_start, _end);
    }

    public boolean isUnbreakable() {
        return _unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        _unbreakable = unbreakable;
    }

    public boolean isNeedsNewLine() {
        return _needsNewLine;
    }

    public void setNeedsNewLine(boolean needsLineBreak) {
        _needsNewLine = needsLineBreak;
    }

    public int getWidth() {
        return _width;
    }

    public void setWidth(int width) {
        _width = width;
    }
    
    public boolean isFinished() {
        return _end == getMaster().length();
    }

    public void resetEnd() {
        _end = _savedEnd;
    }
    
    public void saveEnd() {
        _savedEnd = _end;
    }

    public boolean isEndsOnNL() {
        return _endsOnNL;
    }

    public void setEndsOnNL(boolean b) {
        _endsOnNL = b;
    }

    public boolean isEndsOnSoftHyphen() {
        return this._endsOnSoftHyphen;
    }
    
    public void setEndsOnSoftHyphen(boolean b) {
        this._endsOnSoftHyphen = b;
    }

    /**
     * If needs newline, returns the graphics width of the next unbreakable sequence.
     * We use this to test if we should actually put in a newline before a long word
     * when break-word is on. If getNextWidth would fit on an empty line we put in the 
     * new line else we split in the long word immediately.
     */
    public int getNextWidth() {
        return _nextWidth;
    }

    public void setNextWidth(int nextWidth) {
        this._nextWidth = nextWidth;
    }

    public boolean isEndsOnWordBreak() {
        return _endsOnWordBreak;
    }

    public void setEndsOnWordBreak(boolean _endsOnWordBreak) {
        this._endsOnWordBreak = _endsOnWordBreak;
    }

    public void setFinishedInCharBreakingMode(boolean mode) {
        this._finishedInCharBreakingMode = mode;
    }

    /**
     * If this is true, it means we finished in char breaking mode because
     * a word was too large. The next line should begin in char breaking mode.
     */
    public boolean isFinishedInCharBreakingMode() {
        return _finishedInCharBreakingMode;
    }
}
