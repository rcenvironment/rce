/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;

/**
 * Test cases for {@link CallbackProxyServiceImplTest}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class CallbackProxyServiceImplTest {

    private CallbackProxyServiceImpl service;

    private final String objectID = "objectID";

    private final InstanceNodeSessionId piLocal = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("local");

    private CallbackProxy proxy = new CallbackProxy() {

        private static final long serialVersionUID = -268316561397338077L;

        @Override
        public String getObjectIdentifier() {
            return objectID;
        }

        @Override
        public InstanceNodeSessionId getHomePlatform() {
            return piLocal;
        }
    };

    /** Set up. */
    @Before
    public void setUp() {
        service = new CallbackProxyServiceImpl();
    }

    /** Test. */
    @Test
    public void test() {
        service.addCallbackProxy(proxy);

        assertEquals(proxy, service.getCallbackProxy(objectID));
        assertNull(service.getCallbackProxy("rumpelstielzchen"));

        service.setTTL(objectID, new Long(7));
        service.setTTL("mobby", new Long(9));

        service.activate(EasyMock.createNiceMock(BundleContext.class));
        service.deactivate(EasyMock.createNiceMock(BundleContext.class));

        proxy = null;
        System.gc();
        assertNull(service.getCallbackProxy(objectID));
    }
}
