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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Provisional solution for "verbose logging" flag handling. Could benefit from startup (or even runtime) configuration.
 * 
 * @author Robert Mischke
 */
public final class DebugSettings {

    protected static final String VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY = "rce.verboseLogging";

    // default setting: everything disabled
    private static final String DEFAULT_VERBOSE_LOGGING_PATTERN = "";

    // template/example for development settings; this is useful to enable logging for multiple development instances at once, instead of
    // adding/editing the runtime system property in several launch configurations
    // private static final String DEFAULT_VERBOSE_LOGGING_PATTERN = "*.NodePropertiesService,*.DistributedComponentKnowledgeService";

    // singleton field
    private static final DebugSettings INSTANCE = new DebugSettings();

    // the effective setting, configured from the constant and the optional system property
    private final ClassNameFilter sharedVerboseLoggingClassNameFilter;

    // mainly intended to suppress logging the flag state more than once per class; the caching is just an almost-free side effect - misc_ro
    private final Map<Class<?>, Boolean> cache = new ConcurrentHashMap<Class<?>, Boolean>();

    /**
     * Compiles a given pattern string to a regular expression and tests given class names against it. If the pattern string is empty or
     * contains errors, no class name is ever matched (ie, logging is disabled).
     * 
     * Note: This class is only "protected" (instead of private) for unit testing.
     * 
     * @author Robert Mischke
     */
    protected static final class ClassNameFilter {

        private final Pattern regexp; // null = disabled

        public ClassNameFilter(String patternString) {
            if (patternString.isEmpty()) {
                regexp = null; // disable
                return;
            }
            // TODO >6.3: check string for invalid characters
            String regexpString = "^(" + (patternString.replace(".", "\\.").replace("*", ".*").replace(",", "|")) + ")$";
            Pattern tempRegexp = null;
            try {
                tempRegexp = Pattern.compile(regexpString);
                LogFactory.getLog(getClass()).info(
                    "Using verbose logging configuration value '" + patternString + "', compiled to class name regexp '"
                        + regexpString + "'");
            } catch (PatternSyntaxException e) {
                System.err.println("Error in verbose logging configuration value '" + patternString + "', compiled to class name regexp '"
                    + regexpString + "': " + e.toString());
            }
            regexp = tempRegexp;
        }

        public boolean matches(String className) {
            if (regexp == null) {
                return false;
            } else {
                return regexp.matcher(className).matches();
            }
        }
    }

    protected DebugSettings() {
        String pattern = System.getProperty(VERBOSE_LOGGING_PATTERN_SYSTEM_PROPERTY);
        // note that "null" (undefined) is handled differently from setting an empty string, which overrides the default with "no logging"
        if (pattern == null) {
            pattern = DEFAULT_VERBOSE_LOGGING_PATTERN;
        }
        sharedVerboseLoggingClassNameFilter = new ClassNameFilter(pattern);
    }

    /**
     * @param callerClass the caller's class, so verbose logging can be controlled selectively
     * @return true if verbose logging should be enabled for this caller
     */
    public static boolean getVerboseLoggingEnabled(Class<?> callerClass) {
        return INSTANCE.getVerboseLoggingEnabledInternal(callerClass);
    }

    protected boolean getVerboseLoggingEnabledInternal(Class<?> callerClass) {
        Boolean cached = cache.get(callerClass);
        if (cached != null) {
            return cached;
        }
        boolean enableLogging = false;
        String className = callerClass.getName();

        enableLogging = sharedVerboseLoggingClassNameFilter.matches(className);

        cache.put(callerClass, enableLogging); // thread-safe map
        LogFactory.getLog(DebugSettings.class).debug(
            StringUtils.format("Set 'verbose logging' flag to %s for %s", enableLogging, className));
        return enableLogging;
    }
}
