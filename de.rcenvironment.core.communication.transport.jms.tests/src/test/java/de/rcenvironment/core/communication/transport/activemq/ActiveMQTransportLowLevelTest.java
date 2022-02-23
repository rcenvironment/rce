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
import de.rcenvironment.core.communication.testutils.templates.AbstractTransportLowLevelTest;

/**
 * ActiveMQ implementation of the low-level transport tests.
 * 
 * @author Robert Mischke
 */
public class ActiveMQTransportLowLevelTest extends AbstractTransportLowLevelTest {

    @Override
    protected TestConfiguration defineTestConfiguration() {
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
