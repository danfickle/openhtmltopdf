package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;

public class PaintRootElementBackground implements DisplayListOperation {
	private final RenderingContext context;
	private final Box root;

	public PaintRootElementBackground(RenderingContext c, Box root) {
		this.context = c;
		this.root = root;
	}
	
	public RenderingContext getContext() {
		return this.context;
	}
	
	public Box getRoot() {
		return this.root;
	}
}
