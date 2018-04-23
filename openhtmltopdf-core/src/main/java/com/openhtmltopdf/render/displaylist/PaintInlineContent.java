package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.DisplayListItem;

public final class PaintInlineContent implements DisplayListOperation {

	private final List<DisplayListItem> inlines;

	public PaintInlineContent(List<DisplayListItem> inlines) {
		this.inlines = inlines;
	}

	public List<DisplayListItem> getInlines() {
		return inlines;
	}
}
