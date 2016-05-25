/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
