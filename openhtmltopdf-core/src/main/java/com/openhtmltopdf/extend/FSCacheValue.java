package com.openhtmltopdf.extend;

/**
 * Note: Classes implementing this interface should be thread safe as we document the cache being useable accross threads.
 */
public interface FSCacheValue {
    /**
     * @return the (very) approximate weight of a cache value in bytes if known or -1 otherwise.
     */
    public int weight();
}
