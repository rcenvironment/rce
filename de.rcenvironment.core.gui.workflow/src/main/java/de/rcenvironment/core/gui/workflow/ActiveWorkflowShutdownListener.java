/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.spi.SingleWorkflowStateChangeListener;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Prevents the Workbench to be closed as long as there are active workflows.
 * 
 * @author Christian Weiss
 * @author Doreen Seider
 */
final class ActiveWorkflowShutdownListener implements IWorkbenchListener {

    private static final String WORKFLOW_HANDLE_ERROR = "Failed to handle active workflows during shutdown";

    private static final Log LOGGER = LogFactory.getLog(ActiveWorkflowShutdownListener.class);

    /**
     * In case active workflows exist the user is presented a dialog to confirm the disposal.
     * 
     * @see org.eclipse.ui.IWorkbenchListener#preShutdown(org.eclipse.ui.IWorkbench, boolean)
     */
    @Override
    public boolean preShutdown(final IWorkbench workbench, final boolean forced) {

        boolean shutdown = true;

        final Map<String, WorkflowState> wfStates = new HashMap<>();
        final Map<String, ComponentState> compStates = new HashMap<>();

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        final WorkflowExecutionService wfExeService = serviceRegistryAccess.getService(WorkflowExecutionService.class);

        // Note: it is worked on snapshots. newly created workflow or component instances will not be considered. this needs to be improved
        // by adding support for kind of graceful shutdown
        final Set<WorkflowExecutionInformation> localWfExeInfosSnapshot = wfExeService.getLocalWorkflowExecutionInformations();
        final Set<WorkflowExecutionInformation> localActiveWfExeInfosSnapshot = getActiveWorkflows(localWfExeInfosSnapshot, wfStates);

        final ComponentExecutionService compExeService = serviceRegistryAccess.getService(ComponentExecutionService.class);
        final Set<ComponentExecutionInformation> localCompExeInfosSnapshot = compExeService.getLocalComponentExecutionInformations();
        final Set<ComponentExecutionInformation> localActiveCompExeInfosSnapshot = getActiveComponents(compExeService,
            localCompExeInfosSnapshot, compStates);
        boolean wfOrCompActive = localActiveWfExeInfosSnapshot.size() > 0 || localActiveCompExeInfosSnapshot.size() > 0;
        try {
            if (!forced && wfOrCompActive) {
                final int maxLines = 15;
                int lines = 0;
                Set<String> activeWfExeIds = new HashSet<>();
                String message = "\n";
                message += "\nWorkflows:\n";
                for (WorkflowExecutionInformation wfExeInfo : localActiveWfExeInfosSnapshot) {
                    message += StringUtils.format("- %s -> %s\n", wfExeInfo.getInstanceName(),
                        wfStates.get(wfExeInfo.getExecutionIdentifier()).getDisplayName());
                    lines++;
                    activeWfExeIds.add(wfExeInfo.getExecutionIdentifier());
                    if (lines > 10) {
                        message += "...\n";
                        break;
                    }
                }
                if (lines == 0) {
                    message += "-\n";
                }
                int workflowLines = lines;
                message += "\nComponents:\n";
                for (ComponentExecutionInformation compExeInfo : localActiveCompExeInfosSnapshot) {
                    if (!activeWfExeIds.contains(compExeInfo.getWorkflowExecutionIdentifier())) {
                        message += StringUtils.format("- %s (%s) -> %s\n", compExeInfo.getInstanceName(),
                            compExeInfo.getWorkflowInstanceName(), compStates.get(compExeInfo.getExecutionIdentifier()).getDisplayName());
                        lines++;
                        if (lines > maxLines) {
                            message += "...\n";
                            break;
                        }
                    }
                }
                if (workflowLines == lines) {
                    message += "-\n";
                }
                shutdown = MessageDialog.openQuestion(workbench.getActiveWorkbenchWindow().getShell(), Messages.activeWorkflowsTitle,
                    Messages.activeWorkflowsMessage + message);
            }
        } catch (IllegalStateException e) {
            LOGGER.error(WORKFLOW_HANDLE_ERROR, e);
        }

        if (shutdown && wfOrCompActive) {
            final DistributedNotificationService notificationService = serviceRegistryAccess
                .getService(DistributedNotificationService.class);

            Job job = new Job("Cancel and dispose all active workflows") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    List<String> cancelledDisposedWfExeIds = new ArrayList<>();

                    CallablesGroup<Void> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Void.class);
                    for (WorkflowExecutionInformation wfExeInfo : localWfExeInfosSnapshot) {
                        final WorkflowExecutionInformation finalWfExeInfo = wfExeInfo;
                        if (!cancelledDisposedWfExeIds.contains(finalWfExeInfo.getExecutionIdentifier())) {
                            callablesGroup.add(new Callable<Void>() {

                                @Override
                                @TaskDescription("Call method of workflow component")
                                public Void call() throws Exception {
                                    cancelAndDisposeWorkflow(wfExeService, notificationService, finalWfExeInfo);
                                    return null;
                                }
                            }, "Cancel/dispose workflow: " + finalWfExeInfo.getExecutionIdentifier());
                            cancelledDisposedWfExeIds.add(finalWfExeInfo.getExecutionIdentifier());
                        }
                    }

                    // as it causes errors, currently remote workflows with local active component are not cancelled (as it was before). Due
                    // to heartbeat sending, the remote workflow controller will recognize the missing components and will fail. That is an
                    // improvement in 6.0.0 compared to 5.2.1 -seid_do
                    // for (ComponentExecutionInformation compExeInfo : localCompExeInfosSnapshot) {
                    // final ComponentExecutionInformation finalCompExeInfo = compExeInfo;
                    // if (!cancelledDisposedWfExeIds.contains(finalCompExeInfo.getWorkflowExecutionIdentifier())) {
                    // callablesGroup.add(new Callable<Void>() {
                    //
                    // @Override
                    // @TaskDescription("Call method of workflow component")
                    // public Void call() throws Exception {
                    // cancelAndDisposeWorkflow(wfExeService, notificationService,
                    // finalCompExeInfo.getWorkflowExecutionIdentifier(),
                    // finalCompExeInfo.getWorkflowNodeId(), finalCompExeInfo.getWorkflowInstanceName(),
                    // wfStates.get(finalCompExeInfo.getWorkflowExecutionIdentifier()));
                    // return null;
                    // }
                    // }, "Cancel/dispose workflow: " + finalCompExeInfo.getWorkflowExecutionIdentifier());
                    // cancelledDisposedWfExeIds.add(finalCompExeInfo.getWorkflowExecutionIdentifier());
                    // }
                    // }

                    callablesGroup.executeParallel(new AsyncExceptionListener() {

                        @Override
                        public void onAsyncException(Exception e) {
                            LOGGER.error("Failed to cancel/dispose workflow", e);
                        }
                    });

                    return Status.OK_STATUS;
                }

