/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentState;
import org.junit.Assert;

/**
 * Tests for {@link ComponentStatesChangedEntirelyVerifier}.
 * 
 * @author Doreen Seider
 */
public class ComponentStatesChangedEntirelyVerifierTest {

    private static final String EXE_ID_2 = "exe-id-2";

    private static final String EXE_ID_1 = "exe-id-1";

    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onComponentStatesChangedCompletelyToPrepared()} is called properly.
     */
    @Test
    public void testPreparedComponentStateNotification() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        EasyMock.replay(listener);
        verifier.addListener(listener);
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PROCESSING_INPUTS);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PREPARED);
        verifier.announceComponentState(EXE_ID_1, ComponentState.CANCELLING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.CANCELED);
        verifier.announceComponentState(EXE_ID_2, ComponentState.FINISHED);
        
        EasyMock.reset(listener);
        listener.onComponentStatesChangedCompletelyToPrepared();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);
        
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARED);

        EasyMock.verify(listener);
    }

    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onComponentStatesChangedCompletelyToPaused()} is called properly.
     */
    @Test
    public void testPausedComponentStateNotification() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        verifier.addListener(listener);
        for (int i = 0; i < 2; i++) {
            EasyMock.reset(listener);
            EasyMock.replay(listener);
            verifier.announceComponentState(EXE_ID_1, ComponentState.CANCELED);
            verifier.announceComponentState(EXE_ID_2, ComponentState.PAUSED);

            EasyMock.verify(listener);

            EasyMock.reset(listener);
            listener.onComponentStatesChangedCompletelyToPaused();
            EasyMock.expectLastCall().times(1);
            EasyMock.replay(listener);
            verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
            verifier.addListener(listener);
            verifier.enablePausedComponentStateVerification();

            verifier.announceComponentState(EXE_ID_1, ComponentState.CANCELED);
            verifier.announceComponentState(EXE_ID_2, ComponentState.PAUSED);

            EasyMock.verify(listener);
        }
        
    }
    
    /**
     * Tests {@link ComponentStatesChangedEntirelyVerifier#getComponentsInFinalState()} and
     * {@link ComponentStatesChangedEntirelyVerifier#getDisposedComponents()}.
     */
    @Test
    public void testGetComponentsInCertainState() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createNiceMock(ComponentStatesChangedEntirelyListener.class);
        EasyMock.replay(listener);
        verifier.addListener(listener);
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PAUSED);

        assertEquals(0, verifier.getComponentsInFinalState().size());
        assertEquals(0, verifier.getDisposedComponents().size());
        

        verifier.announceComponentState(EXE_ID_1, ComponentState.CANCELED);
        verifier.announceComponentState(EXE_ID_2, ComponentState.RESUMING);

        assertEquals(1, verifier.getComponentsInFinalState().size());
        assertEquals(0, verifier.getDisposedComponents().size());

        verifier.announceComponentState(EXE_ID_1, ComponentState.DISPOSED);
        verifier.announceComponentState(EXE_ID_2, ComponentState.FAILED);

        assertEquals(2, verifier.getComponentsInFinalState().size());
        assertEquals(1, verifier.getDisposedComponents().size());
        
        verifier.announceComponentState(EXE_ID_2, ComponentState.DISPOSED);
        assertEquals(2, verifier.getDisposedComponents().size());
    }

    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onComponentStatesChangedCompletelyToResumed()} is called properly.
     */
    @Test
    public void testResumedComponentStateNotification() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        verifier.addListener(listener);
        for (int i = 0; i < 2; i++) {
            EasyMock.reset(listener);
            EasyMock.replay(listener);
            verifier.announceComponentState(EXE_ID_1, ComponentState.PROCESSING_INPUTS);
            verifier.announceComponentState(EXE_ID_2, ComponentState.FINISHED);

            EasyMock.verify(listener);

            EasyMock.reset(listener);
            listener.onComponentStatesChangedCompletelyToResumed();
            EasyMock.expectLastCall().times(1);
            EasyMock.replay(listener);
            verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
            verifier.addListener(listener);
            verifier.enableResumedComponentStateVerification();

            verifier.announceComponentState(EXE_ID_1, ComponentState.PROCESSING_INPUTS);
            verifier.announceComponentState(EXE_ID_2, ComponentState.FINISHED);

            EasyMock.verify(listener);
        }
        
    }

    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onComponentStatesChangedCompletelyToAnyFinalState()} is called properly.
     */
    @Test
    public void testFinalComponentStatesNotification() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        listener.onComponentStatesChangedCompletelyToAnyFinalState();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);
        verifier.addListener(listener);
        
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PREPARED);
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PAUSED);
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PREPARED);
        verifier.announceComponentState(EXE_ID_1, ComponentState.FAILED);
        verifier.announceComponentState(EXE_ID_2, ComponentState.RESUMING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.CANCELED);

        EasyMock.verify(listener);
    }

    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onComponentStatesChangedCompletelyToFinished()} is called properly and if
     * {@link ComponentStatesChangedEntirelyVerifier#isComponentInFinalState(String)} returns valid results.
     */
    @Test
    public void testFinishedComponentStatesHandling() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        listener.onComponentStatesChangedCompletelyToAnyFinalState();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);
        verifier.addListener(listener);

        Assert.assertFalse(verifier.isComponentInFinalState(EXE_ID_1));
        Assert.assertFalse(verifier.isComponentInFinalState(EXE_ID_2));
        
        verifier.announceComponentState(EXE_ID_1, ComponentState.CANCELED);
        Assert.assertTrue(verifier.isComponentInFinalState(EXE_ID_1));
        Assert.assertFalse(verifier.isComponentInFinalState(EXE_ID_2));
        verifier.announceComponentState(EXE_ID_1, ComponentState.FINISHED_WITHOUT_EXECUTION);
        verifier.announceComponentState(EXE_ID_2, ComponentState.FAILED);
        Assert.assertTrue(verifier.isComponentInFinalState(EXE_ID_1));
        Assert.assertTrue(verifier.isComponentInFinalState(EXE_ID_2));

        EasyMock.verify(listener);

        EasyMock.reset(listener);
        listener.onComponentStatesChangedCompletelyToFinished();
        EasyMock.expectLastCall().times(1);
        listener.onComponentStatesChangedCompletelyToAnyFinalState();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);

        verifier.announceComponentState(EXE_ID_2, ComponentState.FINISHED);

        EasyMock.verify(listener);
    }
    
    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onComponentStatesChangedCompletelyToDisposed()} is called properly and if
     * {@link ComponentStatesChangedEntirelyVerifier#isComponentDisposed(String)} returns valid results.
     */
    @Test
    public void testDisposedComponentStatesHandling() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        EasyMock.replay(listener);
        verifier.addListener(listener);
        Assert.assertFalse(verifier.isComponentDisposed(EXE_ID_1));
        Assert.assertFalse(verifier.isComponentDisposed(EXE_ID_2));
        
        verifier.announceComponentState(EXE_ID_1, ComponentState.PREPARING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PROCESSING_INPUTS);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PREPARED);
        verifier.announceComponentState(EXE_ID_1, ComponentState.CANCELLING);
        verifier.announceComponentState(EXE_ID_2, ComponentState.DISPOSED);
        Assert.assertFalse(verifier.isComponentDisposed(EXE_ID_1));
        Assert.assertTrue(verifier.isComponentDisposed(EXE_ID_2));
        verifier.announceComponentState(EXE_ID_2, ComponentState.CANCELED);
        verifier.announceComponentState(EXE_ID_2, ComponentState.FINISHED);
        
        EasyMock.reset(listener);
        listener.onComponentStatesChangedCompletelyToDisposed();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);
        
        verifier.announceComponentState(EXE_ID_1, ComponentState.DISPOSED);

        Assert.assertTrue(verifier.isComponentDisposed(EXE_ID_1));
        Assert.assertTrue(verifier.isComponentDisposed(EXE_ID_2));
        EasyMock.verify(listener);
    }

    /**
     * Tests if {@link ComponentStatesChangedEntirelyListener#onLastConsoleRowsReceived()} is called properly.
     */
    @Test
    public void testLastConsoleRowNotification() {
        int compCount = 2;
        ComponentStatesChangedEntirelyVerifier verifier = new ComponentStatesChangedEntirelyVerifier(compCount);
        ComponentStatesChangedEntirelyListener listener = EasyMock.createStrictMock(ComponentStatesChangedEntirelyListener.class);
        listener.onLastConsoleRowsReceived();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);
        verifier.addListener(listener);
        
        verifier.announceLastConsoleRow(EXE_ID_1);
        verifier.accounceComponentInAnyFinalState(EXE_ID_2);
        verifier.announceComponentState(EXE_ID_1, ComponentState.DISPOSED);
        verifier.announceLastConsoleRow(EXE_ID_2);
        verifier.announceComponentState(EXE_ID_2, ComponentState.PROCESSING_INPUTS);
        
        EasyMock.verify(listener);
    }
}
