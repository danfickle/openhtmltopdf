package com.openhtmltopdf.css.parser;

public interface CSSValue {
    public static final short CSS_INHERIT = 0;
    public static final short CSS_PRIMITIVE_VALUE = 1;
    public static final short CSS_VALUE_LIST = 2;
    public static final short CSS_CUSTOM = 3;
    
    public String getCssText();
    public short getCssValueType();
}
