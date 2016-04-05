package com.openhtmltopdf.extend;

public interface FSUriResolver {

	/**
	 * Used to find a uri that may be relative to the BaseURL.
	 * The returned value will always only be used via methods in the same
	 * implementation of this interface, therefore may be a private uri-space.
	 *
	 * @param uri an absolute or relative (to baseURL) uri to be resolved.
	 * @return the full uri in uri-spaces known to the current implementation.
	 */
	public String resolveURI(String uri);
	
    /**
     * Does not need to be a correct URL, only an identifier that the
     * implementation can resolve.
     *
     * @param url A URL against which relative references can be resolved.
     */
	public void setBaseURL(String uri);
	
    /**
     * @return the base uri, possibly in the implementations private uri-space
     */
	public String getBaseURL();
}
