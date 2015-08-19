/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.activemq;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.templates.AbstractCommonVirtualInstanceTest;

/**
 * ActiveMQ implementation of the "common" {@link VirtualInstance} tests.
 * 
 * @author Robert Mischke
 */
public class ActiveMQCommonVirtualInstanceTest extends AbstractCommonVirtualInstanceTest {

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
