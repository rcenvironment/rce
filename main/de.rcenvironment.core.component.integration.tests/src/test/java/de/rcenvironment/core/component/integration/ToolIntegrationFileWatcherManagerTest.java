/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcher;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationFileWatcherManager;
import de.rcenvironment.core.utils.common.CompressingHelper;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test the {@link ToolIntegrationFileWatcherManager} and the {@link ToolIntegrationFileWatcher}.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationFileWatcherManagerTest {

    private static final int TEST_TIMEOUT = 10000;

    private static File toolDirectory = null;

    private final Object lockObject = new Object();

    private File testDirectory;

    private MockToolIntegrationContext mockContext;

    private File integrationDir;

    private CountDownLatch latch;

    private String currentToolName;

    private int methodCount = 0;

    private ToolIntegrationFileWatcherManager manager;

    /**
     * Copy test resources to a temp dir, for a better access (copy dir to dir).
     */
    @BeforeClass
    public static void setupTestDirectories() {
        if (toolDirectory == null) {
            TempFileServiceAccess.setupUnitTestEnvironment();
            try {
                toolDirectory = TempFileServiceAccess.getInstance().createManagedTempDir();
                CompressingHelper.unzip(ToolIntegrationFileWatcherManagerTest.class.getResourceAsStream("/TestTools.zip"), toolDirectory);
            } catch (IOException | ArchiveException e) {
                Assert.fail(e.getMessage());
            }

        }
    }

    /**
     * Clean up temp dir.
     */
    @AfterClass
    public static void cleanUpTestDirectories() {

        if (toolDirectory != null) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(toolDirectory);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    /**
     * Set up the environment.
     */
    @Before
    public void setup() {
        try {
            testDirectory = TempFileServiceAccess.getInstance().createManagedTempDir();
        } catch (IOException e) {
            Assert.fail("Could not set up test directory: " + e.getMessage());
        }
        currentToolName = null;
        methodCount = 0;
        mockContext = new MockToolIntegrationContext();
        integrationDir = new File(mockContext.getRootPathToToolIntegrationDirectory(), mockContext.getContextType());
        ToolIntegrationService integrationServiceMock = new ToolIntegrationServiceMock();
        manager = new ToolIntegrationFileWatcherManager(integrationServiceMock);
        manager.createWatcherForToolRootDirectory(mockContext);
    }

    /**
     * Clean up.
     */
    @After
    public void tearDown() {
        manager.unregisterRootDirectory(mockContext);
        if (integrationDir.exists()) {
            integrationDir.delete();
        }

        if (testDirectory.exists()) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(testDirectory);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    /**
     * Test if the {@link ToolIntegrationFileWatcher} works correct if a directory with a configuration is copied.
     */
    @Test
    public void testCopyToolDirIntoFolder() {
        latch = new CountDownLatch(2);
        currentToolName = "TestDirectoryValidConfiguration";
        try {
            FileUtils.copyDirectoryToDirectory(new File(toolDirectory, currentToolName), integrationDir);
            latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        manager.unregister(currentToolName, mockContext);
        Assert.assertEquals(2, getMethodCount());

    }

    /**
     * Test if the {@link ToolIntegrationFileWatcher} works correct if a directory with a configuration is copied.
     */
    @Test
    public void testCopyToolDirWithDocsIntoFolder() {
        if (OS.isFamilyWindows()) {
            latch = new CountDownLatch(10);
        } else {
            latch = new CountDownLatch(2);
        }
        currentToolName = "TestDirectoryWithDocs";
        try {
            FileUtils.copyDirectoryToDirectory(new File(toolDirectory, currentToolName), integrationDir);
            latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        manager.unregister(currentToolName, mockContext);
        if (OS.isFamilyWindows()) {
            Assert.assertEquals(10, getMethodCount());
        } else {
            Assert.assertEquals(2, getMethodCount());
        }

    }

    /**
     * Test if the {@link ToolIntegrationFileWatcher} works correct if a directory with a configuration is copied.
     */
    @Test
    public void testCopyToolDirWithEmptyDocsIntoFolder() {
        if (OS.isFamilyWindows()) {
            latch = new CountDownLatch(4);
        } else {
            latch = new CountDownLatch(2);
        }
        currentToolName = "TestDirectoryWithEmptyDocs";
        try {
            FileUtils.copyDirectoryToDirectory(new File(toolDirectory, currentToolName), integrationDir);
            latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        manager.unregister(currentToolName, mockContext);
        if (OS.isFamilyWindows()) {
            Assert.assertTrue(getMethodCount() >= 4);
        } else {
            Assert.assertEquals(2, getMethodCount());
        }
    }

    /**
     * Test if the {@link ToolIntegrationFileWatcher} works correct if an empty directory is pasted.
     */
    @Test
    public void testFillUpEmptyDirectory() {
        currentToolName = "Tool";
        new File(integrationDir, currentToolName).mkdir();
        if (OS.isFamilyWindows()) {
            latch = new CountDownLatch(6);
        } else {
            latch = new CountDownLatch(2);
        }
        try {
            FileUtils.copyFileToDirectory(new File(toolDirectory, "configuration.json"), new File(integrationDir, currentToolName));
            latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        if (OS.isFamilyWindows()) {
            Assert.assertEquals(6, getMethodCount());
        } else {
            Assert.assertEquals(2, getMethodCount());
        }

    }

    /**
     * Test if the {@link ToolIntegrationFileWatcher} works correct if an empty directory is pasted.
     */
    @Test
    public void testEmptyDirectoryPasted() {
        currentToolName = "Empty";
        new File(integrationDir, currentToolName).mkdir();
        // Since the file watcher is in another thread and there is no way of flagging if it was
        // triggered yet, the test waits a bit hoping that the file watcher is done.
        manager.unregister(currentToolName, mockContext);
        Assert.assertEquals(0, getMethodCount());
    }

    public File getTempDirectory() {
        return testDirectory;
    }

    private String getCurrentToolName() {
        return currentToolName;
    }

    private int getMethodCount() {
        int result = 0;
        synchronized (lockObject) {
            result = methodCount;
        }
        return result;
    }

    private void increaseMethodCount() {
        synchronized (lockObject) {
            methodCount++;
        }
    }

    /**
     * Mock for {@link ToolIntegrationService} to count down if file watcher was activated.
     * 
     * @author Sascha Zur
     */
    private class ToolIntegrationServiceMock extends DefaultToolIntegrationServiceStub {

        private Map<String, String> toolNameToPath = new HashMap<>();

        @Override
        public void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context) {
            Assert.assertEquals(context, mockContext);
            increaseMethodCount();
            latch.countDown();
        }

        @Override
        public void putToolNameToPath(String toolName, File parentFile) {
            Assert.assertEquals(getCurrentToolName(), toolName);
            toolNameToPath.put(parentFile.getAbsolutePath(), toolName);
            increaseMethodCount();
            latch.countDown();
        }

        @Override
        public void updatePublishedComponents(ToolIntegrationContext context) {
            Assert.assertEquals(context, mockContext);
            increaseMethodCount();
            latch.countDown();
        }

        @Override
        public String getToolNameToPath(String path) {
            increaseMethodCount();
            latch.countDown();
            return toolNameToPath.get(path);
        }

        @Override
        public void removeTool(String toolName, ToolIntegrationContext context) {
            Assert.assertEquals(context, mockContext);
            increaseMethodCount();
            latch.countDown();
        }
    }

    /**
     * Mock of a {@link ToolIntegrationContext} to test the {@link ToolIntegrationFileWatcher}.
     * 
     * @author Sascha Zur
     */
    private class MockToolIntegrationContext implements ToolIntegrationContext {

        @Override
        public String getContextId() {
            return "Mock";
        }

        @Override
        public String getContextType() {
            return "Mock";
        }

        @Override
        public String getRootPathToToolIntegrationDirectory() {
            return getTempDirectory().getAbsolutePath();
        }

        @Override
        public String getNameOfToolIntegrationDirectory() {
            return getContextType();
        }

        @Override
        public String getToolDirectoryPrefix() {
            return "";
        }

        @Override
        public String getConfigurationFilename() {
            return "configuration.json";
        }

        @Override
        public String getImplementingComponentClassName() {
            return null;
        }

        @Override
        public String getPrefixForComponentId() {
            return null;
        }

        @Override
        public String getComponentGroupId() {
            return null;
        }

        @Override
        public String[] getDisabledIntegrationKeys() {
            return null;
        }

        @Override
        public File[] getReadOnlyPathsList() {
            return null;
        }

    }
}
