/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;

/**
 * Test for <code>RCEFileURIUtils</code>.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class RCEFileURIUtilsTest {

    private static final String TEST_INSTANCE_ID = NodeIdentifierTestUtils.createTestInstanceNodeIdString();

    private static final String DM_REF = "94jdm2sos0fpk20kd";

    /**
     * Valid URI for file in data management.
     * 
     * TODO >=8.0.0 wrap this into builder method?
     */
    private static final String RCE_FILE_URI = "rce://" + TEST_INSTANCE_ID + "/" + DM_REF;

    /**
     * Common setup.
     */
    @Before
    public void setup() {
        NodeIdentifierTestUtils.attachTestNodeIdentifierServiceToCurrentThread();
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
    public void testGetPlatformForSuccess() throws Exception {
        ResolvableNodeId platform = RCEFileURIUtils.getNodeIdentifier(new URI(RCE_FILE_URI));
        assertNotNull(platform);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetPlatformForSanity() throws Exception {
        ResolvableNodeId platform = RCEFileURIUtils.getNodeIdentifier(new URI(RCE_FILE_URI));
        assertNotNull(platform);
        assertTrue(platform.getInstanceNodeIdString().equals(TEST_INSTANCE_ID));
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetForFailure() throws Exception {
        try {
            RCEFileURIUtils.getNodeIdentifier(new URI("rce:node-id/server/94jdm2sos0fpk20kd"));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetPathForSuccess() throws Exception {
        String path = RCEFileURIUtils.getPath(new URI(RCE_FILE_URI));
        assertNotNull(path);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetPathForSanity() throws Exception {
        String path = RCEFileURIUtils.getPath(new URI(RCE_FILE_URI));
        assertNotNull(path);
        assertEquals(DM_REF, path);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetPathForFailure() throws Exception {
        try {
            RCEFileURIUtils.getPath(new URI("rce:/node-id/server"));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetTypeForSuccess() throws Exception {
        FileType type = RCEFileURIUtils.getType(new URI(RCE_FILE_URI));
        assertNotNull(type);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetTypeForSanity() throws Exception {

        FileType type = RCEFileURIUtils.getType(new URI(RCE_FILE_URI));
        assertNotNull(type);
        assertEquals(FileType.RCE_DM, type);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetTypeForFailure() throws Exception {
        try {
            RCEFileURIUtils.getType(new URI("rcee://node-id/server/94jdm2sos0fpk20kd"));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }

        try {
            RCEFileURIUtils.getType(new URI("rce/node-id/server/94jdm2sos0fpk20kd"));
            fail();
        } catch (CommunicationException e) {
            assertTrue(true);
        }
    }
}
