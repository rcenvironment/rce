/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.file.service.legacy.api.RemotableFileStreamAccessService;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;

/**
 * Test cases for {@link ServiceRemoteFileConnectionFactory}.
 * 
 * @author Doreen Seider
 */
@Deprecated
public class ServiceRemoteFileConnectionFactoryTest {

    private final UUID dmUuid = UUID.randomUUID();

    private final String nodeIdString = "node-id";

    private final String uri = "rce://" + nodeIdString + "/" + dmUuid + "/7";

    private ServiceRemoteFileConnectionFactory factory;

    /**
     * Set up.
     * 
     * @throws Exception if an error occurred.
     */
    @Before
    public void setUp() throws Exception {
        factory = new ServiceRemoteFileConnectionFactory();
        factory.bindCommunicationService(new DummyCommunicationService());
        factory.activate(EasyMock.createNiceMock(BundleContext.class));
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void test() throws Exception {
        RemoteFileConnection conncetion = factory.createRemoteFileConnection(new URI(uri));
        assertNotNull(conncetion);

    }

    /**
     * Dummy {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getRemotableService(Class<T> iface, NodeIdentifier nodeId2) throws IllegalStateException {
            // TODO 3.0: recheck nodeId condition; changed during PlatformIdentifier elimination
            if (nodeId2.equals(NodeIdentifierFactory.fromNodeId(nodeIdString)) && iface == RemotableFileStreamAccessService.class) {
                return (T) EasyMock.createNiceMock(RemotableFileStreamAccessService.class);
            }
            return null;
        }

    }

}
