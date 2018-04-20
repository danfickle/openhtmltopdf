package com.openhtmltopdf.render.displaylist;

import java.util.List;
import java.util.Map;

import com.openhtmltopdf.layout.CollapsedBorderSide;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.RenderingContext;

public final class PaintBackgroundAndBorders implements DisplayListOperation {
	private final List<DisplayListItem> blocks;
	private final RenderingContext c;
	private final Map<TableCellBox, List<CollapsedBorderSide>> collapedTableBorders;
	
	public PaintBackgroundAndBorders(List<DisplayListItem> blocks, RenderingContext c, Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders) {
		this.blocks = blocks;
		this.c = c;
		this.collapedTableBorders = collapsedTableBorders;
	}
	
	public List<DisplayListItem> getBlocks() {
		return this.blocks;
	}
	
	public RenderingContext getContext() {
		return this.c;
	}

	public Map<TableCellBox, List<CollapsedBorderSide>> getCollapedTableBorders() {
		return collapedTableBorders;
	}
}
