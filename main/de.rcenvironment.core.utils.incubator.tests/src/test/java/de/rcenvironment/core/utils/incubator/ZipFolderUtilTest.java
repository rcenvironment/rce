/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.incubator;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Test for utility class ZipFolderUtil.
 *
 * @author Markus Kunde
 */
public class ZipFolderUtilTest {

    /** Constants for assertion texts. */
    private static final String CONTENT_FILES_NOT_EQUAL = "Content of files after zip + unzip is not equal.";
    private static final String STRUCTURE_FILES_NOT_EQUAL = "Structure of files after zip + unzip is not equal.";
    
    /** Constants for names, content of files. */
    private static final String SUBFOLDER_ZIP_CONTENT2 = "subfolderZipContent2";
    private static final String SUBFOLDER_ZIP_CONTENT1 = "subfolderZipContent1";
    private static final String SUBFOLDER = "subfolder";
    private static final String ROOT_ZIP_CONTENT2 = "rootZipContent2";
    private static final String ROOT_ZIP_CONTENT1 = "rootZipContent1";

    /** JUnit temp file handler. */
    @Rule
    public TemporaryFolder junitTempFolder = new TemporaryFolder();
    
    private File workingDir;
    private File rootZipDir;
    private File rootZipContent1;
    private File rootZipContent2;
    private File subZipFolder;
    private File subfolderZipContent1;
    private File subfolderZipContent2;
   
    /**
     * SetUp-method. 
     * 
     * @throws Exception exception
     */
    @Before
    public void setUp() throws Exception {
        workingDir = junitTempFolder.getRoot();
        rootZipDir = new File(workingDir, "rootDirForZipping");
        FileUtils.forceMkdir(rootZipDir);
        
        rootZipContent1 = new File(rootZipDir, ROOT_ZIP_CONTENT1);
        FileUtils.touch(rootZipContent1);
        
        rootZipContent2 = new File(rootZipDir, ROOT_ZIP_CONTENT2);
        FileUtils.touch(rootZipContent2);
        
        subZipFolder = new File(rootZipDir, SUBFOLDER);
        FileUtils.forceMkdir(subZipFolder);
        
        subfolderZipContent1 = new File(subZipFolder, SUBFOLDER_ZIP_CONTENT1);
        FileUtils.touch(subfolderZipContent1);
        
        subfolderZipContent2 = new File(subZipFolder, SUBFOLDER_ZIP_CONTENT2);
        FileUtils.touch(subfolderZipContent2);
        
        FileUtils.writeStringToFile(rootZipContent1, ROOT_ZIP_CONTENT1);
        FileUtils.writeStringToFile(rootZipContent2, ROOT_ZIP_CONTENT2);
        FileUtils.writeStringToFile(subfolderZipContent1, SUBFOLDER_ZIP_CONTENT1);
        FileUtils.writeStringToFile(subfolderZipContent2, SUBFOLDER_ZIP_CONTENT2);
    }

    /**
     * Test if zipping and unzipping is successful.
     * @throws IOException thrown if an I/O error occurs
     * 
     */
    @Test
    public void testZipFolderContent() throws IOException {
        File zipFile = new File(workingDir, "zipFile");
        ZipFolderUtil.zipFolderContent(rootZipDir, zipFile);
        
        assertTrue("zip file does not exist", zipFile.exists() && zipFile.isFile());
        
        
        File unzipFolder = new File(workingDir, "unzipFolder");
        FileUtils.forceMkdir(unzipFolder);
        ZipFolderUtil.extractZipToFolder(unzipFolder, zipFile);
        
        
        // Check if structure is equal.
        File unzippedSubfolder = new File(unzipFolder, SUBFOLDER);
        assertTrue("Unzipped subfolder does not exist", unzippedSubfolder.exists() && unzippedSubfolder.isDirectory());
        
        assertTrue(STRUCTURE_FILES_NOT_EQUAL, 
            new File(unzipFolder, ROOT_ZIP_CONTENT1).exists() && new File(unzipFolder, ROOT_ZIP_CONTENT1).isFile());
        assertTrue(STRUCTURE_FILES_NOT_EQUAL, 
            new File(unzipFolder, ROOT_ZIP_CONTENT2).exists() && new File(unzipFolder, ROOT_ZIP_CONTENT2).isFile());
        
        assertTrue(STRUCTURE_FILES_NOT_EQUAL, 
            new File(unzippedSubfolder, SUBFOLDER_ZIP_CONTENT1).exists() && new File(unzippedSubfolder, SUBFOLDER_ZIP_CONTENT1).isFile());
        assertTrue(STRUCTURE_FILES_NOT_EQUAL, 
            new File(unzippedSubfolder, SUBFOLDER_ZIP_CONTENT2).exists() && new File(unzippedSubfolder, SUBFOLDER_ZIP_CONTENT2).isFile());
        
        
        // Check if content is equal.
        assertTrue(CONTENT_FILES_NOT_EQUAL, 
            FileUtils.contentEquals(rootZipContent1, new File(unzipFolder, ROOT_ZIP_CONTENT1)));
        assertTrue(CONTENT_FILES_NOT_EQUAL, 
            FileUtils.contentEquals(rootZipContent2, new File(unzipFolder, ROOT_ZIP_CONTENT2)));
        assertTrue(CONTENT_FILES_NOT_EQUAL, 
            FileUtils.contentEquals(subfolderZipContent1, new File(unzippedSubfolder, SUBFOLDER_ZIP_CONTENT1)));
        assertTrue(CONTENT_FILES_NOT_EQUAL, 
            FileUtils.contentEquals(subfolderZipContent2, new File(unzippedSubfolder, SUBFOLDER_ZIP_CONTENT2)));   
    }

}
