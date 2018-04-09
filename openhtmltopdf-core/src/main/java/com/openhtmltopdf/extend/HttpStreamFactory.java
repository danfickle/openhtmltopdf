package com.openhtmltopdf.extend;

/**
 * Represents a very simple stream/reader client for any protocol.
 * @see {@link HttpStream}
 */
public interface HttpStreamFactory 
{
	public HttpStream getUrl(String url);
}
