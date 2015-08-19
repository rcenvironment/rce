/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleModelSnapshot;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowFilter;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowLogService;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowModelService;
import de.rcenvironment.core.component.workflow.execution.api.GenericSubscriptionManager;
import de.rcenvironment.core.component.workflow.execution.impl.ConsoleSubscriptionEventProcessor;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Default {@link ConsoleRowModelService} implementation.
 * 
 * @author Doreen Seider (initial version)
 * @author Robert Mischke (current)
 */
public class ConsoleRowModelServiceImpl implements ConsoleRowModelService, ConsoleRowProcessor {

    /**
     * The maximum total number of rows to keep.
     */
    // TODO check limiting details; sufficient or add more detail? count chars instead?
    private static final int MAX_UNFILTERED_ROWS_RETENTION = 35000;

    /**
     * The maximum number of (filtered) rows to return in a snapshot.
     */
    private static final int MAX_SNAPSHOT_SIZE = 25000;

    // TODO make non-static
    private static GenericSubscriptionManager subscriptionManager;

    // TODO make non-static
    private static CountDownLatch initialSubscriptionLatch;

    private Deque<ConsoleRow> allRows;

    /**
     * Note: The current concept is based on a single client view using this model; if required, this could be changed to a map of
     * registered filters.
     */
    private ConsoleRowFilter currentFilter;

    private Deque<ConsoleRow> filteredRows;

    private SortedSet<String> workflows;

    private SortedSet<String> components;

    /**
     * Incremented on each model change; used for efficient change testing. Initialized with "+1" so a query with INITIAL_SEQUENCE_ID as
     * parameter will always signal an initial "change".
     * 
     * May be changed in the future to filter-specific sequence ids.
     */
    private int sequenceIdCounter = INITIAL_SEQUENCE_ID + 1;

    private int filteredListLastChanged;

    private int workflowListLastChanged;

    private int componentListLastChanged;

    private ConsoleRowLogService consoleRowLogService;

    private WorkflowHostService workflowHostService;
    
    private CommunicationService communicationService;

