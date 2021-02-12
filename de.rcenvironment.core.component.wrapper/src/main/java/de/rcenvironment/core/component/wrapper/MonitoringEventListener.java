/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper;

/**
 * Listener interface for raw stdout/stderr lines, and for lines of user information.
 * 
 * @author Robert Mischke
 * 
 */
public interface MonitoringEventListener {

    /**
     * Callback for received STDOUT lines.
     * 
     * @param line stdout line
     */
    void appendStdout(String line);

    /**
     * Callback for received STDERR lines.
     * 
     * @param line stderr line
     */
    void appendStderr(String line);

    /**
     * Callback for additional user information lines (validation, progress, ...).
     * 
     * @param line information line
     */
    void appendUserInformation(String line);
}
