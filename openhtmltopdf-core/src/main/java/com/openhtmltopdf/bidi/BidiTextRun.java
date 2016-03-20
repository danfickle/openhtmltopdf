package com.openhtmltopdf.bidi;


/**
 * Simple class to hold a visual ordering text run.
 */
public class BidiTextRun {
    private final int startIndex;
    private final int length;
    private final byte direction;
    
    public BidiTextRun(int startIndex, int length, byte direction) {
        this.startIndex = startIndex;
        this.length = length;
        this.direction = direction;
    }
    
    public int getStart() {
        return startIndex;
    }
    
    public int getLength() {
        return length;
    }

    /**
     * @return either LTR or RTL.
     */
    public byte getDirection() {
        return direction;
    }
}
