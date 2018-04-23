package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;

public class PaintRootElementBackground implements DisplayListOperation {
	private final Box root;

	public PaintRootElementBackground(Box root) {
		this.root = root;
	}
	
	public Box getRoot() {
		return this.root;
	}
}
