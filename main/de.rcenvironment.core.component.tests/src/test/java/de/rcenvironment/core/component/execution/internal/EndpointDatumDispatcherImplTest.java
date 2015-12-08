/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.execution.api.RemotableComponentExecutionControllerService;
import de.rcenvironment.core.component.execution.api.RemotableEndpointDatumDispatcher;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.testutils.EndpointDatumDefaultStub;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link EndpointDatumDispatcherImpl}.
 * 
 * @author Doreen Seider
 * 
 */
public class EndpointDatumDispatcherImplTest {

    private static final String LOCAL_INP_EXE_ID_1 = "local-inp-exe-id-1";
    
    private static final String LOCAL_INP_EXE_ID_2 = "local-inp-exe-id-2";

    private static final String LOCAL_INP_EXE_ID_3 = "local-inp-exe-id-3";

    private static final String REMOTE_INP_EXE_ID_1 = "remote-inp-exe-id-1";

    private static final String REMOTE_INP_EXE_ID_2 = "remote-inp-exe-id-2";

    private static final int TEST_TIMEOUT = 2000;
    
    private final BundleContext bundleContextMock = EasyMock.createStrictMock(BundleContext.class);
    
    private final RemoteOperationException remoteOperationException = new RemoteOperationException("ROE");
    
    private final NodeIdentifier localNodeId = NodeIdentifierFactory.fromNodeId("local-target-node");
    
    private final NodeIdentifier remoteCompNodeId = NodeIdentifierFactory.fromNodeId("remote-target-node");
    
    private final NodeIdentifier remoteWfCtrlNodeId = NodeIdentifierFactory.fromNodeId("wf-ctrl-node");
    
    private final NodeIdentifier reachableCompNodeId = NodeIdentifierFactory.fromNodeId("reachable-comp-node");
    
    private final NodeIdentifier unreachableCompNodeId = NodeIdentifierFactory.fromNodeId("unreachable-comp-node");
    
    
    /**
     * Set up before any of the test cases run.
     */
    @BeforeClass
    public static void setUp() {
        ComponentExecutionUtils.waitUntilRetryMsec = 1;
    }

    /**
     * Set up after all of the test cases run.
     */
    @AfterClass
    public static void tearDown() {
        ComponentExecutionUtils.waitUntilRetryMsec = ComponentExecutionUtils.WAIT_UNIL_RETRY_MSEC;
    }
    
