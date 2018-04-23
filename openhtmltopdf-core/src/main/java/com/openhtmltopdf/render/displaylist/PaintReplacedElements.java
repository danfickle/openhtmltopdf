package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.DisplayListItem;

public final class PaintReplacedElements implements DisplayListOperation {
	private final List<DisplayListItem> blocks;

	public PaintReplacedElements(List<DisplayListItem> blocks) {
		this.blocks = blocks;
	}

	public List<DisplayListItem> getReplaceds() {
		return blocks;
	}
}
