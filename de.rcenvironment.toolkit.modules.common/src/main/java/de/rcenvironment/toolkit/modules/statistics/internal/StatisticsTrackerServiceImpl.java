/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.statistics.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.utils.ThreadsafeAutoCreationMap;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionContributor;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;
import de.rcenvironment.toolkit.modules.statistics.api.ValueEventCategory;
import de.rcenvironment.toolkit.modules.statistics.setup.StatisticsModuleConfiguration;
import de.rcenvironment.toolkit.modules.statistics.utils.CompactStacktraceBuilder;
import de.rcenvironment.toolkit.utils.internal.StringUtils;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;

/**
 * Default {@link StatisticsTrackerService} implementation.
 * 
 * @author Robert Mischke
 */
public class StatisticsTrackerServiceImpl implements StatisticsTrackerService {

    /**
     * Default {@link CounterCategory} implementation.
     * 
     * @author Robert Mischke
     */
    public static final class CounterCategoryImpl implements CounterCategory {

        private final ThreadsafeAutoCreationMap<String, AtomicLong> counters = new ThreadsafeAutoCreationMap<String, AtomicLong>() {

            @Override
            protected AtomicLong createNewEntry(String key) {
                return new AtomicLong();
            }
        };

        private final CompactStacktraceBuilder defaultCompactStacktraceBuilder;

        public CounterCategoryImpl(CompactStacktraceBuilder defaultCompactStacktraceBuilder) {
            this.defaultCompactStacktraceBuilder = defaultCompactStacktraceBuilder;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void count(String key) {
            counters.get(key).incrementAndGet();
        }

        @Override
        public void count(String key, long delta) {
            counters.get(key).addAndGet(delta);
        }

        @Override
        public void countClass(Object object) {
            if (object != null) {
                count(object.getClass().getName());
            } else {
                count("<null>");
            }
        }

        @Override
        public void countStacktrace() {
            count(defaultCompactStacktraceBuilder.getSingleLineStacktrace(1)); // "1" = skip this service method itself
        }

    }

    /**
     * Default {@link ValueEventCategory} implementation.
     * 
     * @author Robert Mischke
     */
    public static final class ValueEventCategoryImpl implements ValueEventCategory {

        private final ThreadsafeAutoCreationMap<String, ValueTrackerEntry> valueTrackerEntries =
            new ThreadsafeAutoCreationMap<String, StatisticsTrackerServiceImpl.ValueTrackerEntry>() {

                @Override
                protected ValueTrackerEntry createNewEntry(String key) {
                    return new ValueTrackerEntry();
                }
            };

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void registerEvent(String key, long value) {
            valueTrackerEntries.get(key).register(value);
        }
    }

    /**
     * The {@link CounterCategory} implementation returned for disabled categories.
     * 
     * @author Robert Mischke
     */
    private static final class NoOperationCounterCategory implements CounterCategory {

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void countClass(Object object) {}

        @Override
        public void count(String key, long delta) {}

        @Override
        public void count(String key) {}

        @Override
        public void countStacktrace() {}
    }

    /**
     * The {@link ValueEventCategory} implementation returned for disabled categories.
     * 
     * @author Robert Mischke
     */
    private static final class NoOperationValueEventCategory implements ValueEventCategory {

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void registerEvent(String key, long value) {}
    }

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

    private static final CounterCategory NO_OPERATION_COUNTER = new NoOperationCounterCategory();

    private static final ValueEventCategory NO_OPERATION_EVENT_TRACKER = new NoOperationValueEventCategory();

    private static ThreadsafeAutoCreationMap<String, CounterCategoryImpl> counterMap;

    private static ThreadsafeAutoCreationMap<String, ValueEventCategoryImpl> valueTrackerMap;

    private final Set<String> categoriesAlreadyLoggedAsDisabled = new HashSet<>();

    private final StatisticsFilterLevel globalFilterLevel;

    private final Log log = LogFactory.getLog(getClass());

    public StatisticsTrackerServiceImpl(StatisticsModuleConfiguration moduleConfiguration,
        StatusCollectionRegistry statusCollectionRegistry) {

        final CompactStacktraceBuilder defaultCompactStacktraceBuilder = moduleConfiguration.getDefaultCompactStacktraceBuilder();

        globalFilterLevel = moduleConfiguration.getStatisticsFilterLevel();

        counterMap = new ThreadsafeAutoCreationMap<String, CounterCategoryImpl>() {

            @Override
            protected CounterCategoryImpl createNewEntry(String key) {
                return new CounterCategoryImpl(defaultCompactStacktraceBuilder);
            }
        };
        valueTrackerMap = new ThreadsafeAutoCreationMap<String, ValueEventCategoryImpl>() {

            @Override
            protected ValueEventCategoryImpl createNewEntry(String key) {
                return new ValueEventCategoryImpl();
            }
        };

        statusCollectionRegistry.addContributor(new StatusCollectionContributor() {

            @Override
            public String getStandardDescription() {
                return "Statistics";
            }

            @Override
            public void printDefaultStateInformation(TextLinesReceiver receiver) {
                receiver.addLines(getFullReportAsStandardTextRepresentation(""));
            }

            @Override
            public String getUnfinishedOperationsDescription() {
                return null;
            }

            @Override
            public void printUnfinishedOperationsInformation(TextLinesReceiver receiver) {}
        });
    }

