package com.github.cloudkj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class PriorityExpiryCache<K, V> {

    private final Map<K, Entry> items;
    private final SortedMap<Long, Set<K>> expiryItems;
    private final SortedMap<Integer, LinkedHashSet<K>> priorityItems;

    private int maxItems;

    public PriorityExpiryCache(final int maxItems) {
        this.maxItems = maxItems;
        this.items = new HashMap<>(maxItems);
        this.expiryItems = new TreeMap<>();
        this.priorityItems = new TreeMap<>();
    }

    public Set<K> keys() {
        return items.keySet();
    }

    public V get(final K key) {
        if (!items.containsKey(key)) {
            return null;
        }

        final Entry item = items.get(key);

        // Update LRU status of the key
        final LinkedHashSet<K> priorityKeys = priorityItems.get(item.getPriority());
        priorityKeys.remove(key);
        priorityKeys.add(key);

        return item.getValue();
    }

    public void set(final K key, final V value, final int priority, final long timeToLiveMs) {
        final long expirationTimestamp = System.currentTimeMillis() + timeToLiveMs;

        // Remove existing entry, if it exists
        if (items.containsKey(key)) {
            removeKey(key);
        }

        // Add to lookup table
        final Entry item = new Entry(key, value, priority, expirationTimestamp);
        items.put(key, item);

        // Add to expiration tree
        final Set<K> expirationKeys = expiryItems.getOrDefault(expirationTimestamp, new HashSet<>());
        expirationKeys.add(key);
        expiryItems.put(expirationTimestamp, expirationKeys);

        // Add to priority tree
        final LinkedHashSet<K> priorityKeys = priorityItems.getOrDefault(priority, new LinkedHashSet<>());
        priorityKeys.add(key);
        priorityItems.put(priority, priorityKeys);

        evictItems();
    }

    public void setMaxItems(final int maxItems) {
        this.maxItems = maxItems;
        evictItems();
    }

    public void evictItems() {
        final long now = System.currentTimeMillis();
        while (shouldEvict()) {

            // Evict an expired item if any expired item exists
            final long earliestExpiration = expiryItems.firstKey();
            if (earliestExpiration < now) {
                final Set<K> expiredKeys = expiryItems.get(earliestExpiration);
                // Choose any key from the set of expired keys to remove
                final K key = expiredKeys.iterator().next();
                removeKey(key);
                continue;
            }

            // Evict the least recently used item of the lowest priority
            final int lowestPriority = priorityItems.firstKey();
            final K lruKey = priorityItems.get(lowestPriority).iterator().next();
            removeKey(lruKey);
        }

    }

    private void removeKey(final K key) {
        final Entry item = items.get(key);

        // Remove from lookup table
        items.remove(key);

        // Remove from expiration tree
        final long expirationTime = item.getExpireTime();
        final Set<K> expirationKeys = expiryItems.get(expirationTime);
        expirationKeys.remove(item.getKey());
        if (expirationKeys.isEmpty()) {
            expiryItems.remove(expirationTime);
        }

        // Remove from priority tree
        final int priority = item.getPriority();
        final LinkedHashSet<K> priorityKeys = priorityItems.get(priority);
        priorityKeys.remove(item.getKey());
        if (priorityKeys.isEmpty()) {
            priorityItems.remove(priority);
        }
    }

    private boolean shouldEvict() {
        return items.size() > maxItems;
    }

    private class Entry {
        private final K key;
        private final V value;
        private final int priority;
        private final long expireTime;

        private Entry(K key, V value, int priority, long expireTime) {
            this.key = key;
            this.value = value;
            this.priority = priority;
            this.expireTime = expireTime;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public int getPriority() {
            return priority;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }
}
