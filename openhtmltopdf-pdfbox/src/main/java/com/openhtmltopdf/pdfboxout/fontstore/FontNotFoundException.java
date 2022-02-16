package com.openhtmltopdf.pdfboxout.fontstore;

import com.openhtmltopdf.css.value.FontSpecification;

/**
 * An extension thrown when a font isn't found.
 *
 * @author Quentin Ligier
 **/
public class FontNotFoundException extends RuntimeException {

    /**
     * Constructs a new font not found exception with the specified font specification.
     *
     * @param fontSpecification The specification of the font that hasn't been found.
     */
    public FontNotFoundException(final FontSpecification fontSpecification) {
        super("No font for the following specification has been found: " + fontSpecification.toString());
    }
}
