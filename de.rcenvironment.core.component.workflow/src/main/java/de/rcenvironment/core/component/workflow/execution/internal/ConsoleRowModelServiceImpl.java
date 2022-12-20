/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleModelSnapshot;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowFilter;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowLogService;
import de.rcenvironment.core.component.workflow.execution.api.ConsoleRowModelService;
import de.rcenvironment.core.component.workflow.execution.api.GenericSubscriptionManager;
import de.rcenvironment.core.component.workflow.execution.impl.ConsoleSubscriptionEventProcessor;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * Default {@link ConsoleRowModelService} implementation.
 * 
 * @author Doreen Seider (initial version)
 * @author Robert Mischke (current)
 * @author Kathrin Schaffert (#17869)
 */
@Component(immediate = true)
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

    private GenericSubscriptionManager subscriptionManager;

    private final CountDownLatch initialSubscriptionLatch;

    private Deque<ConsoleRow> allRows;

    /**
     * Note: The current concept is based on a single client view using this model; if required, this could be changed to a map of
     * registered filters.
     */
    private ConsoleRowFilter currentFilter;

    private Deque<ConsoleRow> filteredRows;

    private Map<String, Collection<String>> workflowComponentsMap;

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

    private DistributedNotificationService notificationService;

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
    @Activate
    public void activate() {
        ConcurrencyUtils.getAsyncTaskService().execute("Initial ConsoleRow model subscriptions", () -> {

            subscriptionManager = new GenericSubscriptionManager(new ConsoleSubscriptionEventProcessor(
                ConsoleRowModelServiceImpl.this, consoleRowLogService), communicationService, workflowHostService, notificationService);
            subscriptionManager.updateSubscriptionsForPrefixes(new String[] { ConsoleRow.NOTIFICATION_ID_PREFIX_CONSOLE_EVENT });
            initialSubscriptionLatch.countDown();
        });
    }

    @Override
    public void ensureConsoleCaptureIsInitialized() throws InterruptedException {
        initialSubscriptionLatch.await();
    }

    /**
     * Updates subscriptions to known server instances.
     */
    @Override
    public synchronized void updateSubscriptions() {
        try {
            initialSubscriptionLatch.await();
        } catch (InterruptedException e) {
            // TODO better handling?
            throw new RuntimeException("Interrupted while waiting for initial subscriptions to complete", e);
        }
        subscriptionManager.updateSubscriptionsForPrefixes(new String[] { ConsoleRow.NOTIFICATION_ID_PREFIX_CONSOLE_EVENT });
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
            snapshot.setWorkflowComponentsMap(workflowComponentsMap);
            snapshot.setWorkflowListChanged(true);
        }
        // if modified, set a copy of the component list
        if (componentListLastChanged > sequenceId) {
            snapshot.setWorkflowComponentsMap(workflowComponentsMap);
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
                // add to the map of workflows and related components
                // note: currently, workflowComponentsMap is only purged on clearAll
                if (workflowComponentsMap.containsKey(row.getWorkflowName())) {
                    if (workflowComponentsMap.get(row.getWorkflowName()).add(row.getComponentName())) {
                        componentListLastChanged = sequenceIdCounter;
                    }
                } else {
                    Set<String> set = new HashSet<>();
                    set.add(row.getComponentName());
                    workflowComponentsMap.put(row.getWorkflowName(), set);
                    componentListLastChanged = sequenceIdCounter;
                    workflowListLastChanged = sequenceIdCounter;
                }
            }
        }

        // trim model to retention limits
        trimUnfilteredModel();
        // trim filtered list to max capacity
        trimFilteredList();
    }

    private boolean accept(ConsoleRow row) {
        if (row.getType().equals(ConsoleRow.Type.WORKFLOW_ERROR)) {
            return row.getWorkflowName() != null && !row.getWorkflowName().isEmpty();
        } else {
            return row.getWorkflowName() != null && !row.getWorkflowName().isEmpty()
                && row.getComponentName() != null && !row.getComponentName().isEmpty();
        }
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

    private void resetModel() {
        allRows = new LinkedList<>();
        filteredRows = new LinkedList<>();
        filteredListLastChanged = sequenceIdCounter;
        workflowListLastChanged = sequenceIdCounter;
        componentListLastChanged = sequenceIdCounter;
        workflowComponentsMap = new HashMap<>();
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
    @Override
    public synchronized void clearAll() {
        sequenceIdCounter++;
        resetModel();
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    protected void bindConsoleRowLogService(ConsoleRowLogService newInstance) {
        this.consoleRowLogService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    protected void bindCommunicationService(CommunicationService newInstance) {
        this.communicationService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    protected void bindWorkflowHostService(WorkflowHostService newWorkflowHostService) {
        workflowHostService = newWorkflowHostService;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    protected void bindNotificationService(DistributedNotificationService newInstance) {
        this.notificationService = newInstance;
    }

}
