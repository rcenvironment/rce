/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.authorization.api.ComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.execution.api.ComponentControllerRoutingMap;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.execution.api.ExecutionContext;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallbackService;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionContextImpl;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionInformationImpl;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumRecipientImpl;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Implementation of {@link ComponentExecutionControllerService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
@Component
public class ComponentExecutionControllerServiceImpl
    implements ComponentExecutionControllerService, RemotableComponentExecutionControllerService {

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    /**
     * Wait one minute max for component to get cancelled. If it takes longer, it will cancel in the background and will be disposed the
     * next time of garbage collecting or the time afterwards etc.
     */
    private static final int CANCEL_TIMEOUT_MSEC = 60 * 1000;

    private static final int COMPONENT_CONTROLLER_GARBAGE_COLLECTION_INTERVAL_MSEC = 90 * 1000;

    private BundleContext bundleContext;

    private CommunicationService communicationService;

    private LocalExecutionControllerUtilsService exeCtrlUtilsService;

    private DistributedComponentKnowledgeService compKnowledgeService;

    private ComponentExecutionAuthorizationService componentExecutionAuthorizationService;

    private EndpointDatumSerializer endpointDatumSerializer;

    private Map<String, ServiceRegistration<?>> componentServiceRegistrations = Collections.synchronizedMap(
        new HashMap<String, ServiceRegistration<?>>());

    private Map<String, ComponentExecutionInformation> componentExecutionInformations = Collections.synchronizedMap(
        new HashMap<String, ComponentExecutionInformation>());

    private ScheduledFuture<?> componentControllerGarbargeCollectionFuture;

    private final Log log = LogFactory.getLog(getClass());

    private PlatformService platformService;

    @Activate
    protected void activate(BundleContext context) {
        bundleContext = context;

        componentControllerGarbargeCollectionFuture = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedInterval(
            "Garbage collection: Component controllers", this::runGarbageCollection, COMPONENT_CONTROLLER_GARBAGE_COLLECTION_INTERVAL_MSEC);
    }

    @Deactivate
    protected void deactivate() {
        if (componentControllerGarbargeCollectionFuture != null) {
            componentControllerGarbargeCollectionFuture.cancel(true);
        }
    }

    @Override
    @AllowRemoteAccess
    public String createExecutionController(ComponentExecutionContext compExeCtx, String authToken,
        Long currentTimestampOnWorkflowNode) throws ComponentExecutionException, RemoteOperationException {

        if (!checkComponentExecutionAuthorization(compExeCtx, authToken)) {
            log.debug("Verification of a provided authorization token failed; aborting creation of component controller "
                + compExeCtx.getExecutionIdentifier());
            throw new ComponentExecutionException(StringUtils.format(
                "The workflow initiator's permission to execute \"%s\" could not be verified; "
                    + "if you think this is an error, try starting the workflow again",
                compExeCtx.getInstanceName()));
        }

        // TODO this does not seem to be used; remove?
        Map<String, String> searchProperties = new HashMap<>();
        searchProperties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, compExeCtx.getWorkflowExecutionIdentifier());
        // end of unused block

        ComponentControllerRoutingMap componentControllerRoutingMap = new ComponentControllerRoutingMap();

        final LogicalNodeId wfControllerNodeId = compExeCtx.getWorkflowNodeId();
        final LogicalNodeId wfStorageNodeId = compExeCtx.getStorageNodeId();
        // verify assumption of controller == storage node; if this changes, the code below must be adapted
        if (!wfControllerNodeId.equals(wfStorageNodeId)) {
            throw new IllegalStateException();
        }

        // create a shared reliable RPC session and set them as abstract NetworkDestinations for controller and storage communication
        NetworkDestination commonWfControllerNetworkDestination;
        try {
            // TODO this could be optimized by using the direct (local) node id if the wf controller is on the same node;
            // alternatively, let the rRPC system detect that internally -- misc_ro
            commonWfControllerNetworkDestination = communicationService.createReliableRPCStream(wfControllerNodeId);
        } catch (RemoteOperationException e) {
            throw new ComponentExecutionException("Failed to set up a reliable communication channel "
                + "from component to workflow controller", e);
        }

        ((ComponentExecutionContextImpl) compExeCtx).setStorageNetworkDestination(commonWfControllerNetworkDestination);

        WorkflowExecutionControllerCallbackService wfExeCtrlCallbackService =
            communicationService.getRemotableService(WorkflowExecutionControllerCallbackService.class,
                commonWfControllerNetworkDestination);

        // deserialize recipients of runtime component output
        // TODO (p3) get rid of the ugly typecast? this should probably all go into a factory or similar
        final List<EndpointDatumRecipient> deserializedEndpointDatumRecipients =
            ((ComponentExecutionContextImpl) compExeCtx).deserializeEndpointDatumRecipients(communicationService);
        // create and assign rRPC streams or target network ids to these recipients
        for (EndpointDatumRecipient recipient : deserializedEndpointDatumRecipients) {
            final String componentExecutionIdentifier = recipient.getInputsComponentExecutionIdentifier();
            NetworkDestination networkDestination = componentControllerRoutingMap
                .getNetworkDestinationForComponentController(componentExecutionIdentifier);
            if (networkDestination == null) {
                networkDestination = determineNetworkDestinationForComponentController(recipient, wfControllerNodeId);
                componentControllerRoutingMap.setNetworkDestinationForComponentController(componentExecutionIdentifier, networkDestination);
            }
            ((EndpointDatumRecipientImpl) recipient).setNetworkDestination(networkDestination);
        }

        ComponentExecutionController componentController =
            new ComponentExecutionControllerImpl(compExeCtx, wfExeCtrlCallbackService, commonWfControllerNetworkDestination,
                currentTimestampOnWorkflowNode);

        final String executionId = compExeCtx.getExecutionIdentifier();
        registerExecutionController(componentController, executionId);

        ComponentExecutionInformationImpl componentExecutionInformation = new ComponentExecutionInformationImpl(compExeCtx);
        synchronized (componentExecutionInformations) {
            componentExecutionInformations.put(compExeCtx.getExecutionIdentifier(), componentExecutionInformation);
        }

        return compExeCtx.getExecutionIdentifier();
    }

    private NetworkDestination determineNetworkDestinationForComponentController(EndpointDatumRecipient recipient,
        ResolvableNodeId workflowControllerNodeId)
        throws RemoteOperationException {
        final LogicalNodeId destinationNodeId = recipient.getDestinationNodeId();
        final NetworkDestination networkDestination;
        // TODO (p2) it would be better to receive a logical node SESSION id from upstream for added safety -- misc_ro
        if (platformService.matchesLocalInstance(destinationNodeId)) {
            // target component is on the same node as the sender -> simply set the local node id to use local service calls
            networkDestination = destinationNodeId;
        } else if (isDestinationNodeReachable(destinationNodeId)) { // is the target node visible/reachable?
            // create a new rRPC stream, contacting the target node directly
            networkDestination = communicationService.createReliableRPCStream(recipient.getDestinationNodeId());
        } else {
            log.debug("Cannot create a direct connection to component " + recipient.getInputsComponentExecutionIdentifier() + " on "
                + recipient.getInputsComponentInstanceName() + "; creating an indirect routing via the workflow controller");
            // create a new rRPC stream to the wf controller, contacting the target node indirectly
            // NOTE: it is a design/performance decision whether this should use a shared or an individual stream; adjust as needed
            networkDestination = communicationService.createReliableRPCStream(workflowControllerNodeId);
        }
        return networkDestination;
    }

    private boolean isDestinationNodeReachable(final LogicalNodeId destinationNodeId) {
        // checking the reachability of the equivalent default logical node id for now; could be changed once we have active
        // logical node announcements -- misc_ro
        return communicationService.getReachableLogicalNodes().contains(destinationNodeId.convertToDefaultLogicalNodeId());
    }

    private boolean checkComponentExecutionAuthorization(ExecutionContext executionContext, String authToken) {
        if (authToken == null) {
            log.error("Received a 'null' authorization token for component execution " + executionContext.getExecutionIdentifier());
            return false;
        }
        return componentExecutionAuthorizationService.verifyAndUnregisterExecutionToken(authToken);
    }

    @Override
    @AllowRemoteAccess
    public void performPrepare(String executionId) throws ExecutionControllerException, RemoteOperationException {
        getExecutionController(executionId).prepare();
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
    public ComponentExecutionInformation getComponentExecutionInformation(String verificationToken) throws RemoteOperationException {
        for (Entry<String, ComponentExecutionController> entry : listExecutionControllers().entrySet()) {
            if (entry.getValue().getVerificationToken() != null && entry.getValue().getVerificationToken().equals(verificationToken)) {
                return componentExecutionInformations.get(entry.getKey());
            }
        }
        return null;
    }

    @Override
    @AllowRemoteAccess
    public Boolean performVerifyResults(String executionId, String verificationToken, Boolean verified)
        throws ExecutionControllerException, RemoteOperationException {
        return getExecutionController(executionId)
            .verifyResults(verificationToken, verified);
    }

    @Override
    @AllowRemoteAccess
    public void performDispose(String executionId) throws ExecutionControllerException, RemoteOperationException {
        try {
            getExecutionController(executionId).dispose();
        } catch (ServiceException e) {
            log.warn("Ignored component disposal request as there is no component controller registered (anymore);"
                + " most likely disposal was requested more than once: " + e.toString());
        }

        synchronized (componentExecutionInformations) {
            componentExecutionInformations.remove(executionId);
        }
        unregisterExecutionController(executionId);
    }

    @Override
    @AllowRemoteAccess
    public ComponentState getComponentState(String executionId) throws ExecutionControllerException, RemoteOperationException {
        return getExecutionController(executionId).getState();
    }

    @Override
    public Collection<ComponentExecutionInformation> getComponentExecutionInformations() {
        synchronized (componentExecutionInformations) {
            return new HashSet<ComponentExecutionInformation>(componentExecutionInformations.values());
        }
    }

    @Override
    public void onSendingEndointDatumFailed(String executionId, String serializedEndpointDatum, RemoteOperationException e)
        throws ExecutionControllerException {
        getExecutionController(executionId)
            .onSendingEndointDatumFailed(endpointDatumSerializer.deserializeEndpointDatum(serializedEndpointDatum), e);
    }

    @Reference
    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    @Reference
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    @Reference
    protected void bindLocalExecutionControllerUtilsService(LocalExecutionControllerUtilsService newService) {
        exeCtrlUtilsService = newService;
    }

    @Reference
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService componentKnowledgeService) {
        this.compKnowledgeService = componentKnowledgeService;
    }

    @Reference
    protected void bindEndpointDatumSerializer(EndpointDatumSerializer newService) {
        this.endpointDatumSerializer = newService;
    }

    @Reference
    protected void bindComponentExecutionAuthorizationService(ComponentExecutionAuthorizationService newService) {
        this.componentExecutionAuthorizationService = newService;
    }

    // TODO there is no real point of registering the controllers at the OSGi service registry - simply use a map instead? -- misc_ro
    private void registerExecutionController(ComponentExecutionController componentController, final String executionId) {
        Dictionary<String, String> registerProperties = new Hashtable<String, String>();
        registerProperties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, executionId);
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(ComponentExecutionController.class.getName(),
            componentController, registerProperties);
        synchronized (componentServiceRegistrations) {
            componentServiceRegistrations.put(executionId, serviceRegistration);
        }
    }

    private Map<String, ComponentExecutionController> listExecutionControllers() {
        return exeCtrlUtilsService.getExecutionControllers(ComponentExecutionController.class, bundleContext);
    }

    private ComponentExecutionController getExecutionController(String executionId) throws ExecutionControllerException {
        return exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext);
    }

    private void unregisterExecutionController(String executionId) {
        synchronized (componentServiceRegistrations) {
            if (componentServiceRegistrations.containsKey(executionId)) {
                componentServiceRegistrations.get(executionId).unregister();
                componentServiceRegistrations.remove(executionId);
            }
        }
    }

    private void runGarbageCollection() {
        Set<String> compExeIds = new HashSet<>(componentExecutionInformations.keySet());
        if (VERBOSE_LOGGING) {
            log.debug("Running garbage collection for component controllers: " + compExeIds);
        }
        for (String executionId : compExeIds) {
            ComponentExecutionController componentController = null;
            try {
                componentController = getExecutionController(executionId);
            } catch (ExecutionControllerException e) {
                log.debug(StringUtils.format("Component controller garbage collection: Skip component controller: %s; cause: %s",
                    executionId, e.getMessage()));
                continue;
            }
            if (!componentController.isWorkflowControllerReachable()) {
                log.debug("Found component controller with unreachable workflow controller: " + executionId);
                if (!ComponentConstants.FINAL_COMPONENT_STATES_WITH_DISPOSED.contains(componentController.getState())) {
                    try {
                        log.debug("Cancel component controller: " + executionId);
                        componentController.cancelSync(CANCEL_TIMEOUT_MSEC);
                    } catch (InterruptedException e) {
                        Thread.interrupted(); // ignore and try to go further
                    } catch (RuntimeException e) {
                        log.error("Cancelling component during garbage collecting failed: " + executionId, e);
                    }
                }
                if (ComponentConstants.FINAL_COMPONENT_STATES.contains(componentController.getState())) {
                    try {
                        log.debug("Dispose component controller: " + executionId);
                        performDispose(executionId);
                    } catch (ExecutionControllerException | RemoteOperationException e) {
                        log.error(StringUtils.format("Failed to dispose component during garbage collecting: %s; cause: %s",
                            executionId, e.toString()));
                    }
                }
            }
        }
    }
}
