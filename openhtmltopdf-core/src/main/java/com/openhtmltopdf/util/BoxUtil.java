package com.openhtmltopdf.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.render.Box;

public class BoxUtil {
    /**
     * Looks at the children of root.
     *
     * @return The body element, last element child of
     * root or root in order of preference.
     */
    public static Element getBodyElementOrSomething(Element root) {
        Node child = root.getFirstChild();
        Element body = null;

        while (child != null) {
            if (child instanceof Element) {
                body = (Element) child;
                if (child.getNodeName().equals("body")) {
                    return body;
                }
            }
            child = child.getNextSibling();
        }

        return body != null ? body : root;
    }

    /**
     * @return body box, last child of root or root in order of preference.
     */
    public static Box getBodyBoxOrSomething(Box root) {
        Box secondBest = null;
        for (Box child : root.getChildren()) {
            if (Box.isBody(child)) {
                return child;
            }
            secondBest = child;
        }

        return secondBest != null ? secondBest : root;
    }

    /**
     * Looks at the direct children of root to find one with an element with
     * node name body.
     */
    public static Box getBodyOrNull(Box root) {
        for (Box child : root.getChildren()) {
            if (Box.isBody(child)) {
                return child;
            }
        }

        return null;
    }
}
