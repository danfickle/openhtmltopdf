package com.openhtmltopdf.render.displaylist;

import java.awt.Rectangle;

public final class PaintPushClipRect implements DisplayListOperation {
	private final Rectangle clipBox;
	
	public PaintPushClipRect(Rectangle clipBox) {
		this.clipBox = clipBox;
	}
	
	public Rectangle getClipBox() {
		return this.clipBox;
	}
}
