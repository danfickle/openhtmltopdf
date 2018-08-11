package com.openhtmltopdf.render.displaylist;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector.PageResult;

public class SinglePageDisplayListCollector extends DisplayListCollector {
    private final int pageNumber;
    private final PageBox pageBox;
    
    public SinglePageDisplayListCollector(PageBox pageBox, int pageNo) {
        super(Collections.singletonList(pageBox));
        this.pageNumber = pageNo;
        this.pageBox = pageBox;
    }
    
    @Override
    protected void addItem(DisplayListOperation item, int pgStart, int pgEnd, DisplayListContainer dlPages) {
        dlPages.getPageInstructions(this.pageNumber).addOp(item);
    }
    
    @Override
    protected PagedBoxCollector createBoundedBoxCollector(int pageStart, int pageEnd) {
        return new SinglePageBoxCollector(this.pageNumber, this.pageBox);
    }
    
    @Override
    protected int findStartPage(RenderingContext c, Layer layer) {
        return this.pageNumber;
    }
    
    @Override
    protected int findEndPage(RenderingContext c, Layer layer) {
        return this.pageNumber;
    }
    
    public DisplayListContainer collectFixed(RenderingContext c, Layer layer) {
        // This is called from the painter to collect fixed boxes just before paint.
        DisplayListContainer res = new DisplayListContainer(this.pageNumber, this.pageNumber);
        collect(c, layer, res, EnumSet.of(CollectFlags.INCLUDE_FIXED_BOXES));
        return res;
    }
    
    public void collectInlineBlockBoxForSinglePage(RenderingContext c, BlockBox bb, DisplayListPageContainer pageInstructions, Set<CollectFlags> flags) {
        int pageNumber = c.getPageNo();
        // TODO
        PagedBoxCollector collector = createBoundedBoxCollector(pageNumber, pageNumber);
        
        if (pageNumber < collector.getMinPageNumber() || pageNumber > collector.getMaxPageNumber()) {
            return;
        }

        collector.collect(c, bb.getContainingLayer(), bb, pageNumber, pageNumber, PagedBoxCollector.PAGE_ALL /* TODO */);
            
        PageResult pg = collector.getPageResult(pageNumber);
        // TODO
        processPage(c, bb.getContainingLayer(), pg, pageInstructions, /* includeFloats: */ false, pageNumber, -1);
    }
}
