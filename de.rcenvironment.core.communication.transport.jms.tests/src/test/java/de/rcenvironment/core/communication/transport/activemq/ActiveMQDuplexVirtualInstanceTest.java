/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.activemq;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.templates.AbstractSwitchableVirtualInstanceTest;

/**
 * ActiveMQ implementation of the duplex connection {@link VirtualInstance} tests.
 * 
 * @author Robert Mischke
 */
public class ActiveMQDuplexVirtualInstanceTest extends AbstractSwitchableVirtualInstanceTest {

    @Override
    protected TestConfiguration defineTestConfiguration() {
        // NOTE: as the ActiveMQ transport does not support duplex yet, these
        // tests are actually running in non-duplex mode for now
        return new ActiveMQTestConfiguration();
    }

    /**
     * Dummy test case to make this class being detected as a test outside of eclipse.
     */
    @Test
    public void dummyTest() {
        assertTrue(true);
    }
}
