package com.github.cloudkj;

import java.util.Arrays;
import java.util.HashSet;

public class PriorityExpiryCacheTest {

    public static void main(String[] args) throws InterruptedException {
        testExpiration();
        testPriority();
        testLRU();
    }

    public static void test() throws InterruptedException {
        final PriorityExpiryCache<String, Integer> cache = new PriorityExpiryCache<>(5);
        cache.set("A", 1, 5, 1000);
        cache.set("B", 2, 15, 30);
        cache.set("C", 3, 5, 100);
        cache.set("D", 4, 1, 150);
        cache.set("E", 5, 5, 1500);
        cache.get("C");

        cache.setMaxItems(5);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")));

        Thread.sleep(50);

        cache.setMaxItems(4);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "C", "D", "E")));

        cache.setMaxItems(3);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "C", "E")));

        cache.setMaxItems(2);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "E")));

        cache.setMaxItems(1);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C")));
    }

    public static void testExpiration() throws InterruptedException {
        final PriorityExpiryCache<String, Integer> cache = new PriorityExpiryCache<>(5);
        cache.set("A", 1, 0, 100);
        cache.set("B", 2, 0, 0);
        cache.set("C", 3, 0, 1000);
        cache.set("D", 4, 0, 500);
        cache.set("E", 5, 0, 250);

        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")));

        Thread.sleep(50);
        cache.setMaxItems(4);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "C", "D", "E")));

        Thread.sleep(100);
        cache.setMaxItems(3);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "D", "E")));

        Thread.sleep(200);
        cache.setMaxItems(2);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "D")));

        Thread.sleep(250);
        cache.setMaxItems(1);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C")));
    }

    public static void testPriority() {
        final PriorityExpiryCache<String, Integer> cache = new PriorityExpiryCache<>(5);
        cache.set("A", 1, 2, 100);
        cache.set("B", 2, 1, 100);
        cache.set("C", 3, 5, 100);
        cache.set("D", 4, 4, 100);
        cache.set("E", 5, 3, 100);

        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")));

        cache.setMaxItems(4);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "C", "D", "E")));

        cache.setMaxItems(3);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "D", "E")));

        cache.setMaxItems(2);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "D")));

        cache.setMaxItems(1);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C")));
    }

    public static void testLRU() {
        final PriorityExpiryCache<String, Integer> cache = new PriorityExpiryCache<>(5);
        cache.set("A", 1, 0, 100);
        cache.set("B", 2, 0, 100);
        cache.set("C", 3, 0, 100);
        cache.set("D", 4, 0, 100);
        cache.set("E", 5, 0, 100);
        cache.get("B");
        cache.get("A");
        cache.get("E");
        cache.get("D");
        cache.get("C");

        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "B", "C", "D", "E")));

        cache.setMaxItems(4);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("A", "C", "D", "E")));

        cache.setMaxItems(3);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "D", "E")));

        cache.setMaxItems(2);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C", "D")));

        cache.setMaxItems(1);
        assert cache.keys().equals(new HashSet<>(Arrays.asList("C")));
    }
}
