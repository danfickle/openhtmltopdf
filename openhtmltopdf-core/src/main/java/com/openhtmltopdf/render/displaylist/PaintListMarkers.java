package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.DisplayListItem;

public final class PaintListMarkers implements DisplayListOperation {
	private final List<DisplayListItem> blocks;

	public PaintListMarkers(List<DisplayListItem> blocks) {
		this.blocks = blocks;
	}

	public List<DisplayListItem> getBlocks() {
		return blocks;
	}
}