                @Override
                public boolean belongsTo(Object family) {
                    return family == UncompletedJobsShutdownListener.MUST_BE_COMPLETED_ON_SHUTDOWN_JOB_FAMILY;
                }
            };
            job.setSystem(true);
            job.schedule();
        }
        return shutdown;
    }

    private void cancelAndDisposeWorkflow(final WorkflowExecutionService workflowExecutionService,
        final DistributedNotificationService notificationService, final WorkflowExecutionInformation wfExeInfo) {

        final CountDownLatch wfDisposedLatch = new CountDownLatch(2);

        WorkflowStateNotificationSubscriber workflowStateChangeListener =
            new WorkflowStateNotificationSubscriber(new SingleWorkflowStateChangeListener() {

                @Override
                public void onWorkflowStateChanged(WorkflowState newState) {
                    LOGGER.debug(StringUtils.format("Received state change event for workflow %s: ",
                        wfExeInfo.getExecutionIdentifier(), newState.getDisplayName()));
                    switch (newState) {
                    case CANCELLED:
                    case FAILED:
                    case RESULTS_REJECTED:
                    case FINISHED:
                        try {
                            workflowExecutionService.dispose(wfExeInfo.getWorkflowExecutionHandle());
                        } catch (ExecutionControllerException | RemoteOperationException e) {
                            LOGGER.error(StringUtils.format("Failed to dispose workflow '%s' (%s)",
                                wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier()), e);
                            wfDisposedLatch.countDown();
                        }
                        break;
                    case DISPOSED:
                        wfDisposedLatch.countDown();
                        break;
                    default:
                        break;
                    }
                }

                @Override
                public void onWorkflowNotAliveAnymore(String errorMessage) {
                    LOGGER.error(StringUtils.format("Failed to dispose workflow '%s' (%s) - %s",
                        wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier(), errorMessage));
                    wfDisposedLatch.countDown();
                }

            }, wfExeInfo.getExecutionIdentifier());

        try {
            notificationService.subscribe(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeInfo.getExecutionIdentifier(),
                workflowStateChangeListener, wfExeInfo.getNodeId());
            notificationService.subscribe(
                ConsoleRowUtils.composeConsoleNotificationId(wfExeInfo.getNodeId(), wfExeInfo.getExecutionIdentifier()),
                new ConsoleRowSubscriber(wfDisposedLatch), wfExeInfo.getNodeId());
            if (!WorkflowConstants.FINAL_WORKFLOW_STATES.contains(wfExeInfo.getWorkflowState())) {
                workflowExecutionService.cancel(wfExeInfo.getWorkflowExecutionHandle());
            } else {
                workflowExecutionService.dispose(wfExeInfo.getWorkflowExecutionHandle());
            }
        } catch (ExecutionControllerException | RemoteOperationException e) {
            LOGGER.error(StringUtils.format("Failed to cancel/dispose workflow '%s' (%s): %s",
                wfExeInfo.getExecutionIdentifier(), wfExeInfo.getExecutionIdentifier(), e.getMessage()));
        }
        try {
            wfDisposedLatch.await();
        } catch (InterruptedException e) {
            LOGGER.debug(StringUtils.format("Was interupted when cancelling/disposing workflow '%s' (%s)",
                wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier()), e);
            Thread.currentThread().interrupt();
        }
    }

    private Set<WorkflowExecutionInformation> getActiveWorkflows(Set<WorkflowExecutionInformation> localWfExeInfos,
        Map<String, WorkflowState> wfStates) {
        Set<WorkflowExecutionInformation> activeWfExeInfos = new HashSet<>();
        Iterator<WorkflowExecutionInformation> iterator = localWfExeInfos.iterator();
        while (iterator.hasNext()) {
            WorkflowExecutionInformation wfExeInfo = iterator.next();
            WorkflowState state = wfExeInfo.getWorkflowState();
            if (!WorkflowConstants.FINAL_WORKFLOW_STATES_WITH_DISPOSED.contains(state)) {
                activeWfExeInfos.add(wfExeInfo);
            }
            if (state == WorkflowState.DISPOSED) {
                iterator.remove();
            }
            wfStates.put(wfExeInfo.getExecutionIdentifier(), state);
        }
        return activeWfExeInfos;
    }

    private Set<ComponentExecutionInformation> getActiveComponents(ComponentExecutionService componentExecutionService,
        Set<ComponentExecutionInformation> localCompExeInfos, Map<String, ComponentState> compStates) {
        Set<ComponentExecutionInformation> activeCompExeInfos = new HashSet<>();
        Iterator<ComponentExecutionInformation> iterator = localCompExeInfos.iterator();
        while (iterator.hasNext()) {
            ComponentExecutionInformation compExeInfo = iterator.next();
            try {
                ComponentState state = componentExecutionService.getComponentState(compExeInfo.getExecutionIdentifier(),
                    compExeInfo.getNodeId());
                if (!ComponentConstants.FINAL_COMPONENT_STATES_WITH_DISPOSED.contains(state)) {
                    activeCompExeInfos.add(compExeInfo);
                }
                if (state == ComponentState.DISPOSED) {
                    iterator.remove();
                }
                compStates.put(compExeInfo.getExecutionIdentifier(), state);
            } catch (RemoteOperationException | ExecutionControllerException e) {
                LOGGER.error(StringUtils.format("Failed to get state for component '%s' (%s); cause: %s",
                    compExeInfo.getInstanceName(), compExeInfo.getExecutionIdentifier(), e.toString()));
            }
        }
        return activeCompExeInfos;
    }

    @Override
    public void postShutdown(final IWorkbench workbench) {}

    /**
     * Listens for workflow disposed status on console row level.
     * 
     * @author Doreen Seider
     */
    private static class ConsoleRowSubscriber extends DefaultNotificationSubscriber {

        private static final long serialVersionUID = 6177970783784847691L;

        private final transient CountDownLatch wfDisposeLatch;

        ConsoleRowSubscriber(CountDownLatch countDownLatch) {
            this.wfDisposeLatch = countDownLatch;
        }

        @Override
        public Class<?> getInterface() {
            return NotificationSubscriber.class;
        }

        @Override
        protected void processNotification(Notification notification) {
            Serializable body = notification.getBody();
            if (!(notification.getBody() instanceof ConsoleRow)) {
                LOGGER.error("Unexpected notification type on ConsoleRow channel: body class is " + body.getClass());
                return;
            }
            ConsoleRow row = (ConsoleRow) body;
            if (row.getType() == Type.LIFE_CYCLE_EVENT) {
                LOGGER.debug("Received workflow life-cycle event: " + row.getPayload());
                if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name())
                    && row.getPayload().endsWith(WorkflowState.DISPOSED.name())) {
                    wfDisposeLatch.countDown();
                }
            }
        }
    }

}
