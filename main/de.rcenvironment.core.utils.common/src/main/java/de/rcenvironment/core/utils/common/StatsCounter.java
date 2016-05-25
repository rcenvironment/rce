/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private static ThreadsafeAutoCreationMap<String, ThreadsafeAutoCreationMap<String, ValueTrackerEntry>> valueTrackerMap;

    /**
     * A holder for simple statistics over an unknown number of values added over time.
     * 
     * @author Robert Mischke
     */
    private static final class ValueTrackerEntry {

        private long n;

        private long min = Long.MAX_VALUE;

        private long max = Long.MIN_VALUE;

        private double sum;

        public synchronized void register(long value) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
            n++;
        }

        public String render() {
            if (n == 0) {
                return "-";
            }
            return StringUtils.format("Total %,.2f, Average %,.2f, Min %,d, Max %,d, counted %,d times", sum, sum / n, min, max, n);
        }
    }

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
        valueTrackerMap = new ThreadsafeAutoCreationMap<String, ThreadsafeAutoCreationMap<String, ValueTrackerEntry>>() {

            protected ThreadsafeAutoCreationMap<String, ValueTrackerEntry> createNewEntry(String key) {
                return new ThreadsafeAutoCreationMap<String, ValueTrackerEntry>() {

                    @Override
                    protected ValueTrackerEntry createNewEntry(String key) {
                        return new ValueTrackerEntry();
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
     * Registers an event's value, for example an task's duration.
     * 
     * @param category the category identifier
     * @param key the key within the category
     * @param value the value associated with an event
     */
    public static void registerValue(String category, String key, long value) {
        if (!ENABLED) {
            return;
        }
        valueTrackerMap.get(category).get(key).register(value);
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
    public static Map<String, Map<String, String>> getFullReport() {
        Map<String, Map<String, String>> result = new TreeMap<>(); // sort by category names

        Map<String, ThreadsafeAutoCreationMap<String, AtomicLong>> countersSnapshot = counterMap.getShallowCopy();
        for (Map.Entry<String, ThreadsafeAutoCreationMap<String, AtomicLong>> category : countersSnapshot.entrySet()) {
            Map<String, AtomicLong> categoryCopy = category.getValue().getShallowCopy();
            result.put(category.getKey(), renderCategoryCounters(categoryCopy));
        }

        Map<String, ThreadsafeAutoCreationMap<String, ValueTrackerEntry>> valueTrackersSnapshot = valueTrackerMap.getShallowCopy();
        for (Entry<String, ThreadsafeAutoCreationMap<String, ValueTrackerEntry>> category : valueTrackersSnapshot.entrySet()) {
            Map<String, ValueTrackerEntry> valueTrackersCopy = category.getValue().getShallowCopy();
            Map<String, String> newMap = renderValueTrackers(valueTrackersCopy);
            Map<String, String> existingMap = result.put(category.getKey(), newMap);
            if (existingMap != null) {
                // the same category name was used for counters and value trackers; merge the two maps
                newMap.putAll(existingMap);
            }
        }

        return result;
    }

    /**
     * Gets the counter map for a single category.
     * 
     * @param category the category id
     * @return the counter map
     */
    public static Map<String, String> getReportForCategory(String category) {
        return renderCategoryCounters(counterMap.get(category).getShallowCopy());
    }

    /**
     * Fetches all data via {@link #getFullReport()} and renders it into a standard representation.
     * 
     * @return the rendered text lines
     */
    public static List<String> getFullReportAsStandardTextRepresentation() {
        List<String> output = new ArrayList<>();
        Map<String, Map<String, String>> report = StatsCounter.getFullReport();
        for (Map.Entry<String, Map<String, String>> category : report.entrySet()) {
            output.add(category.getKey());
            for (Map.Entry<String, String> entry : category.getValue().entrySet()) {
                output.add(StringUtils.format("  %s - %s", entry.getValue(), entry.getKey()));
            }
        }
        return output;
    }

    private static Map<String, String> renderCategoryCounters(Map<String, AtomicLong> categoryCopy) {
        Map<String, String> categoryResult = new TreeMap<String, String>();
        for (Map.Entry<String, AtomicLong> entry : categoryCopy.entrySet()) {
            String rendered = StringUtils.format("%,d", entry.getValue().get());
            categoryResult.put(entry.getKey(), rendered);
        }
        return categoryResult;
    }

    private static Map<String, String> renderValueTrackers(Map<String, ValueTrackerEntry> categoryCopy) {
        Map<String, String> categoryResult = new TreeMap<String, String>();
        for (Entry<String, ValueTrackerEntry> entry : categoryCopy.entrySet()) {
            String rendered = entry.getValue().render();
            categoryResult.put(entry.getKey(), rendered);
        }
        return categoryResult;
    }

}
