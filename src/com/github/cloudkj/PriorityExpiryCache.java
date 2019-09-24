package com.github.cloudkj;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A cache with support for priorities, expiration times, and a max capacity.
 *
 * <p>This implementation provides {@code log(p)} runtime for the {@code get} operation, where p is the number of
 * distinct priority levels amongst entries in the cache.</p>
 *
 * <p>Similarly, the {@code set} operation runs in {@code log(p) + log(t)} time, where t is the number of distinct
 * distinct expiration timestamps. A performance improvement can be made by decreasing the granularity at which
 * the timestamps are stored (and thus decreasing t, the number of distinct expiration timestamps), at the expense of
 * lower precision in controlling the expiration time.</p>
 */
public class PriorityExpiryCache<K, V> {

    private final Map<K, Entry> items;
    private final SortedMap<Long, Set<K>> expiryItems;
    private final SortedMap<Integer, LinkedHashSet<K>> priorityItems;

    private int maxItems;

    /**
     * Constructs a new, empty cache with the specified max capacity.
     *
     * @param maxItems the max capacity of the cache
     */
    public PriorityExpiryCache(final int maxItems) {
        this.maxItems = maxItems;
        this.items = new HashMap<>(maxItems);
        this.expiryItems = new TreeMap<>();
        this.priorityItems = new TreeMap<>();
    }

    /**
     * Get a view of the keys contained in this cache.
     *
     * @return an unmodifiable view of the cache keys.
     */
    public Set<K> keys() {
        return Collections.unmodifiableSet(items.keySet());
    }

    /**
     * Get the value of the key if the key exists in the cache and is not expired.
     *
     * @param  key the key of entry to retrieve
     * @return value of the key, or null if key doesn't exist or is expired.
     */
    public V get(final K key) {
        if (!items.containsKey(key)) {
            return null;
        }

        final Entry item = items.get(key);
        if (item.getExpireTime() <  System.currentTimeMillis()) {
            return null;
        }

        // Update LRU status of the key - removing then adding the key back in maintains insertion order
        final LinkedHashSet<K> priorityKeys = priorityItems.get(item.getPriority());
        priorityKeys.remove(key);
        priorityKeys.add(key);

        return item.getValue();
    }

    /**
     * Update or insert the value of the key with a priority value and expiration time.
     *
     * @param key          the key of entry to store
     * @param value        the value of entry to store
     * @param priority     the priority level of the cache entry
     * @param timeToLiveMs number of milliseconds (relative to current time) after which the cache entry expires
     */
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

    /**
     * Evicts items from the cache (if needed) to ensure the number of items never exceeds {@code maxItems}.
     *
     * <p>The eviction policy is as follows:</p>
     *
     * <ol>
     *   <li>1. Evict an expired item if any expired item exists.</li>
     *   <li>2. If no expired item exists, then evict the LRU item of the lowest priority</li>
     * </ol>
     */
    private void evictItems() {
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

    /**
     * Removes a key from all underlying data structures.
     */
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
