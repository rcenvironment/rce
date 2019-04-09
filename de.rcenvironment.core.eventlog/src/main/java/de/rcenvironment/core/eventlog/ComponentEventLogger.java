/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.eventlog;

/**
 * A logger for workflow component events. Provides additional support for stdout/stderr handling.
 * Could also provide support for state change events (like {@link WorkflowEventLogger}) if useful.
 * 
 * @author Robert Mischke
 * 
 */
public interface ComponentEventLogger extends EventLogger {

    /**
     * Reports a standard output line captured by the associated component.
     * 
     * @param line the standard output (console) line, without trailing CR or LF
     */
    void stdout(String line);

    /**
     * Reports a standard error line captured by the associated component.
     * 
     * @param line the standard error (console) line, without trailing CR or LF
     */
    void stderr(String line);
}
