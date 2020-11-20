/*
 * {{{ header & license
 * Copyright (c) 2005 Joshua Marinacci
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
package com.openhtmltopdf.render;

import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.layout.Breaker;
import com.openhtmltopdf.layout.FunctionData;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.WhitespaceStripper;
import com.openhtmltopdf.util.OpenUtil;

/**
 * A lightweight object which contains a chunk of text from an inline element.  
 * It will never extend across a line break nor will it extend across an element 
 * nested within its inline element.
 */
public class InlineText {
    private InlineLayoutBox _parent;
    
    private int _x;
    private int _width;
    
    private String _masterText;
    private int _start;
    private int _end;
    
    private boolean _containedLF = false;
    
    private boolean _trimmedLeadingSpace;
    private boolean _trimmedTrailingSpace;
    
    static class InlineTextRareData {
        FunctionData _functionData;
        boolean _endsOnSoftHyphen = false;
        float _letterSpacing = 0f;
        byte _textDirection = BidiSplitter.LTR;
    }
    
    private CharCounts _counts;
    
    private InlineTextRareData _rareData;
    
    private void ensureRareData() {
        if (_rareData == null) {
            _rareData = new InlineTextRareData();
        }
    }
    
    /**
     * @param direction either LTR or RTL from BidiSplitter interface.
     */
    public void setTextDirection(byte direction) {
        if (direction != BidiSplitter.LTR) {
            ensureRareData();
        }
        
        if (_rareData != null) {
            _rareData._textDirection = direction;
        }
    }
    
    /**
     * @return either LTR or RTL from BidiSplitter interface.
     */
    public byte getTextDirection() {
    	return _rareData != null ? _rareData._textDirection : BidiSplitter.LTR;
    }
    
    public void setLetterSpacing(float letterSpacing) {
        if (letterSpacing != 0f) {
            ensureRareData();
        }
        
        if (_rareData != null) {
            _rareData._letterSpacing = letterSpacing;
        }
    }
    
    public float getLetterSpacing() {
        return _rareData != null ? _rareData._letterSpacing : 0f;
    }
    
    public void trimTrailingSpace(LayoutContext c) {
        if (! isEmpty() && _masterText.charAt(_end-1) == ' ') {
            _end--;
            setWidth(Breaker.getTextWidthWithLetterSpacing(c,
                    getParent().getStyle().getFSFont(c),
                    getSubstring(),
                    getLetterSpacing()));
            setTrimmedTrailingSpace(true);
        } 
    }
    
    public boolean isEmpty() {
        return _start == _end && ! _containedLF;
    }
    
    public String getSubstring() {
        if (getMasterText() != null) {
            if (_start == -1 || _end == -1) {
                throw new RuntimeException("negative index in InlineBox");
            }
            if (_end < _start) {
                throw new RuntimeException("end is less than setStartStyle");
            }
            return getMasterText().substring(_start, _end);
        } else {
            throw new RuntimeException("No master text set!");
        }
    }
    
    public void setSubstring(int start, int end) {
        if (end < start) {
            String msg = String.format("(start = %d, end = %d)", start, end);
            throw new RuntimeException("set substring length too long " + msg + ": " + this);
        } else if (end < 0 || start < 0) {
            throw new RuntimeException("Trying to set negative index to inline box");
        }
        _start = start;
        _end = end;
        
        if (_end > 0 && _masterText.charAt(_end-1) == WhitespaceStripper.EOLC) {
            _containedLF = true;
            _end--;
        }
    }

    public String getMasterText() {
        return _masterText;
    }

    public void setMasterText(String masterText) {
        _masterText = masterText;
    }

    public int getX() {
        return _x;
    }

    public void setX(int x) {
        _x = x;
    }

    public int getWidth() {
        return _width;
    }

    public void setWidth(int width) {
        _width = width;
    }
    
    public void paint(RenderingContext c) {
        c.getOutputDevice().drawText(c, this);
    }
    
    public void paintSelection(RenderingContext c) {
        c.getOutputDevice().drawSelection(c, this);
    }

    public InlineLayoutBox getParent() {
        return _parent;
    }

    public void setParent(InlineLayoutBox parent) {
        _parent = parent;
    }

    public boolean isDynamicFunction() {
        return _rareData != null && _rareData._functionData != null;
    }

