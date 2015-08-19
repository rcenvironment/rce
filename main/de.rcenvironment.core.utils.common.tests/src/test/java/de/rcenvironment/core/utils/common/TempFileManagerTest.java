/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileManager.TempFileServiceImpl;

/**
 * Tests for {@link TempFileManager}.
 * 
 * @author Robert Mischke
 */
public class TempFileManagerTest {

    private static final int COLLISION_TEST_ITERATIONS = 100;

    private static final int PARALLEL_TEST_TIMEOUT_MSEC = 30000;

    private TempFileServiceImpl defaultInstance;

    /**
     * Creates the test instance.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        defaultInstance = (TempFileServiceImpl) TempFileServiceAccess.getInstance();
    }

    /**
     * Fixture teardown. Discards/resets the default {@link TempFileManager} instance.
     */
    @After
    public void tearDown() {
        TempFileServiceAccess.discardCurrentSetup();
    }

    /**
     * Tests that {@link TempFileServiceAccess#getDefaultTestRootDir()} is set after {@link TempFileServiceAccess#setupTestEnvironment()}
     * was called.
     */
    @Test
    public void testDefaultTestRootDir() {
        File globalRootDir = ((TempFileServiceImpl) TempFileServiceAccess.getInstance()).getGlobalRootDir();
        assertEquals(globalRootDir, TempFileServiceAccess.getDefaultTestRootDir());
    }

    /**
     * Basic Test: Checks that createManagedTempDir returns different directories on repeated calls.
     * 
     * @throws IOException on I/O errors
     */
    @Test
    public void testCreateManagedTempDir() throws IOException {
        File dir1 = defaultInstance.createManagedTempDir();
        File dir2 = defaultInstance.createManagedTempDir();
        Assert.assertTrue(dir1.isDirectory());
        Assert.assertTrue(dir2.isDirectory());
        Assert.assertFalse(dir1.getAbsolutePath().equals(dir2.getAbsolutePath()));
    }

    /**
     * Checks that createTempFileWithFixedFilename returns different files on repeated calls, and that the expected filename is met.
     * 
     * @throws IOException on I/O errors
     */
    @Test
    public void testCreateTempFileWithFixedFilename() throws IOException {
        String filename = "fixedNameTest.ext";
        File file1 = defaultInstance.createTempFileWithFixedFilename(filename);
        File file2 = defaultInstance.createTempFileWithFixedFilename(filename);
        Assert.assertTrue(file1.isFile());
        Assert.assertTrue(file2.isFile());
        Assert.assertFalse(file1.getAbsolutePath().equals(file2.getAbsolutePath()));
        Assert.assertTrue(file1.getName().equals(filename));
        Assert.assertTrue(file2.getName().equals(filename));

        // test that a collision with an externally-created directory of the same name does not fail
        String testDirName = "dir";
        File collidingDir = new File(file2.getParentFile(), testDirName);
        collidingDir.mkdir();
        File collidingFile = defaultInstance.createTempFileWithFixedFilename(testDirName);
        assertTrue(collidingDir.isDirectory());
        assertEquals(testDirName, collidingDir.getName());
        assertTrue(collidingFile.isFile());
        assertEquals(testDirName, collidingFile.getName());
    }

