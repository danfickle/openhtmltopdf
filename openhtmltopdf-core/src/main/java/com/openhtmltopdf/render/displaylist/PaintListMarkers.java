package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.RenderingContext;

public final class PaintListMarkers implements DisplayListOperation {

	private final List<DisplayListItem> blocks;
	private final RenderingContext context;

	public PaintListMarkers(List<DisplayListItem> blocks, RenderingContext c) {
		this.blocks = blocks;
		this.context = c;
	}

	public RenderingContext getContext() {
		return context;
	}

	public List<DisplayListItem> getBlocks() {
		return blocks;
	}

}