    public FunctionData getFunctionData() {
        return _rareData != null ? _rareData._functionData : null;
    }

    public void setFunctionData(FunctionData functionData) {
        if (functionData != null) {
            ensureRareData();
        }
        
        if (_rareData != null) {
            _rareData._functionData = functionData;
        }
    }
    
    public void updateDynamicValue(RenderingContext c) {
        String value = _rareData._functionData.getContentFunction().calculate(
                c, _rareData._functionData.getFunction(), this);
        _start = 0;
        _end = value.length();
        _masterText = value;
        
        setWidth(Breaker.getTextWidthWithLetterSpacing(c,
                getParent().getStyle().getFSFont(c),
                value,
                getLetterSpacing()));
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("InlineText: ");
        if (_containedLF || isDynamicFunction()) {
            result.append("(");
            if (_containedLF) {
                result.append('L');
            }
            if (isDynamicFunction()) {
                result.append('F');
            }
            result.append(") ");
        }
        result.append('(');
        result.append(getSubstring());
        result.append(')');
        
        return result.toString();
    }
    
    public String getTextExportText() {
        char[] ch = getSubstring().toCharArray();
        StringBuilder result = new StringBuilder();
        if (isTrimmedLeadingSpace()) {
            result.append(' ');
        }
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (c != '\n') {
                result.append(c);
            }
        }
        if (isTrimmedTrailingSpace()) {
            result.append(' ');
        }
        return result.toString();
    }

    public boolean isTrimmedLeadingSpace() {
        return _trimmedLeadingSpace;
    }

    public void setTrimmedLeadingSpace(boolean trimmedLeadingSpace) {
        _trimmedLeadingSpace = trimmedLeadingSpace;
    }

    private void setTrimmedTrailingSpace(boolean trimmedTrailingSpace) {
        _trimmedTrailingSpace = trimmedTrailingSpace;
    }

    private boolean isTrimmedTrailingSpace() {
        return _trimmedTrailingSpace;
    }

    public static boolean isJustifySpaceCodePoint(int cp) {
        return cp == ' ' || cp == '\u00a0' || cp == '\u3000';
    }

    public void countJustifiableChars(CharCounts counts) {
        if (getLetterSpacing() != 0f) {
            // We can't mess with character spacing if
            // letter spacing is already explicitly set.
            return;
        }

        // Our own personal copy we can use in the calcTotalAdjustment method.
        _counts = new CharCounts();

        getSubstring().codePoints().forEach(cp -> {
            if (isJustifySpaceCodePoint(cp)) {
                _counts.incrementSpaceCount();
            } else if (!OpenUtil.isCodePointPrintable(cp)) {
                // Do nothing...
                // FIXME: This will actually depend on font.
            } else {
                _counts.incrementNonSpaceCount();
            }
        });

        if (isEndsOnSoftHyphen()) {
            _counts.incrementNonSpaceCount();
        }

        counts.setSpaceCount(counts.getSpaceCount() + _counts.getSpaceCount());
        counts.setNonSpaceCount(counts.getNonSpaceCount() + _counts.getNonSpaceCount());
    }

    public float calcTotalAdjustment(JustificationInfo info) {
        if (getLetterSpacing() != 0f) {
            // We can't mess with character spacing if
            // letter spacing is already explicitly set.
            return 0f;
        }

        if (_counts == null) {
            // This will only happen for non-justifiable text nested inside
            // justifiable text (eg. white-space: pre).
            // Therefore the correct answer is 0.
            // See InlineLayoutBox#countJustifiableChars.
            return 0f;
        }
        
        return (_counts.getSpaceCount() * info.getSpaceAdjust()) +
               (_counts.getNonSpaceCount() * info.getNonSpaceAdjust());
    }
    
    public int getStart(){
        return _start;
    }
    
    public int getEnd(){
        return _end;
    }

    public void setEndsOnSoftHyphen(boolean endsOnSoftHyphen) {
        if (endsOnSoftHyphen) {
            ensureRareData();
        }
        
        if (_rareData != null) {
            _rareData._endsOnSoftHyphen = endsOnSoftHyphen;
        }
    }

    public boolean isEndsOnSoftHyphen() {
        return _rareData != null ? _rareData._endsOnSoftHyphen : false;
    }
}

