package com.openhtmltopdf.context;

import com.openhtmltopdf.css.extend.ContentFunction;

public abstract class ContentFunctionAbstract implements ContentFunction {
    public boolean isStatic() {
        return false;
    };
}
