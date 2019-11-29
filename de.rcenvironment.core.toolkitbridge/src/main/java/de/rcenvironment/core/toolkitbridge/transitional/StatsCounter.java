/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.transitional;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * Provides a central mechanism for key/counter statistics. The key space is two-layered: on the top layer, arbitrary string keys define
 * categories; below that, each category holds a key-value map with string keys and a value holder. This holder can either be a "counter",
 * which represents a simple numeric (Java "long") value, or a "value event tracker", which registers events as numerical values, and
 * collects statistics about these values (for example, minimum, average, and maximum).
 * 
 * All methods are safe for concurrent usage, in the sense that no values or events will ever be lost in the case of concurrent calls. Note
 * that, however, the generated reports are not strictly serialized with regards to concurrent counter events. This has the effect that it
 * is undefined whether a counter call that is made <b>during</b> report creation will still be reflected in that report or not. In the use
 * cases this statistics service is used for, this distinction is irrelevant, and was therefore designed for minimizing overhead instead.
 * 
 * Note that this static class is a backwards compatibility wrapper for {@link StatisticsTrackerService}; new code should inject and use
 * this service directly. This static class will probably be deprecated in the future.
 * 
 * @author Robert Mischke
 */
public final class StatsCounter {

    // Note: If there is a need to disable statistics gathering in the future, the actual service could simply be substituted with a
    // NOP service
    private static final StatisticsTrackerService sharedServiceInstance = initializeInstance();

    // prevent instantiation
    private StatsCounter() {}

    /**
     * Checks whether statistics are enabled; should be used to prevent unneeded string concatenations for {@link #count(String, String)}
     * calls when statistics are disabled.
     * 
     * @return true if statistics collection is enabled
     */
    public static boolean isEnabled() {
        return true; // always enabled for now; to disable, provide a NOP StatisticsTrackerService implementation and return "false" here
    }

    /**
     * Increments the given counter by one.
     * 
     * @param category the category identifier
     * @param key the counter key within the category
     */
    public static void count(String category, String key) {
        sharedServiceInstance.getCounterCategory(category).count(key);
    }

    /**
     * Increments the given counter by an arbitrary value.
     * 
     * @param category the category identifier
     * @param key the counter key within the category
     * @param delta the number to add to the counter; negative values and zero are allowed
     */
    public static void count(String category, String key, long delta) {
        sharedServiceInstance.getCounterCategory(category).count(key, delta);
    }

    /**
     * Convenience method for the common case where occurrences of classes should be counted, with proper handling of null objects. The
     * counter key is the name of the object's class acquired using {@link Class#getName()}; for null objects, "&lt;null>" is used as the
     * counter key.
     * 
     * @param category the category identifier
     * @param object the object to count
     */
    public static void countClass(String category, Object object) {
        sharedServiceInstance.getCounterCategory(category).countClass(object);
    }

    /**
     * Registers an event's value, for example an task's duration.
     * 
     * @param category the category identifier
     * @param key the key within the category
     * @param value the value associated with an event
     */
    public static void registerValue(String category, String key, long value) {
        sharedServiceInstance.getValueEventCategory(category).registerEvent(key, value);
    }

    /**
     * Gets a map of all counter maps; the outer map holds the categories, while the inner maps hold each categories' counter entries.
     * 
     * @return the map of counter maps
     */
    public static Map<String, Map<String, String>> getFullReport() {
        return sharedServiceInstance.getFullReport();
    }

    /**
     * Gets the counter map for a single category.
     * 
     * @param category the category id
     * @return the counter map
     */
    public static Map<String, String> getReportForCategory(String category) {
        return sharedServiceInstance.getReportForCategory(category);
    }

    /**
     * Fetches all data via {@link #getFullReport()} and renders it into a standard representation.
     * 
     * @return the rendered text lines
     */
    public static List<String> getFullReportAsStandardTextRepresentation() {
        return sharedServiceInstance.getFullReportAsStandardTextRepresentation();
    }

    private static StatisticsTrackerService initializeInstance() {
        return StaticToolkitHolder.getServiceWithUnitTestFallback(StatisticsTrackerService.class);
    }
}
