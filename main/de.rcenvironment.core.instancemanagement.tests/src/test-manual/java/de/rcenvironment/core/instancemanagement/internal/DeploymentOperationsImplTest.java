/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Manual integration tests for {@link DeploymentOperationsImpl}.
 * 
 * @author Robert Mischke
 */
public class DeploymentOperationsImplTest {

    private TempFileService tfs;

    private DeploymentOperationsImpl operations;

    /**
     * Common setup.
     * 
     * @throws Exception on uncaught errors
     */
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tfs = TempFileServiceAccess.getInstance();
        operations = new DeploymentOperationsImpl();
    }

    /**
     * Common teardown.
     * 
     * @throws Exception on uncaught errors
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Test for {@link DeploymentOperationsImpl#downloadFile(String, File, boolean)}.
     * 
     * @throws IOException on unexpected test failure
     */
    @Test
    public void testDownload() throws IOException {
        File tempFile = tfs.createTempFileFromPattern("download-*.tmp");
        assertEquals("", FileUtils.readFileToString(tempFile));
        operations.downloadFile("http://software.dlr.de/updates/rce/6.x/products/standard/releases/latest/zip/VERSION",
            tempFile, true, false);
        String content = FileUtils.readFileToString(tempFile);
        assertTrue(content.startsWith("6."));
    }

    /**
     * Test for {@link DeploymentOperationsImpl#installFromProductZip(File, File)}.
     * 
     * @throws IOException on uncaught errors
     */
    @Test
    @Ignore("needs configuration setup; disabled as quick fix")
    public void testInstallation() throws IOException {
        File targetDir = new File("insert_your_test_directory_here");
        operations.installFromProductZip(new File("E:\\Nb\\RCE_Tests\\builds\\6.0.3\\rce-6.0.3.201502031344-standard-win32.x86_64.zip"),
            targetDir);
    }
}
