/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;

/**
 * Test cases for {@link DistributableInputStream}.
 * 
 * seid_do: removed remote test, because it was not testable due to internal classes of communication bundle which needs to be initialized
 * before test run
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class DistributableInputStreamTest {

    private InputStream inputStream;

    private Integer read = 7;

    private byte[] bytes;

    private int off;

    private int len;

    private int n;

    private long skipped = 9;

    private DataReference dataRef;

    private UUID uuid = UUID.randomUUID();

    private URI location = URI.create("test");

    private InstanceNodeSessionId instanceId = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("dummy");

    // /**
    // * Set up.
    // *
    // * @throws Exception if an error occurred.
    // */
    // @Before
    // public void setUp() throws Exception {
    // bytes = new byte[len];
    // inputStream = EasyMock.createNiceMock(InputStream.class);
    // EasyMock.expect(inputStream.read()).andReturn(read).anyTimes();
    // EasyMock.expect(inputStream.read(bytes)).andReturn(read).anyTimes();
    // EasyMock.expect(inputStream.read(bytes, off, len)).andReturn(read).anyTimes();
    // EasyMock.expect(inputStream.skip(n)).andReturn(skipped).anyTimes();
    // EasyMock.replay(inputStream);
    //
    // dataRef = new DataReference(DataReferenceType.fileObject, uuid, pi, location);
    // }
    //
    // /**
    // * Test.
    // *
    // * @throws Exception if an error occurred.
    // */
    // @Test
    // public void testLocal() throws Exception {
    // DistributableInputStream dis = new DistributableInputStream(cert, dataRef, inputStream);
    //
    // assertEquals(read.intValue(), dis.read());
    // assertEquals(read.intValue(), dis.read(bytes));
    // assertEquals(read.intValue(), dis.read(bytes, off, len));
    // assertEquals(skipped, dis.skip(n));
    // dis.close();
    // }
}
