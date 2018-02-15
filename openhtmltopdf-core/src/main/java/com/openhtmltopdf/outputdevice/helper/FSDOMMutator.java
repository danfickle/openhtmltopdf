package com.openhtmltopdf.outputdevice.helper;

import java.io.IOException;

/**
 * Allows to modify the HTML document DOM after it has been parsed
 */
public interface FSDOMMutator {
	void mutateDocument(org.w3c.dom.Document document);
}
