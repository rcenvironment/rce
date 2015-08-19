/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.LogFactory;

/**
 * Provisional solution for "verbose logging" flag handling. Could benefit from startup (or even runtime) configuration.
 * 
 * @author Robert Mischke
 */
public final class DebugSettings {

    // default setting: everything disabled
    private static final String[] CLASS_NAME_SUBSTRING_TRIGGERS = new String[] {};

    // development settings
    // private static final String[] CLASS_NAME_SUBSTRING_TRIGGERS = new String[] { ".NodePropertiesService",
    // ".DistributedComponentKnowledgeService" };

    // mainly intended to suppress logging the flag state more than once per class; the caching is just an almost-free side effect - misc_ro
    private static final Map<Class<?>, Boolean> CACHE = new ConcurrentHashMap<Class<?>, Boolean>();

    private DebugSettings() {}

    /**
     * @param callerClass the caller's class, so verbose logging can be controlled selectively
     * @return true if verbose logging should be enabled for this caller
     */
    public static boolean getVerboseLoggingEnabled(Class<?> callerClass) {
        Boolean cached = CACHE.get(callerClass);
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        String className = callerClass.getName();

        for (String trigger : CLASS_NAME_SUBSTRING_TRIGGERS) {
            if (className.contains(trigger)) {
                result = true;
                break;
            }
        }
        CACHE.put(callerClass, result); // thread-safe map
        LogFactory.getLog(DebugSettings.class).debug(String.format("Set 'verbose logging' flag to %s for %s", result, className));
        return result;
    }
}
