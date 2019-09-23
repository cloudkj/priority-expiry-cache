package com.github.cloudkj;

public class PriorityExpiryCacheTest {

    public static void main(String[] args) throws InterruptedException {
        PriorityExpiryCache<String, Integer> cache = new PriorityExpiryCache<>(5);
        cache.set("A", 1, 5, 100);
        cache.set("B", 2, 15, 3);
        cache.set("C", 3, 5, 10);
        cache.set("D", 4, 1, 15);
        cache.set("E", 5, 5, 150);

        cache.get("C");
        cache.setMaxItems(5);
        System.out.println(cache.keys());

        Thread.sleep(5);

        cache.setMaxItems(4);
        System.out.println(cache.keys());

        cache.setMaxItems(3);
        System.out.println(cache.keys());

        cache.setMaxItems(2);
        System.out.println(cache.keys());

        cache.setMaxItems(1);
        System.out.println(cache.keys());
    }
}
