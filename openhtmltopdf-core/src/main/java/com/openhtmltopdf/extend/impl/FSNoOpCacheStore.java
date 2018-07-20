package com.openhtmltopdf.extend.impl;

import java.util.concurrent.Callable;

import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;

public class FSNoOpCacheStore implements FSCacheEx<String, FSCacheValue> {
    public final static FSNoOpCacheStore INSTANCE = new FSNoOpCacheStore();
    
    @Override
    public void put(String key, FSCacheValue value) {
    }

    @Override
    public FSCacheValue get(String key, Callable<? extends FSCacheValue> loader) {
        return null;
    }

    @Override
    public FSCacheValue get(String key) {
        return null;
    }
}
