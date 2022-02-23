/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Unit test for {@link FileSystemOperations} methods.
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class FileSystemOperationsTest {

    private static final String TEST_SUBDIR_NAME = "subdir";

    private static final String TEST_FILENAME_2 = "file2.txt";

    private static final String TEST_FILENAME_1 = "file1.txt";

    private TempFileService tempFileService;

    private final Log log = LogFactory.getLog(getClass());

    private File sandboxDir;

    private File testRootDir;

    /**
     * Common test setup.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setup() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testRootDir = tempFileService.createManagedTempDir();
        sandboxDir = createAndVerifySubdir(testRootDir, "sandbox");
        log.debug("Testing in temporary directory " + testRootDir.getAbsolutePath());
    }

    /**
     * Test that a simple sandbox with a few files is properly deleted.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testBasicSandboxDeletion() throws Exception {
        createAndVerifyFile(sandboxDir, TEST_FILENAME_1);
        File subDir = createAndVerifySubdir(sandboxDir, TEST_SUBDIR_NAME);
        createAndVerifyFile(subDir, TEST_FILENAME_2);
        assertEquals(2, sandboxDir.listFiles().length);
        FileSystemOperations.deleteSandboxDirectory(sandboxDir);
        assertFalse(sandboxDir.exists());
    }

    /**
     * Tests that a symbolic link in the sandbox directory that points to another file in the sandbox directory is properly deleted.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testInternalLinkDeletion() throws Exception {

        // Manipulating symlinks requires special permissions on Windows, so this cannot be tested out-of-the-box;
        // Current solution: check if symlinks can be created, else use junctions (for directories) and hard links (for files) instead.

        // create internal file link
        File targetFile = createAndVerifyFile(sandboxDir, TEST_FILENAME_1);
        File symLinkToTargetFile = new File(sandboxDir, "symLinkToFile");
        createOSSpecificTestLink(symLinkToTargetFile, targetFile);

        // create internal directory link
        File subDir = createAndVerifySubdir(sandboxDir, TEST_SUBDIR_NAME);
        createAndVerifyFile(subDir, TEST_FILENAME_2);
        File symLinkToSubDir = new File(sandboxDir, "symlinkToSubDir");
        createOSSpecificTestLink(symLinkToSubDir, subDir);

        assertEquals(4, sandboxDir.listFiles().length); // file+link, directory+link
        assertEquals(1, subDir.listFiles().length);
        assertEquals(1, symLinkToSubDir.listFiles().length); // should "look" like the actual external directory
        FileSystemOperations.deleteSandboxDirectory(sandboxDir);
        assertFalse(sandboxDir.exists());
    }

    /**
     * Tests that a symbolic link in the sandbox directory that points to a file outside of the sandbox directory does *not* cause the
     * link's target to be deleted.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    // @Ignore("WIP; not complete yet")
    public void testExternalLinkTargetSurvival() throws Exception {

        // Manipulating symlinks requires special permissions on Windows, so this cannot be tested out-of-the-box;
        // Current solution: check if symlinks can be created, else use junctions (for directories) and hard links (for files) instead.

        // create link to external file
        File symLinkToFile = new File(sandboxDir, "toExternalFile");
        File externalFile = createAndVerifyFile(testRootDir, "externalFile.txt");
        createOSSpecificTestLink(symLinkToFile, externalFile);

        // create link to external directory
        File symLinkToDir = new File(sandboxDir, "toExternalDir");
        File subDir = createAndVerifySubdir(sandboxDir, TEST_SUBDIR_NAME);
        createAndVerifyFile(subDir, TEST_FILENAME_1);
        File externalDir = createAndVerifySubdir(testRootDir, "externalDir");
        File fileInExternalDir = createAndVerifyFile(externalDir, "fileInExternalDir");
        createOSSpecificTestLink(symLinkToDir, externalDir);

        assertEquals(3, sandboxDir.listFiles().length); // normal directory, file symlink, and directory symlink
        assertEquals(1, externalDir.listFiles().length);
        assertEquals(1, symLinkToDir.listFiles().length); // should "look" like the actual external directory

        // verify external files' existence just before deletion
        assertTrue(externalDir.isDirectory());
        assertTrue(fileInExternalDir.isFile());
        assertTrue(externalFile.isFile());

        FileSystemOperations.deleteSandboxDirectory(sandboxDir);

        assertFalse(sandboxDir.exists());
        // check external files' "survival"
        assertTrue(externalDir.isDirectory());
        assertTrue(fileInExternalDir.isFile());
        assertTrue(externalFile.isFile());
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

    private void createOSSpecificTestLink(File linkDir, File linkTargetDir) throws IOException, InterruptedException {
        Path linkPath = linkDir.toPath();
        Path linkTargetPath = linkTargetDir.toPath();
        createOSSpecificTestLink(linkPath, linkTargetPath);
    }

    private void createOSSpecificTestLink(Path linkPath, Path linkTargetPath) throws IOException, InterruptedException {
        if (OSFamily.isLinux()) {
            // use Java 7 code to symlink on Linux
            Files.createSymbolicLink(linkPath, linkTargetPath, new FileAttribute<?>[0]);
        } else if (OSFamily.isWindows()) {
            // Windows requires a special permission to create symbolic links, so test with a NTFS Junction (for a directory) or a hardlink
            // (for a file) instead if creating symbolic link is not possible.
            try {
                //WARNING: This could not be tested yet due to missing permissions.
                Files.createSymbolicLink(linkPath, linkTargetPath, new FileAttribute<?>[0]);
            } catch (FileSystemException e) {
                log.debug("Symlink could not be created due to missing permissions. Creating junction/hardlink instead.");
                if (Files.isDirectory(linkTargetPath)) {
                    Process process = Runtime.getRuntime().exec(
                        new String[] { "cmd.exe", "/c", "mklink", "/j", linkPath.toString(), linkTargetPath.toString() });
                    assertEquals(0, process.waitFor());
                } else {
                    // Cannot use junctions for simple files, use hard link instead
                    Process process = Runtime.getRuntime().exec(
                        new String[] { "cmd.exe", "/c", "mklink", "/h", linkPath.toString(), linkTargetPath.toString() });
                    assertEquals(0, process.waitFor());
                }
            }
        } else {
            throw new IllegalArgumentException(OSFamily.getLocal().toString());
        }
    }
}
