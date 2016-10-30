package com.openhtmltopdf.bidi;

import java.util.Collections;
import java.util.List;

public class SimpleBidiSplitter implements BidiSplitter {

	private List<BidiTextRun> runs;
	
	@Override
	public void setParagraph(String paragraph, byte defaultDirection) {
// Commented out test code for testing only!
//		int idx = paragraph.indexOf("Flying Saucer");
//        
//        if (idx >= 0)
//        {
//        	runs = new ArrayList<BidiTextRun>(2);
//        	
//        	runs.add(new BidiTextRun(0, idx, BidiSplitter.LTR));
//        	runs.add(new BidiTextRun(idx, paragraph.length() - idx, BidiSplitter.RTL));
//        	return;
//        }
        
        // Actual code starts here.
        runs = Collections.singletonList(new BidiTextRun(0, paragraph.length(), defaultDirection));
 	}

	@Override
	public int countTextRuns() {
		return runs.size();
	}

	@Override
	public BidiTextRun getVisualRun(int runIndex) {
		return runs.get(runIndex);
	}

	@Override
	public byte getBaseDirection(String paragraph) {
		return BidiSplitter.LTR;
	}
}
