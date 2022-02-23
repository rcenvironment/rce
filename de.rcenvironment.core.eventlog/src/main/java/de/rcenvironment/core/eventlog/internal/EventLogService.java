/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal;

import de.rcenvironment.core.eventlog.internal.impl.EventLogMessage;

/**
 * The internal interface for log event dispatch services.
 * 
 * @author Robert Mischke
 * 
 */
public interface EventLogService {

    /**
     * Handles a single {@link EventLogMessage}.
     * 
     * @param message the message to handle
     */
    void dispatchMessage(EventLogMessage message);

}
