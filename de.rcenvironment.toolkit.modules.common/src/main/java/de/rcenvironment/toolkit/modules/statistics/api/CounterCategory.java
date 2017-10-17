/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.statistics.api;

/**
 * A category of counters.
 * 
 * As an example, a counter category could be "Incoming remote message calls", which would then hold individual counters for each available
 * method.
 * 
 * @author Robert Mischke
 * 
 */
public interface CounterCategory {

    /**
     * @return true if this category is enabled under the current global filter level; calling code should use this method to avoid costly
     *         preparations for calls that will not be registered anyway (which is the case if this method returns "false")
     */
    boolean isEnabled();

    /**
     * Increments the given counter by one.
     * 
     * @param key the counter key within the category
     */
    void count(String key);

    /**
     * Increments the given counter by an arbitrary value.
     * 
     * @param key the counter key within the category
     * @param delta the number to add to the counter; negative values and zero are allowed
     */
    void count(String key, long delta);

    /**
     * Convenience method for the common case where occurrences of classes should be counted, with proper handling of null objects. The
     * counter key is the name of the object's class acquired using {@link Class#getName()}; for null objects, "<null>" is used as the
     * counter key.
     * 
     * @param object the object to count
     */
    void countClass(Object object);

    /**
     * Convenience method for counting the stacktraces a certain code point was reached by. Equivalent to
     * <code>count(stacktraceBuilder.getSingleLineStacktrace())</code> using the module's configured default
     * {@link CompactStacktraceBuilder}.
     */
    void countStacktrace();
}
