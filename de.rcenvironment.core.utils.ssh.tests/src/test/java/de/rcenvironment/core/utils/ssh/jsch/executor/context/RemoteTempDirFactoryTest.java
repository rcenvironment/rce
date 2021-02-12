/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch.executor.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

/**
 * Test case for {@link RemoteTempDirFactory}.
 * @author Doreen Seider
 */
public class RemoteTempDirFactoryTest {

    private final String givenRootDir = RandomStringUtils.random(5);
    
    private final String normalizedRootDir = givenRootDir + "/";
    
    /** Test. */
    @Test
    public void testGetRootDir() {
        RemoteTempDirFactory factory = new RemoteTempDirFactory(givenRootDir);        
        assertEquals(normalizedRootDir, factory.getRootDir());
        
        try {
            new RemoteTempDirFactory(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        
    }
    
    /** Test. */
    @Test
    public void testCreateTempDirPath() {
        RemoteTempDirFactory factory = new RemoteTempDirFactory(givenRootDir);
        String contextHint = RandomStringUtils.random(7);
        String separator = RandomStringUtils.random(1);
        String path = factory.createTempDirPath(contextHint, separator);
        assertNotNull(path);
        assertTrue(path.length() > 7); // contextHint + separator
    }
}
