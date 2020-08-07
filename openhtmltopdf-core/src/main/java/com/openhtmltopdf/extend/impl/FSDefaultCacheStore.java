package com.openhtmltopdf.extend.impl;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;


/**
 * A simple default cache implementation, mainly for testing. For production you will probably want to wrap Guava's cache implementation
 * or something similar. This implementation does not use synchronisation beyond using a <code>ConcurrentHashMap</code> internally.
 * Specifically, the {@link #get(String, Callable)} may call the loader multiple times if called in close succession.
 */
public class FSDefaultCacheStore implements FSCacheEx<String, FSCacheValue> {
    private final Map<String, FSCacheValue> _store = new ConcurrentHashMap<>();
    
    @Override
    public void put(String key, FSCacheValue value) {
        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_PUTTING_KEY_IN_CACHE, key);
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
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_CACHE_VALUE_FOR_KEY, key, e);
            value = null;
        }

        XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_CACHE_HIT_STATUS, (value == null ? "Missed" : "Hit"), key);
        return value;
    }

    @Override
    public FSCacheValue get(String key) {
        FSCacheValue value = _store.get(key);
        XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_CACHE_HIT_STATUS, (value == null ? "Missed" : "Hit"), key);
        return value;
    }
}
