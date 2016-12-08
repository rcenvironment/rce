/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

/**
 * Unit test for {@link CompressingHelper}.
 * 
 * @author Tobias Brieden
 */
public class CompressingHelperTest {

    /**
     * Test if .nfs files are ignored.
     * 
     * @throws IOException 
     *
     */
    @Test
    public void testCreateZippedByteArrayFromFolderOmitsNFSFiles() throws IOException { //TODO name

        TempFileServiceAccess.setupUnitTestEnvironment();
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        
        // create a managed dir with three files
        File inputDir = tempFileService.createManagedTempDir();
        createAndVerifyFile(inputDir, "test.txt");
        createAndVerifyFile(inputDir, ".nfs000000000095a01200000e8");
        createAndVerifyFile(inputDir, "test2.PDF");
        
        // compress and decompress the folder
        byte[] zippedByteArray = CompressingHelper.createZippedByteArrayFromFolder(inputDir);
        File outputDir = tempFileService.createManagedTempDir();
        CompressingHelper.decompressFolderByteArray(zippedByteArray, outputDir);
        
        // check that all files but the .nfs file is present
        String[] filenameArray = outputDir.list();
        assertTrue(ArrayUtils.contains(filenameArray, "test.txt"));
        assertTrue(ArrayUtils.contains(filenameArray, "test2.PDF"));
        assertFalse(ArrayUtils.contains(filenameArray, ".nfs000000000095a01200000e8"));
        
        // delete the manged dirs
        tempFileService.disposeManagedTempDirOrFile(inputDir);
        tempFileService.disposeManagedTempDirOrFile(outputDir);
    }
    
    //TODO copied from InputProviderComponentTest
    private File createAndVerifyFile(File parentDir, String name) throws IOException {
        File file = new File(parentDir, name);
        file.createNewFile();
        assertTrue(file.isFile() && file.canRead());
        return file;
    }

}
