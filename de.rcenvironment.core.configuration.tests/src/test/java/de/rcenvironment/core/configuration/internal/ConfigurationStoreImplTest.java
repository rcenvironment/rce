/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.WritableConfigurationSegment;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * {@link ConfigurationStoreImpl} unit tests.
 * 
 * @author Robert Mischke
 */
public class ConfigurationStoreImplTest {

    private static final String MINIMAL_CONFIG_TEST_FILE_PATH = "/configurationStore/minimal.json";

    private static final double DOUBLE_TEST_VALUE = 1.5;

    private static final String NON_EXISTING_SUB_PATH = "sub";

    private TempFileService tempFileService;

    private File testFile;

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Common test setup.
     * 
     * @throws IOException on uncaught errors
     */
    @Before
    public void setup() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        testFile = tempFileService.createTempFileWithFixedFilename("configTest.json");
        // log.debug("Using test file " + testFile);
    }

    /**
     * Tests the fallback in case placeholder configuration returned by {@link ConfigurationStoreImpl#createEmptyPlaceholder()}; the most
     * relevant result is that access to missing keys does not cause errors.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Test
    public void testNullValuesFromEmptyPlaceholderConfiguration() throws IOException {
        ConfigurationSegment rootSegment = setupPlaceholderConfiguration();

        assertNullValuesForVariousRelativePaths(rootSegment);

        // expected behaviour: non-existing sub-segments should be returned, but
        ConfigurationSegment segment = rootSegment.getSubSegment(NON_EXISTING_SUB_PATH);
        assertNotNull(segment);
        assertFalse(segment.isPresentInCurrentConfiguration());

        assertNullValuesForVariousRelativePaths(segment);
    }

    /**
     * Tests an empty configuration file; the most relevant result is that access to missing keys does not cause errors.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Test
    public void testNullValuesFromEmptyConfigurationFile() throws IOException {

        ConfigurationSegment rootSegment = setupEmptyFileConfiguration();

        assertNullValuesForVariousRelativePaths(rootSegment);

        // expected behaviour: non-existing sub-segments should be returned, but
        ConfigurationSegment segment = rootSegment.getSubSegment(NON_EXISTING_SUB_PATH);
        assertNotNull(segment);
        assertFalse(segment.isPresentInCurrentConfiguration());

        assertNullValuesForVariousRelativePaths(segment);
    }

    /**
     * Tests that default values are applied properly, using a placeholder configuration.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Test
    public void testDefaultValueHandlingFromEmptyPlaceholderConfiguration() throws IOException {

        ConfigurationSegment rootSegment = setupPlaceholderConfiguration();

        assertDefaultValueBehaviorForVariousRelativePaths(rootSegment);

        // expected behaviour: non-existing sub-segments should be returned, but
        ConfigurationSegment segment = rootSegment.getSubSegment(NON_EXISTING_SUB_PATH);
        assertNotNull(segment);
        assertFalse(segment.isPresentInCurrentConfiguration());

        assertDefaultValueBehaviorForVariousRelativePaths(segment);
    }

    /**
     * Tests that default values are applied properly, using an empty configuration file.
     * 
     * @throws IOException on uncaught exceptions
     */
    @Test
    public void testDefaultValueHandlingFromEmptyConfigurationFile() throws IOException {

        ConfigurationSegment rootSegment = setupEmptyFileConfiguration();

        assertDefaultValueBehaviorForVariousRelativePaths(rootSegment);

        // expected behaviour: non-existing sub-segments should be returned, but
        ConfigurationSegment segment = rootSegment.getSubSegment(NON_EXISTING_SUB_PATH);
        assertNotNull(segment);
        assertFalse(segment.isPresentInCurrentConfiguration());

        assertDefaultValueBehaviorForVariousRelativePaths(segment);
    }

    /**
     * Tests standard hierarchical read operations.
     * 
     * @throws IOException on uncaught exceptions
     * @throws ConfigurationException on uncaught exceptions
     */
    @Test
    public void testBasicNavigation() throws IOException, ConfigurationException {
        copyResourceToFile(MINIMAL_CONFIG_TEST_FILE_PATH, testFile);

        ConfigurationStore configStore = new ConfigurationStoreImpl(testFile);
        ConfigurationSegment rootSegment = configStore.getSnapshotOfRootSegment();
        assertTrue(rootSegment.isPresentInCurrentConfiguration());

        assertEquals("testValue", rootSegment.getString("general/testStringKey"));
        assertEquals(Long.valueOf(1), rootSegment.getLong("general/testIntegerKey"));
        assertEquals(Double.valueOf(5.0), rootSegment.getDouble("general/testFloatKey"));
        assertEquals(null, rootSegment.getString("general/invalidKey"));

        ConfigurationSegment subSegment = rootSegment.getSubSegment("general");
        assertEquals("testValue", subSegment.getString("testStringKey"));
        assertEquals(Long.valueOf(1), subSegment.getLong("testIntegerKey"));
        assertEquals(Double.valueOf(5.0), subSegment.getDouble("testFloatKey"));

        configStore.update(rootSegment);
    }

    /**
     * Test the basic read/write cycle.
     * 
     * @throws IOException on uncaught exceptions
     * @throws ConfigurationException on uncaught exceptions
     */
    @Test
    public void testConfigurationWriting() throws IOException, ConfigurationException {
        copyResourceToFile(MINIMAL_CONFIG_TEST_FILE_PATH, testFile);
        ConfigurationStore configStore = new ConfigurationStoreImpl(testFile);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        final WritableConfigurationSegment addedSegment = root.getOrCreateWritableSubSegment("added");
        addedSegment.setString("stringKey", "stringValue");
        configStore.update(root);
        final String content = FileUtils.readFileToString(testFile);
        // whitespace-tolerant RegExp test for the new configuration entries
        assertTrue(content, content.matches("(?s).*\"added\"\\s?: \\{\\s+\"stringKey\"\\s?: \"stringValue\"\\s+\\}.*"));
    }

    private ConfigurationSegment setupPlaceholderConfiguration() {
        testFile.delete();
        assertFalse(testFile.exists());
        ConfigurationStore configStore = new ConfigurationStoreImpl(testFile);
        ConfigurationSegment rootSegment = configStore.createEmptyPlaceholder();
        assertFalse(rootSegment.isPresentInCurrentConfiguration());
        return rootSegment;
    }

    private ConfigurationSegment setupEmptyFileConfiguration() throws IOException {
        FileUtils.write(testFile, "{}");

        ConfigurationStore configStore = new ConfigurationStoreImpl(testFile);
        ConfigurationSegment rootSegment = configStore.getSnapshotOfRootSegment();
        assertTrue(rootSegment.isPresentInCurrentConfiguration());
        return rootSegment;
    }

    private void copyResourceToFile(String resourcePath, File file) throws IOException, FileNotFoundException {
        InputStream testDataStream = getClass().getResourceAsStream(resourcePath);
        assertNotNull("Expected test resource not found", testDataStream);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(testDataStream, fos);
        }
    }

    private void assertNullValuesForVariousRelativePaths(ConfigurationSegment segment) {
        assertNull(segment.getString("a"));
        assertNull(segment.getInteger("a2"));
        assertNull(segment.getLong("b"));
        assertNull(segment.getDouble("c"));
        assertNull(segment.getBoolean("d"));

        assertNull(segment.getString("e/f"));
        assertNull(segment.getInteger("g/h"));
        assertNull(segment.getLong("g/h2"));
        assertNull(segment.getDouble("i/j"));
        assertNull(segment.getBoolean("k/l"));
    }

    private void assertDefaultValueBehaviorForVariousRelativePaths(ConfigurationSegment segment) {
        assertEquals("theDefault", segment.getString("a", "theDefault"));
        assertEquals(Integer.valueOf(3), segment.getInteger("a2", 3));
        assertEquals(Long.valueOf(2L), segment.getLong("b", 2L));
        assertEquals(Double.valueOf(DOUBLE_TEST_VALUE), segment.getDouble("c", DOUBLE_TEST_VALUE));
        assertEquals(Boolean.TRUE, segment.getBoolean("d", true));

        assertEquals("theDefault2", segment.getString("x/a", "theDefault2"));
        assertEquals(Integer.valueOf(2), segment.getInteger("x/a2", 2));
        assertEquals(Long.valueOf(3L), segment.getLong("x/b", 3L));
        assertEquals(Double.valueOf(DOUBLE_TEST_VALUE), segment.getDouble("x/c", DOUBLE_TEST_VALUE));
        assertEquals(Boolean.TRUE, segment.getBoolean("x/d", true));
    }
}
