package com.openhtmltopdf.render;

import java.util.ArrayList;
import java.util.List;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.layout.LayoutContext;

public class FlowingColumnContainerBox extends BlockBox {
	private FlowingColumnBox _child;
	
	private int findPageIndex(List<PageBox> pages, int y) {
		int idx = 0;
		for (PageBox page : pages) {
			if (y >= page.getTop() && y <= page.getBottom()) {
				return idx;
			}
			idx++;
		}
		return idx - 1;
	}
	
	private static class ColumnPosition {
		private final int copyY;  // Absolute, What y position starts the column in the long column block.
		private final int pasteY; // Absolute, What y position starts the column in the flowing column block for final render.
		private final int maxColHeight; // Absolute, Maximum bottom of the column.
		private final int pageIdx;
		
		private ColumnPosition(int copyY, int pasteY, int maxColHeight, int pageIdx) {
			this.copyY = copyY;
			this.pasteY = pasteY;
			this.maxColHeight = maxColHeight;
			this.pageIdx = pageIdx;
		}
	}
	
	private int adjust(LayoutContext c, Box child, int colGap, int colWidth, int columnCount, int xStart) {
		int startY = this.getAbsY();
		List<PageBox> pages = c.getRootLayer().getPages();

		int pageIdx = findPageIndex(pages, startY);
		int colStart = startY;
		int colHeight = pages.get(pageIdx).getBottom();
		int colIdx = 0;
		int finalHeight = 0;
		
		List<ColumnPosition> cols = new ArrayList<ColumnPosition>();
		cols.add(new ColumnPosition(0, startY, colHeight, pageIdx));
		
		for (Object chld : child.getChildren()) {
			Box ch = (Box) chld;
			
			int yAdjust = cols.get(colIdx).pasteY - cols.get(colIdx).copyY;
			int yProposedFinal = ch.getY() + yAdjust;
			
			if (yProposedFinal + ch.getHeight() > cols.get(colIdx).maxColHeight) {
				int newColIdx = colIdx + 1;
				int newPageIdx = newColIdx % columnCount == 0 ? cols.get(colIdx).pageIdx + 1 : cols.get(colIdx).pageIdx;
				
				if (newPageIdx >= pages.size()) {
					c.getRootLayer().addPage(c);
				}
				
				int pasteY = newColIdx % columnCount == 0 ? pages.get(newPageIdx).getTop() : cols.get(colIdx).pasteY;
				
				cols.add(new ColumnPosition(ch.getY(), pasteY, pages.get(newPageIdx).getBottom(), newPageIdx));
				colIdx++;

				yAdjust = cols.get(colIdx).pasteY - cols.get(colIdx).copyY;
				yProposedFinal = ch.getY() + yAdjust;
			}
			
			ch.setY(yProposedFinal - colStart);
			finalHeight = Math.max(yProposedFinal - colStart + ch.getHeight(), finalHeight);
			
			int xAdjust = ((colIdx % columnCount) * colWidth) + ((colIdx % columnCount) * colGap);
			ch.setX(ch.getX() + xAdjust + xStart);
		}
		
		return finalHeight;
	}
	
	@Override
	public void layout(LayoutContext c, int contentStart) {
		this.calcDimensions(c);
		
		int colCount = getStyle().columnCount();
		int colGapCount = colCount - 1;

		float colGap = getStyle().isIdent(CSSName.COLUMN_GAP, IdentValue.NORMAL) ?
				getStyle().getLineHeight(c) : /* Use the line height as a normal column gap. */
				getStyle().getFloatPropertyProportionalWidth(CSSName.COLUMN_GAP, getContentWidth(), c);
		
		float totalGap = colGap * colGapCount;
		int colWidth = (int) ((this.getContentWidth() - totalGap) / colCount);
		
		_child.setContainingLayer(this.getContainingLayer());
		_child.setContentWidth(colWidth);
		_child.setColumnWidth(colWidth);
		_child.setAbsX(this.getAbsX());
		_child.setAbsY(this.getAbsY());
		
		c.setIsPrintOverride(false);
		  _child.layout(c, contentStart);
		c.setIsPrintOverride(null);
		
		int height = adjust(c, _child, (int) colGap, colWidth, colCount, this.getLeftMBP() + this.getX());
		_child.setHeight(0);
		this.setHeight(height);
		_child.calcChildLocations();
	}
	
	public void setOnlyChild(LayoutContext c, FlowingColumnBox child) {
		this._child = child;
		this.addChild(child);
	}
	
	public FlowingColumnBox getChild() {
		return _child;
	}
}
