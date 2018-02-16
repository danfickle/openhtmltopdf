package com.openhtmltopdf.extend;

/**
 * Allows to modify the HTML document DOM after it has been parsed
 */
public interface FSDOMMutator {
	void mutateDocument(org.w3c.dom.Document document);
}
