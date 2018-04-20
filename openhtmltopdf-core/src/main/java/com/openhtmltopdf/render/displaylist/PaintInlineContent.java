package com.openhtmltopdf.render.displaylist;

import java.util.List;

import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.RenderingContext;

public final class PaintInlineContent implements DisplayListOperation {

	private final List<DisplayListItem> inlines;
	private final RenderingContext context;

	public PaintInlineContent(List<DisplayListItem> inlines, RenderingContext c) {
		this.inlines = inlines;
		this.context = c;
	}

	public List<DisplayListItem> getInlines() {
		return inlines;
	}

	public RenderingContext getContext() {
		return context;
	}

}