    /**
     * Tests that concurrent calls to {@link TempFileService#createTempFileWithFixedFilename(String)} work as intended.
     * 
     * Note that with the current service implementation, the test is somewhat pointless, as the tested method is synchronized, so it is not
     * actually parallelized. The test still guards against regressions by future changes.
     * 
     * @throws IOException on I/O errors
     * @throws InterruptedException on test interruption
     */
    @Test
    public void testCreateTempFileWithFixedFilenameCollisions() throws IOException, InterruptedException {
        final String filename = "fixedNameTest.ext";
        ExecutorService executor = Executors.newFixedThreadPool(COLLISION_TEST_ITERATIONS); // ensure maximum parallelity
        final Set<String> absolutePaths = Collections.synchronizedSet(new HashSet<String>());
        for (int i = 1; i <= COLLISION_TEST_ITERATIONS; i++) {
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    File file;
                    try {
                        file = defaultInstance.createTempFileWithFixedFilename(filename);
                        Assert.assertTrue(file.isFile());
                        Assert.assertTrue(file.getName().equals(filename));
                        absolutePaths.add(file.getAbsolutePath());
                    } catch (IOException e) {
                        // will fail the test as no path is added to the global set
                        LogFactory.getLog(getClass()).error("Failed to create temp file", e);
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(PARALLEL_TEST_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
        // test that all generated paths were unique
        assertEquals(COLLISION_TEST_ITERATIONS, absolutePaths.size());
    }

    /**
     * Test for {@link TempFileUtils#disposeManagedTempDirOrFile(File)}.
     * 
     * @throws IOException on internal test errors
     */
    @Test
    public void testDisposeManagedTempDirOrFile() throws IOException {
        File dir1 = defaultInstance.createManagedTempDir();
        File dir2 = defaultInstance.createManagedTempDir("123asd()_-");
        File file1 = defaultInstance.createTempFileFromPattern("dummy*file.txt");

        // should succeed
        defaultInstance.disposeManagedTempDirOrFile(dir1);

        // test deleting some other temp file or directory; should fail
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            Assert.assertNotNull(tempDir);
            defaultInstance.disposeManagedTempDirOrFile(new File(tempDir, "deleteme.txt"));
            Assert.fail("Exception expected");
        } catch (IOException e) {
            // expected: an exception text about the root directory mismatch
            Assert.assertTrue(e.getMessage().contains("root"));
        }

        // should succeed
        defaultInstance.disposeManagedTempDirOrFile(dir2);

        // test deleting a file (instead of a directory)
        defaultInstance.disposeManagedTempDirOrFile(file1);
    }

    /**
     * Tests that the garbage collection does not delete currently-used managed temp directories.
     * 
     * @throws IOException on I/O errors
     */
    @Test
    @Ignore("target code not implemented yet")
    public void testGCDoesNotDeleteLiveInstanceRootDirs() throws IOException {
        TempFileManager instance2 = createNewInstanceWithSameGlobalRootDir();
        instance2.runGCOnGlobalRootDir();
        assertTrue("Own instance root dir should still exist after GC",
            defaultInstance.getInstanceRootDir().isDirectory());
        assertTrue("External instance root dir should still exist after GC",
            // TODO review: correct instance?
            defaultInstance.getInstanceRootDir().isDirectory());
    }

    /**
     * Tests that the garbage collection deletes all "instance root" directories except the ones that are currently in use (by this test).
     * 
     * @throws IOException on I/O errors
     */
    @Test
    @Ignore("target code not implemented yet")
    public void testGCDeletesAllNonLiveInstanceRootDirs() throws IOException {
        TempFileManager instance2 = createNewInstanceWithSameGlobalRootDir();
        instance2.runGCOnGlobalRootDir();
        File globalRootDir = instance2.getGlobalRootDir();
        List<File> content = TempFileManager.getActualDirectoryContent(globalRootDir);
        assertEquals("Global root dir " + globalRootDir.getAbsolutePath() + " should only contain two directories after GC test", 2,
            content.size());
        assertTrue("Fresh instance root dir should still exist after external GC",
            defaultInstance.getInstanceRootDir().isDirectory());
    }

    private TempFileManager createNewInstanceWithSameGlobalRootDir() throws IOException {
        // verify implicit creation of instance root
        assertTrue("Default instance should have created its instance root dir",
            defaultInstance.getInstanceRootDir().isDirectory());
        TempFileManager instance2 = new TempFileManager(defaultInstance.getGlobalRootDir(), null);
        // verify *global* root dir equality
        assertTrue(defaultInstance.getGlobalRootDir().equals(instance2.getGlobalRootDir()));
        // verify *instance* root dir *in*equality
        assertFalse(defaultInstance.getInstanceRootDir().equals(instance2.getServiceImplementation().getInstanceRootDir()));
        return instance2;
    }
}
