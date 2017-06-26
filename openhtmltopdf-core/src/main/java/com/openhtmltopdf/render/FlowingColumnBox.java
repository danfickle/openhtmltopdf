package com.openhtmltopdf.render;

public class FlowingColumnBox extends BlockBox {
	private final Box parent;
	private int _colWidth;
	
	public FlowingColumnBox(Box parent) {
		this.parent = parent;
	}
	
	@Override
	public boolean isFlowingColumnBox() {
		return true;
	}
	
	@Override
	public int getWidth() {
		return this._colWidth;
	}
	
	@Override
	public int getContentWidth() {
		return this._colWidth;
	}

	@Override
	public Box getParent() {
		return parent;
	}

	public void setColumnWidth(int columnWidth) {
		this._colWidth = columnWidth;
	}
}
