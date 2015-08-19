/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerService;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Implementation of {@link WorkflowExecutionService}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionServiceImpl.class);
    
    /**
     * The interval (in msec) between the "heartbeat" notifications sent for active workflows. Workflows are considered active when they are
     * running or paused, or in the transitional states in-between.
     */
    private static final int ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC = 6 * 1000;
    
    private static final int CACHE_SIZE = 20;
        
    private BundleContext bundleContext;
    
    private CommunicationService communicationService;
    
    private DistributedNotificationService notificationService;
    
    private PlatformService platformService;
    
    private WorkflowHostService workflowHostService;
    
    private ComponentExecutionControllerService componentExecutionControllerService;
    
    private Map<String, WeakReference<WorkflowExecutionControllerService>> wfExeCtrlServices = new LRUMap<>(CACHE_SIZE);
    
    private Set<WorkflowExecutionInformation> workflowExecutionInformations;
    
    private Object wfExeFetchLock = new Object();
    
    private ScheduledFuture<?> heartbeatSendFuture;
    
    protected void activate(BundleContext context) {
        bundleContext = context;
        
        heartbeatSendFuture = SharedThreadPool.getInstance().scheduleAtFixedRate(new Runnable() {

            @Override
            @TaskDescription("Send heartbeat for active workflows")
            public void run() {
                Set<WorkflowExecutionInformation> wfExeInfoSnapshot = new HashSet<>(getExecutionControllerService(
                    platformService.getLocalNodeId()).getWorkflowExecutionInformations());
                for (WorkflowExecutionInformation wfExeInfo : wfExeInfoSnapshot) {
                    String wfExeId = wfExeInfo.getExecutionIdentifier();
                    WorkflowState state = getExecutionControllerService(platformService.getLocalNodeId()).getWorkflowState(wfExeId);
                    switch (state) {
                    case RUNNING:
                    case PAUSING:
                    case PAUSED:
                    case RESUMING:
                        // can be removed if to verbose
//                        LOG.debug(StringUtils.format("Sending heartbeat notification for active workflow %s (%s)",
//                            wfExeInfo.getInstanceName(), wfExeId));
                        notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeId, WorkflowState.IS_ALIVE.name());
                        break;
                    default:
                        // do nothing
                        break;
                    }
                }
            }
        }, ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC);
    }
    
    protected void deactivate() {
        if (heartbeatSendFuture != null) {
            heartbeatSendFuture.cancel(true);            
        }
    }
    
    @Override
    public WorkflowExecutionInformation execute(WorkflowExecutionContext wfExeCtx)
        throws WorkflowExecutionException, CommunicationException {
        WorkflowExecutionInformation workflowExecutionInformation = createExecutionController(wfExeCtx);
        performStartOnExecutionController(workflowExecutionInformation.getExecutionIdentifier(), wfExeCtx.getNodeId());
        return workflowExecutionInformation;
    }

    private WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext wfExeCtx) throws CommunicationException, 
        WorkflowExecutionException {
        
        Map<String, String> authTokens = createAndRegisterLocalComponentExecutionAuthTokens((wfExeCtx).getWorkflowDescription()); 
        return getExecutionControllerService(wfExeCtx.getNodeId()).createExecutionController(wfExeCtx, authTokens,
            !platformService.isLocalNode(wfExeCtx.getNodeId()));
    }

    /**
     * Creates an auth token for each component which must be instantiated locally from an remote workflow execution controller that was
     * instantiated from local node. It ensures that local components, which were not published, can be instantiated from remote, but only
     * from workflow execution controllers, which were created from local node and thus, which are allowed to instantiate local components
     * even if they are not published.
     */
    private Map<String, String> createAndRegisterLocalComponentExecutionAuthTokens(WorkflowDescription workflowDescription) {
        Map<String, String> compIdToTokenMapping = new HashMap<String, String>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            NodeIdentifier node = wfNode.getComponentDescription().getNode();
            // Use empty string instead of null to avoid "remote method not found" issue. If null is passed the method can not be inspected
            String token = "";
            if (node == null || platformService.isLocalNode(node)) {
                token = UUID.randomUUID().toString();
                componentExecutionControllerService.addComponentExecutionAuthToken(token);
            }
            compIdToTokenMapping.put(wfNode.getIdentifier(), token);
        }
        return compIdToTokenMapping;
    }
    
    private void performStartOnExecutionController(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performStart(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }
    
    @Override
    public void cancel(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performCancel(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }
    
    @Override
    public void pause(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performPause(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public void resume(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performResume(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }

    @Override
    public void dispose(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performDispose(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }
    
    @Override
    public WorkflowState getWorkflowState(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            return getExecutionControllerService(node).getWorkflowState(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
            return WorkflowState.UNKNOWN;
        }
    }
    
    @Override
    public Set<WorkflowExecutionInformation> getLocalWorkflowExecutionInformations() {
        return new HashSet<WorkflowExecutionInformation>(getExecutionControllerService(platformService.getLocalNodeId())
            .getWorkflowExecutionInformations());
    }
    
    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations() {
        return getWorkflowExecutionInformations(false);
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations(boolean forceRefresh) {
        if (!forceRefresh && workflowExecutionInformations != null) {
            return new HashSet<>(workflowExecutionInformations);
        } else {
            synchronized (wfExeFetchLock) {
                if (forceRefresh || workflowExecutionInformations == null) {
                    Set<WorkflowExecutionInformation> tempWfExeInfos = new HashSet<>();

                    CallablesGroup<Collection> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Collection.class);

                    for (NodeIdentifier node : workflowHostService.getWorkflowHostNodesAndSelf()) {
                        final NodeIdentifier finalNode = node;
                        callablesGroup.add(new Callable<Collection>() {

                            @Override
                            @TaskDescription("Distributed query: getWorkflowInformations()")
                            public Collection call() throws Exception {
                                WorkflowExecutionControllerService executionControllerService = getExecutionControllerService(finalNode);
                                try {
                                    return executionControllerService.getWorkflowExecutionInformations();
                                } catch (UndeclaredThrowableException e) {
                                    try {
                                        handleUndeclaredThrowableException(e);
                                    } catch (CommunicationException e1) {
                                        LOG.error("Failed to query remote workflows on node: " + finalNode, e1);
                                    }
                                    return null;
                                }
                            }
                        });
                    }
                    List<Collection> results = callablesGroup.executeParallel(new AsyncExceptionListener() {

                        @Override
                        public void onAsyncException(Exception e) {
                            LOG.warn("Exception during asynchrous execution", e);
                        }
                    });
                    // merge results
                    for (Collection singleResult : results) {
                        if (singleResult != null) {
                            tempWfExeInfos.addAll(singleResult);
                        }
                    }
                    workflowExecutionInformations = tempWfExeInfos;
                }
                return new HashSet<>(workflowExecutionInformations);
            }
        }
        
    }
    
    private WorkflowExecutionControllerService getExecutionControllerService(NodeIdentifier node) {
        WorkflowExecutionControllerService wfCtrlService = null;
        synchronized (wfExeCtrlServices) {
            if (wfExeCtrlServices.containsKey(node.getIdString())) {
                wfCtrlService = wfExeCtrlServices.get(node.getIdString()).get();
            }
            if (wfCtrlService == null) {
                wfCtrlService = (WorkflowExecutionControllerService)
                    communicationService.getService(WorkflowExecutionControllerService.class, node, bundleContext);
                wfExeCtrlServices.put(node.getIdString(), new WeakReference<WorkflowExecutionControllerService>(wfCtrlService));
            }
        }
        return wfCtrlService;
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
    
    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }
    
    protected void bindNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }
    
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }
    
    protected void bindComponentExecutionControllerService(ComponentExecutionControllerService newService) {
        componentExecutionControllerService = newService;
    }

    protected void bindWorkflowHostService(WorkflowHostService newService) {
        workflowHostService = newService;
    }

}
