package com.openhtmltopdf.render.displaylist;

import java.util.List;
import java.util.Map;

import com.openhtmltopdf.layout.CollapsedBorderSide;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.render.DisplayListItem;

public final class PaintBackgroundAndBorders implements DisplayListOperation {
	private final List<DisplayListItem> blocks;
	private final Map<TableCellBox, List<CollapsedBorderSide>> collapedTableBorders;
	
	public PaintBackgroundAndBorders(List<DisplayListItem> blocks, Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders) {
		this.blocks = blocks;
		this.collapedTableBorders = collapsedTableBorders;
	}
	
	public List<DisplayListItem> getBlocks() {
		return this.blocks;
	}

	public Map<TableCellBox, List<CollapsedBorderSide>> getCollapedTableBorders() {
		return collapedTableBorders;
	}
}
