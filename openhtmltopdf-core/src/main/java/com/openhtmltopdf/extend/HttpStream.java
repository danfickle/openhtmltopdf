package com.openhtmltopdf.extend;

import java.io.InputStream;
import java.io.Reader;

/**
 * Represents an http or https stream. We have a getReader method in case the connection knows more
 * about the charset encoding through the headers returned from the object. 
 */
public interface HttpStream {
	public InputStream getStream();
	public Reader getReader();
}
