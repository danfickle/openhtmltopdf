package com.openhtmltopdf.extend;

/**
 * Represents a very simple http or https client.
 */
public interface HttpStreamFactory 
{
	public HttpStream getUrl(String url);
}
