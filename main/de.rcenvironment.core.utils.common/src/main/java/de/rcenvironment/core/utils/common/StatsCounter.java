/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import de.rcenvironment.core.utils.common.concurrent.ThreadsafeAutoCreationMap;

/**
 * Provides a central mechanism for key/counter statistics. The key space is two-layered: on the top layer, arbitrary string keys define
 * "categories"; below that, each category holds a key-value map with string keys and a numeric (Java "long") counter.
 * 
 * All methods are safe for concurrent usage.
 * 
 * @author Robert Mischke
 */
public final class StatsCounter {

    private static final boolean ENABLED = true;

    private static ThreadsafeAutoCreationMap<String, ThreadsafeAutoCreationMap<String, AtomicLong>> counterMap;

    // prevent instantiation
    private StatsCounter() {}

    static {
        counterMap = new ThreadsafeAutoCreationMap<String, ThreadsafeAutoCreationMap<String, AtomicLong>>() {

            protected ThreadsafeAutoCreationMap<String, AtomicLong> createNewEntry(String key) {
                return new ThreadsafeAutoCreationMap<String, AtomicLong>() {

                    @Override
                    protected AtomicLong createNewEntry(String key) {
                        return new AtomicLong();
                    }
                };
            };
        };
    }

    /**
     * Checks whether statistics are enabled; should be used to prevent unneeded string concatenations for {@link #count(String, String)}
     * calls when statistics are disabled.
     * 
     * @return true if statistics collection is enabled
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Increments the given counter by one.
     * 
     * @param category the category identifier
     * @param key the counter key within the category
     */
    public static void count(String category, String key) {
        if (!ENABLED) {
            return;
        }
        counterMap.get(category).get(key).incrementAndGet();
    }

    /**
     * Convenience method for the common case where occurrences of classes should be counted, with proper handling of null objects. The
     * counter key is the name of the object's class acquired using {@link Class#getName()}; for null objects, "<null>" is used as the
     * counter key.
     * 
     * @param category the category identifier
     * @param object the object to count
     */
    public static void countClass(String category, Object object) {
        if (!ENABLED) {
            return;
        }
        if (object != null) {
            count(category, object.getClass().getName());
        } else {
            count(category, "<null>");
        }
    }

    /**
     * Increments the given counter by an arbitrary value.
     * 
     * @param category the category identifier
     * @param key the counter key within the category
     * @param delta the number to add to the counter; negative values and zero are allowed
     */
    public static void count(String category, String key, long delta) {
        if (!ENABLED) {
            return;
        }
        counterMap.get(category).get(key).addAndGet(delta);
    }

    /**
     * Clears/resets all contained statistics.
     */
    public static void resetAll() {
        counterMap.clear();
    }

    /**
     * Clears/resets all contained statistics of the given category.
     * 
     * @param category the category to clear/reset.
     */
    public static void resetCategory(String category) {
        counterMap.remove(category);
    }

    /**
     * Gets a map of all counter maps; the outer map holds the categories, while the inner maps hold each categories' counter entries.
     * 
     * @return the map of counter maps
     */
    public static Map<String, Map<String, Long>> getFullReport() {
        Map<String, Map<String, Long>> result = new TreeMap<>();

        Map<String, ThreadsafeAutoCreationMap<String, AtomicLong>> snapshot = counterMap.getShallowCopy();
        for (Map.Entry<String, ThreadsafeAutoCreationMap<String, AtomicLong>> category : snapshot.entrySet()) {
            Map<String, AtomicLong> categoryCopy = category.getValue().getShallowCopy();
            result.put(category.getKey(), convertCategoryMap(categoryCopy));
        }

        return result;
    }

    /**
     * Gets the counter map for a single category.
     * 
     * @param category the category id
     * @return the counter map
     */
    public static Map<String, Long> getReportForCategory(String category) {
        return convertCategoryMap(counterMap.get(category).getShallowCopy());
    }

    private static Map<String, Long> convertCategoryMap(Map<String, AtomicLong> categoryCopy) {
        Map<String, Long> categoryResult = new TreeMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : categoryCopy.entrySet()) {
            categoryResult.put(entry.getKey(), entry.getValue().get());
        }
        return categoryResult;
    }

}
