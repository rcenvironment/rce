/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.internal;

import java.lang.reflect.UndeclaredThrowableException;
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
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.datamanagement.DataManagementService;
import de.rcenvironment.core.datamanagement.DistributedMetaDataService;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of {@link DataManagementService}.
 * 
 * @author Doreen Seider
 * @author Jan Flink
 * @author Robert Mischke
 */
public class DistributedMetaDataServiceImpl implements DistributedMetaDataService {

    private CommunicationService communicationService;

    private WorkflowHostService workflowHostService;

    private BundleContext context;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime,
        NodeIdentifier storageNodeId) throws CommunicationException {

        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            return service.addComponentRun(componentInstanceId, nodeId, count, starttime);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return null;
    }

    @Override
    public void addInputDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count,
        NodeIdentifier storageNodeId) throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.addInputDatum(componentRunId, typedDatumId, endpointInstanceId, count);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public Long addOutputDatum(Long componentRunId, Long endpointInstanceId, String datum, Integer count,
        NodeIdentifier storageNodeId) throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            return service.addOutputDatum(componentRunId, endpointInstanceId, datum, count);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return null;
    }

    @Override
    public void addComponentRunProperties(Long componentRunId, Map<String, String> properties, NodeIdentifier storageNodeId)
        throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.addComponentRunProperties(componentRunId, properties);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime, Long relatedComponentId,
        NodeIdentifier storageNodeId) throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.addTimelineInterval(workflowRunId, intervalType, starttime, relatedComponentId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return null;
    }

    @Override
    public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime, NodeIdentifier storageNodeId)
        throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.setTimelineIntervalFinished(timelineIntervalId, endtime);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem, NodeIdentifier storageNodeId)
        throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.setOrUpdateHistoryDataItem(componentRunId, historyDataItem);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public void setComponentRunFinished(Long componentRunId, Long endtime, NodeIdentifier storageNodeId)
        throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.setComponentRunFinished(componentRunId, endtime);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState, NodeIdentifier storageNodeId)
        throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            service.setComponentInstanceFinalState(componentInstanceId, finalState);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions() throws CommunicationException {
        Set<WorkflowRunDescription> descriptions = new HashSet<WorkflowRunDescription>();

        CallablesGroup<Set> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Set.class);

        for (final NodeIdentifier remoteNodeId : workflowHostService.getWorkflowHostNodesAndSelf()) {
            final String remoteNodeIdString = remoteNodeId.getIdString();
            callablesGroup.add(new Callable<Set>() {

                @Override
                @TaskDescription("Distributed query: getWorkflowDescriptions()")
                public Set<WorkflowRunDescription> call() throws Exception {
                    MetaDataService service =
                        (MetaDataService) communicationService.getService(MetaDataService.class, remoteNodeId, context);
                    Set<WorkflowRunDescription> workflowRunDescriptions = service.getWorkflowRunDescriptions();
                    workflowRunDescriptions = fixInconsistentControllerNodeIds(remoteNodeId, workflowRunDescriptions);
                    return workflowRunDescriptions;
                }

                private Set<WorkflowRunDescription> fixInconsistentControllerNodeIds(final NodeIdentifier remoteNodeId,
                    Set<WorkflowRunDescription> workflowRunDescriptions) {
                    boolean hasInconsistenControllerNodeIds = false;
                    for (WorkflowRunDescription wrd : workflowRunDescriptions) {
                        final String controllerNodeId = wrd.getControllerNodeID();
                        final String dmNodeId = wrd.getDatamanagementNodeID();
                        if (!remoteNodeIdString.equals(controllerNodeId) || !remoteNodeIdString.equals(dmNodeId)) {
                            hasInconsistenControllerNodeIds = true;
                        }
                    }
                    if (hasInconsistenControllerNodeIds) {
                        // found at least one inconsistent workflow run, so rewrite the collection
                        Set<WorkflowRunDescription> newSet = new HashSet<>();
                        for (WorkflowRunDescription wrd : workflowRunDescriptions) {
                            final String controllerNodeId = wrd.getControllerNodeID();
                            final String dmNodeId = wrd.getDatamanagementNodeID();
                            if (!remoteNodeIdString.equals(controllerNodeId) || !remoteNodeIdString.equals(dmNodeId)) {
                                log.warn(StringUtils
                                    .format(
                                        "Replacing an inconsistent controller and/or storage node id (%s, %s) in workflow run #%d received "
                                            + "from node %s - most likely, the remote node's id has changed since the workflow was run",
                                        controllerNodeId, dmNodeId, wrd.getWorkflowRunID(), remoteNodeId));
                                newSet.add(WorkflowRunDescription.cloneAndReplaceNodeIds(wrd, remoteNodeIdString));
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
    public WorkflowRun getWorkflowRun(Long workflowRunId, NodeIdentifier storageNodeId) throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            return service.getWorkflowRun(workflowRunId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return null;
    }

    @Override
    public WorkflowRunTimline getWorkflowTimeline(Long workflowRunId, NodeIdentifier storageNodeId) throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            return service.getWorkflowTimeline(workflowRunId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return null;
    }

    @Override
    public Boolean deleteWorkflowRun(Long workflowRunId, NodeIdentifier storageNodeId) throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            return service.deleteWorkflowRun(workflowRunId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return false;
    }

    @Override
    public Boolean deleteWorkflowRunFiles(Long workflowRunId, NodeIdentifier storageNodeId)
        throws CommunicationException {
        MetaDataService service = (MetaDataService) communicationService.getService(MetaDataService.class, storageNodeId, context);
        try {
            return service.deleteWorkflowRunFiles(workflowRunId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
        return false;
    }

    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void bindWorkflowHostService(WorkflowHostService newWorkflowHostService) {
        workflowHostService = newWorkflowHostService;
    }

    private void handleUndeclaredThrowableException(UndeclaredThrowableException e) throws CommunicationException {
        if (e.getCause() instanceof CommunicationException) {
            throw (CommunicationException) e.getCause();
        } else if (e.getCause() instanceof RuntimeException) {
            throw (RuntimeException) e.getCause();
        } else {
            // should not happen as checked exceptions are thrown directly
            throw e;
        }
    }

}
