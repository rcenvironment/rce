/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.gui.log.LogListener;
import de.rcenvironment.core.log.DistributedLogReaderService;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Provides central local access to the whole logging data (log entries, remote platforms) to display.
 * 
 * @author Doreen Seider
 * @author Enrico Tappert
 * @author Robert Mischke
 */
public final class LogModel {

    private static final int LOG_POOL_SIZE = 7000;

    private static LogModel instance;

    private final List<Listener> listeners = new LinkedList<Listener>();

    private Set<InstanceNodeSessionId> currentWorkflowHostsAndSelf;

    private InstanceNodeSessionId selectedLogSource;

    private Map<InstanceNodeSessionId, Map<Integer, SortedSet<SerializableLogEntry>>> allLogEntries;

    private final WorkflowHostService workflowHostService;

    private final PlatformService platformService;

    private final DistributedLogReaderService logReaderService;

    private LogModel() {
        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        workflowHostService = registryAccess.getService(WorkflowHostService.class);
        platformService = registryAccess.getService(PlatformService.class);
        logReaderService = registryAccess.getService(DistributedLogReaderService.class);

        allLogEntries = new ConcurrentHashMap<InstanceNodeSessionId, Map<Integer, SortedSet<SerializableLogEntry>>>();
        currentWorkflowHostsAndSelf = workflowHostService.getWorkflowHostNodesAndSelf();
    }

    /**
     * Singleton to enable central access to the data.
     * 
     * @return Singleton instance.
     */
    public static synchronized LogModel getInstance() {
        if (null == instance) {
            instance = new LogModel();
        }
        return instance;
    }

    /**
     * Returns a list of {@link LogEntry} for the specified {@link InstanceNodeSessionId} set by
     * {@link LogModel#setSelectedLogSource(String)}.
     * 
     * @return {@link SortedSet} of {@link LogEntry}.
     */
    public SortedSet<SerializableLogEntry> getLogEntries() {

        SortedSet<SerializableLogEntry> entries = new TreeSet<SerializableLogEntry>();

        synchronized (allLogEntries) {
            if (selectedLogSource != null && !allLogEntries.containsKey(selectedLogSource)) {
                allLogEntries.put(selectedLogSource, new HashMap<Integer, SortedSet<SerializableLogEntry>>());
                // the log model of this instance does not contain any entries of remote instance until this subscribe is called for the
                // first time
                subscribeForNewLogEntriesAndRetrieveOldOnes(selectedLogSource);
            } else {
                for (Integer level : allLogEntries.get(selectedLogSource).keySet()) {
                    if (level != LogService.LOG_DEBUG) {
                        Map<Integer, SortedSet<SerializableLogEntry>> platformEntries = allLogEntries.get(selectedLogSource);
                        SortedSet<SerializableLogEntry> levelEntries = platformEntries.get(level);
                        synchronized (levelEntries) {
                            entries.addAll(levelEntries);
                        }
                    }
                }
            }
        }
        return entries;
    }

    /**
     * Adds a {@link LogEntry} to the whole list of the specified {@link InstanceNodeSessionId}.
     * 
     * @param logEntry The {@link LogEntry} to add.
     */
    public void addLogEntry(SerializableLogEntry logEntry) {
        InstanceNodeSessionId nodeId = logEntry.getPlatformIdentifer();

        synchronized (allLogEntries) {

            // If a node ID was removed from the allLogEntries map during a call of updateListOfLogSources, but we still receive a log entry
            // for this node, it can be ignored. The ignored log entry is not relevant for now (since the corresponding node is offline) and
            // the entry will be retrieved from the source instance again if the instance becomes available next time (and is selected as
            // log source and getLogEntries is called).
            if (allLogEntries.get(nodeId) == null) {
                return;
            }

            if (!allLogEntries.get(nodeId).containsKey(logEntry.getLevel())) {
                allLogEntries.get(nodeId).put(logEntry.getLevel(),
                    Collections.synchronizedSortedSet(new TreeSet<SerializableLogEntry>()));
            }

            SortedSet<SerializableLogEntry> logEntries = allLogEntries.get(nodeId).get(logEntry.getLevel());
            while (logEntries.size() >= LOG_POOL_SIZE) {
                final SerializableLogEntry logEntryToRemove = logEntries.first();
                logEntries.remove(logEntryToRemove);
                for (final Listener listener : listeners) {
                    listener.handleLogEntryRemoved(logEntryToRemove);
                }
            }
            if (logEntries.add(logEntry)) {
                for (final Listener listener : listeners) {
                    listener.handleLogEntryAdded(logEntry);
                }
            }
        }

    }

    /**
     * Lets identify the current platform for which logging messages has to be shown.
     * 
     * @param nodeId The current platform identifier to set.
     */
    public synchronized void setSelectedLogSource(InstanceNodeSessionId nodeId) {
        selectedLogSource = nodeId;
    }

