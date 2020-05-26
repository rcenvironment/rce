/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.tests.execution;

import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.uplink.client.execution.api.DataTransferUtils;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadContext;
import de.rcenvironment.core.communication.uplink.client.execution.api.FileDataSource;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test for DataTransferUtils.
 *
 * @author Brigitte Boden
 */
public class DataTransferUtilsTest {

    private static final String INPUT_DIRECTORY = "inputDirectory";

    private static final String OUTPUT_DIRECTORY = "outputDirectory";

    private File testRootDir;

    private TempFileService tempFileService;

    private File inputDirectory;

    private File inputDirectoryEmptySubdir;

    private File inputFile;

    private File outputDirectory;

    /**
     * Create test environment.
     * 
     * @throws Exception on error
     */
    @Before
    public void setup() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testRootDir = tempFileService.createManagedTempDir();
        inputDirectory = createAndVerifySubdir(testRootDir, INPUT_DIRECTORY);
        inputDirectoryEmptySubdir = createAndVerifySubdir(inputDirectory, "empty");
        outputDirectory = createAndVerifySubdir(testRootDir, OUTPUT_DIRECTORY);

        // Add some files to the input directory
        inputFile = createAndVerifyFile(inputDirectory, "fileInDir1");
        createAndVerifyFile(inputDirectory, "fileInDir2");
        assertEquals(3, inputDirectory.listFiles().length);

    }

    /**
     * Test directory upload.
     * 
     * @throws Exception on error
     */
    @Test
    public void testUpload() throws Exception {
        DirectoryUploadContext uploadContextMock = EasyMock.createMock(DirectoryUploadContext.class);

        Capture<FileDataSource> uploadFileCapture1 = Capture.newInstance();
        Capture<FileDataSource> uploadFileCapture2 = Capture.newInstance();
        uploadContextMock.provideFile(capture(uploadFileCapture1));
        uploadContextMock.provideFile(capture(uploadFileCapture2));

        EasyMock.replay(uploadContextMock);
        DataTransferUtils.uploadDirectory(inputDirectory, uploadContextMock, "someDirName");

        assertTrue(uploadFileCapture1.getValue().getRelativePath().startsWith("someDirName/fileInDir"));
        assertTrue(uploadFileCapture2.getValue().getRelativePath().startsWith("someDirName/fileInDir"));
    }

    /**
     * Test receiving file.
     * 
     * @throws Exception on error
     */
    @Test
    public void testReceiveFile() throws Exception {
        byte[] byteArray = Files.readAllBytes(inputFile.toPath());
        InputStream byteStream = new ByteArrayInputStream(byteArray);
        FileDataSource annotatedStream = new FileDataSource("some/relative/path", byteArray.length, byteStream);
        DataTransferUtils.receiveFile(annotatedStream, testRootDir);
        File expectedFile = new File(testRootDir, "some/relative/path");
        assertTrue(expectedFile.exists());
    }

    /**
     * Test receiving file with incorrect path. 
     * 
     * @throws Exception on error
     */
    @Test(expected = IOException.class)
    public void testReceiveFileWithIncorrectRelativePath() throws Exception {
        byte[] byteArray = Files.readAllBytes(inputFile.toPath());
        InputStream byteStream = new ByteArrayInputStream(byteArray);
        FileDataSource annotatedStream = new FileDataSource("../../invalid/path", byteArray.length, byteStream);
        DataTransferUtils.receiveFile(annotatedStream, testRootDir);
    }

    /**
     * Test receiving directory listing.
     * 
     * @throws IOException on error
     */
    @Test
    public void testGetAndReceiveDirectoryListing() throws IOException {
        List<String> directoryListing = new ArrayList<>();
        DataTransferUtils.getDirectoryListing(inputDirectory, directoryListing, "");
        assertEquals(1, directoryListing.size());
        assertEquals("/empty", directoryListing.get(0));

        DataTransferUtils.receiveDirectoryListing(directoryListing, outputDirectory);
        assertEquals(1, outputDirectory.listFiles().length);
    }

    /**
     * Test receiving directory listing with incorrect path.
     * 
     * @throws Exception on error
     */
    @Test(expected = IOException.class)
    public void testReceiveDirectoryListingWithIncorrectRelativePath() throws Exception {
        List<String> directoryListing = new ArrayList<>();
        directoryListing.add("../../invalid/path");
        DataTransferUtils.receiveDirectoryListing(directoryListing, outputDirectory);
    }

    /**
     * Cleanup.
     * 
     * @throws IOException on error
     */
    @After
    public void cleanup() throws IOException {
        tempFileService.disposeManagedTempDirOrFile(testRootDir);
    }

    private File createAndVerifySubdir(File parentDir, String name) {
        File dir = new File(parentDir, name);
        dir.mkdir();
        assertTrue(dir.isDirectory());
        return dir;
    }

    private File createAndVerifyFile(File parentDir, String name) throws IOException {
        File file = new File(parentDir, name);
        file.createNewFile();
        assertTrue(file.isFile() && file.canRead());
        return file;
    }
}
