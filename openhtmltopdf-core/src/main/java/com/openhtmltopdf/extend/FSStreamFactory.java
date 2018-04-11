package com.openhtmltopdf.extend;

/**
 * Represents a very simple stream/reader client for any protocol.
 * @see {@link FSStream}
 */
public interface FSStreamFactory 
{
	public FSStream getUrl(String url);
}
