package com.openhtmltopdf.outputdevice.helper;

import java.io.File;
import java.io.InputStream;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;

public class AddedFont {
    public final FSSupplier<InputStream> supplier;
    public final File fontFile;
    public final Integer weight;
    public final String family;
    public final boolean subset;
    public final FontStyle style;

    public AddedFont(FSSupplier<InputStream> supplier, File fontFile, Integer weight, String family, boolean subset,
            FontStyle style) {
        this.supplier = supplier;
        this.fontFile = fontFile;
        this.weight = weight;
        this.family = family;
        this.subset = subset;
        this.style = style;
    }
}
