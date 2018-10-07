package com.openhtmltopdf.extend;

/**
 * A external cache for the renderer that may be used accross threads.
 * Typically, this would be used to create a wrapper for the cache
 * implementation of your choice such as the cache from Guava. 
 *
 * If rolling your own cache implementation and using accross threads,
 * please make sure that the put method publishes the value accross threads.
 * 
 * @param <Value> Type of the cached value
 */
public interface FSMultiThreadCache<Value> {
	/**
	 * @param uri The resolved uri as returned from any uri resolver. Never null.
	 * @return The contents of that uri or null if not cached.
	 */
	public Value get(String uri);
	
	/**
	 * Store value in cache. Please note that in a multi-threaded environment
	 * this may be called multiple times with the same uri while get returns null.
	 * 
	 * @param uri The resolved uri as returned from any uri resolver. Never null.
	 * @param value The contents of the uri as returned from any protocol handlers or the default user agent. Never null.
	 */
	public void put(String uri, Value value);
}
