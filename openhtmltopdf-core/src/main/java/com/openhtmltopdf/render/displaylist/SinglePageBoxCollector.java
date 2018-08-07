package com.openhtmltopdf.render.displaylist;

import java.awt.geom.AffineTransform;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;

public class SinglePageBoxCollector extends PagedBoxCollector {
    private final PageResult pageResult;
    private final int pageNo;
    private final PageBox pageBox;
    
    public SinglePageBoxCollector(int pageNo, PageBox pageBox) {
        super();
        this.pageResult = new PageResult();
        this.pageNo = pageNo;
        this.pageBox = pageBox;
    }
    
    @Override
    protected int findStartPage(CssContext c, Box container, AffineTransform transform) {
        return pageNo;
    }
    
    @Override
    protected int findEndPage(CssContext c, Box container, AffineTransform transform) {
        return pageNo;
    }
    
    @Override
    protected PageResult getPageResult(int pageNo) {
        assert pageNo == this.pageNo;
        return this.pageResult;
    }
    
    @Override
    protected int getMaxPageNumber() {
        return this.pageNo;
    }
    
    @Override
    protected int getMinPageNumber() {
        return this.pageNo;
    }
    
    @Override
    protected PageBox getPageBox(int pageNo) {
        assert pageNo == this.pageNo;
        return this.pageBox;
    }
}
