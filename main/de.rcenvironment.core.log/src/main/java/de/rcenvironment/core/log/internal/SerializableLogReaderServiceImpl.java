/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.log.internal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.log.SerializableLogListener;
import de.rcenvironment.core.log.SerializableLogReaderService;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link SerializableLogReaderService}.
 * 
 * @author Doreen Seider
 * @author Mark Geiger
 * @author Robert Mischke
 */
public class SerializableLogReaderServiceImpl implements SerializableLogReaderService {

    /**
     * Implementation of the OSGi {@link LogListener} interface that asynchronously forwards each event to a given
     * {@link SerializableLogListener}.
     * 
     * @author Robert Mischke
     */
    private final class WrappingOsgiListener implements LogListener {

        private static final String ASYNC_TASK_DESCRIPTION = "Forward log event to listener";

        private final SerializableLogListener externalListener;

        // TODO review exception policy; which is better?
        private final AsyncOrderedExecutionQueue orderedExecutionQueue = new AsyncOrderedExecutionQueue(
            AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER, SharedThreadPool.getInstance());

        private WrappingOsgiListener(SerializableLogListener externalListener) {
            this.externalListener = externalListener;
        }

        @Override
        public void logged(final LogEntry entry) {
            orderedExecutionQueue.enqueue(new Runnable() {

                @Override
                @TaskDescription(ASYNC_TASK_DESCRIPTION)
                public void run() {
                    externalListener.logged(new SerializableLogEntry(
                        entry.getBundle().getSymbolicName(),
                        entry.getLevel(),
                        entry.getMessage().replaceAll("\n", SerializableLogEntry.RCE_SEPARATOR),
                        entry.getTime(),
                        entry.getException()));
                    // the @TaskDescription is not forwarded by AsyncOrderedExecutionQueue, so count a stats event for monitoring - misc_ro
                    StatsCounter.count(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME, ASYNC_TASK_DESCRIPTION);
                }
            });
        }
    }

    private static final long serialVersionUID = -7406557933348370062L;

    private LogReaderService osgiLogReaderService;

    private Map<SerializableLogListener, WrappingOsgiListener> wrappingOsgiListenersByExternalListener =
        new HashMap<SerializableLogListener, WrappingOsgiListener>();

    protected void bindLogReaderService(LogReaderService newLogReaderService) {
        osgiLogReaderService = newLogReaderService;
    }

    @Override
    @AllowRemoteAccess
    public void addLogListener(final SerializableLogListener externalListener) {
        WrappingOsgiListener wrappingOsgiListener = new WrappingOsgiListener(externalListener);
        synchronized (wrappingOsgiListenersByExternalListener) {
            wrappingOsgiListenersByExternalListener.put(externalListener, wrappingOsgiListener);
        }
        osgiLogReaderService.addLogListener(wrappingOsgiListener);
    }

    @Override
    @AllowRemoteAccess
    public void removeLogListener(SerializableLogListener listener) {
        WrappingOsgiListener wrappingOsgiListener;
        synchronized (wrappingOsgiListenersByExternalListener) {
            wrappingOsgiListener = wrappingOsgiListenersByExternalListener.get(listener);
            wrappingOsgiListenersByExternalListener.remove(listener);
        }
        // note: no need to cancel asynchronous listener events
        osgiLogReaderService.removeLogListener(wrappingOsgiListener);
    }

    @Override
    @AllowRemoteAccess
    public List<SerializableLogEntry> getLog() {
        List<SerializableLogEntry> entries = new LinkedList<SerializableLogEntry>();
        @SuppressWarnings("unchecked") Enumeration<LogEntry> retrievedEntries = osgiLogReaderService.getLog();

        while (retrievedEntries.hasMoreElements()) {
            LogEntry entry = retrievedEntries.nextElement();
            entries.add(entries.size(), new SerializableLogEntry(
                entry.getBundle().getSymbolicName(),
                entry.getLevel(),
                entry.getMessage().replaceAll("\n", SerializableLogEntry.RCE_SEPARATOR),
                entry.getTime(),
                entry.getException()));
        }
        return entries;
    }
}
