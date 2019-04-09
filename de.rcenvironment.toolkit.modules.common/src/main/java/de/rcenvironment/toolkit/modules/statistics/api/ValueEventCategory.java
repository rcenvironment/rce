/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.statistics.api;

/**
 * A category of value events. As an example, a value event category could be "Database call durations", which would then track/count
 * individual value events corresponding to the various database calls.
 * 
 * @author Robert Mischke
 * 
 */
public interface ValueEventCategory {

    /**
     * @return true if this category is enabled under the current global filter level; calling code should use this method to avoid costly
     *         preparations for calls that will not be registered anyway (which is the case if this method returns "false")
     */
    boolean isEnabled();

    /**
     * Registers an event's value, for example an task's duration.
     * 
     * @param key the key within the category
     * @param value the value associated with an event
     */
    void registerEvent(String key, long value);
}
