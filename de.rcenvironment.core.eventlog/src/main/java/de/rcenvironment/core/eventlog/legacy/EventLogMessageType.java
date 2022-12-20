/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.legacy;

/**
 * The internal type marker for event log messages.
 * 
 * @author Robert Mischke
 */

public enum EventLogMessageType {
    /**
     * @see EventLogger#info(boolean, String, Object...)
     */
    INFO,
    /**
     * @see EventLogger#warn(boolean, String, Object...)
     */
    WARNING,
    /**
     * @see EventLogger#error(boolean, String, Object...)
     */
    ERROR,
    /**
     * @see EventLogger#debug(String, Object...)
     */
    DEBUG_DEFAULT,
    /**
     * @see EventLogger#debugVerbose(String, Object...)
     */
    DEBUG_VERBOSE,
    /**
     * @see ComponentEventLogger#stdout(String)
     */
    STDOUT,
    /**
     * @see ComponentEventLogger#stderr(String)
     */
    STDERR;
}
