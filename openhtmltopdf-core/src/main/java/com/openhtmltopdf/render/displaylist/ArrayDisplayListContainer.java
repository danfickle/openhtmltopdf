package com.openhtmltopdf.render.displaylist;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an ArrayList backed DisplayListContainer for when we expect to get page instructions for all pages
 * such as collecting the root box.
 */
public class ArrayDisplayListContainer extends DisplayListContainer {
    private final List<DisplayListPageContainer> pageInstructions;
    private final int startPage;
    
    public ArrayDisplayListContainer(int startPage, int endPage) {
        this.pageInstructions = new ArrayList<DisplayListPageContainer>(endPage - startPage + 1);
        this.startPage = startPage;
        
        for (int i = 0; i < endPage - startPage + 1; i++) {
            this.pageInstructions.add(new DisplayListPageContainer(null));
        }
    }
    
    @Override
    public DisplayListPageContainer getPageInstructions(int pg) {
        return this.pageInstructions.get(pg - this.startPage);
    }
    
    @Override
    public int getMinPage() {
        return this.startPage;
    }
    
    @Override
    public int getMaxPage() {
        return this.startPage + this.pageInstructions.size() - 1;
    }
}
