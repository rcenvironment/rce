/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.testutils.EndpointDatumDefaultStub;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Tests for {@link EndpointDatumSenderImpl}.
 * 
 * @author Doreen Seider
 * 
 */
public class EndpointDatumSenderImplTest {
    
    private static final int TEST_TIMEOUT = 2000;
    
    private final BundleContext bundleContextMock = EasyMock.createStrictMock(BundleContext.class);
    
    private final NodeIdentifier reachableCompNodeId = NodeIdentifierFactory.fromNodeId("reachable-comp-node");
    
    private final NodeIdentifier unreachableCompNodeId = NodeIdentifierFactory.fromNodeId("unreachable-comp-node");
    
    private final NodeIdentifier wfCtrlNodeId = NodeIdentifierFactory.fromNodeId("wf-ctrl-node");
    
    /**
     * Tests if passed {@link EndpointDatum}s are sent in correct order to {@link EndpointDatumProcessor} of the right node.
     * 
     * @throws InterruptedException if waiting for endpoint datum sent failed
     */
    @Test(timeout = TEST_TIMEOUT)
    public void sendEndpointDatumOrderedAsync() throws InterruptedException {

        Capture<String> captSerialEpDtsOnCompNode1 = new Capture<>();
        Capture<String> captSerialEpDtsOnCompNode2 = new Capture<>();
        Capture<String> captSerialEpDtsOnCompNode3 = new Capture<>();
        Capture<String> captSerialEpDtsOnCompNode4 = new Capture<>();
        EndpointDatumProcessor compsEndpointDatumProcessorMock = EasyMock.createStrictMock(EndpointDatumProcessor.class);
        compsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnCompNode1));
        compsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnCompNode2));
        compsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnCompNode3));
        compsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnCompNode4));
        EasyMock.replay(compsEndpointDatumProcessorMock);
        
        Capture<String> captSerialEpDtsOnWfCtrl1 = new Capture<>();
        Capture<String> captSerialEpDtsOnWfCtrl2 = new Capture<>();
        Capture<String> captSerialEpDtsOnWfCtrl3 = new Capture<>();
        EndpointDatumProcessor wfCtrlsEndpointDatumProcessorMock = EasyMock.createStrictMock(EndpointDatumProcessor.class);
        wfCtrlsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnWfCtrl1));
        wfCtrlsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnWfCtrl2));
        wfCtrlsEndpointDatumProcessorMock.onEndpointDatumReceived(EasyMock.capture(captSerialEpDtsOnWfCtrl3));
        EasyMock.replay(wfCtrlsEndpointDatumProcessorMock);
        
        CommunicationService communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);
        EasyMock.expect(communicationServiceMock.getService(EndpointDatumProcessor.class, reachableCompNodeId, bundleContextMock))
            .andReturn(compsEndpointDatumProcessorMock).anyTimes();
        EasyMock.expect(communicationServiceMock.getService(EndpointDatumProcessor.class, wfCtrlNodeId, bundleContextMock))
            .andReturn(wfCtrlsEndpointDatumProcessorMock).anyTimes();
        Set<NodeIdentifier> nodeIds = new HashSet<>();
        nodeIds.add(reachableCompNodeId);
        nodeIds.add(wfCtrlNodeId);
        EasyMock.expect(communicationServiceMock.getReachableNodes()).andReturn(nodeIds).anyTimes();
        EasyMock.replay(communicationServiceMock);
        
        EndpointDatumSerializer endpointDatumSerializerMock = EasyMock.createNiceMock(EndpointDatumSerializer.class);
        EasyMock.expect(endpointDatumSerializerMock.serializeEndpointDatum(EasyMock.anyObject(EndpointDatum.class))).andAnswer(
            new IAnswer<String>() {
                @Override
                public String answer() {
                    return ((EndpointDatum) EasyMock.getCurrentArguments()[0]).getInputComponentExecutionIdentifier();
                }
            }
        ).anyTimes();
        EasyMock.replay(endpointDatumSerializerMock);
        
        EndpointDatumSenderImpl endpointDatumSender = new EndpointDatumSenderImpl();
        endpointDatumSender.activate(bundleContextMock);
        endpointDatumSender.bindCommunicationService(communicationServiceMock);
        endpointDatumSender.bindEndpointDatumSerializer(endpointDatumSerializerMock);
        
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
            endpointDatumSender.sendEndpointDatumOrderedAsync(endpointDatum);
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
     * {@link EndpointDatum} which can be directly sent to receiving component node.
     * 
     * @author Doreen Seider
     *
     */
    private class DirectlyTransferableEndpointDatum extends UniqueEndpointDatum {
        
        private static final long serialVersionUID = 7716414691496332299L;
        
        protected DirectlyTransferableEndpointDatum(String id) {
            super(id);
        }
        
        @Override
        public NodeIdentifier getInputsNodeId() {
            return reachableCompNodeId;
        }
    }

    /**
     * {@link EndpointDatum} which cannot be directly sent to receiving component node and must be sent to workflow controller node instead.
     * 
     * @author Doreen Seider
     *
     */
    private class NotDirectlyTransferableEndpointDatum extends UniqueEndpointDatum {
        
        private static final long serialVersionUID = -4674875739270373068L;

        protected NotDirectlyTransferableEndpointDatum(String id) {
            super(id);
        }
        
        @Override
        public NodeIdentifier getInputsNodeId() {
            return unreachableCompNodeId;
        }
        
        @Override
        public NodeIdentifier getWorkflowNodeId() {
            return wfCtrlNodeId;
        }
    }
    
    /**
     * {@link EndpointDatum} which can be directly sent to receiving component node.
     * 
     * @author Doreen Seider
     *
     */
    private class UniqueEndpointDatum extends EndpointDatumDefaultStub {
        
        private static final long serialVersionUID = 7716414691496332299L;
        
        private final String id;
        
        protected UniqueEndpointDatum(String id) {
            this.id = id;
        }
        
        @Override
        public String getInputComponentExecutionIdentifier() {
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