    /**
     * @return current platform.
     */
    public synchronized InstanceNodeSessionId getCurrentLogSource() {
        return selectedLogSource;
    }

    /**
     * Gathers all platform identifiers and provides them in array.
     * 
     * @return Array of platform identifiers.
     */
    public synchronized List<InstanceNodeSessionId> updateListOfLogSources() {

        Set<InstanceNodeSessionId> newWorkflowHostNodesAndSelf = workflowHostService.getWorkflowHostNodesAndSelf();

        // remove all entries for nodes that are not reachable anymore
        Set<InstanceNodeSessionId> nodeIdsRemoved = new HashSet<>(currentWorkflowHostsAndSelf);
        nodeIdsRemoved.removeAll(newWorkflowHostNodesAndSelf);
        for (InstanceNodeSessionId nodeIdRemoved : nodeIdsRemoved) {
            allLogEntries.remove(nodeIdRemoved);
        }

        currentWorkflowHostsAndSelf = newWorkflowHostNodesAndSelf;

        // gather all log sources ...
        List<InstanceNodeSessionId> logSources = new ArrayList<>();

        InstanceNodeSessionId localNodeId = null;
        for (InstanceNodeSessionId nodeId : currentWorkflowHostsAndSelf) {
            // ... but skip the current instance ...
            if (platformService.matchesLocalInstance(nodeId)) {
                localNodeId = nodeId;
            } else {
                logSources.add(nodeId);
            }
        }

        Collections.sort(logSources, new Comparator<InstanceNodeSessionId>() {

            @Override
            public int compare(InstanceNodeSessionId o1, InstanceNodeSessionId o2) {
                return o1.getAssociatedDisplayName().compareTo(o2.getAssociatedDisplayName());
            }
        });

        // ... and add it to the front of the log sources after they have been sorted
        if (localNodeId != null) {
            logSources.add(0, localNodeId);
        }

        return logSources;
    }

    /** Removes log entries. **/
    public void clear() {
        synchronized (allLogEntries) {
            if (selectedLogSource == null) {
                for (InstanceNodeSessionId pi : allLogEntries.keySet()) {
                    allLogEntries.get(pi).clear();
                }
            } else {
                allLogEntries.get(selectedLogSource).clear();
            }
        }
    }

    private synchronized void subscribeForNewLogEntriesAndRetrieveOldOnes(final InstanceNodeSessionId node) {

        Job job = new Job("Fetching log") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.fetchingLogs, 7);
                    monitor.worked(1);
                    // set the listener to recognize new message in future
                    LogListener logListener = new LogListener(selectedLogSource);
                    logReaderService.addLogListener(logListener, selectedLogSource);
                    monitor.worked(1);
                    List<SerializableLogEntry> retrievedLogEntries = logReaderService.getLog(node);
                    monitor.worked(2);
                    Map<Integer, SortedSet<SerializableLogEntry>> logEntries = new ConcurrentHashMap<Integer,
                        SortedSet<SerializableLogEntry>>();
                    for (SerializableLogEntry retrievedLogEntry : retrievedLogEntries) {
                        if (!logEntries.containsKey(retrievedLogEntry.getLevel())) {
                            logEntries.put(retrievedLogEntry.getLevel(),
                                Collections.synchronizedSortedSet(new TreeSet<SerializableLogEntry>()));
                        }
                        retrievedLogEntry.setPlatformIdentifer(node);
                        logEntries.get(retrievedLogEntry.getLevel()).add(retrievedLogEntry);
                    }
                    monitor.worked(1);
                    monitor.worked(1);
                    for (Set<SerializableLogEntry> entries : logEntries.values()) {
                        for (SerializableLogEntry entry : entries) {
                            addLogEntry(entry);
                        }
                    }
                    monitor.worked(1);
                    return Status.OK_STATUS;
                } finally {
                    monitor.done();
                }
            };
        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * Adds a {@link Listener}.
     * 
     * @param listener the {@link Listener} to add
     */
    public void addListener(final Listener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a {@link Listener}.
     * 
     * @param listener the {@link Listener} to remove
     */
    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Listener interface to listen to {@link LogModel} changes.
     * 
     * @author Christian Weiss
     */
    public interface Listener {

        /**
         * Handle the addition of a {@link LogEntry}.
         * 
         * @param logEntry the newly added {@link LogEntry}
         */
        void handleLogEntryAdded(SerializableLogEntry logEntry);

        /**
         * Handle the removal of a {@link LogEntry}.
         * 
         * @param logEntry the removed {@link LogEntry}
         */
        void handleLogEntryRemoved(SerializableLogEntry logEntry);

    }

}
