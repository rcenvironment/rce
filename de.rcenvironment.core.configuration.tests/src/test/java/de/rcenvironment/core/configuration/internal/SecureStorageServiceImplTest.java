/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.SecureStorageSection;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * {@link SecureStorageServiceImpl} test.
 *
 * @author Robert Mischke
 */
@RunWith(Parameterized.class)
public class SecureStorageServiceImplTest {

    /**
     * As there are spurious failures (less than 1%) caused by the underlying Eclipse secure storage API, the past, run these tests
     * repeatedly to verify that our safeguards them this work. The exact number should be a compromise between a decent detection
     * probability, and keeping the test duration sane. -- misc_ro, Mar 2019
     */
    private static final int NUMBER_OF_TEST_REPETITIONS = 100;

    private static final String NON_NULL_DEFAULT_VALUE = "nonNullDefault";

    private static final String MESSAGE_TEXT_TEST_CANNOT_RUN_OUTSIDE_OF_PLUGIN_ENVIRONMENT =
        "These test steps require Eclipse infrastructure "
            + "and can therefore not be run as a plain unit test; skipping";

    private static final String TEST_SECTION_ID = "test1";

    private static final String VAL1 = "val1";

    private static final String KEY1 = "key1";

    private static final String KEY_FOR_NULL_VALUE = "key_for_null_value";

    private final Log log = LogFactory.getLog(getClass());

    private TempFileService tempFileService;

    private File profileInternalDir;

    private File userCommonDir;

    private File keyFile;

    private File storeFile;

    private File backupFile;

    public SecureStorageServiceImplTest() {
        this.tempFileService = TempFileServiceAccess.getInstance();
    }

    /**
     * Common, single-run test initialization.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * @Before method.
     * @throws IOException on setup failure
     */
    @Before
    public void before() throws IOException {

        System.clearProperty(SecureStorageServiceImpl.PASSWORD_OVERRIDE_PROPERTY);

        profileInternalDir = tempFileService.createManagedTempDir();
        userCommonDir = tempFileService.createManagedTempDir();

        keyFile = new File(userCommonDir, SecureStorageServiceImpl.KEY_FILE_NAME);
        storeFile = new File(profileInternalDir, SecureStorageServiceImpl.SECURE_SETTINGS_FILE_NAME);
        backupFile = new File(profileInternalDir, SecureStorageServiceImpl.SECURE_SETTINGS_BACKUP_FILE_NAME);

        assertFalse(keyFile.isFile());
        assertFalse(storeFile.isFile());
        assertFalse(backupFile.isFile());
    }

