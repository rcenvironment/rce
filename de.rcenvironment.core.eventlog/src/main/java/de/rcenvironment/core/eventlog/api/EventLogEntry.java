/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.api;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an event log entry for application code. In particular, this interface is used to attach attribute data to an event before it
 * is processed/logged.
 *
 * @author Robert Mischke
 */
public interface EventLogEntry {

    EventLogEntry set(String key, String value);

    /**
     * Convenience method for adding long values. Note that these are (currently) not logged as "native" JSON integers, but converted to
     * string values.
     * 
     * @param key the string key
     * @param value the long value to be converted to and used as a string value
     * 
     * @return this instance (for command chaining)
     */
    EventLogEntry set(String key, long value);

    /**
     * Convenience method for adding integer values. Note that these are (currently) not logged as "native" JSON integers, but converted to
     * string values.
     * 
     * @param key the string key
     * @param value the integer value to be converted to and used as a string value
     * 
     * @return this instance (for command chaining)
     */
    EventLogEntry set(String key, int value);

    /**
     * Convenience method for adding a map of values. All provided entries are added as they are; existing entries with matching keys are
     * silently replaced.
     * 
     * @param dataMap the map of entries to add to the log entry
     * 
     * @return this instance (for command chaining)
     */
    EventLogEntry setAll(Map<String, String> dataMap);

    /**
     * @return the selected {@link EventType}
     */
    EventType getEventType();

    /**
     * Convenience shortcut for getEventType().getId().
     * 
     * @return equivalent to getEventType().getId()
     */
    String getEventTypeId();

    /**
     * Convenience shortcut for getEventType().getTitle().
     * 
     * @return equivalent to getEventType().getTitle()
     */
    String getEventTypeTitle();

    /**
     * @return the event's UTC-like timestamp as a Java {@link Instant}
     */
    Instant getTimestamp();

    /**
     * @return the map of attribute key-value pairs; never null, even if a null map was provided to the constructor
     */
    Map<String, String> getAttributeData();

}
