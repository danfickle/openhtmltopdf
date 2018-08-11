package com.openhtmltopdf.render.displaylist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisplayListContainer {
	public static class DisplayListPageContainer {
		private List<DisplayListOperation> ops = null;
		private List<DisplayListPageContainer> shadowPages = null;
		private final DisplayListPageContainer basePage;
		
		public DisplayListPageContainer(DisplayListPageContainer basePage) {
		    this.basePage = basePage;
		}
		
		public boolean isShadowPage() {
		    return this.basePage != null;
		}
		
		private DisplayListPageContainer getBasePage() {
		    return this.basePage;
		}
		
		public void addOp(DisplayListOperation dlo) {
			if (this.ops == null) {
				this.ops = new ArrayList<DisplayListOperation>();
			}
			this.ops.add(dlo);
		}
		
		private void addShadowsUntil(int shadow) {
		    for (int i = this.shadowPages.size(); i <= shadow; i++) {
		        this.shadowPages.add(new DisplayListPageContainer(this));
		    }
		}
		
		public DisplayListPageContainer getShadowPage(int shadowNumber) {
		    if (this.isShadowPage()) {
		        return this.getBasePage().getShadowPage(shadowNumber);
		    }
		    
		    if (this.shadowPages == null) {
		        this.shadowPages = new ArrayList<DisplayListPageContainer>();
		    }
		    addShadowsUntil(shadowNumber);
		    
		    return this.shadowPages.get(shadowNumber);
		}
		
		public List<DisplayListPageContainer> shadowPages() {
		    if (this.isShadowPage()) {
		        return this.basePage.shadowPages();
		    }

		    return this.shadowPages == null ? Collections.<DisplayListPageContainer>emptyList() : this.shadowPages;
		}
		
		public List<DisplayListOperation> getOperations() {
			return this.ops == null ? Collections.<DisplayListOperation>emptyList() : this.ops;
		}
	}
	
	private final List<DisplayListPageContainer> pageInstructions;
	private final int startPage;
	
	public DisplayListContainer(int startPage, int endPage) {
		this.pageInstructions = new ArrayList<DisplayListPageContainer>(endPage - startPage + 1);
		this.startPage = startPage;
		
		for (int i = 0; i < endPage - startPage + 1; i++) {
			this.pageInstructions.add(new DisplayListPageContainer(null));
		}
	}
	
	public DisplayListPageContainer getPageInstructions(int pg) {
		return this.pageInstructions.get(pg - this.startPage);
	}
	
	public int getMinPage() {
	    return this.startPage;
	}
	
	public int getMaxPage() {
	    return this.startPage + this.pageInstructions.size() - 1;
	}
}