    public ConsoleRowModelServiceImpl() {
        // initialize internal model
        resetModel();
        // set default filter
        currentFilter = new ConsoleRowFilter();
        initialSubscriptionLatch = new CountDownLatch(1);
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {
        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription("Initial ConsoleRow model subscriptions")
            public void run() {
                subscriptionManager = new GenericSubscriptionManager(new ConsoleSubscriptionEventProcessor(
                    ConsoleRowModelServiceImpl.this, consoleRowLogService), communicationService, workflowHostService);
                subscriptionManager.updateSubscriptions(new String[] { ConsoleRow.NOTIFICATION_SUFFIX });
                initialSubscriptionLatch.countDown();
            }
        });
    }

    @Override
    public void ensureConsoleCaptureIsInitialized() throws InterruptedException {
        initialSubscriptionLatch.await();
    }

    /**
     * Updates subscriptions to known server instances.
     */
    public synchronized void updateSubscriptions() {
        try {
            initialSubscriptionLatch.await();
        } catch (InterruptedException e) {
            // TODO better handling?
            throw new RuntimeException("Interrupted while waiting for initial subscriptions to complete", e);
        }
        subscriptionManager.updateSubscriptions(new String[] { ConsoleRow.NOTIFICATION_SUFFIX });
    }

    /**
     * Returns a new {@link ConsoleModelSnapshot} of the current model state if the model was modified since the given sequence id. The
     * typical source of this sequence id is calling getSequenceId() on a previously returned snapshot. If no change has occured, this
     * method returns null.
     * 
     * @param sequenceId the last sequence id known to the caller
     * @return a new model snapshot, or null if no change has occured since the given sequence id
     */
    @Override
    public synchronized ConsoleModelSnapshot getSnapshotIfModifiedSince(int sequenceId) {

        // any change at all?
        if (sequenceId == sequenceIdCounter) {
            return null;
        }

        // any relevant change?
        if (sequenceId >= filteredListLastChanged && sequenceId >= workflowListLastChanged && sequenceId >= componentListLastChanged) {
            return null;
        }

        // create & return snapshot object
        ConsoleModelSnapshotImpl snapshot = new ConsoleModelSnapshotImpl();
        if (filteredListLastChanged > sequenceId) {
            // if modifed, set a copy of the filtered list
            snapshot.setFilteredRows(new ArrayList<ConsoleRow>(filteredRows));
        }
        // if modified, set a copy of the workflow list
        if (workflowListLastChanged > sequenceId) {
            snapshot.setWorkflowList(new ArrayList<String>(workflows));
        }
        // if modified, set a copy of the component list
        if (componentListLastChanged > sequenceId) {
            snapshot.setComponentList(new ArrayList<String>(components));
        }
        snapshot.setSequenceId(sequenceIdCounter);

        return snapshot;
    }

    /**
     * Batch version of {@link #addConsoleRow(ConsoleRow)} to reduce synchronization overhead.
     * 
     * @param rows the list of {@link ConsoleRow}s to add
     */
    @Override
    public synchronized void processConsoleRows(List<ConsoleRow> rows) {
        sequenceIdCounter++;
        for (ConsoleRow row : rows) {
            if (accept(row)) {
                // add unfiltered
                allRows.addLast(row);
                // add to filtered list if filter matches
                if (currentFilter.accept(row)) {
                    filteredRows.addLast(row);
                    filteredListLastChanged = sequenceIdCounter;
                }
                // add to the set of workflows
                // note: currently, workflows are only purged on clearAll
                if (workflows.add(row.getWorkflowName())) {
                    workflowListLastChanged = sequenceIdCounter;
                }
                // add to the set of components
                // note: currently, components are only purged on clearAll
                if (components.add(row.getComponentName())) {
                    componentListLastChanged = sequenceIdCounter;
                }
            }
        }

        // trim model to retention limits
        trimUnfilteredModel();
        // trim filtered list to max capacity
        trimFilteredList();
    }

    private boolean accept(ConsoleRow row) {
        return row.getComponentName() != null && !row.getComponentName().isEmpty();
    }

    /**
     * Set the new {@link ConsoleRowFilter} for building future snapshots. Null is not permitted; set a permissive filter instead.
     * 
     * @param newFilter the new {@link ConsoleRowFilter}
     */
    @Override
    public synchronized void setRowFilter(ConsoleRowFilter newFilter) {
        // mark modification
        sequenceIdCounter++;
        // use a clone to prevent external modification
        currentFilter = newFilter.clone();
        // rebuild filtered list with new filter
        filteredRows = new LinkedList<ConsoleRow>();
        for (ConsoleRow row : allRows) {
            // add to filtered list if filter matches
            if (currentFilter.accept(row)) {
                filteredRows.addLast(row);
            }
        }
        filteredListLastChanged = sequenceIdCounter;
        // trim filtered list to max capacity
        trimFilteredList();
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindConsoleRowLogService(ConsoleRowLogService newInstance) {
        this.consoleRowLogService = newInstance;
    }

    private void resetModel() {
        allRows = new LinkedList<ConsoleRow>();
        filteredRows = new LinkedList<ConsoleRow>();
        filteredListLastChanged = sequenceIdCounter;
        workflows = new TreeSet<String>();
        workflowListLastChanged = sequenceIdCounter;
        components = new TreeSet<String>();
        componentListLastChanged = sequenceIdCounter;
        currentFilter = new ConsoleRowFilter();
    }

    private void trimUnfilteredModel() {
        // TODO could be expanded to retention limits per type etc.
        while (allRows.size() > MAX_UNFILTERED_ROWS_RETENTION) {
            allRows.removeFirst();
        }
    }

    private void trimFilteredList() {
        while (filteredRows.size() > MAX_SNAPSHOT_SIZE) {
            filteredRows.removeFirst();
        }
    }

    /**
     * Removes all console rows.
     **/
    public synchronized void clearAll() {
        sequenceIdCounter++;
        resetModel();
    }
    
    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindCommunicationService(CommunicationService newInstance) {
        this.communicationService = newInstance;
    }
    
    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    protected void bindWorkflowHostService(WorkflowHostService newWorkflowHostService) {
        workflowHostService = newWorkflowHostService;
    }

}
