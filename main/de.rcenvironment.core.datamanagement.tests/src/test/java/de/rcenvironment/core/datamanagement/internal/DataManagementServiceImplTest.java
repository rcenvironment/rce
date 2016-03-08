/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.datamanagement.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Tests for {@link DataManagementServiceImpl}.
 * 
 * @author Doreen Seider
 */
public class DataManagementServiceImplTest {
    
    private TempFileService tempFileService = TempFileServiceAccess.getInstance();
    
    private List<File> tempFiles = new ArrayList<>();
    
    /**
     * Set up.
     */
    @BeforeClass
    public static void setUpTempFileTestEnvironment() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }
    
    /**
     * Clean up.
     * 
     * @throws IOException on unexpected error
     */
    @After
    public void tearDown() throws IOException {
        for (File tempFile : tempFiles) {
            tempFileService.disposeManagedTempDirOrFile(tempFile);
        }
    }
    
    /**
     * Tests packing and unpacking of tar archives.
     * 
     * @throws IOException on unexpected error
     */
    @Test
    public void testTarGzUnPacking() throws IOException {
        // set to 1000 as this caused a StackOverflowError with older implementation of
        // DataManagementServiceImpl#createFileOrDirForTarEntry (see https://mantis.sc.dlr.de/view.php?id=13499)
        final int fileCount = 1000;
        
        DataManagementServiceImpl dmService = new DataManagementServiceImpl();
        TempFileServiceAccess.setupUnitTestEnvironment();
        File rootDir = tempFileService.createManagedTempDir();
        tempFiles.add(rootDir);
        File dir = rootDir;
        for (int i = 0; i < 5; i++) {
            dir = createDirAndBunchOfFiles(dir, fileCount);
        }
        
        File archiveFile = tempFileService.createTempFileWithFixedFilename("arc.tar.gz");
        tempFiles.add(archiveFile);
        dmService.createTarGz(rootDir, archiveFile);
        
        File targetDir = tempFileService.createManagedTempDir();
        tempFiles.add(targetDir);
        dmService.createDirectoryFromTarGz(archiveFile, targetDir);
        
        File[] targetDirFileList = targetDir.listFiles();
        assertEquals(1, targetDirFileList.length);
        
        compareDirectories(rootDir, targetDirFileList[0]);
    }
    
    private void compareDirectories(File rootDir, File targetRootDir) throws IOException {
        assertEquals(rootDir.getName(), targetRootDir.getName());
        
        List<File> rootDirFileList = Arrays.asList(rootDir.listFiles());
        Collections.sort(rootDirFileList);
        
        List<File> targetRootDirFileList = Arrays.asList(targetRootDir.listFiles());
        Collections.sort(targetRootDirFileList);
        
        assertEquals(rootDirFileList.size(), targetRootDirFileList.size());
        
        for (int i = 0; i < rootDirFileList.size(); i++) {
            File rootDirFile = rootDirFileList.get(i);
            File targetRootDirFile = targetRootDirFileList.get(i);
            assertEquals(rootDirFile.getName(), targetRootDirFile.getName());
            assertEquals(rootDirFile.isDirectory(), targetRootDirFile.isDirectory());
            if (!rootDirFile.isDirectory()) {
                FileUtils.contentEquals(rootDirFile, targetRootDirFile);
            } else {
                compareDirectories(rootDirFile, targetRootDirFile);
            }
        }
    }
    
    private File createDirAndBunchOfFiles(File rootDir, int fileCount) throws IOException {
        File dir = new File(rootDir, String.valueOf("0"));
        dir.mkdirs();
        for (int i = 1; i < fileCount; i++) {
            File file = new File(rootDir, String.valueOf(i));
            file.createNewFile();
        }
        return dir;
    }

}