    /**
     * Tests if decision is made correctly, whether {@link EndpointDatum}s are processed locally or forwarded to an
     * {@link RemotableEndpointDatumDispatcher} of another node.
     * @throws InterruptedException on unexpected error
     * @throws RemoteOperationException on unexpected error
     * @throws ExecutionControllerException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testDispatchEndpointDatum() throws InterruptedException, RemoteOperationException, ExecutionControllerException {
        
        final String serializedEndpointDatumToProcess1 = "serial-ED-to-process-1";
        final String serializedEndpointDatumToProcess2 = "serial-ED-to-process-2";
        final String serializedEndpointDatumToProcess3 = "serial-ED-to-process-3";
        final String serializedEndpointDatumToForward1 = "serial-ED-to-forward-1";
        final String serializedEndpointDatumToForward2 = "serial-ED-to-forward-2";
        final String serializedEndpointDatumToForwardFailing1 = "serial-ED-to-forward-failing-1";
        final String serializedEndpointDatumToForwardFailing2 = "serial-ED-to-forward-failing-2";
        final String serializedEndpointDatumToForwardFailing3 = "serial-ED-to-forward-failing-3";
        final String serializedEndpointDatumToForwardFailing4 = "serial-ED-to-forward-failing-4";
        
        EndpointDatum endpointDatumToProcessMock1 = createEndpointDatumMock(
            LOCAL_INP_EXE_ID_1, localNodeId, LOCAL_INP_EXE_ID_1, localNodeId);
        EndpointDatum endpointDatumToProcessMock2 = createEndpointDatumMock(
            LOCAL_INP_EXE_ID_2, localNodeId, LOCAL_INP_EXE_ID_3, localNodeId);
        EndpointDatum endpointDatumToProcessMock3 = createEndpointDatumMock(
            LOCAL_INP_EXE_ID_2, localNodeId, LOCAL_INP_EXE_ID_1, localNodeId);
        
        EndpointDatum endpointDatumToForwardMock1 = createEndpointDatumMock(
            REMOTE_INP_EXE_ID_1, remoteCompNodeId, LOCAL_INP_EXE_ID_2, localNodeId);
        EndpointDatum endpointDatumToForwardMock2 = createEndpointDatumMock(
            REMOTE_INP_EXE_ID_2, remoteCompNodeId, LOCAL_INP_EXE_ID_3, localNodeId);
        
        EndpointDatum endpointDatumToForwardFailingMock1 = createEndpointDatumMock(
            REMOTE_INP_EXE_ID_1, remoteCompNodeId, LOCAL_INP_EXE_ID_2, localNodeId);
        EndpointDatum endpointDatumToForwardFailingMock2 = createEndpointDatumMock(
            REMOTE_INP_EXE_ID_1, remoteCompNodeId, REMOTE_INP_EXE_ID_2, remoteCompNodeId);
        EndpointDatum endpointDatumToForwardFailingMock3 = createEndpointDatumMock(
            REMOTE_INP_EXE_ID_1, remoteCompNodeId, LOCAL_INP_EXE_ID_2, localNodeId);
        EndpointDatum endpointDatumToForwardFailingMock4 = createEndpointDatumMock(
            REMOTE_INP_EXE_ID_1, remoteCompNodeId, REMOTE_INP_EXE_ID_2, remoteCompNodeId);
        
        EndpointDatumSerializer endpointDatumSerializerMock = EasyMock.createNiceMock(EndpointDatumSerializer.class);
        
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToProcessMock1))
            .andReturn(serializedEndpointDatumToProcess1).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToProcess1))
            .andReturn(endpointDatumToProcessMock1).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToProcessMock2))
            .andReturn(serializedEndpointDatumToProcess2).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToProcess2))
            .andReturn(endpointDatumToProcessMock2).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToProcessMock3))
            .andReturn(serializedEndpointDatumToProcess3).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToProcess3))
            .andReturn(endpointDatumToProcessMock3).anyTimes();
        
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToForwardMock1))
            .andReturn(serializedEndpointDatumToForward1).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToForward1))
            .andReturn(endpointDatumToForwardMock1).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToForwardMock2))
            .andReturn(serializedEndpointDatumToForward2).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToForward2))
            .andReturn(endpointDatumToForwardMock2).anyTimes();
        
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToForwardFailingMock1))
            .andReturn(serializedEndpointDatumToForwardFailing1).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToForwardFailing1))
            .andReturn(endpointDatumToForwardFailingMock1).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToForwardFailingMock2))
            .andReturn(serializedEndpointDatumToForwardFailing2).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToForwardFailing2))
            .andReturn(endpointDatumToForwardFailingMock2).anyTimes();
        
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToForwardFailingMock3))
            .andReturn(serializedEndpointDatumToForwardFailing3).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToForwardFailing3))
            .andReturn(endpointDatumToForwardFailingMock3).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(endpointDatumToForwardFailingMock4))
            .andReturn(serializedEndpointDatumToForwardFailing4).anyTimes();
        EasyMock.expect(endpointDatumSerializerMock.deserializeEndpointDatum(serializedEndpointDatumToForwardFailing4))
            .andReturn(endpointDatumToForwardFailingMock4).anyTimes();
        
        EasyMock.replay(endpointDatumSerializerMock);
        
        ComponentExecutionController componentExecutionControllerMock = EasyMock.createStrictMock(ComponentExecutionController.class);
        componentExecutionControllerMock.onEndpointDatumReceived(endpointDatumToProcessMock1);
        componentExecutionControllerMock.onSendingEndointDatumFailed(endpointDatumToForwardFailingMock3, remoteOperationException);
        componentExecutionControllerMock.onEndpointDatumReceived(endpointDatumToProcessMock2);
        componentExecutionControllerMock.onEndpointDatumReceived(endpointDatumToProcessMock1);
        Capture<EndpointDatum> finalEndpointDatumSentCapture = new Capture<>();
        componentExecutionControllerMock.onEndpointDatumReceived(EasyMock.capture(finalEndpointDatumSentCapture));
        EasyMock.replay(componentExecutionControllerMock);
        
        RemotableComponentExecutionControllerService remotableCompExeCtrlServiceMock =
            createRemotableExecutionControllerService(endpointDatumToForwardFailingMock4);
        //  code disabled as long as retrying is disabled
        RemotableEndpointDatumDispatcher endpointDatumDispatcherMock = EasyMock.createStrictMock(RemotableEndpointDatumDispatcher.class);
        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForward1);
//        for (int i = 0; i < 5; i++) {
        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing3);
        EasyMock.expectLastCall().andThrow(remoteOperationException);
//        }
        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForward2);
//        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing1);
//        EasyMock.expectLastCall().andThrow(remoteOperationException);
//        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing1);
//        for (int i = 0; i < 5; i++) {
        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing4);
        EasyMock.expectLastCall().andThrow(remoteOperationException);
//        }
//        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForward2);
//        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing2);
//        EasyMock.expectLastCall().andThrow(remoteOperationException);
//        endpointDatumDispatcherMock.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing2);
        EasyMock.replay(endpointDatumDispatcherMock);
        
        EndpointDatumDispatcherImpl endpointDatumDispatcher = new EndpointDatumDispatcherImpl();
        endpointDatumDispatcher.activate(bundleContextMock);
        endpointDatumDispatcher.bindEndpointDatumSerializer(endpointDatumSerializerMock);
        endpointDatumDispatcher.bindPlatformService(createPlatformServiceMock());
        endpointDatumDispatcher.bindCommunicationService(
            createCommunicationServiceMock(remotableCompExeCtrlServiceMock, endpointDatumDispatcherMock));
        endpointDatumDispatcher.bindLocalExecutionControllerUtilsService(
            createLocalExecutionControllerUtilsServiceMock(componentExecutionControllerMock));
        
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToProcess1);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForward1);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing3);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForward2);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToProcess2);
//        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing1);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing4);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToProcess1);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToProcess3);
        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForward2);
//        endpointDatumDispatcher.dispatchEndpointDatum(serializedEndpointDatumToForwardFailing2);
        
        
        final int sleepInterval = 50;
        while (!finalEndpointDatumSentCapture.hasCaptured()) {
            Thread.sleep(sleepInterval);            
        }
        
        finalEndpointDatumSentCapture.getValue().equals(endpointDatumToProcessMock3);
        
        EasyMock.verify(componentExecutionControllerMock);
        EasyMock.verify(endpointDatumDispatcherMock);
        EasyMock.verify(remotableCompExeCtrlServiceMock);
    }
    
    /**
     * Tests if the retrying forwarding is able to succeed again.
     * 
     * @throws InterruptedException on expected errors
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testRetriesInCaseForwardingFailedAndSucceeded() throws InterruptedException {
        final NodeIdentifier inputNodeId = NodeIdentifierFactory.fromNodeId("input-node-id");
        final CountDownLatch forwardCalledLatch = new CountDownLatch(1);
        final EndpointDatumDispatcherImpl dispatcher = new EndpointDatumDispatcherImpl() {
            @Override
            protected void forwardEndpointDatum(NodeIdentifier node, EndpointDatum endpointDatum) {
                if (node.equals(inputNodeId) && endpointDatum.equals(endpointDatum)) {
                    forwardCalledLatch.countDown();                    
                } else {
                    fail("forwardEndpointDatum() called with unexpected endpoint datum");
                }
            }
            @Override
            protected void processEndpointDatum(String executionId, EndpointDatum endpointDatum) {
                fail("unexpected call of processEndpointDatum()");
            }
            
            @Override
            protected void callbackComponentExecutionController(EndpointDatum endpointDatum, RemoteOperationException e) {
                fail("unexpected call of callbackComponentExecutionController()");
            }
        };
        
        CommunicationService communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);
        Set<NodeIdentifier> nodeIds = new HashSet<>();
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(new HashSet<NodeIdentifier>());
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(new HashSet<NodeIdentifier>());
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(new HashSet<NodeIdentifier>());
        nodeIds.add(inputNodeId);
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(nodeIds).anyTimes();
        EasyMock.replay(communicationServiceMock);
        
        testRetriesInCaseForwardingFailed(communicationServiceMock, dispatcher, inputNodeId, forwardCalledLatch);
    }
    
    /**
     * Tests if the retrying forwarding doesn't end up in endless loop on continuous failures.
     * 
     * @throws InterruptedException on expected errors
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testRetriesInCaseForwardingFailed() throws InterruptedException {
        final NodeIdentifier inputNodeId = NodeIdentifierFactory.fromNodeId("input-node-id");
        final CountDownLatch callbackCalledLatch = new CountDownLatch(1);
        final EndpointDatumDispatcherImpl dispatcher = new EndpointDatumDispatcherImpl() {
            @Override
            protected void forwardEndpointDatum(NodeIdentifier node, EndpointDatum endpointDatum) {
                fail("unexpected call of forwardEndpointDatum()");
            }
            @Override
            protected void processEndpointDatum(String executionId, EndpointDatum endpointDatum) {
                fail("unexpected call of processEndpointDatum()");
            }
            
            @Override
            protected void callbackComponentExecutionController(EndpointDatum endpointDatum, RemoteOperationException e) {
                if (endpointDatum.equals(endpointDatum)) {
                    callbackCalledLatch.countDown();                    
                } else {
                    fail("callbackComponentExecutionController() called with unexpected endpoint datum");
                }
            }
        };
        
        CommunicationService communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(new HashSet<NodeIdentifier>()).anyTimes();
        EasyMock.replay(communicationServiceMock);
        
        testRetriesInCaseForwardingFailed(communicationServiceMock, dispatcher, inputNodeId, callbackCalledLatch);
    }
    
    private void testRetriesInCaseForwardingFailed(CommunicationService communicationServiceMock, 
        EndpointDatumDispatcherImpl dispatcher, NodeIdentifier inputNodeId, CountDownLatch methodCalledLatch) throws InterruptedException {
        final NodeIdentifier wfCtrlNodeId = NodeIdentifierFactory.fromNodeId("wf-ctrl-node-id");
        
        final EndpointDatum endpointDatumMock = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumMock.getInputsComponentExecutionIdentifier()).andReturn("comp-exe-id").anyTimes();
        EasyMock.expect(endpointDatumMock.getInputsNodeId()).andReturn(inputNodeId).anyTimes();
        EasyMock.expect(endpointDatumMock.getWorkflowNodeId()).andReturn(wfCtrlNodeId).anyTimes();
        EasyMock.replay(endpointDatumMock);
        
        PlatformService platformServiceMock = EasyMock.createStrictMock(PlatformService.class);
        EasyMock.expect(platformServiceMock.isLocalNode(inputNodeId)).andReturn(false);
        EasyMock.expect(platformServiceMock.isLocalNode(wfCtrlNodeId)).andReturn(true);
        EasyMock.replay(platformServiceMock);
        
        dispatcher.bindCommunicationService(communicationServiceMock);
        dispatcher.bindPlatformService(platformServiceMock);
        
        dispatcher.dispatchEndpointDatum(endpointDatumMock);
        
        methodCalledLatch.await();
    }


    private CommunicationService createCommunicationServiceMock(
        RemotableComponentExecutionControllerService remotableCompExeCtrlServiceMock,
        RemotableEndpointDatumDispatcher endpointDatumDispatcherMock) throws RemoteOperationException {
        CommunicationService communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);
        EasyMock.expect(communicationServiceMock.getRemotableService(RemotableEndpointDatumDispatcher.class, remoteCompNodeId))
            .andReturn(endpointDatumDispatcherMock).anyTimes();
        EasyMock.expect(communicationServiceMock.getRemotableService(RemotableComponentExecutionControllerService.class, remoteCompNodeId))
            .andReturn(remotableCompExeCtrlServiceMock).anyTimes();
        Set<NodeIdentifier> nodeIds = new HashSet<>();
        nodeIds.add(remoteCompNodeId);
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(nodeIds).anyTimes();
        EasyMock.replay(communicationServiceMock);
        return communicationServiceMock;
    }


    private PlatformService createPlatformServiceMock() {
        PlatformService platformServiceMock = EasyMock.createNiceMock(PlatformService.class);
        EasyMock.expect(platformServiceMock.isLocalNode(localNodeId)).andReturn(true).anyTimes();
        EasyMock.replay(platformServiceMock);
        return platformServiceMock;
    }


    private RemotableComponentExecutionControllerService createRemotableExecutionControllerService(
        EndpointDatum endpointDatumToForwardFailingMock) throws ExecutionControllerException, RemoteOperationException {
        RemotableComponentExecutionControllerService remotableCompExeCtrlServiceMock = EasyMock.createStrictMock(
            RemotableComponentExecutionControllerService.class);
        remotableCompExeCtrlServiceMock.onSendingEndointDatumFailed(REMOTE_INP_EXE_ID_2,
            endpointDatumToForwardFailingMock, remoteOperationException);
        EasyMock.replay(remotableCompExeCtrlServiceMock);
        return remotableCompExeCtrlServiceMock;
    }


    private LocalExecutionControllerUtilsService createLocalExecutionControllerUtilsServiceMock(
        ComponentExecutionController componentExecutionControllerMock) throws ExecutionControllerException {
        LocalExecutionControllerUtilsService exeCtrlUtilsServiceMock = EasyMock.createNiceMock(LocalExecutionControllerUtilsService.class);
        EasyMock.expect(exeCtrlUtilsServiceMock.getExecutionController(ComponentExecutionController.class, LOCAL_INP_EXE_ID_1, 
            bundleContextMock)).andReturn(componentExecutionControllerMock).anyTimes();
        EasyMock.expect(exeCtrlUtilsServiceMock.getExecutionController(ComponentExecutionController.class, LOCAL_INP_EXE_ID_2, 
            bundleContextMock)).andReturn(componentExecutionControllerMock).anyTimes();
        EasyMock.expect(exeCtrlUtilsServiceMock.getExecutionController(ComponentExecutionController.class, LOCAL_INP_EXE_ID_3, 
            bundleContextMock)).andThrow(new IllegalStateException()).anyTimes();
        EasyMock.replay(exeCtrlUtilsServiceMock);
        return exeCtrlUtilsServiceMock;
    }

    
    private EndpointDatum createEndpointDatumMock(String targetExecutionId, NodeIdentifier inputsNodeId, 
        String outputCompExeId, NodeIdentifier outputsNodeId) {
        EndpointDatum endpointDatumToProcessMock = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToProcessMock.getInputsComponentExecutionIdentifier()).andReturn(targetExecutionId).anyTimes();
        EasyMock.expect(endpointDatumToProcessMock.getInputsNodeId()).andReturn(inputsNodeId).anyTimes();
        EasyMock.expect(endpointDatumToProcessMock.getOutputsComponentExecutionIdentifier()).andReturn(outputCompExeId).anyTimes();
        EasyMock.expect(endpointDatumToProcessMock.getOutputsNodeId()).andReturn(outputsNodeId).anyTimes();
        EasyMock.replay(endpointDatumToProcessMock);
        return endpointDatumToProcessMock;
    }
    
    /**
     * Tests if passed {@link EndpointDatum}s are sent in correct order to {@link EndpointDatumProcessor} of the right node.
     * 
     * @throws InterruptedException if waiting for endpoint datum sent failed
     * @throws RemoteOperationException on unexpected test failures
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testDispatchEndpointDatumViaWorkflowController() throws InterruptedException, RemoteOperationException {

        Capture<String> captSerialEpDtsOnCompNode1 = new Capture<>();
        Capture<String> captSerialEpDtsOnCompNode2 = new Capture<>();
        Capture<String> captSerialEpDtsOnCompNode3 = new Capture<>();
        Capture<String> captSerialEpDtsOnCompNode4 = new Capture<>();
        RemotableEndpointDatumDispatcher compDispatcherMock = EasyMock.createStrictMock(RemotableEndpointDatumDispatcher.class);
        compDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnCompNode1));
        compDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnCompNode2));
        compDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnCompNode3));
        compDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnCompNode4));
        EasyMock.replay(compDispatcherMock);
        
        Capture<String> captSerialEpDtsOnWfCtrl1 = new Capture<>();
        Capture<String> captSerialEpDtsOnWfCtrl2 = new Capture<>();
        Capture<String> captSerialEpDtsOnWfCtrl3 = new Capture<>();
        RemotableEndpointDatumDispatcher wfCtrlDispatcherMock = EasyMock.createStrictMock(RemotableEndpointDatumDispatcher.class);
        wfCtrlDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnWfCtrl1));
        wfCtrlDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnWfCtrl2));
        wfCtrlDispatcherMock.dispatchEndpointDatum(EasyMock.capture(captSerialEpDtsOnWfCtrl3));
        EasyMock.replay(wfCtrlDispatcherMock);
        
        CommunicationService communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);
        EasyMock.expect(communicationServiceMock.getRemotableService(RemotableEndpointDatumDispatcher.class, reachableCompNodeId))
            .andReturn(compDispatcherMock).anyTimes();
        EasyMock.expect(communicationServiceMock.getRemotableService(RemotableEndpointDatumDispatcher.class, remoteWfCtrlNodeId))
            .andReturn(wfCtrlDispatcherMock).anyTimes();
        Set<NodeIdentifier> nodeIds = new HashSet<>();
        nodeIds.add(reachableCompNodeId);
        nodeIds.add(remoteWfCtrlNodeId);
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(nodeIds).anyTimes();
        EasyMock.replay(communicationServiceMock);
        
        EndpointDatumSerializer endpointDatumSerializerMock = EasyMock.createNiceMock(EndpointDatumSerializer.class);
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(EasyMock.anyObject(EndpointDatum.class))).andAnswer(
            new IAnswer<String>() {
                @Override
                public String answer() {
                    return ((EndpointDatum) EasyMock.getCurrentArguments()[0]).getInputsComponentExecutionIdentifier();
                }
            }
        ).anyTimes();
        EasyMock.replay(endpointDatumSerializerMock);
        
        EndpointDatumDispatcherImpl dispatcher = new EndpointDatumDispatcherImpl();
        dispatcher.activate(bundleContextMock);
        dispatcher.bindCommunicationService(communicationServiceMock);
        dispatcher.bindEndpointDatumSerializer(endpointDatumSerializerMock);
        dispatcher.bindPlatformService(createPlatformServiceMock());
        
        EndpointDatum[] endpointDatums = new EndpointDatum[] {
            new DirectlyTransferableEndpointDatum("direct-1"),
            new DirectlyTransferableEndpointDatum("direct-2"),
            new NotDirectlyTransferableEndpointDatum("indirect-1"),
            new DirectlyTransferableEndpointDatum("direct-3"),
            new NotDirectlyTransferableEndpointDatum("indirect-2"),
            new NotDirectlyTransferableEndpointDatum("indirect-3"),
            new DirectlyTransferableEndpointDatum("direct-4"),
        };
        
        for (EndpointDatum endpointDatum : endpointDatums) {
            dispatcher.dispatchEndpointDatum(endpointDatum);
        }
        
        final int sleepInterval = 200;
        while (!captSerialEpDtsOnCompNode4.hasCaptured() || !captSerialEpDtsOnWfCtrl3.hasCaptured()) {
            Thread.sleep(sleepInterval);
        }
        
        assertEquals("direct-1", captSerialEpDtsOnCompNode1.getValue());
        assertEquals("direct-2", captSerialEpDtsOnCompNode2.getValue());
        assertEquals("direct-3", captSerialEpDtsOnCompNode3.getValue());
        assertEquals("direct-4", captSerialEpDtsOnCompNode4.getValue());

        assertEquals("indirect-1", captSerialEpDtsOnWfCtrl1.getValue());
        assertEquals("indirect-2", captSerialEpDtsOnWfCtrl2.getValue());
        assertEquals("indirect-3", captSerialEpDtsOnWfCtrl3.getValue());

    }
    
    /**
     * {@link EndpointDatum} that can be directly sent to target component node.
     * 
     * @author Doreen Seider
     *
     */
    private class DirectlyTransferableEndpointDatum extends UniqueEndpointDatum {
        
