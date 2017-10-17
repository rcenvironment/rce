/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;
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
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link RemotableWorkflowExecutionControllerService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor changes)
 */
public class WorkflowExecutionControllerServiceImpl implements RemotableWorkflowExecutionControllerService {

    private BundleContext bundleContext;

    private WorkflowHostService workflowHostService;

    private LocalExecutionControllerUtilsService exeCtrlUtilsService;

    private NotificationService notificationService;

    private Map<String, ServiceRegistration<?>> workflowServiceRegistrations = Collections.synchronizedMap(
        new HashMap<String, ServiceRegistration<?>>());

    private Map<String, WorkflowExecutionInformation> workflowExecutionInformations = Collections.synchronizedMap(
        new HashMap<String, WorkflowExecutionInformation>());

    private final Log log = LogFactory.getLog(getClass());

    protected void activate(BundleContext context) {
        bundleContext = context;
    }

    @Override
    @AllowRemoteAccess
    public WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext wfExeCtx,
        Map<String, String> executionAuthTokens, Boolean calledFromRemote) throws WorkflowExecutionException, RemoteOperationException {

        if (calledFromRemote && !workflowHostService.getLogicalWorkflowHostNodes().contains(wfExeCtx.getNodeId())) {
            throw new WorkflowExecutionException(StringUtils.format("Workflow execution request refused, as the requested instance is "
                + "not declared as workflow host: %s", wfExeCtx.getNodeId()));
        }
        WorkflowExecutionController workflowController = new WorkflowExecutionControllerImpl(wfExeCtx);
        workflowController.setComponentExecutionAuthTokens(executionAuthTokens);

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, wfExeCtx.getExecutionIdentifier());

        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(
            new String[] { WorkflowExecutionController.class.getName(), WorkflowExecutionControllerCallback.class.getName() },
            workflowController, properties);

        WorkflowExecutionInformationImpl workflowExecutionInformation = new WorkflowExecutionInformationImpl(wfExeCtx);
        workflowExecutionInformation.setIdentifier(wfExeCtx.getExecutionIdentifier());
        workflowExecutionInformation.setWorkflowState(WorkflowState.INIT);

        synchronized (workflowExecutionInformations) {
            workflowExecutionInformations.put(wfExeCtx.getExecutionIdentifier(), workflowExecutionInformation);
            workflowServiceRegistrations.put(wfExeCtx.getExecutionIdentifier(), serviceRegistration);
        }
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
            exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, wfExecutionId, bundleContext).dispose();
        } catch (ExecutionControllerException e) {
            log.warn(StringUtils.format("Failed to dispose workflow (%s). It seems to be already disposed (%s).",
                wfExecutionId, e.getMessage()));
        }
        synchronized (workflowExecutionInformations) {
            workflowExecutionInformations.remove(wfExecutionId);
        }
    }

    private void dispose(String executionId) {
        synchronized (workflowServiceRegistrations) {
            if (workflowServiceRegistrations.containsKey(executionId)) {
                workflowServiceRegistrations.get(executionId).unregister();
                workflowServiceRegistrations.remove(executionId);
            }
        }
    }

    @Override
    @AllowRemoteAccess
    public void performStart(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext).start();
    }

    @Override
    @AllowRemoteAccess
    public void performCancel(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext).cancel();
    }

    @Override
    @AllowRemoteAccess
    public void performPause(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext).pause();
    }

    @Override
    @AllowRemoteAccess
    public void performResume(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext).resume();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowState getWorkflowState(String executionId) throws ExecutionControllerException, RemoteOperationException {
        return exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext).getState();
    }

    @Override
    @AllowRemoteAccess
    public Long getWorkflowDataManagementId(String executionId) throws ExecutionControllerException {
        return exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, executionId, bundleContext)
            .getDataManagementId();
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

    protected void bindWorkflowHostService(WorkflowHostService newService) {
        this.workflowHostService = newService;
    }

    protected void bindLocalExecutionControllerUtilsService(LocalExecutionControllerUtilsService newService) {
        exeCtrlUtilsService = newService;
    }

    protected void bindNotificationService(NotificationService newService) {
        this.notificationService = newService;
    }
}
