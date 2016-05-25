/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import java.net.URI;

import junit.framework.TestCase;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;

/**
 * Test for <code>RCEFileURIUtils</code>.
 * 
 * @author Doreen Seider
 */
public class RCEFileURIUtilsTest extends TestCase {

    private static final String NODE_ID = "node-id";

    private static final String DM_REF = "94jdm2sos0fpk20kd";

    /**
     * Valid URI for file in data management.
     */
    private static final String RCE_FILE_URI = "rce://" + NODE_ID + "/" + DM_REF;

    /**
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    public void testGetPlatformForSuccess() throws Exception {
        NodeIdentifier platform = RCEFileURIUtils.getNodeIdentifier(new URI(RCE_FILE_URI));
        assertNotNull(platform);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    public void testGetPlatformForSanity() throws Exception {
        NodeIdentifier platform = RCEFileURIUtils.getNodeIdentifier(new URI(RCE_FILE_URI));
        assertNotNull(platform);
        assertTrue(platform.getIdString().equals(NODE_ID));
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    public void testGetvForFailure() throws Exception {
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
