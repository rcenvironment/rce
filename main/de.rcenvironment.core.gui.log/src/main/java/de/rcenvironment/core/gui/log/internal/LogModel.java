/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

import de.rcenvironment.core.communication.api.SimpleCommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.gui.log.LogListener;
import de.rcenvironment.core.log.DistributedLogReaderService;
import de.rcenvironment.core.log.SerializableLogEntry;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Provides central local access to the whole logging data (log entries, remote platforms) to
 * display.
 * 
 * @author Doreen Seider
 * @author Enrico Tappert
 */
public final class LogModel {

    private static final int LOG_POOL_SIZE = 7000;

    private static LogModel instance;

    private final List<Listener> listeners = new LinkedList<Listener>();

    private Set<NodeIdentifier> platforms;

    private NodeIdentifier currentNodeId;

    private Map<NodeIdentifier, Map<Integer, SortedSet<SerializableLogEntry>>> allLogEntries;

    private LogModel() {
        allLogEntries = new ConcurrentHashMap<NodeIdentifier, Map<Integer, SortedSet<SerializableLogEntry>>>();
        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        platforms = registryAccess.getService(WorkflowHostService.class).getWorkflowHostNodesAndSelf();
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
     * Returns a list of {@link LogEntry} for the specified {@link NodeIdentifier} set by
     * {@link LogModel#setCurrentPlatform(String)}.
     * 
     * @return {@link SortedSet} of {@link LogEntry}.
     */
    public SortedSet<SerializableLogEntry> getLogEntries() {

        SortedSet<SerializableLogEntry> entries = new TreeSet<SerializableLogEntry>();

        synchronized (allLogEntries) {
            if (currentNodeId != null && !allLogEntries.containsKey(currentNodeId)) {
                allLogEntries.put(currentNodeId, new HashMap<Integer, SortedSet<SerializableLogEntry>>());
                subscribeForNewLogEntriesAndRetrieveOldOnes(currentNodeId);
            } else {
                for (Integer level : allLogEntries.get(currentNodeId).keySet()) {
                    Map<Integer, SortedSet<SerializableLogEntry>> platformEntries = allLogEntries.get(currentNodeId);
                    SortedSet<SerializableLogEntry> levelEntries = platformEntries.get(level);
                    synchronized (levelEntries) {
                        entries.addAll(levelEntries);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * Adds a {@link LogEntry} to the whole list of the specified {@link NodeIdentifier}.
     * 
     * @param logEntry The {@link LogEntry} to add.
     */
    public void addLogEntry(SerializableLogEntry logEntry) {
        NodeIdentifier nodeId = logEntry.getPlatformIdentifer();

        synchronized (allLogEntries) {
            
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
     * @param platform The current platform identifier to set.
     */
    public void setCurrentPlatform(String platform) {
        currentNodeId = null;
        for (NodeIdentifier nodeId : platforms) {
            // search relevant platform

            // TODO searching by dynamically-generated string is brittle; rework
            if (nodeId.getAssociatedDisplayName().equals(platform)) {
                currentNodeId = nodeId;
                break;
            }
        }
    }

    /**
     * @return current platform.
     */
    public String getCurrentPlatform() {
        return currentNodeId.toString();
    }

    /**
     * Gathers all platform identifiers and provides them in array.
     * 
     * @return Array of platform identifiers.
     */
    public String[] getNodeIdsOfLogSources() {
        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        platforms = registryAccess.getService(WorkflowHostService.class).getWorkflowHostNodesAndSelf();
        platforms.toArray();
        List<String> platformsAsStringList = new ArrayList<String>();

        String localPlatform = null;
        for (NodeIdentifier pi : platforms) {
            if (new SimpleCommunicationService().isLocalNode(pi)) {
                localPlatform = pi.getAssociatedDisplayName();
            } else {
                platformsAsStringList.add(pi.getAssociatedDisplayName());
            }
        }

        Collections.sort(platformsAsStringList);

        if (localPlatform != null) {
            platformsAsStringList.add(0, localPlatform);
        }

        return platformsAsStringList.toArray(new String[platformsAsStringList.size()]);
    }

    /** Removes log entries. **/
    public void clear() {
        synchronized (allLogEntries) {
            if (currentNodeId == null) {
                for (NodeIdentifier pi : allLogEntries.keySet()) {
                    allLogEntries.get(pi).clear();
                }
            } else {
                allLogEntries.get(currentNodeId).clear();
            }
        }
    }

    private synchronized void subscribeForNewLogEntriesAndRetrieveOldOnes(final NodeIdentifier node) {

        Job job = new Job("Fetching log") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.fetchingLogs, 7);
                    monitor.worked(1);
                    // set the listener to recognize new message in future
                    LogListener logListener = new LogListener(currentNodeId);
                    ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
                    DistributedLogReaderService logReaderService = serviceRegistryAccess.getService(DistributedLogReaderService.class);
                    logReaderService.addLogListener(logListener, currentNodeId);
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
