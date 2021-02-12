/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallbackService;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Implementation of {@link ComponentExecutionController}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ComponentExecutionControllerImpl implements ComponentExecutionController {

    private static final Log LOG = LogFactory.getLog(ComponentExecutionControllerImpl.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    private static ComponentExecutionRelatedInstancesFactory compExeInstancesFactory;

    private ComponentExecutionRelatedInstances compExeRelatedInstances;

    private String compInstanceName;

    private Object verifyLock = new Object();

    @Deprecated // only for use by test code
    public ComponentExecutionControllerImpl() {}

    public ComponentExecutionControllerImpl(ComponentExecutionContext compExeCtx,
        WorkflowExecutionControllerCallbackService wfExeCtrlBridge, NetworkDestination wfStorageNetworkDestination,
        long currentTimestampOffWorkflowNode) {
        this.compInstanceName = compExeCtx.getInstanceName();
        int timestampOffsetToWorkfowNode = (int) (currentTimestampOffWorkflowNode - System.currentTimeMillis());

        compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeCtx = compExeCtx;
        compExeRelatedInstances.timestampOffsetToWorkfowNode = timestampOffsetToWorkfowNode;
        compExeRelatedInstances.wfExeCtrlBridge = wfExeCtrlBridge;
        compExeRelatedInstances.wfStorageNetworkDestination = wfStorageNetworkDestination;

        compExeRelatedInstances.compExeStorageBridge =
            compExeInstancesFactory.createComponentExecutionStorageBridge(compExeRelatedInstances);
        compExeRelatedInstances.compStateMachine = compExeInstancesFactory.createComponentStateMachine(compExeRelatedInstances);
        compExeRelatedInstances.compExeScheduler = compExeInstancesFactory.createComponentExecutionScheduler(compExeRelatedInstances);
    }

    @Override
    public void prepare() {
        compExeRelatedInstances.compStateMachine
            .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PREPARE_REQUESTED));
    }

    @Override
    public void start() {
        compExeRelatedInstances.compStateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_REQUESTED));
    }

    @Override
    public void pause() {
        compExeRelatedInstances.compStateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PAUSE_REQUESTED));
    }

    @Override
    public void resume() {
        compExeRelatedInstances.compStateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESUME_REQUESTED));
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException("Restarting components not yet implemented");
    }

    @Override
    public void cancel() {
        compExeRelatedInstances.compStateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_REQUESTED));
    }

    @Override
    public boolean cancelSync(long timeoutMsec) throws InterruptedException {
        cancel();
        final int waitIntervalMsec = 400;
        long startTime = System.currentTimeMillis();
        do {
            Thread.sleep(waitIntervalMsec);
            if (ComponentConstants.FINAL_COMPONENT_STATES.contains(compExeRelatedInstances.compStateMachine.getState())) {
                return true;
            }
        } while (System.currentTimeMillis() - startTime > timeoutMsec);
        return false;
    }

    @Override
    public void dispose() {
        compExeRelatedInstances.compStateMachine
            .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.DISPOSE_REQUESTED));
    }

    @Override
    public void onEndpointDatumReceived(final EndpointDatum endpointDatum) {
        compExeRelatedInstances.compExeScheduler.validateAndQueueEndpointDatum(endpointDatum);
        if (VERBOSE_LOGGING) {
            LOG.debug(StringUtils.format("Received at %s@%s: %s (from %s)", endpointDatum.getInputName(), compInstanceName,
                endpointDatum.getValue(), endpointDatum.getOutputsComponentExecutionIdentifier()));
        }
    }

    @Override
    public void onSendingEndointDatumFailed(EndpointDatum endpointDatum, RemoteOperationException e) {
        compExeRelatedInstances.compStateMachine
            .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED,
                new ComponentExecutionException(StringUtils.format("Failed to send output value to input '%s' of '%s' at %s",
                    endpointDatum.getInputName(), endpointDatum.getInputsComponentInstanceName(),
                    endpointDatum.getDestinationNodeId()), e)));
    }

    @Override
    public ComponentState getState() {
        return compExeRelatedInstances.compStateMachine.getState();
    }

    @Override
    public String getVerificationToken() {
        return compExeRelatedInstances.compStateMachine.getVerificationToken();
    }

    @Override
    public boolean isWorkflowControllerReachable() {
        return compExeRelatedInstances.compStateMachine.isWorkflowControllerReachable();
    }

    @Override
    public boolean verifyResults(String verificationToken, boolean verified) {

        synchronized (verifyLock) {
            if (compExeRelatedInstances.compStateMachine.getVerificationToken() == null
                || !compExeRelatedInstances.compStateMachine.getVerificationToken().equals(verificationToken)) {
                return false;
            }
            if (verified) {
                compExeRelatedInstances.compStateMachine
                    .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESULTS_APPROVED));
            } else {
                compExeRelatedInstances.compStateMachine
                    .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESULTS_REJECTED));
            }
            return true;
        }
    }

    protected void bindComponentExecutionRelatedInstancesFactory(ComponentExecutionRelatedInstancesFactory newService) {
        ComponentExecutionControllerImpl.compExeInstancesFactory = newService;
    }

    protected ComponentExecutionRelatedInstances geComponentExecutionRelatedInstances() {
        return compExeRelatedInstances;
    }

}
