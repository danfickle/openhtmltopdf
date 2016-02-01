package com.openhtmltopdf.test;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;

/**
 * @author patrick
*/
public abstract class ElementReplacer {
    public abstract boolean isElementNameMatch();

    public abstract String getElementNameMatch();

    public abstract boolean accept(LayoutContext context, Element element);

    public abstract ReplacedElement replace(final LayoutContext context,
                                   final BlockBox box,
                                   final UserAgentCallback uac,
                                   final int cssWidth,
                                   final int cssHeight
    );

    public abstract void clear(final Element element);

    public abstract void reset();
}
