package com.openhtmltopdf.extend;

import com.openhtmltopdf.swing.FSCacheKey;

public interface FSCache {
	public Object get(FSCacheKey cacheKey);
	public void put(FSCacheKey cacheKey, Object obj);
}
