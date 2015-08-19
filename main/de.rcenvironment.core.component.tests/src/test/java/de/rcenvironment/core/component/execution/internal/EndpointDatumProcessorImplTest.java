/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ExecutionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;

/**
 * Test cases for {@link EndpointDatumProcessorImpl}.
 * 
 * @author Doreen Seider
 * 
 */
public class EndpointDatumProcessorImplTest {

    private static final int TEST_TIMEOUT = 2000;
    
    private final BundleContext bundleContextMock = EasyMock.createStrictMock(BundleContext.class);
    
    private final NodeIdentifier localTargetNodeId = NodeIdentifierFactory.fromNodeId("local-target-node");
    
    private final NodeIdentifier remoteTargetNodeId = NodeIdentifierFactory.fromNodeId("remote-target-node");
    
    /**
     * Tests if decision is made correctly, whether {@link EndpointDatum}s are processed locally or forwarded to an
     * {@link EndpointDatumProcessor} of another node.
     * @throws InterruptedException 
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testOnEndpointDatumReceived() throws InterruptedException {
        
        final String serializedEndpointDatumToProcess1 = "serial-ED-to-process-1";
        final String serializedEndpointDatumToProcess2 = "serial-ED-to-process-2";
        final String serializedEndpointDatumToProcess3 = "serial-ED-to-process-3";
        final String serializedEndpointDatumToForward1 = "serial-ED-to-forward-1";
        final String serializedEndpointDatumToForward2 = "serial-ED-to-forward-2";
        
        EndpointDatum endpointDatumToProcessMock1 = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToProcessMock1.getInputComponentExecutionIdentifier()).andReturn("local-inp-exe-id-1").anyTimes();
        EasyMock.expect(endpointDatumToProcessMock1.getInputsNodeId()).andReturn(localTargetNodeId).anyTimes();
        EasyMock.replay(endpointDatumToProcessMock1);
        
        EndpointDatum endpointDatumToProcessMock2 = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToProcessMock2.getInputComponentExecutionIdentifier()).andReturn("local-inp-exe-id-2").anyTimes();
        EasyMock.expect(endpointDatumToProcessMock2.getInputsNodeId()).andReturn(localTargetNodeId).anyTimes();
        EasyMock.replay(endpointDatumToProcessMock2);
        
        EndpointDatum endpointDatumToProcessMock3 = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToProcessMock3.getInputComponentExecutionIdentifier()).andReturn("local-inp-exe-id-3").anyTimes();
        EasyMock.expect(endpointDatumToProcessMock3.getInputsNodeId()).andReturn(localTargetNodeId).anyTimes();
        EasyMock.replay(endpointDatumToProcessMock3);
        
        EndpointDatum endpointDatumToForwardMock1 = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToForwardMock1.getInputComponentExecutionIdentifier()).andReturn("remote-inp-exe-id-1").anyTimes();
        EasyMock.expect(endpointDatumToForwardMock1.getInputsNodeId()).andReturn(remoteTargetNodeId).anyTimes();
        EasyMock.replay(endpointDatumToForwardMock1);
        
        EndpointDatum endpointDatumToForwardMock2 = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToForwardMock2.getInputComponentExecutionIdentifier()).andReturn("remote-inp-exe-id-2").anyTimes();
        EasyMock.expect(endpointDatumToForwardMock2.getInputsNodeId()).andReturn(remoteTargetNodeId).anyTimes();
        EasyMock.replay(endpointDatumToForwardMock2);
        
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
        EasyMock.replay(endpointDatumSerializerMock);
        
        PlatformService platformServiceMock = EasyMock.createNiceMock(PlatformService.class);
        EasyMock.expect(platformServiceMock.isLocalNode(localTargetNodeId)).andReturn(true).anyTimes();
        EasyMock.expect(platformServiceMock.isLocalNode(remoteTargetNodeId)).andReturn(false).anyTimes();
        EasyMock.replay(platformServiceMock);
        
        Capture<EndpointDatum> captEDToProcess1 = new Capture<>();
        Capture<EndpointDatum> captEDToProcess2 = new Capture<>();
        Capture<EndpointDatum> captEDToProcess3 = new Capture<>();
        ComponentExecutionController componentExecutionControllerMock = EasyMock.createStrictMock(ComponentExecutionController.class);
        componentExecutionControllerMock.onEndpointDatumReceived(EasyMock.capture(captEDToProcess1));
        componentExecutionControllerMock.onEndpointDatumReceived(EasyMock.capture(captEDToProcess2));
        componentExecutionControllerMock.onEndpointDatumReceived(EasyMock.capture(captEDToProcess3));
        EasyMock.replay(componentExecutionControllerMock);
        
        Capture<String> captSerialEDToForward1 = new Capture<>();
        Capture<String> captSerialEDToForward2 = new Capture<>();
        Capture<String> captSerialEDToForward3 = new Capture<>();
        EndpointDatumProcessor endpointDatumProcessorMock = EasyMock.createStrictMock(EndpointDatumProcessor.class);
        endpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEDToForward1));
        endpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEDToForward2));
        endpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEDToForward3));
        EasyMock.replay(endpointDatumProcessorMock);
        
        CommunicationService communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);
        Map<String, String> searchProperties1 = new HashMap<>();
        searchProperties1.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, "local-inp-exe-id-1");
        EasyMock.expect(communicationServiceMock.getService(ComponentExecutionController.class, searchProperties1, null, bundleContextMock))
            .andReturn(componentExecutionControllerMock).anyTimes();
        Map<String, String> searchProperties2 = new HashMap<>();
        searchProperties2.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, "local-inp-exe-id-2");
        EasyMock.expect(communicationServiceMock.getService(ComponentExecutionController.class, searchProperties2, null, bundleContextMock))
            .andReturn(componentExecutionControllerMock).anyTimes();
        Map<String, String> searchProperties3 = new HashMap<>();
        searchProperties3.put(ExecutionConstants.EXECUTION_ID_OSGI_PROP_KEY, "local-inp-exe-id-3");
        EasyMock.expect(communicationServiceMock.getService(ComponentExecutionController.class, searchProperties3, null, bundleContextMock))
            .andThrow(new IllegalStateException()).anyTimes();
        EasyMock.expect(communicationServiceMock.getService(EndpointDatumProcessor.class, remoteTargetNodeId, bundleContextMock))
            .andReturn(endpointDatumProcessorMock).anyTimes();
        EasyMock.replay(communicationServiceMock);
        
        EndpointDatumProcessorImpl endpointDatumProcessor = new EndpointDatumProcessorImpl();
        endpointDatumProcessor.activate(bundleContextMock);
        endpointDatumProcessor.bindEndpointDatumSerializer(endpointDatumSerializerMock);
        endpointDatumProcessor.bindPlatformService(platformServiceMock);
        endpointDatumProcessor.bindCommunicationService(communicationServiceMock);
        
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToProcess1);
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToForward1);
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToForward2);
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToProcess1);
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToProcess3);
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToProcess2);
        endpointDatumProcessor.onEndpointDatumReceived(serializedEndpointDatumToForward1);
        
        
        final int sleepInterval = 200;
        while (!captSerialEDToForward2.hasCaptured() || !captEDToProcess3.hasCaptured()) {
            Thread.sleep(sleepInterval);
        }
        
        assertEquals(endpointDatumToProcessMock1, captEDToProcess1.getValue());
        assertEquals(endpointDatumToProcessMock1, captEDToProcess2.getValue());
        assertEquals(endpointDatumToProcessMock2, captEDToProcess3.getValue());
        assertEquals(serializedEndpointDatumToForward1, captSerialEDToForward1.getValue());
        assertEquals(serializedEndpointDatumToForward2, captSerialEDToForward2.getValue());
        assertEquals(serializedEndpointDatumToForward1, captSerialEDToForward3.getValue());
    }

}
