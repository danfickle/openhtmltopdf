package com.openhtmltopdf.bidi;

public class SimpleBidiSplitterFactory implements BidiSplitterFactory {
	@Override
	public BidiSplitter createBidiSplitter() {
       return new SimpleBidiSplitter();
	}
}
