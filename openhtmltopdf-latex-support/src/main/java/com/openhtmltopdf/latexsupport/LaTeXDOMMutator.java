package com.openhtmltopdf.latexsupport;

import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.openhtmltopdf.extend.FSDOMMutator;
import uk.ac.ed.ph.snuggletex.utilities.CSSUtilities;

/**
 * Allows to use &lt;latex&gt; tags within the HTML to use LaTeX for math and
 * format output. Register using
 */
public class LaTeXDOMMutator implements FSDOMMutator {
	/**
	 * The singleton instance to use.
	 */
	public final static LaTeXDOMMutator INSTANCE = new LaTeXDOMMutator();
	private final DOMConverter converter = new DOMConverter();

	private LaTeXDOMMutator() {
	}

	@Override
	public void mutateDocument(org.w3c.dom.Document document) {
		try {
			NodeList latexNodes = document.getElementsByTagName("latex");
			for (int i = 0; i < latexNodes.getLength(); i++) {
				converter.convert((Element) latexNodes.item(i));
			}
			if (latexNodes.getLength() != 0) {
				/*
				 * We must append the style sheet, otherwise we wont get everything rendered
				 * correctly
				 */
				String defaultCSS = CSSUtilities.writeDefaultStylesheet();
				NodeList list = document.getElementsByTagName("head");
				if (list.getLength() == 0)
					list = document.getElementsByTagName("body");
				if (list.getLength() > 0) {
					Element style = document.createElement("style");
					style.setTextContent(defaultCSS);
					style.setAttribute("type", "text/css");
					/*
					 * We add the style as the first element, so that it can be overwritten
					 */
					list.item(0).insertBefore(style, list.item(0).getFirstChild());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
