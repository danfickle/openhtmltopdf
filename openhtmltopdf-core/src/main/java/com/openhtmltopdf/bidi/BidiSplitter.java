package com.openhtmltopdf.bidi;

public interface BidiSplitter {
    public static final byte LTR = 0;
    public static final byte RTL = 1;
    
    
    /**
     * Sets the text which is to be split on visual ordering.
     * @param paragraph
     * @param defaultDirection either LTR or RTL
     */
    public void setParagraph(String paragraph, byte defaultDirection);
    
    /**
     * Count the number of runs, each of which contains text in one visual order only.
     * Can only be called after setParagraph has run the BIDI algorithm.
     */
    public int countTextRuns();
	
    /**
     * @param runIndex from zero to countTextRuns.
     * @return information about a visual run.
     */
    public BidiTextRun getVisualRun(int runIndex);
}
