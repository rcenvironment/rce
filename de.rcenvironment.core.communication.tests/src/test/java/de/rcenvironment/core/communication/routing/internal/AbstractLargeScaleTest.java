/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import org.junit.Before;

import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;
import de.rcenvironment.core.utils.testing.CommonTestOptions;

/**
 * Obsolete test base class.
 * 
 * TODO rework or discard
 * 
 * @author Phillip Kroll
 */
public abstract class AbstractLargeScaleTest extends AbstractVirtualInstanceTest {

    // TODO rework this field
    protected static int epochs = CommonTestOptions.selectStandardOrExtendedValue(5, 10);

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
