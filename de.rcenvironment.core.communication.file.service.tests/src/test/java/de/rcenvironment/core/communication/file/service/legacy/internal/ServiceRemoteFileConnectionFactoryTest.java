/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.file.service.legacy.api.RemotableFileStreamAccessService;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;

/**
 * Test cases for {@link ServiceRemoteFileConnectionFactory}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
@Deprecated
public class ServiceRemoteFileConnectionFactoryTest {

    private final UUID dmUuid = UUID.randomUUID();

    private final InstanceNodeId instanceId = NodeIdentifierTestUtils.createTestInstanceNodeId();

    private final String instanceIdString = instanceId.getInstanceNodeIdString();

    // TODO review/encapsulate
    private final String uri = "rce://" + instanceIdString + "/" + dmUuid + "/7";

    private ServiceRemoteFileConnectionFactory factory;

    /**
     * Set up.
     * 
     * @throws Exception if an error occurred.
     */
    @Before
    public void setUp() throws Exception {
        NodeIdentifierTestUtils.attachTestNodeIdentifierServiceToCurrentThread();
        factory = new ServiceRemoteFileConnectionFactory();
        factory.bindCommunicationService(new DummyCommunicationService());
        factory.activate(EasyMock.createNiceMock(BundleContext.class));
    }

    /**
     * Common teardown.
     */
    @After
    public void teardown() {
        NodeIdentifierTestUtils.removeTestNodeIdentifierServiceFromCurrentThread();
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void defaultTest() throws Exception {
        RemoteFileConnection conncetion = factory.createRemoteFileConnection(new URI(uri));
        assertNotNull(conncetion);

    }

    /**
     * Dummy {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (8.0.0 id adaptations)
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getRemotableService(Class<T> iface, NetworkDestination dest) throws IllegalStateException {
            ResolvableNodeId nodeId = (ResolvableNodeId) dest;
            // TODO recheck nodeId condition; changed during PlatformIdentifier elimination
            if (nodeId.isSameInstanceNodeAs(instanceId) && iface == RemotableFileStreamAccessService.class) {
                return (T) EasyMock.createNiceMock(RemotableFileStreamAccessService.class);
            }
            return null;
        }

    }

}
