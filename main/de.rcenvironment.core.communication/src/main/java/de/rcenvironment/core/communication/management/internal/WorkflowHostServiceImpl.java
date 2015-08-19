/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.common.WorkflowHostUtils;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.NodePropertyConstants;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListenerAdapter;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallback;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedCallbackManager;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.incubator.ListenerDeclaration;
import de.rcenvironment.core.utils.incubator.ListenerProvider;

/**
 * Default {@link WorkflowHostService} implementation.
 * 
 * @author Robert Mischke
 */
// FIXME temporary package location to simplify migration; move to "de.rcenvironment.core.component.workflow.internal"
// (or similar) when complete - misc_ro
public class WorkflowHostServiceImpl implements WorkflowHostService, ListenerProvider {

    private NodeConfigurationService platformService;

    private NodePropertiesService nodePropertiesService;

    private final Set<NodeIdentifier> workflowHostsWorkingCopy = new HashSet<NodeIdentifier>();

    private Set<NodeIdentifier> workflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<NodeIdentifier>());

    private Set<NodeIdentifier> workflowHostsAndSelfSnapshot = Collections.unmodifiableSet(new HashSet<NodeIdentifier>());

    private final AsyncOrderedCallbackManager<WorkflowHostSetListener> callbackManager =
        new AsyncOrderedCallbackManager<WorkflowHostSetListener>(SharedThreadPool.getInstance(),
            AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private final Log log = LogFactory.getLog(getClass());

    private NodeIdentifier localNodeId;

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {
        boolean isWorkflowHost = platformService.isWorkflowHost();
        nodePropertiesService.addOrUpdateLocalNodeProperty(WorkflowHostUtils.KEY_IS_WORKFLOW_HOST,
            NodePropertyConstants.wrapBoolean(isWorkflowHost));
        localNodeId = platformService.getLocalNodeId();

        // create initial placeholders
        workflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<NodeIdentifier>());
        Set<NodeIdentifier> tempWorkflowHostsAndSelf = new HashSet<NodeIdentifier>();
        tempWorkflowHostsAndSelf.add(localNodeId);
        workflowHostsAndSelfSnapshot = Collections.unmodifiableSet(tempWorkflowHostsAndSelf);
    }

    @Override
    public Collection<ListenerDeclaration> defineListeners() {
        List<ListenerDeclaration> result = new ArrayList<ListenerDeclaration>();
        result.add(new ListenerDeclaration(NodePropertiesChangeListener.class, new NodePropertiesChangeListenerAdapter() {

            @Override
            public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
                Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
                // delegate to keep synchronization approach simple - misc_ro
                updateOnReachableNodePropertiesChanged(addedProperties, updatedProperties, removedProperties);
            }

        }));
        // register listener on self
        result.add(new ListenerDeclaration(WorkflowHostSetListener.class, new WorkflowHostSetListener() {

            @Override
            public void onReachableWorkflowHostsChanged(Set<NodeIdentifier> reachableWfHosts, Set<NodeIdentifier> addedWfHosts,
                Set<NodeIdentifier> removedWfHosts) {
                log.debug("List of reachable workflow hosts updated: " + reachableWfHosts);
            }
        }));
        return result;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindNodeConfigurationService(NodeConfigurationService newInstance) {
        this.platformService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    public void bindNodePropertiesService(NodePropertiesService newInstance) {
        this.nodePropertiesService = newInstance;
    }

    @Override
    public synchronized Set<NodeIdentifier> getWorkflowHostNodes() {
        return workflowHostsSnapshot;
    }

    @Override
    public synchronized Set<NodeIdentifier> getWorkflowHostNodesAndSelf() {
        return workflowHostsAndSelfSnapshot;
    }

    /**
     * Registers a {@link WorkflowHostSetListener} for changes to the set of reachable workflow hosts.
     * 
     * @param listener the new listener
     */
    public synchronized void addWorkflowHostSetListener(WorkflowHostSetListener listener) {
        // create copy in synchronized block
        final Set<NodeIdentifier> currentWorkflowHostsCopy = workflowHostsSnapshot;
        callbackManager.addListenerAndEnqueueCallback(listener, new AsyncCallback<WorkflowHostSetListener>() {

            @Override
            public void performCallback(WorkflowHostSetListener listener) {
                // FIXME implement difference sets or change listener interface
                listener.onReachableWorkflowHostsChanged(currentWorkflowHostsCopy, null, null);
            }
        });
    }

    /**
     * Unregisters a {@link WorkflowHostSetListener}.
     * 
     * @param listener the listener to remove
     */
    public void removeWorkflowHostSetListener(WorkflowHostSetListener listener) {
        callbackManager.removeListener(listener);
    }

    private synchronized void updateOnReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
        Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {

        boolean relevantModification = false;

        for (NodeProperty property : addedProperties) {
            if (WorkflowHostUtils.isWorkflowHostProperty(property)) {
                boolean value = WorkflowHostUtils.getWorkflowHostPropertyValue(property);
                // added properties are only relevant if the indicate a workflow host
                if (!value) {
                    continue;
                }
                NodeIdentifier nodeId = NodeIdentifierFactory.fromNodeId(property.getNodeIdString());
                boolean setChanged = workflowHostsWorkingCopy.add(nodeId);
                if (setChanged) {
                    relevantModification = true;
                    log.info("New workflow host available: " + nodeId);
                } else {
                    log.debug("New workflow host available, but it caused no set modification: " + property);
                }
            }
        }

        for (NodeProperty property : updatedProperties) {
            if (WorkflowHostUtils.isWorkflowHostProperty(property)) {
                NodeIdentifier nodeId = NodeIdentifierFactory.fromNodeId(property.getNodeIdString());
                boolean value = WorkflowHostUtils.getWorkflowHostPropertyValue(property);
                if (value) {
                    boolean setChanged = workflowHostsWorkingCopy.add(nodeId);
                    if (setChanged) {
                        relevantModification = true;
                        log.info("New workflow host available (by configuration change): " + nodeId);
                    } else {
                        log.debug("New workflow host available (by configuration change), but it caused no set modification: " + property);
                    }
                } else {
                    boolean setChanged = workflowHostsWorkingCopy.remove(nodeId);
                    if (setChanged) {
                        relevantModification = true;
                        log.info("Node removed as workflow host (by configuration change): " + nodeId);
                    } else {
                        log.debug("Node removed as workflow host (by configuration change), but it caused no set modification: "
                            + property);
                    }
                }
            }
        }

        for (NodeProperty property : removedProperties) {
            if (WorkflowHostUtils.isWorkflowHostProperty(property)) {
                NodeIdentifier nodeId = NodeIdentifierFactory.fromNodeId(property.getNodeIdString());
                // removed properties are only relevant if the node was a workflow host before
                if (!workflowHostsWorkingCopy.contains(nodeId)) {
                    continue;
                }
                boolean setChanged = workflowHostsWorkingCopy.remove(nodeId);
                if (setChanged) {
                    relevantModification = true;
                    log.info("Workflow host became unavailable: " + nodeId);
                } else {
                    log.debug("Workflow host was removed, but caused no set modification: " + property);
                }
            }
        }

        if (relevantModification) {
            // create new detached copy
            workflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<NodeIdentifier>(workflowHostsWorkingCopy));
            // could be optimized, but for now, just create a copy and add the local node
            Set<NodeIdentifier> tempWorkflowHostsAndSelf = new HashSet<NodeIdentifier>(workflowHostsWorkingCopy);
            tempWorkflowHostsAndSelf.add(localNodeId);
            workflowHostsAndSelfSnapshot = Collections.unmodifiableSet(tempWorkflowHostsAndSelf);

            callbackManager.enqueueCallback(new AsyncCallback<WorkflowHostSetListener>() {

                @Override
                public void performCallback(WorkflowHostSetListener listener) {
                    // FIXME implement difference sets or change listener interface
                    listener.onReachableWorkflowHostsChanged(workflowHostsSnapshot, null, null);
                }
            });
        }
    }
}
