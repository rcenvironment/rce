/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.LRUMap;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;

/**
 * Implementation of {@link ComponentExecutionService}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionServiceImpl implements ComponentExecutionService {

    private static final int CACHE_SIZE = 20;
    
    private BundleContext bundleContext;
    
    private CommunicationService communicationService;
    
    private PlatformService platformService;

    private Map<String, WeakReference<ComponentExecutionControllerService>> executionControllerServices = new LRUMap<>(CACHE_SIZE);
    
    protected void activate(BundleContext context) {
        bundleContext = context;
    }
    
    @Override
    public String init(ComponentExecutionContext executionContext, String authToken, Long referenceTimestamp) throws CommunicationException,
        ComponentExecutionException {
        
        try {
            return getExecutionControllerService(executionContext.getNodeId())
                .createExecutionController(executionContext, authToken, referenceTimestamp);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
            // won't be reached as #handleUndeclaredThrowableException always throws an exception
            return null;
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
    public void cancel(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performCancel(executionId);
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
    public void prepare(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performPrepare(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }
    
    @Override
    public void start(String executionId, NodeIdentifier node) throws CommunicationException {
        try {
            getExecutionControllerService(node).performStart(executionId);
        } catch (UndeclaredThrowableException e) {
            handleUndeclaredThrowableException(e);
        }
    }
    
    private ComponentExecutionControllerService getExecutionControllerService(NodeIdentifier node) {
        ComponentExecutionControllerService compCtrlService = null;
        synchronized (executionControllerServices) {
            if (executionControllerServices.containsKey(node.getIdString())) {
                compCtrlService = executionControllerServices.get(node.getIdString()).get();
            }
            if (compCtrlService == null) {
                compCtrlService = (ComponentExecutionControllerService) communicationService.getService(
                    ComponentExecutionControllerService.class, node, bundleContext);
                executionControllerServices.put(node.getIdString(),
                    new WeakReference<ComponentExecutionControllerService>(compCtrlService));
            }
        }
        return compCtrlService;
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
    
    @Override
    public ComponentState getComponentState(String executionId, NodeIdentifier node) throws CommunicationException {
        return getExecutionControllerService(node).getComponentState(executionId);
    }

    @Override
    public Set<ComponentExecutionInformation> getLocalComponentExecutionInformations() {
        return new HashSet<ComponentExecutionInformation>(getExecutionControllerService(platformService.getLocalNodeId())
            .getComponentExecutionInformations());
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

}
