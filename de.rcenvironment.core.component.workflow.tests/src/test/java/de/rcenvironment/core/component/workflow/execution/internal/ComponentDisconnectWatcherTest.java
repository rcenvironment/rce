/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;

/**
 * Test cases for {@link ComponentDisconnectWatcher}.
 * 
 * @author Doreen Seider
 */
public class ComponentDisconnectWatcherTest {

    private static final String COMP_EXE_ID_1 = "comp-exe-id-1";

    private static final String COMP_EXE_ID_2 = "comp-exe-id-2";

    private static final String COMP_EXE_ID_3 = "comp-exe-id-3";

    private static final String COMP_EXE_ID_4 = "comp-exe-id-4";

    /**
     * Tests if heartbeat losses are recognized properly.
     * 
     * @throws InterruptedException on unexpected error
     */
    @Test
    public void testHeartbeatLossRecognition() throws InterruptedException {

        final int testMaxHeartbeatIntervallMsec = 100;
        ComponentDisconnectWatcher.maxFailureIntervalMsec = testMaxHeartbeatIntervallMsec;

        ComponentStatesChangedEntirelyVerifier compStatesChangedVerifierMock =
            EasyMock.createStrictMock(ComponentStatesChangedEntirelyVerifier.class);
        Capture<Set<String>> compExeIdsCapture = Capture.newInstance();
        EasyMock.expect(compStatesChangedVerifierMock.isComponentInFinalState(COMP_EXE_ID_1)).andStubReturn(false);
        EasyMock.expect(compStatesChangedVerifierMock.isComponentInFinalState(COMP_EXE_ID_2)).andStubReturn(true);
        EasyMock.expect(compStatesChangedVerifierMock.isComponentInFinalState(COMP_EXE_ID_3)).andStubReturn(false);
        EasyMock.expect(compStatesChangedVerifierMock.isComponentInFinalState(COMP_EXE_ID_4)).andStubReturn(false);
        compStatesChangedVerifierMock.announceLostComponents(EasyMock.capture(compExeIdsCapture));
        EasyMock.replay(compStatesChangedVerifierMock);

        WorkflowExecutionContext wfExeCtxMock = EasyMock.createStrictMock(WorkflowExecutionContext.class);
        EasyMock.expect(wfExeCtxMock.getInstanceName()).andReturn("wf instance name");
        EasyMock.expect(wfExeCtxMock.getExecutionIdentifier()).andReturn("wf-exe-id");
        EasyMock.replay(wfExeCtxMock);

        ComponentDisconnectWatcher componentLostWatcher = new ComponentDisconnectWatcher(compStatesChangedVerifierMock, wfExeCtxMock);

        componentLostWatcher.run();
        componentLostWatcher.announceComponentHeartbeat(COMP_EXE_ID_1);
        componentLostWatcher.announceComponentHeartbeat(COMP_EXE_ID_2);
        componentLostWatcher.announceComponentHeartbeat(COMP_EXE_ID_3);
        componentLostWatcher.announceComponentHeartbeat(COMP_EXE_ID_4);
        final int testMaxHeartbeatIntervallWaitOffsetMsec = 50;
        Thread.sleep(ComponentDisconnectWatcher.maxFailureIntervalMsec + testMaxHeartbeatIntervallWaitOffsetMsec);
        componentLostWatcher.announceComponentHeartbeat(COMP_EXE_ID_1);

        componentLostWatcher.run();
        assertTrue(compExeIdsCapture.hasCaptured());
        assertEquals(1, compExeIdsCapture.getValues().size());
        Set<String> compExeIdsCaptured = compExeIdsCapture.getValues().get(0);
        assertEquals(2, compExeIdsCaptured.size());
        assertTrue(compExeIdsCaptured.contains(COMP_EXE_ID_3));
        assertTrue(compExeIdsCaptured.contains(COMP_EXE_ID_4));
    }

}
