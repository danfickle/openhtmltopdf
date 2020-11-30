package com.openhtmltopdf.outputdevice.helper;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;

public class AddedFont {
    public final FSSupplier<InputStream> supplier;
    public final File fontFile;
    public final Integer weight;
    public final String family;
    public final boolean subset;
    public final FontStyle style;
    public final Object pdfontSupplier; // Bit of a hack, not type-safe!
    public final Set<FSFontUseCase> usedFor;

    public AddedFont(
            FSSupplier<InputStream> supplier,
            File fontFile,
            Integer weight,
            String family,
            boolean subset,
            FontStyle style,
            Set<FSFontUseCase> usedFor) {
        this.supplier = supplier;
        this.fontFile = fontFile;
        this.pdfontSupplier = null;
        this.weight = weight;
        this.family = family;
        this.subset = subset;
        this.style = style;
        this.usedFor = usedFor;
    }

    public AddedFont(
            Object pdfontSupplier,
            Integer weight,
            String family,
            boolean subset,
            FontStyle style,
            Set<FSFontUseCase> usedFor) {
        this.supplier = null;
        this.fontFile = null;
        this.pdfontSupplier = pdfontSupplier;
        this.weight = weight;
        this.family = family;
        this.subset = subset;
        this.style = style;
        this.usedFor = usedFor;
    }
}
