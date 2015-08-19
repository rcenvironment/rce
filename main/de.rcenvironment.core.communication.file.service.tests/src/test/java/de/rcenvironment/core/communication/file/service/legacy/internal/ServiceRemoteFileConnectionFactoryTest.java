/*
 * Copyright (C) 2006-2014 DLR, Germany
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

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
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

    private User user = EasyMock.createNiceMock(User.class);

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
        RemoteFileConnection conncetion = factory.createRemoteFileConnection(user, new URI(uri));
        assertNotNull(conncetion);

    }

    /**
     * Dummy {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId2, BundleContext bundleContext)
            throws IllegalStateException {
            // TODO 3.0: recheck nodeId condition; changed during PlatformIdentifier elimination
            if (nodeId2.equals(NodeIdentifierFactory.fromNodeId(nodeIdString)) && iface == FileService.class) {
                return EasyMock.createNiceMock(FileService.class);
            }
            return null;
        }

    }

}
