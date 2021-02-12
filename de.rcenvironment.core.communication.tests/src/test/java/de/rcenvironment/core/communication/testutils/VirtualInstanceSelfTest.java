/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;

/**
 * Tests for the {@link VirtualInstance} class itself.
 * 
 * @author Robert Mischke
 */
public class VirtualInstanceSelfTest extends AbstractVirtualInstanceTest {

    /**
     * Minimal test of {@link VirtualInstance} creation.
     * 
     * @throws InterruptedException on test interruption
     */
    @Test
    public void testMinimalSetup() throws InterruptedException {
        VirtualInstance virtualInstance = new VirtualInstance("The Node Name");
        log.debug("Starting virtual instance");
        virtualInstance.start();
        log.debug("Shutting down virtual instance");
        virtualInstance.shutDown();
        log.debug("Shutdown complete");
    }

    /**
     * Verifies proper behavior of the command-driven state transitions of {@link VirtualInstance}.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void testStateChanges() throws InterruptedException {
        // note: don't use too many instances as threading problems may be masked by startup time
        int numNodes = 5;
        setupInstances(numNodes, true, false); // do not start
        VirtualInstanceGroup group = testTopology.getAsGroup();

        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.INITIAL, vi.getCurrentState());
        }
        group.start();
        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.STARTED, vi.getCurrentState());
        }
        group.shutDown();
        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.STOPPED, vi.getCurrentState());
        }
        group.start();
        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.STARTED, vi.getCurrentState());
        }
        group.simulateCrash();
        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.STOPPED, vi.getCurrentState());
        }
        group.start();
        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.STARTED, vi.getCurrentState());
        }
        group.shutDown();
        for (VirtualInstance vi : allInstances) {
            assertEquals(vi.getInstanceNodeSessionId().toString(), VirtualInstanceState.STOPPED, vi.getCurrentState());
        }
    }

    @Override
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(true);
    }
}
