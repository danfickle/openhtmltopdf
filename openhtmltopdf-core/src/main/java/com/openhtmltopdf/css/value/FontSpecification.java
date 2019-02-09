package com.openhtmltopdf.css.value;

import com.openhtmltopdf.css.constants.IdentValue;

import java.util.Arrays;

public class FontSpecification {
    public float size;
    public IdentValue fontWeight;
    public String[] families;
    public IdentValue fontStyle;
    public IdentValue variant;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Font specification: ");
        sb
                .append(" families: " + Arrays.asList(families).toString())
                .append(" size: " + size)
                .append(" weight: " + fontWeight)
                .append(" style: " + fontStyle)
                .append(" variant: " + variant);
        return sb.toString();
    }
}
