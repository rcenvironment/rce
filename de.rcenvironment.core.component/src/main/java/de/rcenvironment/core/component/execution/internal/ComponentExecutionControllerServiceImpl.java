/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceRegistration;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
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
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallbackService;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionInformationImpl;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link ComponentExecutionControllerService}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionControllerServiceImpl implements ComponentExecutionControllerService {

    private static final Log LOG = LogFactory.getLog(ComponentExecutionControllerServiceImpl.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(ComponentExecutionControllerImpl.class);

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
    
    private EndpointDatumSerializer endpointDatumSerializer;

    // FIXME Currently, tokens, which were not used, are not removed over time-> memory leak, but
    // only minor because of small amount of
    // unused tokens and because of small size of each token. But anyways: token garbage collection
    // must be added -- seid_do, Nov 2013
    // (see: https://www.sistec.dlr.de/mantis/view.php?id=9539)
    private final Set<String> executionAuthTokens = Collections.synchronizedSet(new HashSet<String>());

    private Map<String, ServiceRegistration<?>> componentServiceRegistrations = Collections.synchronizedMap(
        new HashMap<String, ServiceRegistration<?>>());

    private Map<String, ComponentExecutionInformation> componentExecutionInformations = Collections.synchronizedMap(
        new HashMap<String, ComponentExecutionInformation>());

    private ScheduledFuture<?> componentControllerGarbargeCollectionFuture;

    protected void activate(BundleContext context) {
        bundleContext = context;

        componentControllerGarbargeCollectionFuture = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedRate(new Runnable() {

            @Override
            @TaskDescription("Garbage collection: Component controllers")
            public void run() {
                Set<String> compExeIds = new HashSet<>(componentExecutionInformations.keySet());
                if (VERBOSE_LOGGING) {
                    LOG.debug("Running garbage collection for component controllers: " + compExeIds);
                }
                for (String executionId : compExeIds) {
                    ComponentExecutionController componentController = null;
                    try {
                        componentController = exeCtrlUtilsService.getExecutionController(
                            ComponentExecutionController.class, executionId, bundleContext);
                    } catch (ExecutionControllerException e) {
                        LOG.debug(StringUtils.format("Component controller garbage collection: Skip component controller: %s; cause: %s",
                            executionId, e.getMessage()));
                        continue;
                    }
                    if (!componentController.isWorkflowControllerReachable()) {
                        LOG.debug("Found component controller with unreachable workflow controller: " + executionId);
                        if (!ComponentConstants.FINAL_COMPONENT_STATES_WITH_DISPOSED.contains(componentController.getState())) {
                            try {
                                LOG.debug("Cancel component controller: " + executionId);
                                componentController.cancelSync(CANCEL_TIMEOUT_MSEC);
                            } catch (InterruptedException e) {
                                Thread.interrupted(); // ignore and try to go further
                            } catch (RuntimeException e) {
                                LOG.error("Cancelling component during garbage collecting failed: " + executionId, e);
                            }
                        }
                        if (ComponentConstants.FINAL_COMPONENT_STATES.contains(componentController.getState())) {
                            try {
                                LOG.debug("Dispose component controller: " + executionId);
                                performDispose(executionId);
                            } catch (ExecutionControllerException | RemoteOperationException e) {
                                LOG.error(StringUtils.format("Failed to dispose component during garbage collecting: %s; cause: %s",
                                    executionId, e.toString()));
                            }
                        }
                    }
                }
            }
        }, COMPONENT_CONTROLLER_GARBAGE_COLLECTION_INTERVAL_MSEC);
    }

    protected void deactivate() {
        if (componentControllerGarbargeCollectionFuture != null) {
            componentControllerGarbargeCollectionFuture.cancel(true);
        }
    }

    @Override
    @AllowRemoteAccess
    public void addComponentExecutionAuthToken(String authToken) throws RemoteOperationException {
        executionAuthTokens.add(authToken);
    }

    @Override
    @AllowRemoteAccess
    public String createExecutionController(ComponentExecutionContext compExeCtx, String authToken,
        Long currentTimestampOnWorkflowNode) throws ComponentExecutionException, RemoteOperationException {

        if (!isAllowed(compExeCtx, authToken)) {
            throw new ComponentExecutionException("No valid auth token given.");
        }

        Map<String, String> searchProperties = new HashMap<>();
        searchProperties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, compExeCtx.getWorkflowExecutionIdentifier());

        WorkflowExecutionControllerCallbackService wfExeCtrlCallbackService;
        wfExeCtrlCallbackService =
            communicationService.getRemotableService(WorkflowExecutionControllerCallbackService.class, compExeCtx.getWorkflowNodeId());
        ComponentExecutionController componentController =
            new ComponentExecutionControllerImpl(compExeCtx, wfExeCtrlCallbackService, currentTimestampOnWorkflowNode);

        Dictionary<String, String> registerProperties = new Hashtable<String, String>();
        registerProperties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, compExeCtx.getExecutionIdentifier());
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(ComponentExecutionController.class.getName(),
            componentController, registerProperties);
        ComponentExecutionInformationImpl componentExecutionInformation = new ComponentExecutionInformationImpl(compExeCtx);

        synchronized (componentExecutionInformations) {
            componentExecutionInformations.put(compExeCtx.getExecutionIdentifier(), componentExecutionInformation);
            componentServiceRegistrations.put(compExeCtx.getExecutionIdentifier(), serviceRegistration);
        }

        return compExeCtx.getExecutionIdentifier();
    }

    private boolean isAllowed(ExecutionContext executionContext, String authToken) {
        Collection<ComponentInstallation> allPublishedInstallations = compKnowledgeService.getCurrentComponentKnowledge()
            .getAllPublishedInstallations();
        boolean published = false;
        for (ComponentInstallation compInst : allPublishedInstallations) {
            if (compInst.getInstallationId().equals(((ComponentExecutionContext) executionContext).getComponentDescription()
                .getComponentInstallation().getInstallationId())) {
                published = true;
            }
        }
        boolean authTokenExists = executionAuthTokens.remove(authToken);
        return published || authTokenExists;
    }

    @Override
    @AllowRemoteAccess
    public void performPrepare(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).prepare();
    }

    @Override
    @AllowRemoteAccess
    public void performStart(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).start();
    }

    @Override
    @AllowRemoteAccess
    public void performCancel(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).cancel();
    }

    @Override
    @AllowRemoteAccess
    public void performPause(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).pause();
    }

    @Override
    @AllowRemoteAccess
    public void performResume(String executionId) throws ExecutionControllerException, RemoteOperationException {
        exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).resume();
    }

    @Override
    @AllowRemoteAccess
    public ComponentExecutionInformation getComponentExecutionInformation(String verificationToken) throws RemoteOperationException {
        for (Entry<String, ComponentExecutionController> entry : exeCtrlUtilsService
            .getExecutionControllers(ComponentExecutionController.class, bundleContext).entrySet()) {
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
        return exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext)
            .verifyResults(verificationToken, verified);
    }

    @Override
    @AllowRemoteAccess
    public void performDispose(String executionId) throws ExecutionControllerException, RemoteOperationException {
        try {
            exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).dispose();
        } catch (ServiceException e) {
            LOG.warn("Ignored component disposal request as there is no component controller registered (anymore);"
                + " most likely disposal was requested more than once: " + e.toString());
        }

        synchronized (componentExecutionInformations) {
            componentExecutionInformations.remove(executionId);
            if (componentServiceRegistrations.containsKey(executionId)) {
                componentServiceRegistrations.get(executionId).unregister();
                componentServiceRegistrations.remove(executionId);
            }
        }
    }

    @Override
    @AllowRemoteAccess
    public ComponentState getComponentState(String executionId) throws ExecutionControllerException, RemoteOperationException {
        return exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext).getState();
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
        exeCtrlUtilsService.getExecutionController(ComponentExecutionController.class, executionId, bundleContext)
            .onSendingEndointDatumFailed(endpointDatumSerializer.deserializeEndpointDatum(serializedEndpointDatum), e);
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    protected void bindLocalExecutionControllerUtilsService(LocalExecutionControllerUtilsService newService) {
        exeCtrlUtilsService = newService;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService componentKnowledgeService) {
        this.compKnowledgeService = componentKnowledgeService;
    }

    protected void bindEndpointDatumSerializer(EndpointDatumSerializer newService) {
        this.endpointDatumSerializer = newService;
    }

}
