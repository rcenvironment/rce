/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.RemotableMetaDataService;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link DataManagementService}.
 * 
 * @author Doreen Seider
 * @author Jan Flink
 * @author Robert Mischke
 */
public class MetaDataServiceImpl implements MetaDataService {

    private CommunicationService communicationService;

    private WorkflowHostService workflowHostService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime,
        ResolvableNodeId storageNodeId) throws CommunicationException {

        try {
            return getRemoteMetaDataService(storageNodeId).addComponentRun(componentInstanceId, nodeId, count, starttime);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to add component run from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count,
        ResolvableNodeId storageNodeId) throws CommunicationException {
        try {
            getRemoteMetaDataService(storageNodeId).addInputDatum(componentRunId, typedDatumId, endpointInstanceId, count);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to add input datum from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count,
        ResolvableNodeId storageNodeId) throws CommunicationException {
        try {
            return getRemoteMetaDataService(storageNodeId).addOutputDatum(componentRunId, endpointInstanceId, datum, count);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to add output datum from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public void addComponentRunProperties(Long componentRunId, Map<String, String> properties, ResolvableNodeId storageNodeId)
        throws CommunicationException {
        try {
            getRemoteMetaDataService(storageNodeId).addComponentRunProperties(componentRunId, properties);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to add component run properties from remote node @%s: ",
                storageNodeId) + e.getMessage());
        }
    }

    @Override
    public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId,
        ResolvableNodeId storageNodeId) throws CommunicationException {
        try {
            return getRemoteMetaDataService(storageNodeId).addTimelineInterval(workflowRunId, intervalType, starttime, relatedComponentId);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to add timeline interval from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime, ResolvableNodeId storageNodeId)
        throws CommunicationException {
        try {
            getRemoteMetaDataService(storageNodeId).setTimelineIntervalFinished(timelineIntervalId, endtime);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to set endtime of timeline interval from remote node @%s: ",
                storageNodeId) + e.getMessage());
        }
    }

    @Override
    public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem, ResolvableNodeId storageNodeId)
        throws CommunicationException {
        try {
            getRemoteMetaDataService(storageNodeId).setOrUpdateHistoryDataItem(componentRunId, historyDataItem);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to update history data item from remote node @%s: ",
                storageNodeId) + e.getMessage());
        }
    }

    @Override
    public void setComponentRunFinished(Long componentRunId, Long endtime, FinalComponentRunState finalState,
        ResolvableNodeId storageNodeId)
        throws CommunicationException {
        try {
            getRemoteMetaDataService(storageNodeId).setComponentRunFinished(componentRunId, endtime, finalState);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to set endtime of component run from remote node @%s: ",
                storageNodeId) + e.getMessage());
        }
    }

    @Override
    public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState, ResolvableNodeId storageNodeId)
        throws CommunicationException {
        try {
            getRemoteMetaDataService(storageNodeId).setComponentInstanceFinalState(componentInstanceId, finalState);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format(
                "Failed to set final state of component instance from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions() throws CommunicationException {
        Set<WorkflowRunDescription> descriptions = new HashSet<WorkflowRunDescription>();

        CallablesGroup<Set> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Set.class);

        for (final InstanceNodeSessionId remoteInstanceSessionId : workflowHostService.getWorkflowHostNodesAndSelf()) {
            callablesGroup.add(new Callable<Set>() {

                @Override
                @TaskDescription("Distributed query: getWorkflowDescriptions()")
                public Set<WorkflowRunDescription> call() throws Exception {
                    Set<WorkflowRunDescription> workflowRunDescriptions =
                        getRemoteMetaDataService(remoteInstanceSessionId).getWorkflowRunDescriptions();
                    workflowRunDescriptions = fixInconsistentControllerNodeIds(remoteInstanceSessionId, workflowRunDescriptions);
                    return workflowRunDescriptions;
                }

                private Set<WorkflowRunDescription> fixInconsistentControllerNodeIds(final InstanceNodeSessionId remoteNodeId,
                    Set<WorkflowRunDescription> workflowRunDescriptions) {
                    boolean hasInconsistenControllerNodeIds = false;
                    for (WorkflowRunDescription wrd : workflowRunDescriptions) {
                        final LogicalNodeId controllerNodeId = wrd.getControllerLogicalNodeId();
                        final LogicalNodeId dmNodeId = wrd.getStorageLogicalNodeId();
                        if (!remoteInstanceSessionId.isSameInstanceNodeAs(controllerNodeId)
                            || !remoteInstanceSessionId.isSameInstanceNodeAs(dmNodeId)) {
                            hasInconsistenControllerNodeIds = true;
                        }
                    }
                    if (hasInconsistenControllerNodeIds) {
                        // found at least one inconsistent workflow run, so rewrite the collection
                        Set<WorkflowRunDescription> newSet = new HashSet<>();
                        for (WorkflowRunDescription wrd : workflowRunDescriptions) {
                            final LogicalNodeId controllerNodeId = wrd.getControllerLogicalNodeId();
                            final LogicalNodeId dmNodeId = wrd.getStorageLogicalNodeId();
                            if (!remoteInstanceSessionId.isSameInstanceNodeAs(controllerNodeId)
                                || !remoteInstanceSessionId.isSameInstanceNodeAs(dmNodeId)) {
                                log.warn(StringUtils.format(
                                    "Replacing an inconsistent controller and/or storage node id (%s, %s) in workflow run #%d received "
                                        + "from node %s - most likely, the remote node's id has changed since the workflow was run",
                                    controllerNodeId, dmNodeId, wrd.getWorkflowRunID(), remoteNodeId));
                                // TODO this should be the remote node's *logical node* id once migration is complete - misc_ro
                                newSet.add(WorkflowRunDescription.cloneAndReplaceNodeIds(wrd,
                                    remoteInstanceSessionId.getInstanceNodeIdString()));
                            } else {
                                newSet.add(wrd);
                            }
                        }
                        workflowRunDescriptions = newSet;
                    }
                    return workflowRunDescriptions;
                }
            });
        }

        List<Set> results = callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                // log a compact message, no stacktrace
                log.warn("Failed to query a node for workflow data management information: " + e.toString());
            }
        });

        for (Collection<WorkflowRunDescription> singleResult : results) {
            if (singleResult != null) {
                descriptions.addAll(singleResult);
            }
        }
        return descriptions;
    }

    @Override
    public WorkflowRun getWorkflowRun(Long workflowRunId, ResolvableNodeId storageNodeId) throws CommunicationException {
        try {
            return getRemoteMetaDataService(storageNodeId).getWorkflowRun(workflowRunId);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to get workflow run from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public WorkflowRunTimline getWorkflowTimeline(Long workflowRunId, ResolvableNodeId storageNodeId) throws CommunicationException {
        try {
            return getRemoteMetaDataService(storageNodeId).getWorkflowTimeline(workflowRunId);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to get workflow timeline from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public Boolean deleteWorkflowRun(Long workflowRunId, ResolvableNodeId storageNodeId) throws CommunicationException {
        try {
            return getRemoteMetaDataService(storageNodeId).deleteWorkflowRun(workflowRunId);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(
                StringUtils.format("Failed to delete worklfow run from remote node @%s: ", storageNodeId) + e.getMessage());
        }
    }

    @Override
    public Boolean deleteWorkflowRunFiles(Long workflowRunId, ResolvableNodeId storageNodeId)
        throws CommunicationException {
        try {
            return getRemoteMetaDataService(storageNodeId).deleteWorkflowRunFiles(workflowRunId);
        } catch (RemoteOperationException e) {
            throw new CommunicationException(StringUtils.format("Failed to delete files of worklfow run from remote node @%s: ",
                storageNodeId) + e.getMessage());
        }
    }

    protected void activate(BundleContext bundleContext) {}

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void bindWorkflowHostService(WorkflowHostService newWorkflowHostService) {
        workflowHostService = newWorkflowHostService;
    }

    private RemotableMetaDataService getRemoteMetaDataService(ResolvableNodeId nodeId) throws RemoteOperationException {
        return (RemotableMetaDataService) communicationService.getRemotableService(RemotableMetaDataService.class, nodeId);
    }

}
