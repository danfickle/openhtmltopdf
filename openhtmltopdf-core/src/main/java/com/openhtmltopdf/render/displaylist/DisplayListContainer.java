package com.openhtmltopdf.render.displaylist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisplayListContainer {
	public static class DisplayListPageContainer {
		private List<DisplayListOperation> ops = null;
		
		public void addOp(DisplayListOperation dlo) {
			if (this.ops == null) {
				this.ops = new ArrayList<DisplayListOperation>();
			}
			this.ops.add(dlo);
		}
		
		public List<DisplayListOperation> getOperations() {
			return this.ops == null ? Collections.<DisplayListOperation>emptyList() : this.ops;
		}
	}
	
	private final List<DisplayListPageContainer> pageInstructions;
	
	public DisplayListContainer(int pageCount) {
		this.pageInstructions = new ArrayList<DisplayListPageContainer>(pageCount);
		
		for (int i = 0; i < pageCount; i++) {
			this.pageInstructions.add(new DisplayListPageContainer());
		}
	}
	
	public DisplayListPageContainer getPageInstructions(int pg) {
		return this.pageInstructions.get(pg);
	}
	
	public int getNumPages() {
		return this.pageInstructions.size();
	}
}