    @Override
    public CounterCategory getCounterCategory(String name) {
        return getCounterCategory(name, StatisticsFilterLevel.RELEASE);
    }

    @Override
    public CounterCategory getCounterCategory(String categoryName, StatisticsFilterLevel filterLevel) {
        if (filterLevel.compareTo(globalFilterLevel) <= 0) {
            return counterMap.get(categoryName); // automatically creates new entries
        } else {
            synchronized (categoriesAlreadyLoggedAsDisabled) {
                // only log each category once if it is requested repeatedly, e.g. in the constructor of helper classes
                if (!categoriesAlreadyLoggedAsDisabled.contains(categoryName)) {
                    log.debug(StringUtils
                        .format("Returning a disabled receiver for category '%s' as it was requested "
                            + "with level %s while the global level is %s", categoryName, filterLevel, globalFilterLevel));
                    categoriesAlreadyLoggedAsDisabled.add(categoryName); // note: shared with value events
                }
            }
            return NO_OPERATION_COUNTER;
        }
    }

    @Override
    public ValueEventCategory getValueEventCategory(String name) {
        return getValueEventCategory(name, StatisticsFilterLevel.RELEASE);
    }

    @Override
    public ValueEventCategory getValueEventCategory(String categoryName, StatisticsFilterLevel filterLevel) {
        if (filterLevel.compareTo(globalFilterLevel) <= 0) {
            return valueTrackerMap.get(categoryName); // automatically creates new entries
        } else {
            synchronized (categoriesAlreadyLoggedAsDisabled) {
                // only log each category once if it is requested repeatedly, e.g. in the constructor of helper classes
                if (!categoriesAlreadyLoggedAsDisabled.contains(categoryName)) {
                    log.debug(StringUtils
                        .format("Returning a disabled receiver for category '%s' as it was requested "
                            + "with level %s while the global level is %s", categoryName, filterLevel, globalFilterLevel));
                    categoriesAlreadyLoggedAsDisabled.add(categoryName); // note: shared with counters
                }
            }
            return NO_OPERATION_EVENT_TRACKER;
        }
    }

    @Override
    public Map<String, Map<String, String>> getFullReport() {
        Map<String, Map<String, String>> result = new TreeMap<>(); // sort by category names

        Map<String, CounterCategoryImpl> countersSnapshot = counterMap.getShallowCopy();
        for (Map.Entry<String, CounterCategoryImpl> category : countersSnapshot.entrySet()) {
            // TODO refactor
            Map<String, AtomicLong> categoryCopy = category.getValue().counters.getShallowCopy();
            final Map<String, String> renderedCategoryCounters = renderCategoryCounters(categoryCopy);
            if (!renderedCategoryCounters.isEmpty()) {
                result.put(category.getKey(), renderedCategoryCounters);
            }
        }

        Map<String, ValueEventCategoryImpl> valueTrackersSnapshot = valueTrackerMap.getShallowCopy();
        for (Entry<String, ValueEventCategoryImpl> category : valueTrackersSnapshot.entrySet()) {
            // TODO refactor
            Map<String, ValueTrackerEntry> valueTrackersCopy = category.getValue().valueTrackerEntries.getShallowCopy();
            Map<String, String> renderedValueTrackers = renderValueTrackers(valueTrackersCopy);
            if (!renderedValueTrackers.isEmpty()) {
                Map<String, String> existingMap = result.put(category.getKey(), renderedValueTrackers);
                if (existingMap != null) {
                    // the same category name was used for counters and value trackers; merge the two maps
                    renderedValueTrackers.putAll(existingMap);
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, String> getReportForCategory(String category) {
        return renderCategoryCounters(counterMap.get(category).counters.getShallowCopy());
    }

    @Override
    public List<String> getFullReportAsStandardTextRepresentation() {
        return getFullReportAsStandardTextRepresentation("");
    }

    @Override
    public List<String> getFullReportAsStandardTextRepresentation(String linePrefix) {
        List<String> output = new ArrayList<>();
        Map<String, Map<String, String>> report = getFullReport();
        for (Map.Entry<String, Map<String, String>> category : report.entrySet()) {
            output.add(StringUtils.format("%s%s", linePrefix, category.getKey()));
            for (Map.Entry<String, String> entry : category.getValue().entrySet()) {
                output.add(StringUtils.format("%s  %s - %s", linePrefix, entry.getValue(), entry.getKey()));
            }
        }
        return output;
    }

    private Map<String, String> renderCategoryCounters(Map<String, AtomicLong> categoryCopy) {
        Map<String, String> categoryResult = new TreeMap<String, String>();
        for (Map.Entry<String, AtomicLong> entry : categoryCopy.entrySet()) {
            String rendered = StringUtils.format("%,d", entry.getValue().get());
            categoryResult.put(entry.getKey(), rendered);
        }
        return categoryResult;
    }

    private Map<String, String> renderValueTrackers(Map<String, ValueTrackerEntry> categoryCopy) {
        Map<String, String> categoryResult = new TreeMap<String, String>();
        for (Entry<String, ValueTrackerEntry> entry : categoryCopy.entrySet()) {
            String rendered = entry.getValue().render();
            categoryResult.put(entry.getKey(), rendered);
        }
        return categoryResult;
    }

}
