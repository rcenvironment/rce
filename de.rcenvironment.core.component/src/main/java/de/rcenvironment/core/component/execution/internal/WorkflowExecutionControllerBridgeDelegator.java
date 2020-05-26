/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * {@link WorkflowExecutionControllerCallback} which delegates the method calls if the workflow execution controller is reachable. Prevent
 * constantly recurring failures and stack traces.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionControllerBridgeDelegator implements WorkflowExecutionControllerCallback {

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionControllerBridgeDelegator.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    private static final int MAX_CALLBACK_FAILURES = 5;

    private final ComponentExecutionRelatedInstances compExeRelatedInstances;

    private final String wfExeId;

    private AtomicInteger wfExeCtrlCallbackFailureCount = new AtomicInteger(0);

    protected WorkflowExecutionControllerBridgeDelegator(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        this.compExeRelatedInstances = compExeRelatedInstances;
        this.wfExeId = compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier();
    }

    protected boolean isWorkflowControllerReachable() {
        return wfExeCtrlCallbackFailureCount.get() < MAX_CALLBACK_FAILURES;
    }

    @Override
    public synchronized void processConsoleRows(final ConsoleRow[] consoleRows) {
        new WorkflowExecutionControllerCallbackCheckingReachability() {

            @Override
            protected void call() throws ExecutionControllerException, RemoteOperationException {
                compExeRelatedInstances.wfExeCtrlBridge.onConsoleRowsProcessed(wfExeId, consoleRows);
            }
        }.callback();
    }

    @Override
    public synchronized void onComponentStateChanged(final String compExeId, final ComponentState newState, final Integer count,
        final String countOnResets) {
        onComponentStateChanged(compExeId, newState, count, countOnResets, null);
    }

    @Override
    public synchronized void onComponentStateChanged(final String compExeId, final ComponentState newState, final Integer count,
        final String countOnResets, final String errId) {
        onComponentStateChanged(compExeId, newState, count, countOnResets, errId, null);
    }

    @Override
    public synchronized void onComponentStateChanged(final String compExeId, final ComponentState newState, final Integer count,
        final String countOnResets, final String errId, final String errMessage) {
        new WorkflowExecutionControllerCallbackCheckingReachability() {

            @Override
            protected void call() throws ExecutionControllerException, RemoteOperationException {
                if (errId == null) {
                    compExeRelatedInstances.wfExeCtrlBridge.onComponentStateChanged(wfExeId, compExeId, newState, count, countOnResets);
                } else if (errMessage == null) {
                    compExeRelatedInstances.wfExeCtrlBridge.onComponentStateChanged(wfExeId, compExeId, newState, count, countOnResets,
                        errId);
                } else {
                    compExeRelatedInstances.wfExeCtrlBridge.onComponentStateChanged(wfExeId, compExeId, newState, count, countOnResets,
                        errId, errMessage);
                }
            }
        }.callback();
    }

    @Override
    public synchronized void onInputProcessed(final String serializedEndpointDatum) {
        if (isWorkflowControllerReachable()) {
            try {
                compExeRelatedInstances.wfExeCtrlBridge.onInputProcessed(wfExeId, serializedEndpointDatum);
                handleWorkflowControllerCallbackSuccess();
            } catch (ExecutionControllerException | RemoteOperationException e) {
                handleWorkflowControllerCallbackFailure(e);
            }
        }
    }

    @Override
    public synchronized void onComponentHeartbeatReceived(String compExeId) {
        if (isWorkflowControllerReachable()) {
            try {
                if (VERBOSE_LOGGING) {
                    LOG.debug(StringUtils.format("Component '%s' (%s) is sending heartbeat to workflow controller '%s' (%s)",
                        compExeRelatedInstances.compExeCtx.getInstanceName(),
                        compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                        compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                        compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier()));
                }
                compExeRelatedInstances.wfExeCtrlBridge.onComponentHeartbeatReceived(wfExeId, compExeId);
                handleWorkflowControllerCallbackSuccess();
            } catch (ExecutionControllerException | RemoteOperationException e) {
                handleWorkflowControllerCallbackFailure(e);
            }
        }
    }

    private void handleWorkflowControllerCallbackSuccess() {
        if (wfExeCtrlCallbackFailureCount.get() > 0) {
            LOG.debug(StringUtils.format("Callback from local component '%s' (%s) to workflow controller '%s' (%s)"
                + " succeeded again", compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier()));
        }
        wfExeCtrlCallbackFailureCount.set(0);
    }

    private void handleWorkflowControllerCallbackFailure(Throwable e) {
        int failureCount = wfExeCtrlCallbackFailureCount.incrementAndGet();
        String message =
            StringUtils.format("Callback from local component '%s' (%s) to workflow controller '%s' (%s) failed",
                compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier());
        registerCallbackFailureEvent(message, failureCount, e);
    }

    private void registerCallbackFailureEvent(String message, int failureCount, Throwable cause) {
        if (failureCount >= MAX_CALLBACK_FAILURES) {
            LOG.error(message + "; maximum number of workflow controller callback failures (" + MAX_CALLBACK_FAILURES
                + ") exceeded, considering the workflow controller unreachable; last cause: " + cause.toString());
            compExeRelatedInstances.compStateMachine.postEvent(new ComponentStateMachineEvent(
                ComponentStateMachineEventType.WF_CRTL_CALLBACK_FAILED, cause));
        } else {
            LOG.warn(message + "; failure count is " + failureCount + " (threshold: " + MAX_CALLBACK_FAILURES + "); cause: "
                + cause.toString());
        }
    }

    /**
     * Executes callbacks to the workflow execution controller if it is known as reachable.
     * 
     * @author Doreen Seider
     */
    private abstract class WorkflowExecutionControllerCallbackCheckingReachability {

        protected void callback() {
            if (isWorkflowControllerReachable()) {
                try {
                    call();
                } catch (ExecutionControllerException | RemoteOperationException e) {
                    wfExeCtrlCallbackFailureCount.set(MAX_CALLBACK_FAILURES);
                    compExeRelatedInstances.compStateMachine
                        .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.WF_CRTL_CALLBACK_FAILED, e));
                }
            }
        }

        protected abstract void call() throws ExecutionControllerException, RemoteOperationException;
    }
    
}
