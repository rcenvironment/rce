/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.WorkflowHostUtils;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.communication.management.WorkflowHostSetListener;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.NodePropertyConstants;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListenerAdapter;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;

/**
 * Default {@link WorkflowHostService} implementation.
 * 
 * @author Robert Mischke
 */
// FIXME temporary package location to simplify migration; move to "de.rcenvironment.core.component.workflow.internal"
// (or similar) when complete - misc_ro
public class WorkflowHostServiceImpl implements WorkflowHostService, AdditionalServicesProvider {

    private NodeConfigurationService platformService;

    private NodePropertiesService nodePropertiesService;

    private final Set<InstanceNodeSessionId> workflowHostsWorkingCopy = new HashSet<InstanceNodeSessionId>();

    private Set<InstanceNodeSessionId> workflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<InstanceNodeSessionId>());

    private Set<InstanceNodeSessionId> workflowHostsAndSelfSnapshot = Collections.unmodifiableSet(new HashSet<InstanceNodeSessionId>());

    private final AsyncOrderedCallbackManager<WorkflowHostSetListener> callbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private final Log log = LogFactory.getLog(getClass());

    private InstanceNodeSessionId localNodeId;

    private Set<LogicalNodeId> logicalWorkflowHostsSnapshot;

    private Set<LogicalNodeId> logicalWorkflowHostsAndSelfSnapshot;

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {
        boolean isWorkflowHost = platformService.isWorkflowHost();
        nodePropertiesService.addOrUpdateLocalNodeProperty(WorkflowHostUtils.KEY_IS_WORKFLOW_HOST,
            NodePropertyConstants.wrapBoolean(isWorkflowHost));
        localNodeId = platformService.getInstanceNodeSessionId();

        // create initial placeholders
        workflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<InstanceNodeSessionId>());

        logicalWorkflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<LogicalNodeId>());

        Set<InstanceNodeSessionId> tempWorkflowHostsAndSelf = new HashSet<>();
        tempWorkflowHostsAndSelf.add(localNodeId);
        workflowHostsAndSelfSnapshot = Collections.unmodifiableSet(tempWorkflowHostsAndSelf);

        Set<LogicalNodeId> tempLogicalWorkflowHostsAndSelf = new HashSet<>();
        // note: ok to simply use the local default logical node here as long as workflow hosts are not logical-node-specific
        tempLogicalWorkflowHostsAndSelf.add(localNodeId.convertToDefaultLogicalNodeId());
        logicalWorkflowHostsAndSelfSnapshot = Collections.unmodifiableSet(tempLogicalWorkflowHostsAndSelf);
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        List<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(NodePropertiesChangeListener.class, new NodePropertiesChangeListenerAdapter() {

            @Override
            public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
                Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {
                // delegate to keep synchronization approach simple - misc_ro
                updateOnReachableNodePropertiesChanged(addedProperties, updatedProperties, removedProperties);
            }

        }));
        // register listener on self
        result.add(new AdditionalServiceDeclaration(WorkflowHostSetListener.class, new WorkflowHostSetListener() {

            @Override
            public void onReachableWorkflowHostsChanged(Set<InstanceNodeSessionId> reachableWfHosts,
                Set<InstanceNodeSessionId> addedWfHosts,                Set<InstanceNodeSessionId> removedWfHosts) {
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
    public synchronized Set<InstanceNodeSessionId> getWorkflowHostNodes() {
        return workflowHostsSnapshot;
    }

    @Override
    public synchronized Set<LogicalNodeId> getLogicalWorkflowHostNodes() {
        return logicalWorkflowHostsSnapshot;
    }

    @Override
    public synchronized Set<InstanceNodeSessionId> getWorkflowHostNodesAndSelf() {
        return workflowHostsAndSelfSnapshot;
    }

    @Override
    public synchronized Set<LogicalNodeId> getLogicalWorkflowHostNodesAndSelf() {
        return logicalWorkflowHostsAndSelfSnapshot;
    }

    /**
     * Registers a {@link WorkflowHostSetListener} for changes to the set of reachable workflow hosts.
     * 
     * @param listener the new listener
     */
    public synchronized void addWorkflowHostSetListener(WorkflowHostSetListener listener) {
        // create copy in synchronized block
        final Set<InstanceNodeSessionId> currentWorkflowHostsCopy = workflowHostsSnapshot;
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
                InstanceNodeSessionId nodeId = property.getInstanceNodeSessionId();
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
                InstanceNodeSessionId nodeId = property.getInstanceNodeSessionId();
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
                InstanceNodeSessionId nodeId = property.getInstanceNodeSessionId();
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
            workflowHostsSnapshot = Collections.unmodifiableSet(new HashSet<InstanceNodeSessionId>(workflowHostsWorkingCopy));
            // FIXME >8.0 preliminary - this only supports the *default* logical node ids, not the ones published via other mechanisms
            logicalWorkflowHostsSnapshot = convertFromInstanceIdsToLogicalNodesSet(workflowHostsSnapshot);

            // could be optimized, but for now, just create a copy and add the local node
            Set<InstanceNodeSessionId> tempWorkflowHostsAndSelf = new HashSet<InstanceNodeSessionId>(workflowHostsWorkingCopy);
            tempWorkflowHostsAndSelf.add(localNodeId);
            workflowHostsAndSelfSnapshot = Collections.unmodifiableSet(tempWorkflowHostsAndSelf);
            // FIXME >8.0 preliminary - this only supports the *default* logical node ids, not the ones published via other mechanisms
            logicalWorkflowHostsAndSelfSnapshot = convertFromInstanceIdsToLogicalNodesSet(workflowHostsAndSelfSnapshot);

            callbackManager.enqueueCallback(new AsyncCallback<WorkflowHostSetListener>() {

                @Override
                public void performCallback(WorkflowHostSetListener listener) {
                    // FIXME implement difference sets or change listener interface
                    listener.onReachableWorkflowHostsChanged(workflowHostsSnapshot, null, null);
                }
            });
        }
    }

    private Set<LogicalNodeId> convertFromInstanceIdsToLogicalNodesSet(Set<InstanceNodeSessionId> input) {
        // FIXME >8.0 preliminary - this only supports the *default* logical node ids, not the ones published via other mechanisms
        final Set<LogicalNodeId> tempSet = new HashSet<LogicalNodeId>();
        for (InstanceNodeSessionId instanceSessionId : input) {
            tempSet.add(instanceSessionId.convertToDefaultLogicalNodeId());
        }
        return Collections.unmodifiableSet(tempSet);
    }
}