        protected DirectlyTransferableEndpointDatum(String id) {
            super(id);
        }
        
        @Override
        public NodeIdentifier getInputsNodeId() {
            return reachableCompNodeId;
        }
    }

    /**
     * {@link EndpointDatum} that cannot be directly sent to target component node and must be sent to workflow controller node instead.
     * 
     * @author Doreen Seider
     *
     */
    private class NotDirectlyTransferableEndpointDatum extends UniqueEndpointDatum {
        
        protected NotDirectlyTransferableEndpointDatum(String id) {
            super(id);
        }
        
        @Override
        public NodeIdentifier getInputsNodeId() {
            return unreachableCompNodeId;
        }
        
        @Override
        public NodeIdentifier getWorkflowNodeId() {
            return remoteWfCtrlNodeId;
        }
    }
    
    /**
     * Implementation {@link EndpointDatum} for test purposes.
     * 
     * @author Doreen Seider
     *
     */
    private class UniqueEndpointDatum extends EndpointDatumDefaultStub {
        
        private final String id;
        
        protected UniqueEndpointDatum(String id) {
            this.id = id;
        }
        
        @Override
        public String getInputsComponentExecutionIdentifier() {
            return id;
        }
        
        @Override
        public TypedDatum getValue() {
            TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
            EasyMock.expect(typedDatumMock.getDataType()).andReturn(DataType.Boolean).anyTimes();
            EasyMock.replay(typedDatumMock);
            return typedDatumMock;
        }

    }
}
