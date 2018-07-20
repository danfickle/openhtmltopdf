package com.openhtmltopdf.pdfboxout;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import com.openhtmltopdf.extend.FSCacheValue;

public class PdfBoxRawPDFontMetrics implements FSCacheValue {
    public final float _ascent;
    public final float _descent;
    public final float _strikethroughOffset;
    public final float _strikethroughThickness;
    public final float _underlinePosition;
    public final float _underlineThickness;
    
    public PdfBoxRawPDFontMetrics(float ascent, float descent, float strikethroughOffset, float strikethroughThickness, float underlinePosition, float underlineThickness) {
        this._ascent = ascent;
        this._descent = descent;
        this._strikethroughOffset = strikethroughOffset;
        this._strikethroughThickness = strikethroughThickness;
        this._underlinePosition = underlinePosition;
        this._underlineThickness = underlineThickness;
    }
    
    public static PdfBoxRawPDFontMetrics fromPdfBox(PDFont font, PDFontDescriptor descriptor) throws IOException {
        return new PdfBoxRawPDFontMetrics(
            font.getBoundingBox().getUpperRightY(),                 // Ascent
            -font.getBoundingBox().getLowerLeftY(),                 // Descent
            -descriptor.getFontBoundingBox().getUpperRightY() / 3f, // Strikethrough offset
            100f,                                                   // FIXME: Strikethrough thickness
            -descriptor.getDescent(),                               // Underline position
            50f);                                                   // FIXME: Underline thickness
    }

    @Override
    public int weight() {
        return 6 * 4; // Six floats.
    }
}
