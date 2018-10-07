package com.openhtmltopdf.extend.impl;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.util.XRLog;


/**
 * A simple default cache implementation, mainly for testing. For production you will probably want to wrap Guava's cache implementation
 * or something similar. This implementation does not use synchronisation beyond using a <code>ConcurrentHashMap</code> internally.
 * Specifically, the {@link #get(String, Callable)} may call the loader multiple times if called in close succession.
 */
public class FSDefaultCacheStore implements FSCacheEx<String, FSCacheValue> {
    private final Map<String, FSCacheValue> _store = new ConcurrentHashMap<String, FSCacheValue>();
    
    @Override
    public void put(String key, FSCacheValue value) {
        XRLog.load(Level.INFO, "Putting key(" + key + ") in cache.");
        _store.put(key, value);
    }

    @Override
    public FSCacheValue get(String key, Callable<? extends FSCacheValue> loader) {
        if (_store.containsKey(key)) {
            return this.get(key);
        }
    
        FSCacheValue value;
        try {
            value = loader.call();

            if (value != null) {
                _store.put(key, value);
            }
        } catch (Exception e) {
            XRLog.exception("Could not load cache value for key(" + key + ")", e);
            value = null;
        }
        
        XRLog.load(Level.INFO, (value == null ? "Missed" : "Hit") + " key(" + key + ") from cache.");
        return value;
    }

    @Override
    public FSCacheValue get(String key) {
        FSCacheValue value = _store.get(key);
        XRLog.load(Level.INFO, (value == null ? "Missed" : "Hit") + " key(" + key + ") from cache.");
        return value;
    }
}
