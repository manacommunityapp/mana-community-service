package com.manacommunity.api.dsa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TaggedTtlCache<V> {

    private final ConcurrentHashMap<String, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> tagIndex = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, Long> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
    private int maxSize = 10000;

    public void put(String key, V value, long ttlMs, Set<String> tags) {
        long expiresAt = System.currentTimeMillis() + ttlMs;
        CacheEntry<V> entry = new CacheEntry<>(value, expiresAt, tags);

        CacheEntry<V> old = store.get(key);
        if (old != null) {
            for (String tag : old.tags()) {
                Set<String> tagKeys = tagIndex.get(tag);
                if (tagKeys != null) tagKeys.remove(key);
            }
        }

        store.put(key, entry);
        synchronized (accessOrder) {
            accessOrder.put(key, System.currentTimeMillis());
            evictIfNeeded();
        }

        for (String tag : tags) {
            tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(key);
        }
    }

    public V get(String key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null) return null;

        if (System.currentTimeMillis() > entry.expiresAt()) {
            remove(key);
            return null;
        }

        synchronized (accessOrder) {
            accessOrder.put(key, System.currentTimeMillis());
        }
        return entry.value();
    }

    public boolean remove(String key) {
        CacheEntry<V> entry = store.remove(key);
        if (entry == null) return false;

        synchronized (accessOrder) {
            accessOrder.remove(key);
        }
        for (String tag : entry.tags()) {
            Set<String> tagKeys = tagIndex.get(tag);
            if (tagKeys != null) tagKeys.remove(key);
        }
        return true;
    }

    public int invalidateByTag(String tag) {
        Set<String> keys = tagIndex.remove(tag);
        if (keys == null || keys.isEmpty()) return 0;

        int count = 0;
        for (String key : keys) {
            CacheEntry<V> entry = store.remove(key);
            if (entry != null) {
                count++;
                synchronized (accessOrder) {
                    accessOrder.remove(key);
                }
                for (String otherTag : entry.tags()) {
                    if (!otherTag.equals(tag)) {
                        Set<String> otherKeys = tagIndex.get(otherTag);
                        if (otherKeys != null) otherKeys.remove(key);
                    }
                }
            }
        }
        log.info("Tag invalidation '{}': {} keys removed", tag, count);
        return count;
    }

    public CacheStats getStats() {
        long now = System.currentTimeMillis();
        int expired = 0;
        for (CacheEntry<V> entry : store.values()) {
            if (now > entry.expiresAt()) expired++;
        }
        return new CacheStats(store.size(), tagIndex.size(), expired, maxSize);
    }

    public Map<String, Set<String>> getTagIndex() {
        return Collections.unmodifiableMap(tagIndex);
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void clear() {
        store.clear();
        tagIndex.clear();
        synchronized (accessOrder) {
            accessOrder.clear();
        }
    }

    private void evictIfNeeded() {
        while (accessOrder.size() > maxSize) {
            Iterator<String> it = accessOrder.keySet().iterator();
            if (it.hasNext()) {
                String lruKey = it.next();
                it.remove();
                CacheEntry<V> entry = store.remove(lruKey);
                if (entry != null) {
                    for (String tag : entry.tags()) {
                        Set<String> keys = tagIndex.get(tag);
                        if (keys != null) keys.remove(lruKey);
                    }
                }
            }
        }
    }

    public record CacheEntry<V>(V value, long expiresAt, Set<String> tags) {}
    public record CacheStats(int size, int tagCount, int expired, int maxSize) {}
}
