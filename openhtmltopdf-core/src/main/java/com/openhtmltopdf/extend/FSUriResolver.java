package com.openhtmltopdf.extend;


/**
 * Used to find a uri that may be relative to the BaseURL.
 * The returned value will always only be used via methods in the same
 * implementation of this interface, therefore may be a private uri-space.
 *
 * @param uri an absolute or relative (to baseURL) uri to be resolved.
 * @return the full uri in uri-spaces known to the current implementation.
 */
public interface FSUriResolver {
	public String resolveURI(String uri);
}
