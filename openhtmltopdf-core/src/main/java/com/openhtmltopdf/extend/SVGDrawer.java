package com.openhtmltopdf.extend;

import java.io.Closeable;
import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;

public interface SVGDrawer extends Closeable {
    public void importFontFaceRules(List<FontFaceRule> fontFaces,
            SharedContext shared);

    public SVGImage buildSVGImage(Element svgElement, Box box, CssContext cssContext, double cssWidth,
            double cssHeight, double dotsPerPixel);

    public static interface SVGImage {
        public int getIntrinsicWidth();

        public int getIntrinsicHeight();

        public void drawSVG(OutputDevice outputDevice, RenderingContext ctx,
                double x, double y);
    }
}