    /**
     * @return a synthetic array of "parameters" to trigger repeated test runs
     */
    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[NUMBER_OF_TEST_REPETITIONS][0];
    }

    /**
     * @After method.
     * @throws IOException on setup failure
     */
    @After
    public void after() throws IOException {
        tempFileService.disposeManagedTempDirOrFile(profileInternalDir);
        tempFileService.disposeManagedTempDirOrFile(userCommonDir);
    }

    /**
     * Tests initializing the store service, saving and re-reading a value, re-reading from a new service instance, and attempting to
     * re-read from another instance using a different password (which should fail).
     * 
     * @throws Exception on uncaught failures
     */
    @Test
    public void passwordOverrideAndEffectOfPasswordChange() throws Exception {

        SecureStorageServiceImpl service = setupMockService();

        if (runningOutsideOfPluginEnvironment()) {
            log.warn(MESSAGE_TEXT_TEST_CANNOT_RUN_OUTSIDE_OF_PLUGIN_ENVIRONMENT);
            return;
        }

        System.setProperty(SecureStorageServiceImpl.PASSWORD_OVERRIDE_PROPERTY, "pw1");
        service.initialize();
        assertTrue(storeFile.isFile()); // should have been created
        assertFalse(keyFile.isFile()); // should not have been created as password override is being used
        assertFalse(backupFile.isFile());

        service.getSecureStorageSection(TEST_SECTION_ID).store(KEY1, VAL1);
        service.getSecureStorageSection(TEST_SECTION_ID).store(KEY_FOR_NULL_VALUE, null);

        assertEquals(VAL1, service.getSecureStorageSection(TEST_SECTION_ID).read(KEY1, null));
        assertNull(service.getSecureStorageSection(TEST_SECTION_ID).read(KEY_FOR_NULL_VALUE, NON_NULL_DEFAULT_VALUE));

        // try to read from a different service instance using the same password -> should succeed
        SecureStorageServiceImpl service2 = setupMockService();
        service2.initialize();
        assertTrue(backupFile.isFile()); // should have been created now
        assertEquals(VAL1, service2.getSecureStorageSection(TEST_SECTION_ID).read(KEY1, null));

        // try to read from a different service instance using a different password -> should fail
        SecureStorageServiceImpl service3 = setupMockService();
        System.setProperty(SecureStorageServiceImpl.PASSWORD_OVERRIDE_PROPERTY, "pw2");
        service3.initialize();

        try {
            final String value = service3.getSecureStorageSection(TEST_SECTION_ID).read(KEY1, null);
            fail("Re-reading the non-null value with a different password did not fail as expected; re-read value: " + value);
        } catch (OperationFailureException e) {
            // expected behavior
        }

        try {
            final String value = service3.getSecureStorageSection(TEST_SECTION_ID).read(KEY_FOR_NULL_VALUE, NON_NULL_DEFAULT_VALUE);
            fail("Re-reading the null value with a different password did not fail as expected; re-read value: " + value);
        } catch (OperationFailureException e) {
            // expected behavior
        }

        // make it clear in log output whether the full test was run
        log.info("Full secure storage test case completed, including the parts that require Eclipse infrastructure");
    }

    /**
     * Tests initializing the store service, saving and re-reading a value, re-reading from a new service instance, and attempting to
     * re-read from another instance using a different password (which should fail).
     * 
     * @throws Exception on uncaught failures
     */
    @Test
    public void automaticPasswordGenerationAndReuse() throws Exception {

        SecureStorageServiceImpl service = setupMockService();

        if (runningOutsideOfPluginEnvironment()) {
            log.warn(MESSAGE_TEXT_TEST_CANNOT_RUN_OUTSIDE_OF_PLUGIN_ENVIRONMENT);
            return;
        }

        service.initialize();
        assertTrue(storeFile.isFile()); // should have been created
        assertTrue(keyFile.isFile()); // should have been created
        assertFalse(backupFile.isFile());

        service.getSecureStorageSection(TEST_SECTION_ID).store(KEY1, VAL1);
        service.getSecureStorageSection(TEST_SECTION_ID).store(KEY_FOR_NULL_VALUE, null);

        assertEquals(VAL1, service.getSecureStorageSection(TEST_SECTION_ID).read(KEY1, null));
        assertNull(service.getSecureStorageSection(TEST_SECTION_ID).read(KEY_FOR_NULL_VALUE, NON_NULL_DEFAULT_VALUE));

        // try to read from a different service instance using the same password -> should succeed
        SecureStorageServiceImpl service2 = setupMockService();
        service2.initialize();
        assertTrue(keyFile.isFile()); // should still exist
        assertTrue(backupFile.isFile()); // should have been created now
        assertEquals(VAL1, service2.getSecureStorageSection(TEST_SECTION_ID).read(KEY1, null));
        assertNull(service.getSecureStorageSection(TEST_SECTION_ID).read(KEY_FOR_NULL_VALUE, NON_NULL_DEFAULT_VALUE));

        // now delete the key file, and try to read from a different service -> should fail
        assertTrue(keyFile.delete());
        SecureStorageServiceImpl service3 = setupMockService();
        service3.initialize();
        try {
            service3.getSecureStorageSection(TEST_SECTION_ID).read(KEY1, null);
            fail("Re-reading the non-null value with a different (generated) password did not fail as expected");
        } catch (OperationFailureException e) {
            // expected behavior
        }

        try {
            service3.getSecureStorageSection(TEST_SECTION_ID).read(KEY_FOR_NULL_VALUE, NON_NULL_DEFAULT_VALUE);
            fail("Re-reading the null value with a different (generated) password did not fail as expected");
        } catch (OperationFailureException e) {
            // expected behavior
        }

        // make it clear in log output whether the full test was run
        log.info("Full secure storage test case completed, including the parts that require Eclipse infrastructure");
    }

    @SuppressWarnings("restriction") // use of non-API method below
    private boolean runningOutsideOfPluginEnvironment() {
        return org.eclipse.equinox.internal.security.auth.AuthPlugin.getDefault() == null;
    }

    private SecureStorageServiceImpl setupMockService() {
        SecureStorageServiceImpl service = new SecureStorageServiceImpl();
        ConfigurationService configServiceMock = EasyMock.createMock(ConfigurationService.class);
        configServiceMock.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA);
        EasyMock.expectLastCall().andReturn(profileInternalDir);
        configServiceMock.getConfigurablePath(ConfigurablePathId.SHARED_USER_SETTINGS_ROOT);
        EasyMock.expectLastCall().andReturn(userCommonDir).anyTimes(); // not called if password override is used
        EasyMock.replay(configServiceMock);
        service.bindConfigurationService(configServiceMock);
        return service;
    }

    /**
     * Verifies that null values can be stored and retrieved again as expected.
     * 
     * @throws Exception on uncaught failures
     */
    @Test
    public void nullValueHandling() throws Exception {

        SecureStorageServiceImpl service = setupMockService();
        // note: this relies on the other test cases testing basic storage behavior, so this is not done again here

        if (runningOutsideOfPluginEnvironment()) {
            log.warn(MESSAGE_TEXT_TEST_CANNOT_RUN_OUTSIDE_OF_PLUGIN_ENVIRONMENT);
            return;
        }

        System.setProperty(SecureStorageServiceImpl.PASSWORD_OVERRIDE_PROPERTY, "pw1");
        service.initialize();

        final SecureStorageSection storageSection = service.getSecureStorageSection(TEST_SECTION_ID);
        storageSection.store(KEY1, VAL1);
        assertEquals(VAL1, storageSection.read(KEY1, null));

        storageSection.store(KEY1, null);
        assertNull(storageSection.read(KEY1, NON_NULL_DEFAULT_VALUE));

        storageSection.store(KEY1, VAL1);
        assertEquals(VAL1, storageSection.read(KEY1, NON_NULL_DEFAULT_VALUE)); // check against artifacts from non-null default value
    }

}
