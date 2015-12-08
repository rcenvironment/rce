/*
 * Copyright (C) 2006-2012 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.activemq;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.templates.AbstractManualConnectionTest;
import de.rcenvironment.core.communication.testutils.templates.AbstractTransportLowLevelTest;

/**
 * Concrete class for running the {@link AbstractTransportLowLevelTest} test cases with a duplex-enabled
 * {@link VirtualNetworkTransportProvider}.
 * 
 * @author Robert Mischke
 */
public class ActiveMQManualConnectionTest extends AbstractManualConnectionTest {

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
