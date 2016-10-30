package com.openhtmltopdf.bidi.support;

import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.BidiTextRun;

public class ICUBidiSplitter implements BidiSplitter {

	public static class ICUBidiSplitterFactory implements BidiSplitterFactory {
		@Override
		public BidiSplitter createBidiSplitter() {
			return new ICUBidiSplitter();
		}
	}
	
	private Bidi bidi = new Bidi();
	
	@Override
	public void setParagraph(String paragraph, byte defaultDirection) {
		bidi.setPara(paragraph, defaultDirection, null);
	}

	@Override
	public int countTextRuns() {
		return bidi.countRuns();
	}

	@Override
	public BidiTextRun getVisualRun(int runIndex) {
		BidiRun run = bidi.getVisualRun(runIndex);
		BidiTextRun textRun = new BidiTextRun(run.getStart(), run.getLength(), run.getDirection());
		return textRun;
	}

	@Override
	public byte getBaseDirection(String paragraph) {
		return Bidi.getBaseDirection(paragraph);
	}
}
