/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationServiceMessage;
import de.rcenvironment.core.configuration.ConfigurationServiceMessageEvent;
import de.rcenvironment.core.configuration.ConfigurationServiceMessageEventListener;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.bootstrap.ParameterException;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.configuration.bootstrap.SystemExitException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * TestCases for JSONConfigurationServiceTest.java.
 * 
 * @author Heinrich Wendel
 * @author Tobias Menden
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class ConfigurationServiceImplTest {

    private static final String DEFAULT_INSTANCE_DATA_SUBDIR_IN_USER_HOME = ".rce";

    /** Name of the test configuration file. */
    private static final String ID = "de.rcenvironment.rce.configuration.test";

    /** Content of the test configuration file. */
    private static final String CONTENT = "{\n  \"booleanValue\": true\n}";

    /** Default string value. */
    private static final String STRING = "123";

    private static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.core.configuration";

    private static final String USER_HOME_SYSTEM_PROPERTY = "user.home";

    private TempFileService tempFileService;

    private File mockInstallationDir;

    private File mockUserHome;

    private String originalUserHome;

    private final Log log = LogFactory.getLog(getClass());

    private boolean bootstrapSettingsInitialized;

    private File mockInstallationConfigurationDir;

    /**
     * Common test setup.
     * 
     * @throws IOException on setup errors
     */
    @Before
    public void setup() throws IOException {
        RuntimeDetection.allowSimulatedServiceActivation();
        
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        mockInstallationDir = createTempDir("mockInstallationDir");
        mockInstallationConfigurationDir = new File(mockInstallationDir, ConfigurationServiceImpl.CONFIGURATION_SUBDIRECTORY_PATH);
        originalUserHome = System.getProperty(USER_HOME_SYSTEM_PROPERTY);
        mockUserHome = createTempDir("mockUserHome");
        System.setProperty(USER_HOME_SYSTEM_PROPERTY, mockUserHome.getAbsolutePath());
        bootstrapSettingsInitialized = false;
    }

    /**
     * Common test tear-down.
     */
    @After
    public void tearDown() {
        // restore user home
        System.setProperty(USER_HOME_SYSTEM_PROPERTY, originalUserHome);
        // delete override properties
        System.clearProperty(ConfigurationService.SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE);
        System.clearProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH);
    }

    /**
     * Test default behavior, configuration in instance home.
     */
    @Test
    public void testInstance() {
        createMockConfigurationFile(mockInstallationDir, ID, CONTENT);
        ConfigurationServiceImpl service = createDefaultTestInstance();
        DummyConfiguration configuration = service.getConfiguration(ID, DummyConfiguration.class);
        assertEquals(configuration.isBooleanValue(), true);
        assertEquals(configuration.getStringValue(), STRING);
    }

    /**
     * Test default behavior, configuration in user home.
     */
    @Test
    @Ignore
    // TODO review test semantics; is configuration read from user home anymore? - misc_ro
    public void testUserHome() {
        File tempDir = createTempDir();
        // TODO replace with INSTANCE_HOME_PARENT_DIR_OVERRIDE_SYSTEM_PROPERTY?
        System.setProperty(USER_HOME_SYSTEM_PROPERTY, tempDir.getAbsolutePath());
        File rceDir =
            new File(tempDir.getAbsoluteFile() + File.separator + DEFAULT_INSTANCE_DATA_SUBDIR_IN_USER_HOME + File.separator
                + ConfigurationServiceImpl.CONFIGURATION_SUBDIRECTORY_PATH);
        rceDir.mkdirs();
        createMockConfigurationFile(rceDir, ID, CONTENT);

        ConfigurationServiceImpl service = createDefaultTestInstance();
        DummyConfiguration configuration = service.getConfiguration(ID, DummyConfiguration.class);
        assertEquals(configuration.isBooleanValue(), true);
        assertEquals(configuration.getStringValue(), STRING);

        removeTempDir(tempDir);
    }

    /**
     * Tests "instance data directory" default value.
     */
    @Test
    public void testProfileDirDefault() {
        ConfigurationServiceImpl service = createDefaultTestInstance();
        String dataDirPath = service.getProfileDirectory().getAbsolutePath();
        assertTrue(dataDirPath,
            dataDirPath.endsWith(File.separator + DEFAULT_INSTANCE_DATA_SUBDIR_IN_USER_HOME + File.separator + "default"));
    }

    /**
     * Tests "instance data directory" configuration with a relative path.
     */
    @Test
    public void testProfileDirRelativePath() {
        System.setProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH, "myDataDir");

        ConfigurationServiceImpl service = createDefaultTestInstance();

        String dataDirPath = service.getProfileDirectory().getAbsolutePath();
        assertTrue(dataDirPath,
            dataDirPath.endsWith(File.separator + DEFAULT_INSTANCE_DATA_SUBDIR_IN_USER_HOME + File.separator + "myDataDir"));
    }

    /**
     * Tests "instance data directory" configuration with a relative path.
     */
    @Test
    public void testProfileDirAbsolutePath() {

        File tempDir = createTempDir();
        System.setProperty(ProfileUtils.SYSTEM_PROPERTY_DEFAULT_PROFILE_ID_OR_PATH, tempDir.getAbsolutePath());

        ConfigurationServiceImpl service = createDefaultTestInstance();

        String dataDirPath = service.getProfileDirectory().getAbsolutePath();
        assertEquals(dataDirPath, tempDir.getAbsolutePath(), dataDirPath);
    }

    /**
     * Test configuration directory specified by property.
     */
    @Test
    public void testProperty() {
        createMockConfigurationFile(mockInstallationDir, ID, CONTENT);
        ConfigurationServiceImpl service = createDefaultTestInstance();
        DummyConfiguration configuration = service.getConfiguration(ID, DummyConfiguration.class);
        assertEquals(configuration.isBooleanValue(), true);
        assertEquals(configuration.getStringValue(), STRING);
    }

    /**
     * Test broken configuration, default should be provided.
     */
    @Test
    public void testBroken() {
        File tempDir = createTempDir();
        System.setProperty(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA, tempDir.getAbsolutePath() + File.separator);
        createMockConfigurationFile(tempDir, ID, "asdkf");

        ConfigurationServiceImpl service = createDefaultTestInstance();
        DummyConfiguration configuration = service.getConfiguration(ID, DummyConfiguration.class);
        assertEquals(configuration.isBooleanValue(), false);
        assertEquals(configuration.getStringValue(), STRING);

        removeTempDir(tempDir);
    }

    /**
     * Test missing configuration, default should be provided.
     */
    @Test
    public void testMissing() {
        File tempDir = createTempDir();
        System.setProperty(ConfigurationService.SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE, tempDir.getAbsolutePath());

        ConfigurationServiceImpl service = createDefaultTestInstance();
        DummyConfiguration configuration = service.getConfiguration(ID, DummyConfiguration.class);
        assertEquals(configuration.isBooleanValue(), false);
        assertEquals(configuration.getStringValue(), STRING);

        removeTempDir(tempDir);
    }

    /** Test. */
    @Test
    @Ignore
    // TODO review test semantics - misc_ro, April 2014
    public void testGetAbsolutePath() {
        ConfigurationServiceImpl service = createDefaultTestInstance();

        File tempDir = createTempDir();
        assertEquals(tempDir.getAbsolutePath(), service.resolveBundleConfigurationPath(ID, tempDir.getAbsolutePath()));

        System.setProperty(ConfigurationService.SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE, tempDir.getAbsolutePath());

        service = createDefaultTestInstance();

        assertEquals(tempDir.getAbsolutePath() + File.separator + ID + File.separator + tempDir.getName(),
            service.resolveBundleConfigurationPath(ID, tempDir.getName()));

        removeTempDir(tempDir);
    }

    /** Test the default instance name. */
    @Test
    public void testGetInstanceNameForSuccess() {
        ConfigurationServiceImpl service = createDefaultTestInstance();
        String expectedStart = "Unnamed instance started by ";
        // self-test: check pattern for expected start
        assertTrue(ConfigurationServiceImpl.DEFAULT_INSTANCE_NAME_VALUE.startsWith(expectedStart));
        // the actual test
        assertTrue(service.getInstanceName().startsWith(expectedStart));
    }

    /** Test. */
    @Test
    public void testAddErrorListenerSuccess() {
        final ConfigurationServiceImpl service = createDefaultTestInstance();
        final ConfigurationServiceMessageEventListener listener = EasyMock.createMock(ConfigurationServiceMessageEventListener.class);
        listener.handleConfigurationServiceError((ConfigurationServiceMessageEvent) EasyMock.anyObject());
        EasyMock.replay(listener);
        service.addErrorListener(listener);
        final String messageContentString = "message";
        service.fireErrorEvent(new ConfigurationServiceMessage(messageContentString));
        EasyMock.verify(listener);
    }

    /** Test. */
    @Test
    public void testAddErrorListenerFailure() {
        final ConfigurationServiceImpl service = createDefaultTestInstance();
        try {
            service.addErrorListener(null);
            Assert.fail();
        } catch (NullPointerException ok) {
            ok = null;
        }
    }

    /** Test. */
    @Test
    public void testRemoveErrorListenerSuccess() {
        final ConfigurationServiceImpl service = createDefaultTestInstance();
        final ConfigurationServiceMessageEventListener listener = EasyMock.createMock(ConfigurationServiceMessageEventListener.class);
        listener.handleConfigurationServiceError((ConfigurationServiceMessageEvent) EasyMock.anyObject());
        EasyMock.expectLastCall().times(3);
        EasyMock.replay(listener);
        // register the listener and assert it is registered and the handler gets invoked multiple
        // times without being unregistered
        service.addErrorListener(listener);
        final String messageContentString = "message";
        service.fireErrorEvent(new ConfigurationServiceMessage(messageContentString));
        service.fireErrorEvent(new ConfigurationServiceMessage(messageContentString));
        service.fireErrorEvent(new ConfigurationServiceMessage(messageContentString));
        // remove the listener and make sure it does not get invoked any more
        service.removeErrorListener(listener);
        service.fireErrorEvent(new ConfigurationServiceMessage(messageContentString));
        EasyMock.verify(listener);
    }

    /** Test. */
    @Test
    public void testRemoveErrorListenerFailure() {
        final ConfigurationServiceImpl service = createDefaultTestInstance();
        try {
            service.removeErrorListener(null);
            Assert.fail();
        } catch (NullPointerException ok) {
            ok = null;
        }
    }

    /**
     * Test.
     * 
     * @throws Exception on unexpected failure
     */
    // TODO (p2) test does not fit the new mocking approach; review/rework - misc_ro
    @Test
    public void testGetConfigurationParsingErrors() throws Exception {

        // NOTE: This is an ugly and potentially unsafe fix for this test failing if no other test called this method before.
        // The problem is that the initialization may read system properties that have been set by other tests before, which makes it
        // potentially unstable.
        // It is okay as a transient fix, however, as the same initialization happened before already, just randomly by other code.
        // The proper solution is to review and rework this test as a whole, as noted above. - misc_ro, 2017-07
        BootstrapConfiguration.initialize();

        System.setProperty(ConfigurationService.SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE,
            mockInstallationDir.getAbsolutePath());

        /**
         * {@link ConfigurationServiceMessageEventListener} implementation caching the most recent error message.
         */
        class CachingListener implements ConfigurationServiceMessageEventListener {

            private String lastErrorMessage;

            @Override
            public void handleConfigurationServiceError(ConfigurationServiceMessageEvent error) {
                lastErrorMessage = error.getError().getMessage();
            }
        }

        // create a dummy configuration file so the overwritten parsing method is actually called
        mockInstallationConfigurationDir.mkdir();
        new File(mockInstallationConfigurationDir, "dummy.json").createNewFile();

        // note: this must be activated after the mock configuration directory is created
        final CustomTestConfigurationService service = new CustomTestConfigurationService();
        service.mockActivate();

        final CachingListener listener = new CachingListener();
        service.addErrorListener(listener);
        // test parsing error
        // Although the constructor used for constructing a JsonParseException here is deprecated, the Jackson-documentation offers no
        // alternative. Thus, we keep this construction.
        @SuppressWarnings("deprecation") final JsonParseException parseException = new JsonParseException("parsing error", null);
        service.setExceptionToThrow(parseException);
        service.getConfiguration("dummy", Object.class);
        Assert.assertNotNull(listener.lastErrorMessage);
        Assert.assertTrue(!listener.lastErrorMessage.isEmpty());
        // test mapping error
        // Deprecated constructor, see above.
        @SuppressWarnings("deprecation") final JsonMappingException mappingException =
            new JsonMappingException("mapping error", (JsonLocation) null);
        service.setExceptionToThrow(mappingException);
        service.getConfiguration("dummy", Object.class);
        Assert.assertTrue(!listener.lastErrorMessage.isEmpty());
    }

    /** Tests the behavior with no explicit installation path and an undefined OSGi install area. */
    @Test
    public void testUnidentifiableInstallDataLocation() {
        // Initialize bootstrap configuration to avoid NPE in case it has not been initialized before.
        if (!bootstrapSettingsInitialized) {
            try {
                BootstrapConfiguration.initialize(); // apply simulated home directory
            } catch (ProfileException | ParameterException | SystemExitException e) {
                throw new RuntimeException(e); // avoid IOException handling in many tests for a rare failure case
            }
            bootstrapSettingsInitialized = true;
        }
        System.clearProperty(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA);
        final ConfigurationServiceImpl service = new ConfigurationServiceImpl();
        try {
            service.mockActivate();
            fail("Exception expected");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(BootstrapConfiguration.SYSTEM_PROPERTY_OSGI_INSTALL_AREA));
        }
    }

    /** Test. */
    @Test
    public void testGetConfigurationFailure() {
        final ConfigurationServiceImpl service = createDefaultTestInstance();
        /** Test. */
        final class Test {

            private Test() {
                // do nothing
            }
        }
        try {
            log.debug("Expecting an InstantiationException on the next call; ignore related log output");
            service.getConfiguration("test", Test.class);
            Assert.fail();
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof InstantiationException)) {
                Assert.fail();
            }
        }
    }

    /**
     * Test for addSubstitutionProperties() and getConfiguration().
     * 
     * @throws IOException on I/O errors
     */
    @Test
    @Ignore // broken since removing the discovery mechanism
    // TODO (p2) this uses and tests a deprecated method only used by other tests; needs review whether it should be removed completely
    public void testGetPropertySubstitution() throws IOException {

        String configFileBasename = "temp.unittest";
        String testNamespace = "testns";

        File tempDir = TempFileServiceAccess.getInstance().createManagedTempDir();
        System.setProperty(ConfigurationService.SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE, tempDir.getAbsolutePath());
        File testConfigFile = new File(tempDir, configFileBasename + ".json");
        if (testConfigFile.exists()) {
            Assert.fail("Unexpected state: File " + testConfigFile.getAbsolutePath() + " already exists");
        }

        final ConfigurationServiceImpl service = new ConfigurationServiceImpl();

        DummyConfiguration config;
        Map<String, String> testProperties;

        // check basic property file reading
        FileUtils.writeStringToFile(testConfigFile, "{ \"stringValue\": \"hardcoded\" }");
        config = service.getConfiguration(configFileBasename, DummyConfiguration.class);
        Assert.assertEquals("No-substitution test failed", "hardcoded", config.getStringValue());
        // test property file reading with missing property

        // test property file reading with defined property not containing quotes
        FileUtils.writeStringToFile(testConfigFile, "{ \"stringValue\": \"${testns:unquoted}\" }");
        testProperties = new HashMap<String, String>();
        testProperties.put("unquoted", "unquotedValue");
        service.addSubstitutionProperties(testNamespace, testProperties);
        config = service.getConfiguration(configFileBasename, DummyConfiguration.class);
        Assert.assertEquals("Unquoted value test failed", "unquotedValue", config.getStringValue());

        // test property file reading with defined property containing quotes
        FileUtils.writeStringToFile(testConfigFile, "{ \"stringValue\": ${testns:quoted} }");
        testProperties = new HashMap<String, String>();
        testProperties.put("quoted", "\"quotedValue\"");
        service.addSubstitutionProperties(testNamespace, testProperties);
        config = service.getConfiguration(configFileBasename, DummyConfiguration.class);
        Assert.assertEquals("Quoted value test failed", "quotedValue", config.getStringValue());

        // test with boolean property
        FileUtils.writeStringToFile(testConfigFile, "{ \"booleanValue\": ${testns:boolKey} }");
        testProperties = new HashMap<String, String>();
        testProperties.put("boolKey", "true"); // class default is "false"; no quotes
        service.addSubstitutionProperties(testNamespace, testProperties);
        config = service.getConfiguration(configFileBasename, DummyConfiguration.class);
        Assert.assertEquals("Boolean value test failed", true, config.isBooleanValue());
    }

    /**
     * Creates a new temporary directory.
     * 
     * @return The File object of the directory.
     */
    private File createTempDir() {
        try {
            return tempFileService.createManagedTempDir();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test temp dir", e);
        }
    }

    /**
     * Creates a new temporary directory.
     * 
     * @return The File object of the directory.
     */
    private File createTempDir(String infoText) {
        try {
            return tempFileService.createManagedTempDir(infoText);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test temp dir", e);
        }
    }

    /**
     * Recursively removes a temporary directory.
     * 
     * @param file The File object of the directory.
     */
    private void removeTempDir(File file) {
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a test instance if {@link ConfigurationServiceImpl} with an injected mock discovery bootstrap service. In this method
     * variant, the mocked discovery service returns an empty map of discovery properties.
     * 
     * @return the created instance
     */
    public ConfigurationServiceImpl createDefaultTestInstance() {
        return createTestInstance(mockInstallationDir, new HashMap<String, String>());
    }

    /**
     * Creates a test instance if {@link ConfigurationServiceImpl} with an injected mock discovery bootstrap service. In this method
     * variant, the mocked discovery service returns the provided map of discovery properties to the calling
     * {@link ConfigurationServiceImpl}.
     * 
     * @param installationDir
     * @return the created instance
     */
    private ConfigurationServiceImpl createTestInstance(File installationDir, Map<String, String> substitutionProperties) {
        if (!bootstrapSettingsInitialized) {
            try {
                BootstrapConfiguration.initialize(); // apply simulated home directory
            } catch (ProfileException | ParameterException | SystemExitException e) {
                throw new RuntimeException(e); // avoid IOException handling in many tests for a rare failure case
            }
            bootstrapSettingsInitialized = true;
        }
        System.setProperty(ConfigurationService.SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE,
            installationDir.getAbsolutePath());
        ConfigurationServiceImpl service = new ConfigurationServiceImpl();
        service.mockActivate();
        return service;
    }

    /**
     * Creates a test configuration in the given directory.
     * 
     * @param installationDataDir the mock installation directory
     * @param id The id of the test configuration.
     * @param content The content of the test configuration.
     */
    private void createMockConfigurationFile(File installationDataDir, String id, String content) {
        File configDir = new File(installationDataDir, "configuration");
        configDir.mkdir();
        File file = new File(configDir, id + ".json");
        try {
            file.createNewFile();
            FileWriter fstream = new FileWriter(file);
            fstream.write(content);
            fstream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a bundleContextMock.
     * 
     * @return bundleContextMock.
     */
    public BundleContext getBundleContextMock() {
        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME).anyTimes();
        EasyMock.replay(bundleMock);
        BundleContext bundleContextMock = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContextMock.getBundle()).andReturn(bundleMock).anyTimes();
        EasyMock.replay(bundleContextMock);
        return bundleContextMock;
    }

    /**
     * {@link ConfigurationServiceImpl that throws parsing exceptions.
     */
    private class CustomTestConfigurationService extends ConfigurationServiceImpl {

        private Exception exceptionToThrow;

        private void setExceptionToThrow(final JsonParseException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        private void setExceptionToThrow(final JsonMappingException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        protected <T> T parseConfigurationFile(Class<T> clazz, File file) throws IOException, JsonParseException,
            JsonMappingException {
            if (exceptionToThrow instanceof JsonParseException) {
                throw (JsonParseException) exceptionToThrow;
            } else if (exceptionToThrow instanceof JsonMappingException) {
                throw (JsonMappingException) exceptionToThrow;
            } else {
                throw new RuntimeException(exceptionToThrow);
            }
        }

    }

}
