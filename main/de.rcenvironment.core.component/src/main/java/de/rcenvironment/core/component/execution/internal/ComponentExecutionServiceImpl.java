/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link ComponentExecutionService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (id and caching adaptations)
 */
public class ComponentExecutionServiceImpl implements ComponentExecutionService {

    private CommunicationService communicationService;

    private ComponentExecutionControllerService cmpExeCtrlService;
    
    private PlatformService platformService;

    @Override
    public String init(ComponentExecutionContext executionContext, String authToken, Long referenceTimestamp)
        throws RemoteOperationException, ComponentExecutionException {
        return getExecutionControllerService(executionContext.getNodeId())
            .createExecutionController(executionContext, authToken, referenceTimestamp);
    }

    @Override
    public void pause(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPause(executionId);
    }

    @Override
    public void resume(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performResume(executionId);
    }

    @Override
    public void cancel(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performCancel(executionId);
    }

    @Override
    public void dispose(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performDispose(executionId);
    }

    @Override
    public void prepare(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performPrepare(executionId);
    }

    @Override
    public void start(String executionId, ResolvableNodeId node) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(node).performStart(executionId);
    }

    @Override
    public ComponentState getComponentState(String executionId, ResolvableNodeId node) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(node).getComponentState(executionId);
    }

    @Override
    public ComponentExecutionInformation getComponentExecutionInformation(final String verificationToken) throws RemoteOperationException {

        final AtomicReference<RemoteOperationException> remoteOperationExceptionRef = new AtomicReference<RemoteOperationException>(null);
        
        CallablesGroup<ComponentExecutionInformation> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(ComponentExecutionInformation.class);
        for (LogicalNodeId logicalNodeId : communicationService.getReachableLogicalNodes()) {
            final LogicalNodeId nodeId = logicalNodeId;
            callablesGroup.add(new Callable<ComponentExecutionInformation>() {

                @TaskDescription("Fetching component information")
                @Override
                public ComponentExecutionInformation call() throws Exception {
                    ComponentExecutionInformation compExeInfo =
                        getExecutionControllerService(nodeId).getComponentExecutionInformation(verificationToken);
                    if (compExeInfo != null) {
                        return compExeInfo;
                    }
                    return null;
                }
            });
            List<ComponentExecutionInformation> compExeInfos = callablesGroup.executeParallel(new AsyncExceptionListener() {
                @Override
                public void onAsyncException(Exception e) {
                    if (e instanceof RemoteOperationException && remoteOperationExceptionRef.get() == null) {
                        remoteOperationExceptionRef.set((RemoteOperationException) e);
                    }
                    LogFactory.getLog(ComponentExecutionServiceImpl.class)
                        .error("Error in asychronous request when retrieving component execution information for a verification key", e);
                }
            });
            
            if (remoteOperationExceptionRef.get() != null) {
                throw remoteOperationExceptionRef.get();
            }
            
            for (ComponentExecutionInformation compExeInfo : compExeInfos) {
                if (compExeInfo != null) {
                    return compExeInfo;
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean verifyResults(String executionId, ResolvableNodeId node, String verificationToken, boolean verified)
        throws ExecutionControllerException, RemoteOperationException {
        return getExecutionControllerService(node).performVerifyResults(executionId, verificationToken, verified);
    }

    @Override
    public Set<ComponentExecutionInformation> getLocalComponentExecutionInformations() {
        return new HashSet<ComponentExecutionInformation>(cmpExeCtrlService.getComponentExecutionInformations());
    }

    private RemotableComponentExecutionControllerService getExecutionControllerService(ResolvableNodeId node) {
        if (platformService.matchesLocalInstance(node)) {
            return cmpExeCtrlService;
        } else {
            // fetching the service proxy on each call, assuming that it will be cached centrally if necessary
            return communicationService.getRemotableService(RemotableComponentExecutionControllerService.class, node);
        }
    }

    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }
    
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    protected void bindComponentExecutionControllerService(ComponentExecutionControllerService newService) {
        cmpExeCtrlService = newService;
    }

}
