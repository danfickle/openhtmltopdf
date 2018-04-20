package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;

public final class PaintLayerBackgroundAndBorder implements DisplayListOperation {

	private final RenderingContext context;
	private final Box master;
	
	public PaintLayerBackgroundAndBorder(RenderingContext c, Box master) {
		this.context = c;
		this.master = master;
	}
	
	public RenderingContext getContext() {
		return this.context;
	}
	
	public Box getMaster() {
		return this.master;
	}

}
