package com.openhtmltopdf.outputdevice.helper;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.extend.FSTextTransformer;

public class UnicodeImplementation {
    public final BidiReorderer reorderer;
    public final BidiSplitterFactory splitterFactory;
    public final FSTextBreaker lineBreaker;
    public final FSTextBreaker charBreaker;
    public final FSTextTransformer toLowerTransformer;
    public final FSTextTransformer toUpperTransformer;
    public final FSTextTransformer toTitleTransformer;
    public final boolean textDirection;
    
    public UnicodeImplementation(BidiReorderer reorderer, BidiSplitterFactory splitterFactory, 
            FSTextBreaker lineBreaker, FSTextTransformer toLower, FSTextTransformer toUpper,
            FSTextTransformer toTitle, boolean textDirection, FSTextBreaker charBreaker) {
        this.reorderer = reorderer;
        this.splitterFactory = splitterFactory;
        this.lineBreaker = lineBreaker;
        this.toLowerTransformer = toLower;
        this.toUpperTransformer = toUpper;
        this.toTitleTransformer = toTitle;
        this.textDirection = textDirection;
        this.charBreaker = charBreaker;
    }
}
