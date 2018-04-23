package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;

public final class PaintLayerBackgroundAndBorder implements DisplayListOperation {
	private final Box master;
	
	public PaintLayerBackgroundAndBorder(Box master) {
		this.master = master;
	}

	public Box getMaster() {
		return this.master;
	}

}
