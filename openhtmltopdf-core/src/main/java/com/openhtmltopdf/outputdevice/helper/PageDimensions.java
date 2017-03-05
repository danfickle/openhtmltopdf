package com.openhtmltopdf.outputdevice.helper;

public class PageDimensions {
    public final Float w;
    public final Float h;
    public final boolean isSizeInches;
    
    public PageDimensions(Float w, Float h, boolean isSizeInches) {
        this.w = w;
        this.h = h;
        this.isSizeInches = isSizeInches;
    }
}
