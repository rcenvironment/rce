/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.execution.spi.SingleWorkflowStateChangeListener;
import de.rcenvironment.core.eventlog.api.EventLog;
import de.rcenvironment.core.eventlog.api.EventLogConstants;
import de.rcenvironment.core.eventlog.api.EventLogEntry;
import de.rcenvironment.core.eventlog.api.EventType;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Implementation of {@link RemotableWorkflowExecutionControllerService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
@Component
public class WorkflowExecutionControllerServiceImpl implements RemotableWorkflowExecutionControllerService {

    private BundleContext bundleContext;

    private WorkflowHostService workflowHostService;

    private LocalExecutionControllerUtilsService exeCtrlUtilsService;

    private NotificationService notificationService;

    // for visibility/accessibility checking before the actual start of the workflow
    private CommunicationService communicationService;

    // for visibility/accessibility checking before the actual start of the workflow
    private DistributedComponentKnowledgeService distributedComponentKnowledgeService;

    private Map<String, ServiceRegistration<?>> workflowServiceRegistrations = Collections.synchronizedMap(
        new HashMap<String, ServiceRegistration<?>>());

    private Map<String, WorkflowExecutionInformation> workflowExecutionInformations = Collections.synchronizedMap(
        new HashMap<String, WorkflowExecutionInformation>());

    private final Log log = LogFactory.getLog(getClass());

    @Activate
    protected void activate(BundleContext context) {
        bundleContext = context;
    }

    @Override
    @AllowRemoteAccess
    public WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext wfExeCtx,
        Map<String, String> executionAuthTokens, Boolean calledFromRemote) throws WorkflowExecutionException, RemoteOperationException {

        // TODO start_ts; workflow_metadata? workflow title?
        EventLogEntry eventLogEntry = EventLog.newEntry(EventType.WORKFLOW_EXECUTION_REQUESTED)
            .set(EventType.Attributes.WORKFLOW_RUN_ID, wfExeCtx.getExecutionIdentifier())
            // TODO (p1) 11.0.0: trusting the remote side to provide this information is obviously unreliable
            .set(EventType.Attributes.INITIATOR_IS_LOCAL_NODE, EventLogConstants.trueFalseValueFromBoolean(!calledFromRemote))
            // TODO (p1) 11.0.0: trusting the remote side to provide this information is obviously unreliable
            .set(EventType.Attributes.INITIATOR_NODE, wfExeCtx.getNodeIdStartedExecution().getLogicalNodeIdString());

        // TODO (p1) 11.0.0: improve this validation; see #17908
        if (calledFromRemote && !workflowHostService.getLogicalWorkflowHostNodes().contains(wfExeCtx.getNodeId())) {
            eventLogEntry.set(EventType.Attributes.SUCCESS, EventLogConstants.FALSE_VALUE);
            EventLog.append(eventLogEntry);
            throw new WorkflowExecutionException(StringUtils.format("Workflow execution request refused, as the requested instance is "
                + "not declared as workflow host: %s", wfExeCtx.getNodeId()));
        }
        // TODO use global ServiceRegistryAcccess instance once available
        WorkflowExecutionController workflowController =
            new WorkflowExecutionControllerImpl(wfExeCtx, ServiceRegistry.createAccessFor(this));
        workflowController.setComponentExecutionAuthTokens(executionAuthTokens);

        final String executionId = wfExeCtx.getExecutionIdentifier();
        registerExecutionController(workflowController, executionId);

        WorkflowExecutionInformationImpl workflowExecutionInformation = new WorkflowExecutionInformationImpl(wfExeCtx);
        workflowExecutionInformation.setIdentifier(wfExeCtx.getExecutionIdentifier());
        workflowExecutionInformation.setWorkflowState(WorkflowState.INIT);
        synchronized (workflowExecutionInformations) {
            workflowExecutionInformations.put(wfExeCtx.getExecutionIdentifier(), workflowExecutionInformation);
        }

        eventLogEntry.set(EventType.Attributes.SUCCESS, EventLogConstants.TRUE_VALUE);
        EventLog.append(eventLogEntry);

        return workflowExecutionInformation;
    }

    @Override
    @AllowRemoteAccess
    public void performDispose(final String wfExecutionId) throws RemoteOperationException {

        final WorkflowStateNotificationSubscriber workflowStateDisposedListener =
            new WorkflowStateNotificationSubscriber(new SingleWorkflowStateChangeListener() {

                @Override
                public void onWorkflowStateChanged(WorkflowState newState) {
                    if (newState == WorkflowState.DISPOSED) {
                        dispose(wfExecutionId);
                    } else if (newState != WorkflowState.DISPOSING) {
                        log.warn(StringUtils.format(
                            "Received unexpected workflow state '%s' for workflow '%s'", newState.getDisplayName(), wfExecutionId));
                    }
                }

                @Override
                public void onWorkflowNotAliveAnymore(String errorMessage) {}

            }, wfExecutionId) {

                private static final long serialVersionUID = 3168599724769249933L;

                @Override
                public void processNotification(Notification notification) {
                    super.processNotification(notification);
                    if (WorkflowState.isWorkflowStateValid((String) notification.getBody())
                        && (!WorkflowState.valueOf((String) notification.getBody()).equals(WorkflowState.DISPOSING))) {
                        try {
                            notificationService.unsubscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExecutionId, this);
                        } catch (RemoteOperationException e) {
                            log.warn(StringUtils.format("Failed to unsubscribe from state changes "
                                + "for workflow %s: %s", wfExecutionId, e.getMessage()));
                        }
                    }
                }
            };

        try {
            notificationService.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExecutionId, workflowStateDisposedListener);
        } catch (RemoteOperationException e) {
            log.error("Failed to subscribe for workflow state changes before disposing: " + e.getMessage());
            return; // preserve the "old" RTE behavior for now
        }
        try {
            getExecutionController(wfExecutionId).dispose();
        } catch (ExecutionControllerException e) {
            log.warn(StringUtils.format("Failed to dispose workflow (%s). It seems to be already disposed (%s).",
                wfExecutionId, e.getMessage()));
        }
        synchronized (workflowExecutionInformations) {
            workflowExecutionInformations.remove(wfExecutionId);
        }
    }

    private void dispose(String executionId) {
        unregisterExecutionController(executionId);
    }

    @Override
    @AllowRemoteAccess
    public void performStart(String executionId) throws ExecutionControllerException, RemoteOperationException {
        getExecutionController(executionId).start();
    }

    @Override
    @AllowRemoteAccess
    public void performCancel(String executionId) throws ExecutionControllerException, RemoteOperationException {
        getExecutionController(executionId).cancel();
    }

    @Override
    @AllowRemoteAccess
    public void performPause(String executionId) throws ExecutionControllerException, RemoteOperationException {
        getExecutionController(executionId).pause();
    }

    @Override
    @AllowRemoteAccess
    public void performResume(String executionId) throws ExecutionControllerException, RemoteOperationException {
        getExecutionController(executionId).resume();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowState getWorkflowState(String executionId) throws ExecutionControllerException, RemoteOperationException {
        return getExecutionController(executionId).getState();
    }

    @Override
    @AllowRemoteAccess
    public Long getWorkflowDataManagementId(String executionId) throws ExecutionControllerException {
        return getExecutionController(executionId).getDataManagementId();
    }

    @Override
    @AllowRemoteAccess
    public Collection<WorkflowExecutionInformation> getWorkflowExecutionInformations() throws ExecutionControllerException,
        RemoteOperationException {
        Map<String, WorkflowExecutionInformation> wfExeInfoSnapshot = null;
        synchronized (workflowExecutionInformations) {
            wfExeInfoSnapshot = new HashMap<>();
            wfExeInfoSnapshot.putAll(workflowExecutionInformations);
        }
        for (String executionId : wfExeInfoSnapshot.keySet()) {
            WorkflowState state;
            Long dmId;
            try {
                // TODO both of the calls delegate to the workflow execution controller service; should be optimized
                state = getWorkflowState(executionId);
                dmId = getWorkflowDataManagementId(executionId);
            } catch (ExecutionControllerException e) {
                log.debug(StringUtils.format("Removed workflow %s from temporary set of workflow execution infos: %s", executionId,
                    e.getMessage()));
                continue;
            }
            ((WorkflowExecutionInformationImpl) wfExeInfoSnapshot.get(executionId))
                .setWorkflowState(state);
            ((WorkflowExecutionInformationImpl) wfExeInfoSnapshot.get(executionId))
                .setWorkflowDataManagementId(dmId);
        }
        return new HashSet<WorkflowExecutionInformation>(wfExeInfoSnapshot.values());
    }

    @Override
    @AllowRemoteAccess
    public Map<String, String> verifyComponentVisibility(List<String> componentRefs) {
        DistributedComponentKnowledge compKnowledge = distributedComponentKnowledgeService.getCurrentSnapshot();
        Set<LogicalNodeId> reachableLogicalNodes = communicationService.getReachableLogicalNodes();
        Map<String, String> result = new HashMap<>();
        for (String compRef : componentRefs) {
            String[] refParts = StringUtils.splitAndUnescape(compRef);
            final String resultKey = refParts[0];
            final String componentIdAndVersion = refParts[1];
            final LogicalNodeId logicalNodeId;
            try {
                logicalNodeId = NodeIdentifierUtils.parseLogicalNodeIdString(refParts[2]);
            } catch (IdentifierException e) {
                result.put(resultKey, "Invalid node id: " + refParts[2]); // should never happen
                continue;
            }
            // note: for this visibility check, all logical node ids must be converted to default logical node ids,
            // as these are the ones contained in the "reachableLogicalNodes" set
            if (!isNodeVisible(reachableLogicalNodes, logicalNodeId.convertToDefaultLogicalNodeId())) {
                result.put(resultKey, "The instance to run this component on is not visible from the workflow controller's instance. "
                    + "Check if they are located in disconnected networks.");
            } else if (!isComponentVisible(compKnowledge, componentIdAndVersion, logicalNodeId)) {
                result.put(resultKey, "The workflow controller cannot access this component. "
                    + "Check if the controller needs to be in additional authorization groups.");
            }
        }
        return result;
    }

    @Reference
    protected void bindWorkflowHostService(WorkflowHostService newService) {
        this.workflowHostService = newService;
    }

    @Reference
    protected void bindLocalExecutionControllerUtilsService(LocalExecutionControllerUtilsService newService) {
        exeCtrlUtilsService = newService;
    }

    @Reference
    protected void bindNotificationService(NotificationService newService) {
        this.notificationService = newService;
    }

    @Reference
    protected void setCommunicationService(CommunicationService newService) {
        this.communicationService = newService;
    }

    @Reference
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        this.distributedComponentKnowledgeService = newService;
    }

    private boolean isNodeVisible(Set<LogicalNodeId> reachableLogicalNodes, LogicalNodeId logicalNodeId) {
        return reachableLogicalNodes.contains(logicalNodeId);
    }

    private boolean isComponentVisible(DistributedComponentKnowledge compKnowledge, String componentIdAndVersion,
        LogicalNodeId logicalNodeId) {

        for (DistributedComponentEntry comp : compKnowledge.getKnownSharedInstallationsOnNode(logicalNodeId, false)) {
            final ComponentInstallation componentInstallation = comp.getComponentInstallation();
            final String identifierAndVersion = componentInstallation.getComponentInterface().getIdentifierAndVersion();
            if (identifierAndVersion.equals(componentIdAndVersion) && componentInstallation.getNodeIdObject().equals(logicalNodeId)) {
                return true;
            }
        }
        return false; // no match
    }

    // TODO there is no real point of registering the controllers at the OSGi service registry - simply use a map instead? -- misc_ro
    private void registerExecutionController(WorkflowExecutionController workflowController, final String executionId) {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, executionId);
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(
            new String[] { WorkflowExecutionController.class.getName(), WorkflowExecutionControllerCallback.class.getName() },
            workflowController, properties);
        synchronized (workflowServiceRegistrations) {
            workflowServiceRegistrations.put(executionId, serviceRegistration);
        }
    }

    private WorkflowExecutionController getExecutionController(String executionId) throws ExecutionControllerException {
        return exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext);
    }

    private void unregisterExecutionController(String executionId) {
        synchronized (workflowServiceRegistrations) {
            if (workflowServiceRegistrations.containsKey(executionId)) {
                workflowServiceRegistrations.get(executionId).unregister();
                workflowServiceRegistrations.remove(executionId);
            }
        }
    }
}
