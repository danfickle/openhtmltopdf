package com.openhtmltopdf.render.displaylist;

import java.util.Collections;
import java.util.EnumSet;

import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;

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
}
