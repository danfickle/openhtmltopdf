package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.Box;

public final class PaintPushClipLayer implements DisplayListOperation {
	private final List<Box> clipBoxes;
	
	public PaintPushClipLayer(List<Box> clipBoxes) {
		this.clipBoxes = clipBoxes;
	}
	
	public List<Box> getClipBoxes() {
		return this.clipBoxes;
	}
}
