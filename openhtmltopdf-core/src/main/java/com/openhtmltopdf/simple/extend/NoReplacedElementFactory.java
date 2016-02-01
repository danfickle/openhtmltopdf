package com.openhtmltopdf.simple.extend;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.extend.ReplacedElementFactory;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;

public class NoReplacedElementFactory implements ReplacedElementFactory {

    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
            UserAgentCallback uac, int cssWidth, int cssHeight) {
        return null;
    }

    public void remove(Element e) {

    }

    public void setFormSubmissionListener(FormSubmissionListener listener) {
        //TODO
    }

    public void reset() {
    }

}
