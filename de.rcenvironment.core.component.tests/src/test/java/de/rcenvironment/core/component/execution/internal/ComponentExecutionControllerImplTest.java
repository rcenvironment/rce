/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallbackService;
import de.rcenvironment.core.component.testutils.ComponentExecutionContextMock;
import de.rcenvironment.core.component.testutils.ComponentExecutionRelatedInstancesFactoryDefaultStub;

/**
 * Tests for {@link ComponentExecutionControllerImplTest}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionControllerImplTest {

    private static final String TEST_VERIFICATION_TOKEN = "some-token";

    private static final String INVALID_VERIFICATION_TOKEN = "some-invalid-token";

    /**
     * Tests if the result verification is only be done if the verification token is valid.
     */
    @Test
    public void testVerifyResults() {
        WorkflowExecutionControllerCallbackService wfExeCtrlCallbackServiceMock =
            EasyMock.createStrictMock(WorkflowExecutionControllerCallbackService.class);

        ComponentExecutionRelatedInstancesFactory compExeRelatedInstancesFactoryMock =
            new ComponentExecutionRelatedInstancesFactoryDefaultStub();
        new ComponentExecutionControllerImpl().bindComponentExecutionRelatedInstancesFactory(compExeRelatedInstancesFactoryMock);
        ComponentExecutionContext compExeCtx = new ComponentExecutionContextMock();
        ComponentExecutionControllerImpl comExeCtrl =
            new ComponentExecutionControllerImpl(compExeCtx, wfExeCtrlCallbackServiceMock, null, 0); // null = storage network destination

        ComponentStateMachine compStateMachineMock =
            createComponentStateMachineMockAndApply(null, comExeCtrl.geComponentExecutionRelatedInstances());
        assertFalse(comExeCtrl.verifyResults(TEST_VERIFICATION_TOKEN, true));
        assertFalse(comExeCtrl.verifyResults(TEST_VERIFICATION_TOKEN, false));
        EasyMock.verify(compStateMachineMock);

        compStateMachineMock =
            createComponentStateMachineMockAndApply(TEST_VERIFICATION_TOKEN, comExeCtrl.geComponentExecutionRelatedInstances());
        assertFalse(comExeCtrl.verifyResults(INVALID_VERIFICATION_TOKEN, true));
        assertFalse(comExeCtrl.verifyResults(INVALID_VERIFICATION_TOKEN, false));
        EasyMock.verify(compStateMachineMock);

        Capture<ComponentStateMachineEvent> compStateMachineEventCapture = new Capture<>();
        compStateMachineMock =
            createComponentStateMachineMockAndApply(TEST_VERIFICATION_TOKEN, comExeCtrl.geComponentExecutionRelatedInstances(),
                compStateMachineEventCapture);
        assertTrue(comExeCtrl.verifyResults(TEST_VERIFICATION_TOKEN, true));
        EasyMock.verify(compStateMachineMock);
        assertEquals(ComponentStateMachineEventType.RESULTS_APPROVED, compStateMachineEventCapture.getValue().getType());

        compStateMachineEventCapture = new Capture<>();
        compStateMachineMock =
            createComponentStateMachineMockAndApply(TEST_VERIFICATION_TOKEN, comExeCtrl.geComponentExecutionRelatedInstances(),
                compStateMachineEventCapture);
        assertTrue(comExeCtrl.verifyResults(TEST_VERIFICATION_TOKEN, false));
        EasyMock.verify(compStateMachineMock);
        assertEquals(ComponentStateMachineEventType.RESULTS_REJECTED, compStateMachineEventCapture.getValue().getType());

    }

    private ComponentStateMachine createComponentStateMachineMockAndApply(String verificationToken,
        ComponentExecutionRelatedInstances compExeRelatedInstances, Capture<ComponentStateMachineEvent> compStateMachineEventCapture) {
        ComponentStateMachine comStateMachineMock = EasyMock.createStrictMock(ComponentStateMachine.class);
        EasyMock.expect(comStateMachineMock.getVerificationToken()).andStubReturn(verificationToken);
        if (compStateMachineEventCapture != null) {
            comStateMachineMock.postEvent(EasyMock.capture(compStateMachineEventCapture));
            EasyMock.expectLastCall();
        }
        EasyMock.replay(comStateMachineMock);
        compExeRelatedInstances.compStateMachine = comStateMachineMock;
        return comStateMachineMock;
    }

    private ComponentStateMachine createComponentStateMachineMockAndApply(String verificationToken,
        ComponentExecutionRelatedInstances compExeRelatedInstances) {
        return createComponentStateMachineMockAndApply(verificationToken, compExeRelatedInstances, null);
    }

}
