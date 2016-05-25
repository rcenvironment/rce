/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.map.LRUMap;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link ComponentExecutionService}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionServiceImpl implements ComponentExecutionService {

    private static final int CACHE_SIZE = 20;
    
    private CommunicationService communicationService;
    
    private ComponentExecutionControllerService cmpExeCtrlService;
    
    private Map<String, WeakReference<RemotableComponentExecutionControllerService>> executionControllerServices = new LRUMap<>(CACHE_SIZE);
    
    @Override
    public String init(ComponentExecutionContext executionContext, String authToken, Long referenceTimestamp)
        throws RemoteOperationException, ComponentExecutionException {
        return getExecutionControllerService(executionContext.getNodeId())
                .createExecutionController(executionContext, authToken, referenceTimestamp);
    }
    
    @Override
    public void pause(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPause(executionId);
    }

    @Override
    public void resume(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performResume(executionId);
    }

    @Override
    public void cancel(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performCancel(executionId);
    }

    @Override
    public void dispose(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performDispose(executionId);
    }

    @Override
    public void prepare(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPrepare(executionId);
    }
    
    @Override
    public void start(String executionId, NodeIdentifier node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performStart(executionId);
    }
    
    @Override
    public ComponentState getComponentState(String executionId, NodeIdentifier node) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(node).getComponentState(executionId);
    }

    @Override
    public Set<ComponentExecutionInformation> getLocalComponentExecutionInformations() {
        return new HashSet<ComponentExecutionInformation>(cmpExeCtrlService.getComponentExecutionInformations());
    }
    
    private RemotableComponentExecutionControllerService getExecutionControllerService(NodeIdentifier node) {
        RemotableComponentExecutionControllerService compCtrlService = null;
        synchronized (executionControllerServices) {
            if (executionControllerServices.containsKey(node.getIdString())) {
                compCtrlService = executionControllerServices.get(node.getIdString()).get();
            }
            if (compCtrlService == null) {
                compCtrlService = communicationService.getRemotableService(RemotableComponentExecutionControllerService.class, node);
                executionControllerServices.put(node.getIdString(),
                    new WeakReference<RemotableComponentExecutionControllerService>(compCtrlService));
            }
        }
        return compCtrlService;
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }
    
    protected void bindComponentExecutionControllerService(ComponentExecutionControllerService newService) {
        cmpExeCtrlService = newService;
    }

}
