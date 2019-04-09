/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.rce.communication.file.internal;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.URI;

import java.net.URI;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.fileaccess.internal.RemoteFileConnectionSupport;
import de.rcenvironment.core.communication.fileaccess.spi.RemoteFileConnectionFactory;

/**
 * Test cases for {@link RemoteFileConnectionSupport}.
 * 
 * @author Doreen Seider
 */
public class RemoteFileConnectionSupportTest extends TestCase {

    private final String filter = "(" + RemoteFileConnectionFactory.PROTOCOL + "=de.rcenvironment.rce.communication)";

    private BundleContext contextMock = EasyMock.createNiceMock(BundleContext.class);

    private RemoteFileConnectionSupport support;

    @Override
    protected void setUp() throws Exception {
        support = new RemoteFileConnectionSupport();
    }

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    public void testGetRemoteFileConnectionForSuccess() throws Exception {

        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn("de.rcenvironment.rce.communication").anyTimes();
        EasyMock.replay(bundleMock);

        ServiceReference<?> ref = EasyMock.createNiceMock(ServiceReference.class);

        RemoteFileConnectionFactory factoryMock = EasyMock.createStrictMock(RemoteFileConnectionFactory.class);
        EasyMock.expect(factoryMock.createRemoteFileConnection(new URI(URI)))
            .andReturn(EasyMock.createNiceMock(RemoteFileConnection.class));
        EasyMock.replay(factoryMock);

        contextMock = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(contextMock.getBundles()).andReturn(new Bundle[] { bundleMock }).anyTimes();
        EasyMock.expect(contextMock.getAllServiceReferences(EasyMock.eq(RemoteFileConnectionFactory.class.getName()),
            EasyMock.eq((String) null))).andReturn(new ServiceReference[] { ref }).anyTimes();
        
        contextMock.getService(ref);
        EasyMock.expectLastCall().andReturn(factoryMock).anyTimes();
        
        EasyMock.replay(contextMock);

        support.activate(contextMock);

        RemoteFileConnection connection = RemoteFileConnectionSupport.getRemoteFileConnection(new URI(URI));
        assertNotNull(connection);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    public void testGetRemoteInputStreamForFailure() throws Exception {
        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getBundles()).andReturn(null).anyTimes();
        EasyMock.replay(contextMock);
        support.activate(contextMock);
        try {
            RemoteFileConnectionSupport.getRemoteFileConnection(new URI(URI));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }

        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getBundles()).andReturn(new Bundle[] {}).anyTimes();
        EasyMock.expect(contextMock.getAllServiceReferences(EasyMock.eq(RemoteFileConnectionFactory.class.getName()),
            EasyMock.eq(filter))).andReturn(null).anyTimes();
        EasyMock.replay(contextMock);
        support.activate(contextMock);
        try {
            RemoteFileConnectionSupport.getRemoteFileConnection(new URI(URI));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }

        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getBundles()).andReturn(new Bundle[] {}).anyTimes();
        ServiceReference<?> ref = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(contextMock.getAllServiceReferences(EasyMock.eq(RemoteFileConnectionFactory.class.getName()),
            EasyMock.eq(filter))).andReturn(new ServiceReference[] { ref }).anyTimes();
        EasyMock.expect(contextMock.getService(ref)).andReturn(null).anyTimes();
        EasyMock.replay(contextMock);
        support.activate(contextMock);
        try {
            RemoteFileConnectionSupport.getRemoteFileConnection(new URI(URI));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }
    }
}
