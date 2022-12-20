/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.eventlog.internal;

import java.io.IOException;

import de.rcenvironment.core.eventlog.backends.api.EventLogBackend;

/**
 * A no-operation backend to disable logging, typically during unit/integration testing.
 * 
 * @author Robert Mischke
 */
public final class EventLogNOPBackend extends EventLogBackend {

    @Override
    public void close() throws IOException {}

    @Override
    public void append(EventLogEntryImpl logEntry) throws IOException {}
}
