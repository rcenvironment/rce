/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.rce.communication.file;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection;
import de.rcenvironment.core.communication.fileaccess.api.RemoteInputStream;
import de.rcenvironment.core.communication.fileaccess.internal.RemoteFileConnectionSupport;
import de.rcenvironment.core.communication.fileaccess.spi.RemoteFileConnectionFactory;

/**
 * 
 * Test cases for {@link RemoteInputStream}.
 * 
 * @author Doreen Seider
 */
public class RemoteInputStreamTest {

    /**
     * Set up.
     * 
     * @throws Exception if an error occurs.
     */
    @Before
    public void setUp() throws Exception {
        ServiceReference<?> ref = EasyMock.createNiceMock(ServiceReference.class);

        RemoteFileConnectionFactory factoryMock = EasyMock.createNiceMock(RemoteFileConnectionFactory.class);
        EasyMock.expect(factoryMock.createRemoteFileConnection(new URI(URI))).andReturn(new DummyRemoteFileConnection());
        EasyMock.replay(factoryMock);

        BundleContext contextMock = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(contextMock.getBundles()).andReturn(new Bundle[] {}).anyTimes();
        EasyMock.expect(contextMock.getAllServiceReferences(EasyMock.eq(RemoteFileConnectionFactory.class.getName()),
            EasyMock.eq((String) null)))
            .andReturn(new ServiceReference[] { ref }).anyTimes();
        contextMock.getService(ref);
        EasyMock.expectLastCall().andReturn(factoryMock).anyTimes();
        EasyMock.replay(contextMock);

        new RemoteFileConnectionSupport().activate(contextMock);
    }

    /**
     * Test.
     * 
     * @throws Exception if an error occured.
     * */
    @Test
    public void test() throws Exception {
        RemoteInputStream remoteStream = new RemoteInputStream(new URI(URI));
        try {
            remoteStream.read();
            fail();
        } catch (RuntimeException e) {
            // single byte read() should never be called
            assertTrue(e.getMessage().contains("should not"));
        }

        try {
            byte[] b = new byte[7];
            remoteStream.read(b, 0, 7);
            fail();
        } catch (RuntimeException e) {
            assertEquals("read1", e.getMessage());
        }

        try {
            remoteStream.skip(7);
            fail();
        } catch (RuntimeException e) {
            assertEquals("skip", e.getMessage());
        }

        try {
            remoteStream.close();
            fail();
        } catch (RuntimeException e) {
            assertEquals("close", e.getMessage());
        }
    }

    /**
     * Test {@link RemoteFileConnection} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyRemoteFileConnection implements RemoteFileConnection {

        private static final long serialVersionUID = 1L;

        @Override
        public void close() throws IOException {
            throw new RuntimeException("close");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new RuntimeException("read1");
        }

        @Override
        public int read() throws IOException {
            throw new RuntimeException("read2");
        }

        @Override
        public long skip(long n) throws IOException {
            throw new RuntimeException("skip");
        }

    }
}
