package com.openhtmltopdf.latexsupport;

import java.io.IOException;

import org.w3c.dom.Element;

import uk.ac.ed.ph.snuggletex.DOMOutputOptions;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.utilities.StylesheetManager;

class DOMConverter {
	private StylesheetManager stylesheetManager = new StylesheetManager();

	void convert(Element latexElement) throws IOException {
		String rawInputLaTeX = latexElement.getTextContent();
		String inputLaTeX = rawInputLaTeX.replaceAll("(\r\n|\r|\n)", "\n");

		SnuggleEngine engine = createSnuggleEngine();
		SnuggleSession session = engine.createSession();
		SnuggleInput input = new SnuggleInput(inputLaTeX, "LaTeX Element");

		try {
			session.parseInput(input);
		} catch (Exception e) {
			throw new IOException("Error while parsing: " + rawInputLaTeX + ": " + e.getMessage(), e);
		}

		while (latexElement.getChildNodes().getLength() != 0)
			latexElement.removeChild(latexElement.getFirstChild());

		DOMOutputOptions options = new DOMOutputOptions();
		options.setErrorOutputOptions(DOMOutputOptions.ErrorOutputOptions.XHTML);
		try {
			session.buildDOMSubtree(latexElement, options);
		} catch (Exception e) {
			throw new IOException("Error while building DOM for: " + rawInputLaTeX + ": " + e.getMessage(), e);
		}
	}

	private StylesheetManager getStylesheetManager() {
		return stylesheetManager;
	}

	private SnuggleEngine createSnuggleEngine() {
		return new SnuggleEngine(getStylesheetManager());
	}
}
