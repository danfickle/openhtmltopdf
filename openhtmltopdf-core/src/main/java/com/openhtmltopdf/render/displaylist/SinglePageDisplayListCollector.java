package com.openhtmltopdf.render.displaylist;

import java.util.Collections;

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
        dlPages.getPageInstructions(0).addOp(item);
    }
    
    @Override
    protected PagedBoxCollector createBoxCollector() {
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
        DisplayListContainer res = new DisplayListContainer(1);
        collect(c, layer, res, true);
        return res;
    }
}
