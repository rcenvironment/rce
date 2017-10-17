/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.internal.ConfigurationStore;
import de.rcenvironment.core.configuration.internal.ConfigurationStoreImpl;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.ConfigurationFlag;
import de.rcenvironment.core.instancemanagement.internal.ConfigurationSegmentFactory.SegmentBuilder;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;

/**
 * Unit tests for {@link InstanceManagementServiceImpl} that can be run without triggering heavy-weight I/O operations.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class InstanceManagementServiceImplTest {

    private static final String CONFIG_PATH = "/instanceManagementTest/configuration.json";

    private static final String OK_ID = "ok";

    private static final String NOT_EXISTING_SHUTDOWN_ID = "bob";

    private static final String INVALID_ID_1 = "";

    private static final String INVALID_ID_2 = "..";

    /**
     * Expected exception.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private LoggingTextOutReceiver userOutputReceiver;

    private InstanceManagementServiceImpl imService;

    private SegmentBuilder segmentBuilder = ConfigurationSegmentFactory.getSegmentBuilder();

    private ConfigurationStore configStore;

    private ConfigurationChangeSequence sequence = new ConfigurationChangeSequence();

    private TempFileService tempFileService;

    private File testFile;

    /**
     * Common setup.
     * 
     * @throws IOException on failure.
     */
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        tempFileService = TempFileServiceAccess.getInstance();
        File tempDir = tempFileService.createManagedTempDir();
        testFile = tempFileService.createTempFileWithFixedFilename("configuration.json");
        imService = new InstanceManagementServiceImpl();
        userOutputReceiver = new LoggingTextOutReceiver("");
        copyResourceToFile(CONFIG_PATH, testFile);
        configStore = new ConfigurationStoreImpl(testFile);
        imService.setProfilesRootDir(new File(tempDir.getParentFile().getPath()));
        imService.validateLocalConfig();
    }

    private void copyResourceToFile(String resourcePath, File file) throws IOException, FileNotFoundException {
        InputStream testDataStream = getClass().getResourceAsStream(resourcePath);
        assertNotNull("Expected test resource not found", testDataStream);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(testDataStream, fos);
        }
    }

    /**
     * 
     * Clean up.
     * 
     * @throws IOException on failure.
     * @throws ConfigurationException on config failure.
     */
    @After
    public void cleanUp() throws IOException, ConfigurationException {

    }

    /**
     * Tests that stopping an instance, which doesn't exist, won't lead to new folder creation.
     * 
     * @throws IOException on expected failure.
     */
    @Test
    public void stopingNotExistingInstanceIsProperlyHandled() {
        List<String> paramList = new ArrayList<>();
        Exception exception = null;
        paramList.add(NOT_EXISTING_SHUTDOWN_ID);
        try {
            imService.stopInstance(paramList, userOutputReceiver, 0);
        } catch (IOException e) {
            exception = e;
        } finally {
            assertTrue(exception != null);
            assertTrue(exception.getMessage().contains("tried to shutdown instance, which doesn't exist"));
            final File profileDir = new File(imService.getProfilesRootDir(), NOT_EXISTING_SHUTDOWN_ID);
            assertFalse(profileDir.exists());
        }

        // fallback solution if {@link InstanceManagementServiceImpl#stopInstance(List<String>, String, TextOutputReceiver)} method
        // actually succeeds.
        assertTrue(exception != null);
    }

    /**
     * Tests that identical ids in the parameter list of the {@link InstanceManagementServiceImpl#startInstance(List<String>, String,
     * de.rcenvironment.core.utils.common.textstream.TextOutputReceiver)} method are rejected.
     * 
     * @throws IOException on expected failure.
     */
    @Test
    public void identicalIdsAreProperlyRejected() {
        List<String> paramList = new ArrayList<>();
        List<Exception> exceptionList = new ArrayList<>();
        paramList.add(OK_ID);
        paramList.add(OK_ID);
        String message = "";
        boolean success = false;
        try {
            imService.startInstance(OK_ID, paramList, userOutputReceiver, 0, false);
        } catch (IOException e) {
            exceptionList.add(e);
        } finally {
            try {
                imService.stopInstance(paramList, userOutputReceiver, 0);
            } catch (IOException e) {
                exceptionList.add(e);
            } finally {
                assertTrue(exceptionList.size() == 2);
                for (Exception e : exceptionList) {
                    message = e.getMessage();
                    if (message.contains("multiple instances with identical id")) {
                        success = true;
                        break;
                    }
                }
            }
        }
        if (success) {
            assertTrue(message.contains("multiple instances with identical id"));
        }
        // fallback solution if {@link InstanceManagementServiceImpl#startInstance(List<String>, String, TextOutputReceiver)} method
        // actually succeeds.
        assertTrue(exceptionList.size() == 2);
    }

    /**
     * Tests that malformed instance ids are properly rejected by all public methods, ie an exception with an appropriate text is thrown.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void invalidInstanceIdsAreProperlyRejected() throws IOException {
        List<String> paramList = new ArrayList<>();

        paramList.add(INVALID_ID_1);

        thrown.expect(IOException.class);
        thrown.expectMessage("Malformed id:");

        imService.stopInstance(paramList, userOutputReceiver, 0);

        paramList.remove(INVALID_ID_1);
        paramList.add(INVALID_ID_2);

        imService.stopInstance(paramList, userOutputReceiver, 0);

        paramList.remove(INVALID_ID_2);
        paramList.add(null);

        thrown.expectMessage("Malformed command: either no installation id or instance id defined.");
        imService.stopInstance(paramList, userOutputReceiver, 0);
    }

    /**
     * Tests that malformed installation ids are properly rejected by all public methods, ie an exception with an appropriate text is
     * thrown.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void invalidInstalaltionIdsAreProperlyRejected() throws IOException {
        List<String> paramList = new ArrayList<>();

        paramList.add(OK_ID);

        thrown.expect(IOException.class);
        thrown.expectMessage("Malformed command: either no installation id or instance id defined.");

        imService.startInstance(null, paramList, userOutputReceiver, 0, false);

        String id = "Berta";
        thrown.expectMessage("Installation with id: " + id + " does not exist.");
        paramList.add(id);
        imService.startInstance(OK_ID, paramList, userOutputReceiver, 0, false);
    }

    /**
     * 
     * Test if setting the instance name works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingInstanceName() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_NAME, String.class, "Bruno"));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getString(segmentBuilder.general().instanceName().getConfigurationKey()), "Bruno");
    }

    /**
     * 
     * Test if setting the instance comment works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingInstanceComment() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_COMMENT, String.class, "Java ist nice"));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getString(segmentBuilder.general().comment().getConfigurationKey()), "Java ist nice");
    }

    /**
     * 
     * Test if setting the workflow host flag works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingWorkflowHost() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_WORKFLOWHOST, Boolean.class, true));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.general().isWorkflowHost().getConfigurationKey()), true);
    }

    /**
     * 
     * Test if setting the relay flag works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingRelayFlag() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_RELAY, Boolean.class, true));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.general().isRelay().getConfigurationKey()), true);
    }

    /**
     * 
     * Test if setting the temp directory works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingTempDir() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.TEMP_DIR, String.class, "Rachel"));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getString(segmentBuilder.general().tempDirectory().getConfigurationKey()), "Rachel");
    }

    /**
     * 
     * Test if setting the deprecated input tab works corretly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingEnabledDepInTab() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_DEP_INPUT_TAB,
            Boolean.class, true));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.general().enableDeprecatedInputTab().getConfigurationKey()), true);
    }

    /**
     * 
     * Test if setting the background monitoring works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testEnableBackgroundMonitoring() throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("Donna", 2);
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_BACKGROUND_MONITORING, Object.class, map));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.backgroundMonitoring().getPath());
        assertEquals(segment.getString(segmentBuilder.backgroundMonitoring().enableIds().getConfigurationKey()), "Donna");
        assertTrue(segment.getInteger(segmentBuilder.backgroundMonitoring().intervalSeconds().getConfigurationKey()) == 2);
    }

    /**
     * 
     * Test if setting the request timeout works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingRequestTimeout() throws IOException {
        final long niceValue = 42;
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.REQUEST_TIMEOUT,
            Long.class, niceValue));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().getPath());
        assertTrue(segment.getLong(segmentBuilder.network().requestTimeoutMsec().getConfigurationKey()) == niceValue);
    }

    /**
     * 
     * Test if setting the forwarding timeout works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingForwardingTimeout() throws IOException {
        final long niceValue = 42;
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.FORWARDING_TIMEOUT,
            Long.class, niceValue));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().getPath());
        assertTrue(segment.getLong(segmentBuilder.network().forwardingTimeoutMsec().getConfigurationKey()) == niceValue);
    }

    /**
     * 
     * Test if adding a new connection correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testAddingConnection() throws IOException {
        final String name = "Mike";
        final String host = "Ross";
        final int port = 42;
        final boolean connectOnStartup = false;
        final long autoRetrylInitDelay = 42;
        final long autoRetryMax = 42;
        final int multi = 42;

        ConfigurationConnection connection =
            new ConfigurationConnection(name, host, port, connectOnStartup, autoRetrylInitDelay, autoRetryMax, multi);
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_CONNECTION, ConfigurationConnection.class, connection));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().connections().getOrCreateConnection(name).getPath());
        assertTrue(segment.getInteger(segmentBuilder.network().connections().getOrCreateConnection(name).autoRetryDelayMultiplier()
            .getConfigurationKey()) == multi);
        assertTrue(segment.getLong(segmentBuilder.network().connections().getOrCreateConnection(name).autoRetryInitialDelay()
            .getConfigurationKey()) == autoRetrylInitDelay);
        assertTrue(segment.getLong(segmentBuilder.network().connections().getOrCreateConnection(name).autoRetryMaximumDelay()
            .getConfigurationKey()) == autoRetryMax);
        assertEquals(
            segment.getBoolean(segmentBuilder.network().connections().getOrCreateConnection(name).connectOnStartup().getConfigurationKey()),
            false);
        assertTrue(segment.getInteger(segmentBuilder.network().connections().getOrCreateConnection(name).port()
            .getConfigurationKey()) == port);
        assertEquals(segment.getString(segmentBuilder.network().connections().getOrCreateConnection(name).host()
            .getConfigurationKey()), host);
    }

    /**
     * 
     * Test if adding a new server port works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testAddingServerPort() throws IOException {
        final int testPort = 42;
        List<String> objList = new LinkedList<>();
        String name = "sillyPort";
        String ip = "niceIP";
        String port = Integer.toString(testPort);
        objList.add(name);
        objList.add(ip);
        objList.add(port);
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_SERVER_PORT, Object.class, objList));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().ports().getOrCreateServerPort(name).getPath());
        assertEquals(segment.getString(segmentBuilder.network().ports().getOrCreateServerPort(name).ip().getConfigurationKey()), ip);
        assertEquals(Integer.valueOf(testPort),
            segment.getInteger(segmentBuilder.network().ports().getOrCreateServerPort(name).port().getConfigurationKey()));
    }

    /**
     * 
     * Test if setting the ip filter works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingIpFilter() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_DEP_INPUT_TAB, Boolean.class, true));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.general().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.general().enableDeprecatedInputTab().getConfigurationKey()), true);
    }

    /**
     * 
     * Test if adding an allowed ip works correctly.
     * 
     * @throws IOException on failure.
     * @throws ConfigurationException on config failure.
     */
    @Test
    public void testAddingAllowedIP() throws IOException, ConfigurationException {
        String ip = "Jessica";
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_ALLOWED_IP, String.class, ip));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.network().ipFilter().getPath());
        assertTrue(segment.getStringArray(segmentBuilder.network().ipFilter().allowedIps().getConfigurationKey()).contains(ip));
    }

    /**
     * 
     * Test if adding new ssh connection works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testAddingSshConnection() throws IOException {
        final String name = "niceConnection";
        final String displayName = "nice";
        final String host = "bestHost";
        final int port = 42;
        final String loginName = "nicerLoginName";

        ConfigurationSshConnection connection = new ConfigurationSshConnection(name, displayName, host, port, loginName);
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ADD_SSH_CONNECTION, ConfigurationSshConnection.class, connection));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment =
            root.getSubSegment(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).getPath());

        assertEquals(
            segment.getString(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).displayName()
                .getConfigurationKey()), displayName);
        assertEquals(segment.getString(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).host()
            .getConfigurationKey()), host);
        assertEquals(
            segment.getString(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).loginName()
                .getConfigurationKey()), loginName);
        assertTrue(segment.getInteger(segmentBuilder.sshRemoteAccess().sshConnections().getOrCreateSshConnection(name).port()
            .getConfigurationKey()) == port);
    }

    /**
     * 
     * Test if adding new published component correctly.
     * 
     * @throws IOException on failure.
     * @throws ConfigurationException on config failure.
     */
    @Test
    public void testAddingPublishedComponent() throws IOException, ConfigurationException {
        String component = "heftigeKomponente";
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.PUBLISH_COMPONENT, String.class, component));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.publishing().getPath());
        assertTrue(segment.getStringArray(segmentBuilder.publishing().components().getConfigurationKey()).contains(component));
    }

    /**
     * 
     * Test if setting the ssh server works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingSshServer() throws IOException {
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.ENABLE_SSH_SERVER, Boolean.class, true));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.sshServer().getPath());
        assertEquals(segment.getBoolean(segmentBuilder.sshServer().enabled().getConfigurationKey()), true);
    }

    /**
     * 
     * Test if setting the ssh server ip works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingSshServerIP() throws IOException {
        String ip = "Harvey";
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_SSH_SERVER_IP, String.class, ip));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.sshServer().getPath());
        assertEquals(segment.getString(segmentBuilder.sshServer().ip().getConfigurationKey()), ip);
    }

    /**
     * 
     * Test if setting the ssh server port works correctly.
     * 
     * @throws IOException on failure.
     */
    @Test
    public void testSettingSshServerPort() throws IOException {
        final int port = 42;
        sequence.append(new ConfigurationChangeEntry(ConfigurationFlag.SET_SSH_SERVER_PORT, Integer.class, port));
        imService.configureInstance(testFile.getParentFile().getName(), sequence, userOutputReceiver);
        ConfigurationSegment root = configStore.getSnapshotOfRootSegment();
        ConfigurationSegment segment = root.getSubSegment(segmentBuilder.sshServer().getPath());
        assertTrue(segment.getInteger(segmentBuilder.sshServer().port().getConfigurationKey()) == port);
    }
}
