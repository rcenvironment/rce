/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal;

import java.io.IOException;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.eventlog.api.EventLog;
import de.rcenvironment.core.eventlog.backends.api.EventLogBackend;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A backend that logs all events as warnings. This is set as the default backend to log any events that are triggered at runtime before
 * initialization, as these would indicate a startup ordering problem. During unit/integration testing, this is typically left in as the
 * default backend; therefore, all events triggered during tests will be logged by this backend, and can be safely ignored.
 * 
 * @author Robert Mischke
 */
public final class EventLogFallbackBackend extends EventLogBackend {

    @Override
    public void close() throws IOException {}

    @Override
    public void append(EventLogEntryImpl logEntry) throws IOException {
        LogFactory.getLog(EventLog.class).warn(StringUtils.format("Received an event while no log receiver is configured; "
            + "this is normal during unit/integration testing. Data: %s, %s, %s", logEntry.getTimestamp(), logEntry.getEventType().getId(),
            ((EventLogEntryImpl) logEntry).getAttributeDataAsConpactJsonString()));
    }
}
