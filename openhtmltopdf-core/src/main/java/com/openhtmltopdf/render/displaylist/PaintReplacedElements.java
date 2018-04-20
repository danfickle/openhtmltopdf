package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.RenderingContext;

public final class PaintReplacedElements implements DisplayListOperation {
	private final List<DisplayListItem> blocks;
	private final RenderingContext context;

	public PaintReplacedElements(List<DisplayListItem> blocks, RenderingContext c) {
		this.blocks = blocks;
		this.context = c;
	}

	public List<DisplayListItem> getBlocks() {
		return blocks;
	}

	public RenderingContext getContext() {
		return context;
	}
}
