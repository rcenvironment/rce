/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import org.junit.Before;

import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;

/**
 * Obsolete test base class.
 * 
 * TODO rework or discard
 * 
 * @author Phillip Kroll
 */
public abstract class AbstractLargeScaleTest extends AbstractVirtualInstanceTest {

    // TODO rework this field
    protected static int epochs = 10;

    /**
     * Common setup method.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Before
    public void prepareInstances() throws Exception {
        setupInstances(testSize, usingDuplexTransport, true);
    }

    @Override
    // FIXME transitional until all tests are migrated to template base classes
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(false);
    }

    protected long getGlobalRequestCount() {
        return getGlobalTrafficListener().getRequestCount();
    }
}
