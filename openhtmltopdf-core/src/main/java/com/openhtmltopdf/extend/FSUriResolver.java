package com.openhtmltopdf.extend;

public interface FSUriResolver {

	/**
	 * Used to find a uri that may be relative to the BaseURL.
	 * The returned value will always only be used via methods in the same
	 * implementation of this interface, therefore may be a private uri-space.
	 *
	 * <b>Note:</b> The base URI may point to a XHTML document or CSS document or 
	 * be a directory. 
	 *
	 * <b>Note:</b> This method may be called more than once for the same resource.
	 * 
	 * @param uri an absolute or relative (to baseUri) uri to be resolved.
	 * @return the full uri in uri-spaces known to the current implementation or null
	 * to veto the request.
	 */
	public String resolveURI(String baseUri, String uri);
}
