package com.openhtmltopdf.render.displaylist;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a map based DisplayListContainer for when we expect to only get some pages, such
 * as collecting a fixed element for a single page.
 */
public class MapDisplayListContainer extends DisplayListContainer {
    private final Map<Integer, DisplayListPageContainer> pages;
    private final int pageCount;
    
    public MapDisplayListContainer(int pageCount, int expectedCapacity) {
        this.pageCount = pageCount;
        this.pages = new HashMap<Integer, DisplayListContainer.DisplayListPageContainer>(expectedCapacity);
    }
    
    @Override
    public DisplayListPageContainer getPageInstructions(int pg) {
        if (this.pages.containsKey(pg)) {
            return this.pages.get(pg);
        }
        
        DisplayListPageContainer pgInstructions = new DisplayListPageContainer(null);
        this.pages.put(pg, pgInstructions);
        return pgInstructions;
    }

    @Override
    public int getMinPage() {
        return 0;
    }

    @Override
    public int getMaxPage() {
        return this.pageCount - 1;
    }

}
