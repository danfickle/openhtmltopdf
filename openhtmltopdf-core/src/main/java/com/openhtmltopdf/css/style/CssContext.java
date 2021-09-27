package com.openhtmltopdf.css.style;

import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;

public interface CssContext {
    float getMmPerDot();
    
    int getDotsPerPixel();

    float getFontSize2D(FontSpecification font);

    float getXHeight(FontSpecification parentFont);

    FSFont getFont(FontSpecification font);
    
    // FIXME Doesn't really belong here, but this is
    // the only common interface of LayoutContext
    // and RenderingContext
    StyleReference getCss();
    
    FSFontMetrics getFSFontMetrics(FSFont font);
    
    FontContext getFontContext();
    
    TextRenderer getTextRenderer();

    /**
     * Returns true if we are laying out the footnote area rather
     * than general content.
     */
    boolean isInFloatBottom();
}
