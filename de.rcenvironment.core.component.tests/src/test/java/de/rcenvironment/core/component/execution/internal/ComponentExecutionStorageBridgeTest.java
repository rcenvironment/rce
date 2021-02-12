/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;

/**
 * Tests for {@link ComponentExecutionStorageBridge}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
// TODO review: several of these tests seem to use the same node as input source and storage node;
// wouldn't it be better to separate them for a stronger test setup? - misc_ro
public class ComponentExecutionStorageBridgeTest {

    private static final String OUTPUT = "output";

    private static final String HISTORY = "history";

    private static final InstanceNodeSessionId STORAGE_NODE_SESSION_ID = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("storage-node");

    private static final LogicalNodeId STORAGE_NODE_LOGICAL_NODE_ID = STORAGE_NODE_SESSION_ID.convertToDefaultLogicalNodeId();

    private static final InstanceNodeSessionId NODE_ID = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node");

    private static final Long DM_INSTANCE_ID = Long.valueOf(1);

    private static final String INPUT_NAME = "input name";

    private static final Long INPUT_INSTANCE_DM_ID = Long.valueOf(3);

    private static final String OUTPUT_NAME = "output name";

    private static final Long OUTPUT_INSTANCE_DM_ID = Long.valueOf(9);

    private static final String FAIL_MESSAGE = "Exception expected";

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
     * Tests if parameters are passed correctly to underlying data management service when adding a new component execution record.
     * 
     * @throws ComponentExecutionException on error
     * @throws CommunicationException on error
     */
    @Test
    public void testAddComponentExecution() throws ComponentExecutionException, CommunicationException {

        Long compExeDmId = Long.valueOf(2);
        Integer exeCount = Integer.valueOf(3);

        MetaDataService metaDataServiceMock = EasyMock.createStrictMock(MetaDataService.class);

        Capture<Long> instanceDmIdCapture = Capture.newInstance();
        Capture<String> nodeIdStringCapture = Capture.newInstance();
        Capture<Integer> exeCountCapture = Capture.newInstance();
        Capture<Long> startTimeCapture = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture1 = Capture.newInstance();
        EasyMock.expect(metaDataServiceMock.addComponentRun(EasyMock.captureLong(instanceDmIdCapture),
            EasyMock.capture(nodeIdStringCapture), EasyMock.captureInt(exeCountCapture), EasyMock.captureLong(startTimeCapture),
            EasyMock.capture(storageNodeIdCapture1))).andReturn(compExeDmId);

        Capture<Long> compExeDmIdCapture = Capture.newInstance();
        Capture<Long> endTimeCapture = Capture.newInstance();
        Capture<FinalComponentRunState> finalStateCapture = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture2 = Capture.newInstance();
        metaDataServiceMock.setComponentRunFinished(EasyMock.captureLong(compExeDmIdCapture), EasyMock.captureLong(endTimeCapture),
            EasyMock.capture(finalStateCapture), EasyMock.capture(storageNodeIdCapture2));

        EasyMock.replay(metaDataServiceMock);

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createCompExeRelatedInstancesStub(compExeCtxMock);
        ComponentExecutionStorageBridge storageBridge = new ComponentExecutionStorageBridge(compExeRelatedInstancesStub);
        storageBridge.bindMetaDataService(metaDataServiceMock);
        storageBridge.addComponentExecution(compExeCtxMock, exeCount);

        assertEquals(DM_INSTANCE_ID, instanceDmIdCapture.getValue());
        assertEquals(NODE_ID.convertToDefaultLogicalNodeId().getLogicalNodeIdString(), nodeIdStringCapture.getValue());
        assertEquals(exeCount, exeCountCapture.getValue());
        assertTrue(System.currentTimeMillis() >= startTimeCapture.getValue().longValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture1.getValue());

        assertEquals(compExeDmId, storageBridge.getComponentExecutionDataManagementId());
        compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.set(true);
        assertEquals(compExeDmId, storageBridge.getComponentExecutionDataManagementId());

        storageBridge.setComponentExecutionFinished(FinalComponentRunState.FINISHED);
        assertEquals(compExeDmId, compExeDmIdCapture.getValue());
        assertTrue(System.currentTimeMillis() >= endTimeCapture.getValue().longValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture2.getValue());

        assertNull(storageBridge.getComponentExecutionDataManagementId());
    }

    /**
     * Tests if component run dm id is correctly initiated and set to null and if methods fail that requires an id and no one was initiated
     * yet.
     * 
     * @throws ComponentExecutionException on unexpected errors
     */
    @Ignore
    // as long as retrying is disabled
    @Test
    public void testHandlingOfRetriesOnFailure() throws ComponentExecutionException {
        final AtomicInteger failureCount = new AtomicInteger(0);
        // simple test, doesn't capture actual number of retries
        MetaDataService metaDataServiceStub = (MetaDataService) Proxy.newProxyInstance(
            MetaDataService.class.getClassLoader(), new Class<?>[] { MetaDataService.class },
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
                    if (failureCount.getAndIncrement() < 3) {
                        throw new CommunicationException("");
                    } else {
                        return new Random().nextLong(); // run dm id or output dm id
                    }
                }
            });

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();
        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(compExeCtxMock));
        storageBridge.bindMetaDataService(metaDataServiceStub);

        storageBridge.addComponentExecution(compExeCtxMock, 0);
        failureCount.set(0);
        storageBridge.addInput(INPUT_NAME, Long.valueOf(0));
        failureCount.set(0);
        storageBridge.addOutput(OUTPUT_NAME, OUTPUT);
        failureCount.set(0);
        storageBridge.setFinalComponentState(FinalComponentState.CANCELLED);
        failureCount.set(0);
        storageBridge.setOrUpdateHistoryDataItem(HISTORY);
        failureCount.set(0);
        storageBridge.setComponentExecutionFinished(FinalComponentRunState.FINISHED);
    }

    /**
     * Tests if component run dm id is correctly initiated and set to null and if methods fail that requires an id and no one was initiated
     * yet.
     */
    @Test
    public void testHandlingCommunicationExceptions() {

        MetaDataService metaDataServiceStub = (MetaDataService) Proxy.newProxyInstance(
            MetaDataService.class.getClassLoader(), new Class<?>[] { MetaDataService.class },
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
                    throw new CommunicationException("");
                }
            });

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();

        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(compExeCtxMock));
        storageBridge.bindMetaDataService(metaDataServiceStub);

        try {
            storageBridge.addComponentExecution(compExeCtxMock, 0);
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }

        try {
            storageBridge.addInput(INPUT_NAME, Long.valueOf(0));
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }

        try {
            storageBridge.addOutput(OUTPUT_NAME, OUTPUT);
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }

        try {
            storageBridge.setComponentExecutionFinished(FinalComponentRunState.FINISHED);
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }

        try {
            storageBridge.setFinalComponentState(FinalComponentState.CANCELLED);
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }

        try {
            storageBridge.setOrUpdateHistoryDataItem(HISTORY);
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }

    }

    /**
     * Tests if component execution data management id is correctly initiated and set to null and if methods, which requires an id fail, if
     * no one was initiated yet.
     * 
     * @throws ComponentExecutionException on error
     * @throws CommunicationException on error
     */
    @Test
    public void testIfComponenExecutionDataManagementIdIsInitiatedAndSetToNull()
        throws ComponentExecutionException, CommunicationException {
        MetaDataService metaDataServiceMock = EasyMock.createNiceMock(MetaDataService.class);
        EasyMock.expect(metaDataServiceMock.addComponentRun(EasyMock.anyLong(), EasyMock.anyObject(String.class), EasyMock.anyInt(),
            EasyMock.anyLong(), EasyMock.anyObject(InstanceNodeSessionId.class))).andReturn(Long.valueOf(1));
        EasyMock.replay(metaDataServiceMock);

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();

        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(compExeCtxMock));
        storageBridge.bindMetaDataService(metaDataServiceMock);

        callMethodsExpectingActiveComponentRun(storageBridge);

        storageBridge.addComponentExecution(compExeCtxMock, 1);
        storageBridge.setComponentExecutionFinished(FinalComponentRunState.FINISHED);

        callMethodsExpectingActiveComponentRun(storageBridge);

    }

    private void callMethodsExpectingActiveComponentRun(ComponentExecutionStorageBridge storageBridge) {
        String expectedExceptionMessage = "No component run";
        try {
            storageBridge.setComponentExecutionFinished(FinalComponentRunState.FINISHED);
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains(expectedExceptionMessage));
        }

        try {
            storageBridge.setOrUpdateHistoryDataItem("history data");
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains(expectedExceptionMessage));
        }

        try {
            storageBridge.addInput("input name", Long.valueOf(1));
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains(expectedExceptionMessage));
        }

        try {
            storageBridge.addOutput("output name", "serialized output");
            fail(FAIL_MESSAGE);
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains(expectedExceptionMessage));
        }
    }

    /**
     * Tests if parameters are passed correctly to underlying data management service when adding a new input record.
     * 
     * @throws CommunicationException on error
     * @throws ComponentExecutionException on error
     */
    @Test
    public void testAddInput() throws CommunicationException, ComponentExecutionException {
        Long compExeDmId = Long.valueOf(2);
        Long typedDatumId1 = Long.valueOf(5);
        Long typedDatumId2 = Long.valueOf(7);

        MetaDataService metaDataServiceMock = EasyMock.createStrictMock(MetaDataService.class);

        EasyMock.expect(metaDataServiceMock.addComponentRun(EasyMock.anyLong(), EasyMock.anyObject(String.class), EasyMock.anyInt(),
            EasyMock.anyLong(), EasyMock.anyObject(InstanceNodeSessionId.class))).andReturn(compExeDmId);

        Capture<Long> compExeDmIdCapture = Capture.newInstance();
        Capture<Long> typedDatumIdCapture = Capture.newInstance();
        Capture<Long> endpointInstanceIdCapture = Capture.newInstance();
        Capture<Integer> inputCountCapture = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture = Capture.newInstance();
        metaDataServiceMock.addInputDatum(EasyMock.captureLong(compExeDmIdCapture),
            EasyMock.captureLong(typedDatumIdCapture), EasyMock.captureLong(endpointInstanceIdCapture),
            EasyMock.captureInt(inputCountCapture), EasyMock.capture(storageNodeIdCapture));

        Capture<Long> compExeDmIdCapture2 = Capture.newInstance();
        Capture<Long> typedDatumIdCapture2 = Capture.newInstance();
        Capture<Long> endpointInstanceIdCapture2 = Capture.newInstance();
        Capture<Integer> inputCountCapture2 = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture2 = Capture.newInstance();
        metaDataServiceMock.addInputDatum(EasyMock.captureLong(compExeDmIdCapture2),
            EasyMock.captureLong(typedDatumIdCapture2), EasyMock.captureLong(endpointInstanceIdCapture2),
            EasyMock.captureInt(inputCountCapture2), EasyMock.capture(storageNodeIdCapture2));

        EasyMock.replay(metaDataServiceMock);

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();

        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(compExeCtxMock));
        storageBridge.bindMetaDataService(metaDataServiceMock);
        storageBridge.addComponentExecution(compExeCtxMock, 5);
        storageBridge.addInput(INPUT_NAME, typedDatumId1);

        assertEquals(compExeDmId, compExeDmIdCapture.getValue());
        assertEquals(typedDatumId1, typedDatumIdCapture.getValue());
        assertEquals(INPUT_INSTANCE_DM_ID, endpointInstanceIdCapture.getValue());
        assertEquals(0, inputCountCapture.getValue().intValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture.getValue());

        storageBridge.addInput(INPUT_NAME, typedDatumId2);

        assertEquals(compExeDmId, compExeDmIdCapture2.getValue());
        assertEquals(typedDatumId2, typedDatumIdCapture2.getValue());
        assertEquals(INPUT_INSTANCE_DM_ID, endpointInstanceIdCapture2.getValue());
        assertEquals(1, inputCountCapture2.getValue().intValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture2.getValue());

        try {
            storageBridge.addInput(INPUT_NAME, null);
            fail("Exception expected");
        } catch (ComponentExecutionException e) {
            assertTrue(e.getMessage().contains("id of related output was null"));
        }
    }

    /**
     * Tests if parameters are passed correctly to underlying data management service when adding a new output record.
     * 
     * @throws CommunicationException on error
     * @throws ComponentExecutionException on error
     */
    @Test
    public void testAddOutput() throws CommunicationException, ComponentExecutionException {
        Long compExeDmId = Long.valueOf(2);
        String outputString1 = "output 1";
        String outputString2 = "output 2";

        MetaDataService metaDataServiceMock = EasyMock.createStrictMock(MetaDataService.class);

        EasyMock.expect(metaDataServiceMock.addComponentRun(EasyMock.anyLong(), EasyMock.anyObject(String.class), EasyMock.anyInt(),
            EasyMock.anyLong(), EasyMock.anyObject(InstanceNodeSessionId.class))).andReturn(compExeDmId);

        Capture<Long> compExeDmIdCapture = Capture.newInstance();
        Capture<Long> endpointInstanceIdCapture = Capture.newInstance();
        Capture<String> typedDatumStringCapture = Capture.newInstance();
        Capture<Integer> outputCountCapture = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture = Capture.newInstance();
        EasyMock.expect(metaDataServiceMock.addOutputDatum(EasyMock.captureLong(compExeDmIdCapture),
            EasyMock.captureLong(endpointInstanceIdCapture), EasyMock.capture(typedDatumStringCapture),
            EasyMock.captureInt(outputCountCapture), EasyMock.capture(storageNodeIdCapture))).andReturn(Long.valueOf(5));

        Capture<Long> compExeDmIdCapture2 = Capture.newInstance();
        Capture<Long> endpointInstanceIdCapture2 = Capture.newInstance();
        Capture<String> typedDatumStringCapture2 = Capture.newInstance();
        Capture<Integer> outputCountCapture2 = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture2 = Capture.newInstance();
        EasyMock.expect(metaDataServiceMock.addOutputDatum(EasyMock.captureLong(compExeDmIdCapture2),
            EasyMock.captureLong(endpointInstanceIdCapture2), EasyMock.capture(typedDatumStringCapture2),
            EasyMock.captureInt(outputCountCapture2), EasyMock.capture(storageNodeIdCapture2))).andReturn(Long.valueOf(9));

        EasyMock.replay(metaDataServiceMock);

        ComponentExecutionContext componentExecutionContextMock = createComponentExecutionContextMock();

        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(componentExecutionContextMock));
        storageBridge.bindMetaDataService(metaDataServiceMock);
        storageBridge.addComponentExecution(componentExecutionContextMock, 5);
        storageBridge.addOutput(OUTPUT_NAME, outputString1);

        assertEquals(compExeDmId, compExeDmIdCapture.getValue());
        assertEquals(OUTPUT_INSTANCE_DM_ID, endpointInstanceIdCapture.getValue());
        assertEquals(outputString1, typedDatumStringCapture.getValue());
        assertEquals(0, outputCountCapture.getValue().intValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture.getValue());

        storageBridge.addOutput(OUTPUT_NAME, outputString2);

        assertEquals(compExeDmId, compExeDmIdCapture2.getValue());
        assertEquals(OUTPUT_INSTANCE_DM_ID, endpointInstanceIdCapture2.getValue());
        assertEquals(outputString2, typedDatumStringCapture2.getValue());
        assertEquals(1, outputCountCapture2.getValue().intValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture2.getValue());
    }

    /**
     * Tests if parameters are passed correctly to underlying data management service when setting a history item.
     * 
     * @throws CommunicationException on error
     * @throws ComponentExecutionException on error
     */
    @Test
    public void testSetHistoryItem() throws CommunicationException, ComponentExecutionException {

        Long compExeDmId = Long.valueOf(2);
        String historyDataItem = HISTORY;

        MetaDataService metaDataServiceMock = EasyMock.createStrictMock(MetaDataService.class);

        EasyMock.expect(metaDataServiceMock.addComponentRun(EasyMock.anyLong(), EasyMock.anyObject(String.class), EasyMock.anyInt(),
            EasyMock.anyLong(), EasyMock.anyObject(InstanceNodeSessionId.class))).andReturn(compExeDmId);

        Capture<Long> compExeDmIdCapture = Capture.newInstance();
        Capture<String> historyDataItemCapture = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture = Capture.newInstance();
        metaDataServiceMock.setOrUpdateHistoryDataItem(EasyMock.captureLong(compExeDmIdCapture),
            EasyMock.capture(historyDataItemCapture), EasyMock.capture(storageNodeIdCapture));

        EasyMock.replay(metaDataServiceMock);

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();

        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(compExeCtxMock));
        storageBridge.bindMetaDataService(metaDataServiceMock);
        storageBridge.addComponentExecution(compExeCtxMock, 3);
        storageBridge.setOrUpdateHistoryDataItem(historyDataItem);

        assertEquals(compExeDmId, compExeDmIdCapture.getValue());
        assertEquals(historyDataItem, historyDataItemCapture.getValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture.getValue());

    }

    /**
     * Tests if parameters are passed correctly to underlying data management service when setting the final component state.
     * 
     * @throws CommunicationException on error
     * @throws ComponentExecutionException on error
     */
    @Test
    public void testSetFinalComponentState() throws CommunicationException, ComponentExecutionException {

        FinalComponentState finalState = FinalComponentState.FINISHED;

        MetaDataService metaDataServiceMock = EasyMock.createStrictMock(MetaDataService.class);

        Capture<Long> compInstanceDmIdCapture = Capture.newInstance();
        Capture<FinalComponentState> finalCompStateCapture = Capture.newInstance();
        Capture<InstanceNodeSessionId> storageNodeIdCapture = Capture.newInstance();
        metaDataServiceMock.setComponentInstanceFinalState(EasyMock.captureLong(compInstanceDmIdCapture),
            EasyMock.capture(finalCompStateCapture), EasyMock.capture(storageNodeIdCapture));

        EasyMock.replay(metaDataServiceMock);

        ComponentExecutionContext compExeCtxMock = createComponentExecutionContextMock();

        ComponentExecutionStorageBridge storageBridge =
            new ComponentExecutionStorageBridge(createCompExeRelatedInstancesStub(compExeCtxMock));
        storageBridge.bindMetaDataService(metaDataServiceMock);
        storageBridge.setFinalComponentState(finalState);

        assertEquals(DM_INSTANCE_ID, compInstanceDmIdCapture.getValue());
        assertEquals(finalState, finalCompStateCapture.getValue());
        assertEquals(STORAGE_NODE_LOGICAL_NODE_ID, storageNodeIdCapture.getValue());

    }

    private ComponentExecutionContext createComponentExecutionContextMock() {
        ComponentExecutionContext componentExecutionContextMock = EasyMock.createNiceMock(ComponentExecutionContext.class);
        EasyMock.expect(componentExecutionContextMock.getNodeId()).andReturn(NODE_ID.convertToDefaultLogicalNodeId()).anyTimes();
        EasyMock.expect(componentExecutionContextMock.getStorageNetworkDestination())
            .andReturn(STORAGE_NODE_SESSION_ID.convertToDefaultLogicalNodeId())
            .anyTimes();
        EasyMock.expect(componentExecutionContextMock.getStorageNodeId())
            .andReturn(STORAGE_NODE_SESSION_ID.convertToDefaultLogicalNodeId())
            .anyTimes();
        EasyMock.expect(componentExecutionContextMock.getInstanceDataManagementId()).andReturn(DM_INSTANCE_ID).anyTimes();
        Map<String, Long> inputDmInstanceIds = new HashMap<>();
        inputDmInstanceIds.put(INPUT_NAME, INPUT_INSTANCE_DM_ID);
        EasyMock.expect(componentExecutionContextMock.getInputDataManagementIds()).andReturn(inputDmInstanceIds).anyTimes();
        Map<String, Long> outputDmInstanceIds = new HashMap<>();
        outputDmInstanceIds.put(OUTPUT_NAME, OUTPUT_INSTANCE_DM_ID);
        EasyMock.expect(componentExecutionContextMock.getOutputDataManagementIds()).andReturn(outputDmInstanceIds).anyTimes();
        EasyMock.replay(componentExecutionContextMock);
        return componentExecutionContextMock;
    }

    private ComponentExecutionRelatedInstances createCompExeRelatedInstancesStub(ComponentExecutionContext compExeCtx) {
        ComponentExecutionRelatedInstances compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeCtx = compExeCtx;
        compExeRelatedInstances.compExeRelatedStates = new ComponentExecutionRelatedStates();
        compExeRelatedInstances.wfStorageNetworkDestination = STORAGE_NODE_LOGICAL_NODE_ID;
        return compExeRelatedInstances;
    }

}
