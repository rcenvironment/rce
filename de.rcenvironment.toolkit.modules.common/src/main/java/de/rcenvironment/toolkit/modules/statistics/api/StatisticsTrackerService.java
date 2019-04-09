/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.statistics.api;

import java.util.List;
import java.util.Map;

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
 * @author Robert Mischke
 */
public interface StatisticsTrackerService {

    /**
     * Retrieves a reference to a category of counters, which can then be used to send the actual counter calls. The implicit filter level
     * is {@link StatisticsFilterLevel#RELEASE}.
     * 
     * @param categoryName the display name of the category, which also serves as its unique key at the moment
     * @return the new or cached reference
     */
    CounterCategory getCounterCategory(String categoryName);

    /**
     * Retrieves a reference to a category of counters, which can then be used to send the actual counter calls. The "filter level"
     * parameter is compared to a globally defined filter level; if the category's level is below (e.g. DEBUG at a global filter level of
     * RELEASE), the returned category object is disabled. Code calling methods on the returned category object should consider checking
     * {@link CounterCategory#isEnabled()} before performing time-consuming preparations, similar to the "isDebugEnabled()" call in typical
     * log frameworks.
     * 
     * @param categoryName the display name of the category, which also serves as its unique key at the moment
     * @param filterLevel the minimum filter level to enable the returned category
     * @return the new or cached reference
     */
    CounterCategory getCounterCategory(String categoryName, StatisticsFilterLevel filterLevel);

    /**
     * Retrieves a reference to a category of value event trackers, which can then be used to send the actual event tracking calls. The
     * implicit filter level is {@link StatisticsFilterLevel#RELEASE}.
     * 
     * @param categoryName the display name of the category, which also serves as its unique key at the moment
     * @return the new or cached reference
     */
    ValueEventCategory getValueEventCategory(String categoryName);

    /**
     * Retrieves a reference to a category of value event trackers, which can then be used to send the actual event tracking calls. The
     * "filter level" parameter is compared to a globally defined filter level; if the category's level is below (e.g. DEBUG at a global
     * filter level of RELEASE), the returned category object is disabled. Code calling methods on the returned category object should
     * consider checking {@link CounterCategory#isEnabled()} before performing time-consuming preparations, similar to the
     * "isDebugEnabled()" call in typical log frameworks.
     * 
     * @param categoryName the display name of the category, which also serves as its unique key at the moment
     * @param filterLevel the minimum filter level to enable the returned category
     * @return the new or cached reference
     */
    ValueEventCategory getValueEventCategory(String categoryName, StatisticsFilterLevel filterLevel);

    /**
     * Gets the counter map for a single category.
     * 
     * TODO document counter string content
     * 
     * @param categoryName the category's id
     * @return the counter map
     */
    Map<String, String> getReportForCategory(String categoryName);

    /**
     * Gets a map of all counter maps; the outer map holds the categories, while the inner maps hold each categories' counter entries.
     * 
     * TODO document counter string content
     * 
     * @return the map of counter maps
     */
    Map<String, Map<String, String>> getFullReport();

    /**
     * Fetches all data via {@link #getFullReport()} and renders it into a standard representation.
     * 
     * @return the rendered text lines
     */
    List<String> getFullReportAsStandardTextRepresentation();

    /**
     * Fetches all data via {@link #getFullReport()} and renders it into a standard representation.
     * 
     * @param linePrefix a string to prefix every line witih
     * @return the rendered text lines
     */
    List<String> getFullReportAsStandardTextRepresentation(String linePrefix);

}
