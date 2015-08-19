/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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
import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.execution.api.ExecutionContext;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionInformationImpl;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link ComponentExecutionControllerService}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionControllerServiceImpl implements ComponentExecutionControllerService {

    private static final Log LOG = LogFactory.getLog(ComponentExecutionControllerServiceImpl.class);
    /**
     * Wait one minute max for component to get cancelled. If it takes longer, it will cancel in the background and will be disposed the
     * next time of garbage collecting or the time afterwards etc.
     */
    private static final int CANCEL_TIMEOUT_MSEC = 60 * 1000;
    
    private static final int COMPONENT_CONTROLLER_GARBAGE_COLLECTION_INTERVAL_MSEC = 90 * 1000;
    
    private BundleContext bundleContext;

    private CommunicationService communicationService;
    
    private DistributedComponentKnowledgeService compKnowledgeService;
    
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
        
        componentControllerGarbargeCollectionFuture = SharedThreadPool.getInstance().scheduleAtFixedRate(new Runnable() {

            @Override
            @TaskDescription("Garbage collection: Component controllers")
            public void run() {
//                LOG.debug("Running garbage collection for component controllers");
                Set<String> compExeIds = new HashSet<>(componentExecutionInformations.keySet());
                for (String compExeId : compExeIds) {
                    ComponentExecutionController componentController = getComponentController(compExeId);
                    if (!componentController.isWorkflowControllerReachable()) {
                        LOG.debug("Found component controller with unreachable workflow controller: " + compExeId);
                        if (!ComponentConstants.FINAL_COMPONENT_STATES_WITH_DISPOSED.contains(componentController.getState())) {
                            try {
                                LOG.debug("Cancel component controller: " + compExeId);
                                componentController.cancelSync(CANCEL_TIMEOUT_MSEC);
                            } catch (InterruptedException e) {
                                Thread.interrupted(); // ignore and try to go further
                            } catch (RuntimeException e) {
                                LOG.error("Cancelling component during garbage collecting failed: " + compExeId, e);
                            }
                        }
                        if (ComponentConstants.FINAL_COMPONENT_STATES.contains(componentController.getState())) {
                            try {
                                LOG.debug("Dispose component controller: " + compExeId);
                                performDispose(compExeId);
                            } catch (RuntimeException e) {
                                LOG.error("Disposing component during garbage collecting failed: " + compExeId, e);
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
    public void addComponentExecutionAuthToken(String authToken) {
        executionAuthTokens.add(authToken);
    }

    @Override
    @AllowRemoteAccess
    public String createExecutionController(ComponentExecutionContext compExeCtx, String authToken,
        Long currentTimestampOnWorkflowNode) throws ComponentExecutionException {

        if (!isAllowed(compExeCtx, authToken)) {
            throw new ComponentExecutionException("No valid auth token given.");
        }

        Map<String, String> searchProperties = new HashMap<>();
        searchProperties.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, compExeCtx.getWorkflowExecutionIdentifier());

        WorkflowExecutionControllerCallback componentExecutionEventCallback =
            (WorkflowExecutionControllerCallback) communicationService.getService(
                WorkflowExecutionControllerCallback.class, searchProperties, compExeCtx.getWorkflowNodeId(), bundleContext);
        ComponentExecutionController componentController =
            new ComponentExecutionControllerImpl(compExeCtx, componentExecutionEventCallback,
                currentTimestampOnWorkflowNode);

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
    public void performPrepare(String executionId) {
        getComponentController(executionId).prepare();
    }

    @Override
    @AllowRemoteAccess
    public void performStart(String executionId) {
        getComponentController(executionId).start();
    }
    
    @Override
    @AllowRemoteAccess
    public void performCancel(String executionId) {
        getComponentController(executionId).cancel();
    }

    @Override
    @AllowRemoteAccess
    public void performPause(String executionId) {
        getComponentController(executionId).pause();
    }

    @Override
    @AllowRemoteAccess
    public void performResume(String executionId) {
        getComponentController(executionId).resume();
    }
    
    @Override
    @AllowRemoteAccess
    public void performDispose(String executionId) {
        getComponentController(executionId).dispose();
        
        synchronized (componentExecutionInformations) {
            componentExecutionInformations.remove(executionId);
            if (componentServiceRegistrations.containsKey(executionId)) {
                componentServiceRegistrations.get(executionId).unregister();
                componentServiceRegistrations.remove(executionId);
            }
        }
    }

    private String createPropertyFilter(String compCtrlId) {
        return String.format("(%s=%s)", ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, compCtrlId);
    }

    private ComponentExecutionController getComponentController(String executionId) {

        String filter = createPropertyFilter(executionId);
        try {
            ServiceReference[] serviceReferences = bundleContext.getServiceReferences(ComponentExecutionController.class.getName(), filter);
            if (serviceReferences != null) {
                for (ServiceReference<?> ref : serviceReferences) {
                    return (ComponentExecutionController) bundleContext.getService(ref);
                }
            }
        } catch (InvalidSyntaxException e) {
            // should not happen
            LogFactory.getLog(getClass()).error(String.format("Filter '%s' is not valid", filter));
        }
        throw new RuntimeException(String.format("%s with id '%s' not registered as OSGi service",
            ComponentExecutionController.class.getSimpleName(), executionId));
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }
    
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService componentKnowledgeService) {
        this.compKnowledgeService = componentKnowledgeService;
    }

    @Override
    public ComponentState getComponentState(String executionId) {
        return getComponentController(executionId).getState();
    }

    @Override
    public Collection<ComponentExecutionInformation> getComponentExecutionInformations() {
        return new HashSet<ComponentExecutionInformation>(componentExecutionInformations.values());
    }

}
