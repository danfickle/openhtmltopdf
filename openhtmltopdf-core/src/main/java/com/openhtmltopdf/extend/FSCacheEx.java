package com.openhtmltopdf.extend;

import java.util.concurrent.Callable;

public interface FSCacheEx<K, V> {
    public void put(K key, V value);
    public V get(K key, Callable<? extends V> loader);
    public V get(K key);
}
