/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindrot.jbcrypt.BCrypt;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.instancemanagement.InstanceManagementConstants;
import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.AbstractTextOutputReceiver;
import de.rcenvironment.core.utils.incubator.FileSystemOperations;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;

/**
 * Default {@link InstanceManagementService} implementation.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public class InstanceManagementServiceImpl implements InstanceManagementService {

    private static final String ZIP = ".zip";

    private static final String SLASH = "/";

    private static final String INDENT = "- ";

    private static final String TEMPLATES = "templates";

    private static final String VERSION = ".version";

    private static final Pattern VALID_IDS_REGEXP_PATTERN = Pattern.compile("[a-zA-Z0-9-_]+");

    private static final String GENERIC_PLACEHOLDER_STRING = "*";

    private static final String CONFIGURATION_SUBTREE_PATH = "instanceManagement";

    private static final String CONFIGURATION_FILENAME = "configuration.json";

    // TODO find actual value through testing
    private static final int MAX_INSTALLATION_ROOT_PATH_LENGTH = 30;

    private static final String DATA_ROOT_DIRECTORY_PROPERTY = "dataRootDirectory";

    private static final String INSTALLATIONS_ROOT_DIR_PROPERTY = "installationsRootDirectory";

    private static final Map<String, InstanceConfigurationImpl> CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP = new HashMap<>();

    private static final String SUCCESS = " was successful.";

    private static final String TO_INSTANCE = " to instance ";

    private static final String OF_INSTANCE = " of instance ";

    private File dataRootDir;

    private File installationsRootDir;

    private File profilesRootDir;

    private File templatesRootDir;

    private File downloadsCacheDir;

    private volatile boolean hasValidLocalConfiguration = false;

    private volatile boolean hasValidDownloadConfiguration = false;

    private ConcurrentHashMap<String, String> profileIdToInstallationIdMap = new ConcurrentHashMap<>();

    private ConfigurationService configurationService;

    private PersistentSettingsService persistentSettingsService;

    // split into distinct classes to allow separate testing
    private final InstanceOperations instanceOperations = new InstanceOperationsImplSynchronizeDecorator(
        new InstanceOperationsImplReleaseLockDecorator(new InstanceOperationsImpl()));

    private final DeploymentOperationsImpl deploymentOperations = new DeploymentOperationsImpl();

    private TempFileService tfs;

    private String downloadSourceFolderUrlPattern;

    private String downloadFilenamePattern;

    private final TextOutputReceiver fallbackUserOutputReceiver;

    private final Log log = LogFactory.getLog(getClass());

    public InstanceManagementServiceImpl() {
        fallbackUserOutputReceiver = new AbstractTextOutputReceiver() {

            @Override
            public void addOutput(String line) {
                log.info("Operation progress: ");
            }
        };
    }

    /**
     * OSGi-DS lifecycle method; made public for unit testing.
     */
    public void activate() {
        tfs = TempFileServiceAccess.getInstance();
        hasValidLocalConfiguration = false;
        hasValidDownloadConfiguration = false;

        final ConfigurationSegment configuration = this.configurationService.getConfigurationSegment(CONFIGURATION_SUBTREE_PATH);
        if (!configuration.isPresentInCurrentConfiguration()) {
            log.debug("No '" + CONFIGURATION_SUBTREE_PATH + "' configuration segment found, disabling instance management");
            return;
        }
        try {
            applyConfiguration(configuration);
            initProfileIdToInstallationMap();
        } catch (IOException e) {
            // do not fail the activate() method; the failure is marked by the "hasValidConfiguration" flag being "false"
            log.error("Error while configuring " + getClass().getSimpleName(), e);
        }

    }

    /**
     * OSGi-DS lifecycle method; made public for unit testing.
     */
    public void deactivate() {
        hasValidLocalConfiguration = false;
        hasValidDownloadConfiguration = false;
    }

    @Override
    public void setupInstallationFromUrlQualifier(String installationId, String urlQualifier, InstallationPolicy installationPolicy,
        TextOutputReceiver userOutputReceiver, final long timeout) throws IOException {
        validateInstallationId(installationId);
        validateConfiguration(true, true);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);
        deploymentOperations.setUserOutputReceiver(userOutputReceiver);

        // Do not try to reinstall running installations
        if (isInstallationRunning(installationId)) {
            throw new IOException("Cannot replace installation " + installationId
                + " because instances are currently running using this installation. Stop the "
                + "instannces or try the \"im reinstall\" command instead.");
        }

        switch (installationPolicy) {
        case IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT:
            installIfVersionIsDifferent(installationId, urlQualifier, userOutputReceiver);
            break;
        case FORCE_NEW_DOWNLOAD_AND_REINSTALL:
            forceDownloadAndReinstall(installationId, urlQualifier, userOutputReceiver);
            break;
        case FORCE_REINSTALL:
            forceReinstall(installationId, urlQualifier, userOutputReceiver);
            break;
        case ONLY_INSTALL_IF_NOT_PRESENT:
            installIfNotPresent(installationId, urlQualifier, userOutputReceiver);
            break;
        default:
            throw new IOException("Not supported yet: " + installationPolicy);
        }
    }

    @Override
    public void reinstallFromUrlQualifier(String installationId, String urlQualifier, InstallationPolicy installationPolicy,
        TextOutputReceiver userOutputReceiver, final long timeout) throws IOException {
        validateInstallationId(installationId);
        validateConfiguration(true, true);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);
        deploymentOperations.setUserOutputReceiver(userOutputReceiver);

        // Get instances running with this installation
        List<String> instancesForInstallation = getInstancesRunningInstallation(installationId);
        
        //Stop running instances
        if (!instancesForInstallation.isEmpty()) {
            stopInstance(instancesForInstallation, userOutputReceiver, timeout);
        }

        //Reinstall
        switch (installationPolicy) {
        case IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT:
            installIfVersionIsDifferent(installationId, urlQualifier, userOutputReceiver);
            break;
        case FORCE_NEW_DOWNLOAD_AND_REINSTALL:
            forceDownloadAndReinstall(installationId, urlQualifier, userOutputReceiver);
            break;
        case FORCE_REINSTALL:
            forceReinstall(installationId, urlQualifier, userOutputReceiver);
            break;
        default:
            throw new IOException("Not supported yet: " + installationPolicy);
        }
        
        //Start instances with new installation
        if (!instancesForInstallation.isEmpty()) {
            startInstance(installationId, instancesForInstallation, userOutputReceiver, timeout, false);
        }
    }

    @Override
    public void configureInstance(String instanceId, ConfigurationChangeSequence changeSequence, TextOutputReceiver userOutputReceiver)
        throws IOException {

        validateConfiguration(true, false);
        final File destinationConfigFile = new File(profilesRootDir + SLASH + instanceId, CONFIGURATION_FILENAME);
        createProfileWithEmptyConfigFileIfNotPresent(destinationConfigFile);

        List<ConfigurationChangeEntry> changeEntries = changeSequence.getAll();
        if (changeEntries.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one configuration step to perform");
        }

        // perform commands "reset" or "apply template" that can only reasonably happen as the first step,
        // and before creating the in-memory configuration modification class
        ConfigurationChangeEntry firstEntry = changeEntries.get(0);
        switch (firstEntry.getFlag()) {
        case RESET_CONFIGURATION:
            // FIXME backup?!
            writeEmptyConfigFile(destinationConfigFile);
            userOutputReceiver.addOutput("Clearing/resetting the configuration" + OF_INSTANCE + instanceId);
            break;
        case APPLY_TEMPLATE:
            // FIXME backup?!
            userOutputReceiver.addOutput("Replacing configuration" + OF_INSTANCE + instanceId + " with template " + firstEntry.getValue());
            File template = resolveAndCheckTemplateDir((String) firstEntry.getValue() + SLASH + CONFIGURATION_FILENAME);
            Path src = template.toPath();
            Files.copy(src, destinationConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            break;
        default:
            // ignore standard commands here
        }

        applyChangeEntries(changeEntries, destinationConfigFile, instanceId, userOutputReceiver);
    }

    private void applyChangeEntries(List<ConfigurationChangeEntry> changeEntries, final File destinationConfigFile, String instanceId,
        TextOutputReceiver userOutputReceiver) throws IOException {
        final InstanceConfigurationImpl configOperations;
        if (CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(destinationConfigFile) == null) {
            configOperations = new InstanceConfigurationImpl(destinationConfigFile);
            CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.put(destinationConfigFile.getName(), configOperations);
        } else {
            configOperations = CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(destinationConfigFile.getName());
        }

        boolean isFirstCommand = true;
        for (ConfigurationChangeEntry entry : changeEntries) {
            switch (entry.getFlag()) {
            case SET_NAME:
                configOperations.setInstanceName((String) entry.getValue());
                userOutputReceiver.addOutput("Setting instance name of instance " + instanceId + " to " + (String) entry.getValue());
                break;
            case DISABLE_RELAY:
                configOperations.setRelayFlag((Boolean) entry.getValue());
                userOutputReceiver.addOutput("Disabling relay flag" + OF_INSTANCE + instanceId);
                break;
            case ENABLE_RELAY:
                configOperations.setRelayFlag((Boolean) entry.getValue());
                userOutputReceiver.addOutput("Enabling relay flag" + OF_INSTANCE + instanceId);
                break;
            case SET_COMMENT:
                configOperations.setInstanceComment((String) entry.getValue());
                userOutputReceiver.addOutput("Setting comment field " + OF_INSTANCE + instanceId + " to " + entry.getValue());
                break;
            case ADD_ALLOWED_IP:
                configOperations.addAllowedIp((String) entry.getValue());
                userOutputReceiver.addOutput("Adding " + (String) entry.getValue() + " to allowed ips was successfull");
                break;
            case ADD_CONNECTION:
                final ConfigurationConnection connectionData = (ConfigurationConnection) entry.getValue();
                configOperations.addConnection(connectionData);
                userOutputReceiver.addOutput("Adding connection " + connectionData.getConnectionName() + " to instance " + instanceId);
                break;
            case ADD_SERVER_PORT:
                addServerPortToConfig(instanceId, userOutputReceiver, SUCCESS, TO_INSTANCE, configOperations, entry);
                break;
            case SET_BACKGROUND_MONITORING:
                @SuppressWarnings("unchecked") Map<String, Integer> map = (HashMap<String, Integer>) entry.getValue();
                configOperations.setBackgroundMonitoring((String) map.keySet().toArray()[0], map.get(map.keySet().toArray()[0]));
                userOutputReceiver.addOutput("Setting background monitoring " + OF_INSTANCE + instanceId);
                break;
            case ADD_SSH_CONNECTION:
                configOperations.addSshConnection((ConfigurationSshConnection) entry.getValue());
                userOutputReceiver.addOutput("Adding ssh connection " + TO_INSTANCE + instanceId);
                break;
            case DISABLE_DEP_INPUT_TAB:
                configOperations.disableDeprecatedInputTab();
                userOutputReceiver.addOutput("Disabling deprecated input tab" + OF_INSTANCE + instanceId);
                break;
            case DISABLE_SSH_SERVER:
                configOperations.disableSshServer();
                userOutputReceiver.addOutput("Disabling ssh server" + OF_INSTANCE + instanceId);
                break;
            case DISABLE_WORKFLOWHOST:
                configOperations.disableWorkflowHost();
                userOutputReceiver.addOutput("Disabling workflow host flag" + OF_INSTANCE + instanceId);
                break;
            case ENABLE_DEP_INPUT_TAB:
                configOperations.enableDeprecatedInputTab();
                userOutputReceiver.addOutput("Enabling deprecated input tab" + OF_INSTANCE + instanceId);
                break;
            case ENABLE_IP_FILTER:
                configOperations.enableIpFilter();
                userOutputReceiver.addOutput("Enabling ip filter" + OF_INSTANCE + instanceId);
                break;
            case DISABLE_IP_FILTER:
                configOperations.disableIpFilter();
                userOutputReceiver.addOutput("Disabling ip filter" + OF_INSTANCE + instanceId);
                break;
            case ENABLE_SSH_SERVER:
                configOperations.enableSshServer();
                userOutputReceiver.addOutput("Enabling ssh server" + OF_INSTANCE + instanceId);
                break;
            case ENABLE_WORKFLOWHOST:
                configOperations.enableWorkflowHost();
                userOutputReceiver.addOutput("Enabling workflow host flag" + OF_INSTANCE + instanceId);
                break;
            case FORWARDING_TIMEOUT:
                configOperations.setForwardingTimeout((Long) entry.getValue());
                userOutputReceiver.addOutput("Setting forwarding timeout" + OF_INSTANCE + instanceId);
                break;
            case PUBLISH_COMPONENT:
                configOperations.publishComponent((String) entry.getValue());
                userOutputReceiver.addOutput("Publishing component" + OF_INSTANCE + instanceId);
                break;
            case REMOVE_ALLOWED_IP:
                configOperations.removeAllowedIp((String) entry.getValue());
                userOutputReceiver.addOutput("Removing allowed ip" + OF_INSTANCE + instanceId);
                break;
            case REMOVE_CONNECTION:
                configOperations.removeConnection((String) entry.getValue());
                userOutputReceiver.addOutput("Removing connection " + (String) entry.getValue() + OF_INSTANCE + instanceId);
                break;
            case REMOVE_SERVER_PORT:
                configOperations.removeServerPort((String) entry.getValue());
                userOutputReceiver.addOutput("Removing server port " + (String) entry.getValue() + OF_INSTANCE + instanceId);
                break;
            case REMOVE_SSH_CONNECTION:
                configOperations.removeSshConnection((String) entry.getValue());
                userOutputReceiver
                    .addOutput("Removing ssh connection " + (String) entry.getValue() + OF_INSTANCE + instanceId);
                break;
            case REQUEST_TIMEOUT:
                configOperations.setRequestTimeout((Long) entry.getValue());
                userOutputReceiver.addOutput("Setting request timeout" + OF_INSTANCE + instanceId);
                break;
            case SET_SSH_SERVER_IP:
                configOperations.setSshServerIP((String) entry.getValue());
                userOutputReceiver.addOutput("Setting ssh server ip" + OF_INSTANCE + instanceId);
                break;
            case SET_SSH_SERVER_PORT:
                configOperations.setSshServerPort((Integer) entry.getValue());
                userOutputReceiver.addOutput("Setting ssh server port" + OF_INSTANCE + instanceId);
                break;
            case TEMP_DIR:
                configOperations.setTempDirectory((String) entry.getValue());
                userOutputReceiver.addOutput("Setting temp directory" + OF_INSTANCE + instanceId);
                break;
            case UNPUBLISH_COMPONENT:
                configOperations.unPublishComponent((String) entry.getValue());
                userOutputReceiver.addOutput("Unpublishing component" + OF_INSTANCE + instanceId);
                break;
            case ENABLE_IM_SSH_ACCESS:
                configOperations.enableImSshAccess(((Integer) entry.getValue()), getHashedPassphrase());
                userOutputReceiver.addOutput("Configuring ssh access for IM on instance" + instanceId);
                break;
            case RESET_CONFIGURATION:
            case APPLY_TEMPLATE:
                // these operations were already performed; only run a consistency check here
                if (!isFirstCommand) {
                    throw new IOException("Resetting the configuration or applying a template "
                        + "must take place *before* applying any other configuration commands"); // TODO better exception type
                }
                break;
            default:
                throw new IOException("Unhandled configuration change request: " + entry.getFlag()); // TODO better exception type
            }
            isFirstCommand = false;
        }
        configOperations.update();
        userOutputReceiver.addOutput("Updated the configuration file" + OF_INSTANCE + instanceId);
    }

    private void createProfileWithEmptyConfigFileIfNotPresent(File config) throws IOException {
        if (!config.exists()) {
            config.getParentFile().mkdir();
            writeEmptyConfigFile(config);
        }
    }

    private void writeEmptyConfigFile(File config) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("{");
            writer.println("}");
        }
    }

    /**
     * The IM master uses the same passphrase for all instances. This method retreives the passphrase from the persistent settings. If no
     * passphrase is stored yet, it is created randomly.
     * 
     * @return the password hash
     */
    private String getHashedPassphrase() {
        String passphrase = persistentSettingsService.readStringValue(InstanceManagementConstants.IM_MASTER_PASSPHRASE_KEY);
        if (passphrase == null) {
            passphrase = RandomStringUtils.randomAlphanumeric(10);
            persistentSettingsService.saveStringValue(InstanceManagementConstants.IM_MASTER_PASSPHRASE_KEY, passphrase);
        }
        return BCrypt.hashpw(passphrase, BCrypt.gensalt(10));
    }

    private void addServerPortToConfig(String instanceId, TextOutputReceiver userOutputReceiver, final String success,
        final String toInstance, InstanceConfigurationImpl configOperations, ConfigurationChangeEntry key) throws IOException {
        // explicit type cast as we can't guarantee the correct type of the list objects itself with generics.
        Object o = key.getValue();
        if (o instanceof List) {
            @SuppressWarnings("unchecked") List<String> list = (List<String>) o;
            if (list.size() > 3) {
                throw new IOException("Wrong number of parameters.");
            }
            String name = list.get(0);
            String ip = list.get(1);
            int port = Integer.parseInt(list.get(2));

            configOperations.addServerPort((String) name, (String) ip, (int) port);
            userOutputReceiver.addOutput("Adding server port " + ((String) name) + toInstance + instanceId + success);
        } else {
            throw new IOException("Failure.");
        }
    }

    @Override
    public void startInstance(String installationId, List<String> instanceIdList,
        TextOutputReceiver userOutputReceiver, final long timeout, boolean startWithGui) throws IOException {
        // TODO add some user output instead of simply return.
        if (installationId == null || instanceIdList == null || installationId.isEmpty() || instanceIdList.isEmpty()) {
            throw new IOException("Malformed command: either no installation id or instance id defined.");
        }
        File possibleInstallation = new File(installationsRootDir + SLASH + installationId);
        if (!possibleInstallation.exists()) {
            throw new IOException("Installation with id: " + installationId + " does not exist.");
        }
        validateInstallationId(installationId);
        validateInstanceId(instanceIdList, false);
        validateConfiguration(true, false);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);
        final File installationDir = resolveAndCheckInstallationDir(installationId);

        for (String s : new ArrayList<String>(instanceIdList)) {
            if (isInstanceRunning(s)) {
                instanceIdList.remove(s);
                userOutputReceiver.addOutput("Profile with id: " + s + " is already in use.");
            }
        }
        List<File> profileDirList = resolveAndCheckProfileDirList(instanceIdList);
        writeInstallationIdToFile(installationId, profileDirList, userOutputReceiver);
        try {
            instanceOperations.startInstanceUsingInstallation(profileDirList, installationDir, timeout, userOutputReceiver, startWithGui);
        } catch (InstanceOperationException e) {
            if (e.getMessage().contains("Timeout reached")) {
                List<String> names = new ArrayList<String>();
                for (File profile : e.getFailedInstances()) {
                    names.add(profile.getName());
                }
                userOutputReceiver.addOutput("Timeout reached... Aborting startup process of failed instances: " + names);
                stopInstance(names, userOutputReceiver, 0);
            } else {
                throw new IOException(e);
            }
        }

        addInstanceToMap(instanceIdList, installationId);
    }

    @Override
    public void stopInstance(List<String> instanceIdList, TextOutputReceiver userOutputReceiver, final long timeout) throws IOException {
        // TODO add some user output instead of simply return.
        if (instanceIdList == null || instanceIdList.isEmpty()) {
            userOutputReceiver.addOutput("No instance to stop defined.. aborting.");
            return;
        }
        validateInstanceId(instanceIdList, true);
        validateConfiguration(true, false);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);
        final List<File> profileDirList = resolveAndCheckProfileDirList(instanceIdList);

        try {
            instanceOperations.shutdownInstance(profileDirList, timeout, userOutputReceiver);
        } catch (IOException e) {
            checkAndRemoveInstanceLock(profileDirList, instanceIdList, e);
        } finally {
            removeInstanceTopMap(instanceIdList);
        }
    }

    private void checkAndRemoveInstanceLock(List<File> profileDirList, List<String> instanceIdList, IOException e) throws IOException {
        for (File profile : new ArrayList<>(profileDirList)) {
            if (!isInstanceRunning(profile.getName())) {
                profileDirList.remove(profile);
                if (profile.isDirectory()) {
                    for (File file : profile.listFiles()) {
                        if (file.getName().equals(BootstrapConfiguration.PROFILE_DIR_LOCK_FILE_NAME)) {
                            boolean success = file.delete();
                            if (e != null && !success) {
                                throw new IOException("Failed to delete instance.lock file.", e);
                            } else if (!success) {
                                throw new IOException("Failed to delete instance.lock file.");
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isInstanceRunning(String instanceId) throws IOException {
        validateConfiguration(true, false);
        final File profileDir = resolveAndCheckProfileDir(instanceId);
        return instanceOperations.isProfileLocked(profileDir);
    }

    /**
     * returns Checks if an instance is currently running with the given installation.
     * 
     * @param installationId
     * @return true iff an instance is currently running with the given installation
     * @throws IOException
     */
    private boolean isInstallationRunning(String installationId) throws IOException {
        for (Map.Entry<String, String> entry : profileIdToInstallationIdMap.entrySet()) {
            if (entry.getValue().equals(installationId)) {
                if (isInstanceRunning(entry.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns all instances currently running with the given installation ID.
     * 
     * @param installationId
     * @return
     * @throws IOException
     */
    private List<String> getInstancesRunningInstallation(String installationId) throws IOException {
        List<String> instances = new ArrayList<String>();
        for (Map.Entry<String, String> entry : profileIdToInstallationIdMap.entrySet()) {
            if (entry.getValue().equals(installationId)) {
                if (isInstanceRunning(entry.getKey())) {
                    instances.add(entry.getKey());
                }
            }
        }
        return instances;
    }

    protected void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
    }

    protected void bindPersistentSettingsService(PersistentSettingsService newService) {
        this.persistentSettingsService = newService;
    }

    @Override
    public void listInstanceManagementInformation(String scope, TextOutputReceiver userOutputReceiver) throws IOException {
        if ("all".equals(scope)) {
            listInstances(userOutputReceiver);
            listInstallations(userOutputReceiver);
            listTemplates(userOutputReceiver);
        } else if ("instances".equals(scope)) {
            listInstances(userOutputReceiver);
        } else if ("installations".equals(scope)) {
            listInstallations(userOutputReceiver);
        } else if ("templates".equals(scope)) {
            listTemplates(userOutputReceiver);
        }
    }

    @Override
    public void disposeInstance(String instanceId, TextOutputReceiver outputReceiver) throws IOException {
        if (profilesRootDir == null) {
            throw new IOException("Failed to dispose instance. Instances' root directory not defined.");
        }
        for (File instanceFile : profilesRootDir.listFiles()) {
            if (instanceFile.isDirectory() && instanceId.equals(instanceFile.getName())) {
                for (File fileInInstanceFolder : instanceFile.listFiles()) {
                    if (fileInInstanceFolder.getName().equals(BootstrapConfiguration.PROFILE_DIR_LOCK_FILE_NAME)) {
                        throw new IOException("Instance with ID " + instanceId + " currently in use. "
                            + "To stop it use 'im stop " + instanceId + "'.");
                    }
                }
                FileSystemOperations.deleteSandboxDirectory(instanceFile);
                return;
            }
        }
        throw new IOException("Instance with ID " + instanceId + " not found.");
    }

    @Override
    public void showInstanceManagementInformation(TextOutputReceiver outputReceiver) {
        if (profilesRootDir != null) {
            outputReceiver.addOutput("Instances' root directory: " + profilesRootDir.getAbsolutePath());
        } else {
            outputReceiver.addOutput("Instances' root directory not defined.");
        }
        if (installationsRootDir != null) {
            outputReceiver.addOutput("Installations' root directory: " + installationsRootDir.getAbsolutePath());
        } else {
            outputReceiver.addOutput("Installations' root directory not defined.");
        }
        if (templatesRootDir != null) {
            outputReceiver.addOutput("Templates' root directory: " + templatesRootDir.getAbsolutePath());
        } else {
            outputReceiver.addOutput("Templates' root directory not defined.");
        }
        if (dataRootDir != null) {
            outputReceiver.addOutput("Data root directory: " + dataRootDir.getAbsolutePath());
        } else {
            outputReceiver.addOutput("Data root directory not defined.");
        }
        if (downloadsCacheDir != null) {
            outputReceiver.addOutput("Download cache directory: " + downloadsCacheDir.getAbsolutePath());
            if (downloadsCacheDir.listFiles().length > 0) {
                outputReceiver.addOutput("Downloads cached: ");
                for (File cachedDownloadFile : downloadsCacheDir.listFiles()) {
                    outputReceiver.addOutput(INDENT + cachedDownloadFile.getName().replace(ZIP, ""));
                }
            } else {
                outputReceiver.addOutput("No download cached.");
            }
        } else {
            outputReceiver.addOutput("Download cache directory not defined.");
        }
    }

    @Override
    public void startAllInstances(String installationId, TextOutputReceiver userOutputReceiver, final long timeout) throws IOException {
        List<String> instanceIdList = new ArrayList<>();
        addProfiles(profilesRootDir.toPath(), instanceIdList);
        startInstance(installationId, instanceIdList, userOutputReceiver, timeout, false);
    }

    @Override
    public void stopAllInstances(String installationId, TextOutputReceiver userOutputReceiver, final long timeout) throws IOException {
        List<String> instanceIdList = new ArrayList<>();
        if (!installationId.isEmpty()) {
            synchronized (profileIdToInstallationIdMap) {
                for (Map.Entry<String, String> entry : profileIdToInstallationIdMap.entrySet()) {
                    if (entry.getValue().equals(installationId)) {
                        instanceIdList.add(entry.getKey());
                    }
                }
            }
        } else {
            addProfiles(profilesRootDir.toPath(), instanceIdList);
        }
        stopInstance(instanceIdList, userOutputReceiver, timeout);
    }

    /**
     * 
     * Intended for unit tests.
     * 
     * @return the profile root dir.
     */
    protected File getProfilesRootDir() {
        return profilesRootDir;
    }

    /**
     * 
     * Intended for unit tests.
     * 
     */
    protected void setProfilesRootDir(File file) {
        profilesRootDir = file;
    }

    /**
     * 
     * Intended for unit tests.
     * 
     */
    protected void validateLocalConfig() {
        hasValidLocalConfiguration = true;
    }

    protected String fetchVersionInformationFromDownloadSourceFolder(String urlQualifier) throws IOException {
        validateConfiguration(false, true);
        File tempVersionFile = tfs.createTempFileFromPattern("versionfile-*.tmp");
        String versionFileUrl = downloadSourceFolderUrlPattern.replace(GENERIC_PLACEHOLDER_STRING, urlQualifier) + "VERSION";
        log.debug("Fetching remote version information from " + versionFileUrl);
        deploymentOperations.downloadFile(versionFileUrl, tempVersionFile, true, false); // allow overwrite as temp file already exists
        return FileUtils.readFileToString(tempVersionFile).trim();
    }

    protected void downloadInstallationPackage(String urlQualifier, String version, File localFile) throws IOException {
        validateConfiguration(false, true);
        String remoteFileUrl =
            downloadSourceFolderUrlPattern.replace(GENERIC_PLACEHOLDER_STRING, urlQualifier)
                + downloadFilenamePattern.replace(GENERIC_PLACEHOLDER_STRING, version);
        log.debug("Downloading installation package from '" + remoteFileUrl + "' to local file '" + localFile.getAbsolutePath() + "'");
        deploymentOperations.downloadFile(remoteFileUrl, localFile, true, true); // allow overwrite
    }

    private void applyConfiguration(final ConfigurationSegment configuration) throws IOException {
        try {
            hasValidLocalConfiguration = false;

            this.dataRootDir = getConfiguredDirectory(configuration, DATA_ROOT_DIRECTORY_PROPERTY);
            this.installationsRootDir = getConfiguredDirectory(configuration, INSTALLATIONS_ROOT_DIR_PROPERTY);

            if (dataRootDir == null || installationsRootDir == null) {
                throw new IOException("Data or installation root directory (or both) unspecified");
            }
            this.templatesRootDir = new File(dataRootDir, TEMPLATES);
            this.profilesRootDir = new File(dataRootDir, "profiles");
            this.downloadsCacheDir = new File(dataRootDir, "downloads");

            // TODO add new combinations
            if (installationsRootDir.equals(templatesRootDir) || installationsRootDir.equals(profilesRootDir)
                || templatesRootDir.equals(profilesRootDir)) {
                throw new IOException("Two or more configured directory are equal, but they must be unique");
            }

            // TODO run this check on Windows only?
            if (installationsRootDir.getPath().length() > MAX_INSTALLATION_ROOT_PATH_LENGTH) {
                throw new IOException("Installation root path is too long: " + installationsRootDir.getPath());
            }

            prepareAndValidateDirectory(DATA_ROOT_DIRECTORY_PROPERTY, dataRootDir);
            prepareAndValidateDirectory(INSTALLATIONS_ROOT_DIR_PROPERTY, installationsRootDir);
            prepareAndValidateDirectory(TEMPLATES, templatesRootDir);
            prepareAndValidateDirectory("profiles", profilesRootDir);
            prepareAndValidateDirectory("downloads", downloadsCacheDir);

            hasValidLocalConfiguration = true;
        } catch (IOException e) {
            log.info("Disabling local instance management due to missing or invalid configuration: " + e.getMessage());
        }

        // note: these settings use an empty string as "undefined" markers
        this.downloadSourceFolderUrlPattern = configuration.getString("downloadSourceFolderUrlPattern", "");
        // normalize URL pattern to end with "/"
        if (downloadSourceFolderUrlPattern.length() > 0 && !downloadSourceFolderUrlPattern.endsWith(SLASH)) {
            downloadSourceFolderUrlPattern = downloadSourceFolderUrlPattern + SLASH;
        }
        this.downloadFilenamePattern = configuration.getString("downloadFilenamePattern", "");

        try {
            hasValidDownloadConfiguration = false;
            if (downloadSourceFolderUrlPattern.isEmpty()) {
                throw new IOException("Parameter 'downloadSourceFolderUrlPattern' has not been defined, but is required");
            }
            if (downloadFilenamePattern.isEmpty()) {
                throw new IOException("Parameter 'downloadFilenamePattern' has not been defined, but is required");
            }
            hasValidDownloadConfiguration = true;
        } catch (IOException e) {
            log.error("Error in instance management download configuration: " + e.getMessage());
        }
    }

    private File forceFetchingProductZip(String urlQualifier, String remoteVersion) throws IOException {
        File downloadFile = new File(downloadsCacheDir, remoteVersion + ZIP);
        int i = 1;
        while (downloadFile.exists()) {
            String newFilePath =
                downloadFile.getAbsolutePath().replace(downloadFile.getName(), "") + remoteVersion + "(" + (i++) + ")" + ZIP;
            downloadFile = new File(newFilePath);
        }

        downloadInstallationPackage(urlQualifier, remoteVersion, downloadFile);
        return downloadFile;
    }

    private String fetchVersionInformation(TextOutputReceiver userOutputReceiver, String urlQualifier) throws IOException {
        // get remote version
        userOutputReceiver.addOutput("Fetching remote version information");
        return fetchVersionInformationFromDownloadSourceFolder(urlQualifier);
    }

    private File fetchProductZipIfNecessary(String urlQualifier, String remoteVersion) throws IOException {
        File downloadFile = new File(downloadsCacheDir, remoteVersion + ZIP);
        if (downloadFile.exists()) {
            log.info("Version " + remoteVersion + " is already present in downloads cache, not downloading");
        } else {
            downloadInstallationPackage(urlQualifier, remoteVersion, downloadFile);
        }
        return downloadFile;
    }

    private void forceDownloadAndReinstall(String installationId, String urlQualifier, TextOutputReceiver userOutputReceiver)
        throws IOException {
        String version = fetchVersionInformation(userOutputReceiver, urlQualifier);
        reinstall(installationId, version, urlQualifier, userOutputReceiver, true);
    }

    private void forceReinstall(String installationId, String urlQualifier, TextOutputReceiver userOutputReceiver) throws IOException {
        String version = fetchVersionInformation(userOutputReceiver, urlQualifier);
        // TODO validate remote version: not empty, plausible major version
        log.info("Identified remote version: " + version);
        reinstall(installationId, version, urlQualifier, userOutputReceiver, false);
    }

    private void installIfNotPresent(String installationId, String urlQualifier, TextOutputReceiver userOutputReceiver) throws IOException {
        File installationDir = new File(installationsRootDir, installationId);
        if (installationDir.exists()) {
            userOutputReceiver.addOutput("Installation with ID " + installationId + " already exists.");
        } else {
            installIfVersionIsDifferent(installationId, urlQualifier, userOutputReceiver);
        }
    }

    private void installIfVersionIsDifferent(String installationId, String urlQualifier, TextOutputReceiver userOutputReceiver)
        throws IOException {

        String newVersion = fetchVersionInformation(userOutputReceiver, urlQualifier);
        // TODO validate remote version: not empty, plausible major version
        log.info("Identified remote version: " + newVersion);

        // load local installed version information
        String oldVersion = getVersionOfInstallation(installationId);

        if (newVersion.isEmpty()) {
            throw new IOException("Unable to find new version");
        }

        if (newVersion.equals(oldVersion)) {
            userOutputReceiver.addOutput("Remote and installed version are the same; no change required");
            return;
        }

        reinstall(installationId, newVersion, urlQualifier, userOutputReceiver, false);
    }

    private void reinstall(String installationId, String version, String urlQualifier,
        TextOutputReceiver userOutputReceiver, boolean force) throws IOException {

        File zipFile;
        if (force) {
            log.info("Forces new download");
            zipFile = forceFetchingProductZip(urlQualifier, version);
        } else {
            // download installation package if not already present
            zipFile = fetchProductZipIfNecessary(urlQualifier, version);
        }

        File installationDir = new File(installationsRootDir, installationId);
        if (installationDir.exists()) {
            userOutputReceiver.addOutput("Deleting old installation " + installationId);
            deploymentOperations.deleteInstallation(installationDir);
        }
        userOutputReceiver.addOutput("Setting up new installation " + installationId);
        deploymentOperations.installFromProductZip(zipFile, installationDir);

        log.debug("Writing version information for installation " + installationId);
        storeVersionOfInstallation(installationId, version);
    }

    private void listInstances(TextOutputReceiver userOutputReceiver) {
        if (profilesRootDir == null) {
            userOutputReceiver.addOutput("Instances' root directory not defined.");
            return;
        }
        if (profilesRootDir.listFiles().length == 0) {
            userOutputReceiver.addOutput("No instances found.");
            return;
        }
        userOutputReceiver.addOutput("Instances: ");
        for (File instanceFile : profilesRootDir.listFiles()) {
            if (instanceFile.isDirectory()) {
                String runningState = "Not running";
                for (File fileInProfile : instanceFile.listFiles()) {
                    synchronized (profileIdToInstallationIdMap) {
                        if (fileInProfile.isFile() && fileInProfile.getName().equals(BootstrapConfiguration.PROFILE_DIR_LOCK_FILE_NAME)) {
                            runningState = "Running (" + profileIdToInstallationIdMap.get(instanceFile.getName()) + ")";

                        }
                    }
                }
                userOutputReceiver.addOutput(INDENT + instanceFile.getName() + " (" + runningState + ")");
            }
        }
    }

    private void listInstallations(TextOutputReceiver userOutputReceiver) throws IOException {
        if (installationsRootDir == null) {
            userOutputReceiver.addOutput("Installations' root directory not defined.");
            return;
        }
        if (installationsRootDir.listFiles().length == 0) {
            userOutputReceiver.addOutput("No installations found.");
            return;
        }
        userOutputReceiver.addOutput("Installations: ");
        for (File installationFile : installationsRootDir.listFiles()) {
            if (installationFile.isFile() && installationFile.getName().endsWith(VERSION)) {
                String installationsId = installationFile.getName().replace(VERSION, "");
                String version = FileUtils.readFileToString(installationFile);
                userOutputReceiver.addOutput(INDENT + installationsId + " (" + version + ")");
            }
        }
    }

    private void listTemplates(TextOutputReceiver userOutputReceiver) {
        if (templatesRootDir == null) {
            userOutputReceiver.addOutput("Templates' root directory not defined.");
            return;
        }
        if (templatesRootDir.listFiles().length == 0) {
            userOutputReceiver.addOutput("No templates found.");
            return;
        }
        userOutputReceiver.addOutput("Templates: ");
        for (File templateFile : templatesRootDir.listFiles()) {
            if (templateFile.isDirectory()) {
                userOutputReceiver.addOutput(INDENT + templateFile.getName());
            }
        }
    }

    private String getVersionOfInstallation(String installationId) throws IOException {
        String oldVersion;
        File installationVersionFile = new File(installationsRootDir, installationId + VERSION);
        if (installationVersionFile.exists()) {
            oldVersion = FileUtils.readFileToString(installationVersionFile);
        } else {
            oldVersion = ""; // simpler to handle than null
        }
        return oldVersion;
    }

    private void storeVersionOfInstallation(String installationId, String versionString) throws IOException {
        File installationVersionFile = new File(installationsRootDir, installationId + VERSION);
        FileUtils.writeStringToFile(installationVersionFile, versionString);
    }

    private List<File> resolveAndCheckProfileDirList(List<String> instanceIdList) throws IOException {
        final List<File> dirList = new ArrayList<>();
        for (String instanceId : instanceIdList) {
            dirList.add(resolveAndCheckProfileDir(instanceId));
        }

        return dirList;
    }

    private File resolveAndCheckInstallationDir(String installationId) throws IOException {
        final File installationDir = new File(installationsRootDir, installationId);
        if (installationDir.exists()) {
            prepareAndValidateDirectory("installation " + installationId, installationDir);
        } else {
            throw new IOException("The installation directory " + installationId + " does not exist.");
        }

        return installationDir;
    }

    private File resolveAndCheckProfileDir(String instanceId) throws IOException {
        final File profileDir = new File(profilesRootDir, instanceId);
        prepareAndValidateDirectory("instance " + instanceId, profileDir);
        return profileDir;
    }

    private File resolveAndCheckTemplateDir(String templateId) throws IOException {
        final File templateDir = new File(templatesRootDir, templateId);
        // prepareAndValidateDirectory("template " + templateId, templateDir);
        return templateDir;
    }

    private void prepareAndValidateDirectory(String id, File dir) throws IOException {
        dir.mkdirs();
        String absolutePath = dir.getAbsolutePath();
        if (!dir.isDirectory()) {
            throw new IOException("The configured path '" + id + "' ('" + absolutePath + "') could not be created");
        }
        // TODO improve and document validation
        if (absolutePath.contains("\"")) {
            throw new IOException("The directory path '" + absolutePath + "' contains illegal characters");
        }
        log.debug("Final path for id '" + id + "' " + absolutePath);
    }

    private TextOutputReceiver ensureUserOutputReceiverDefined(TextOutputReceiver userOutputReceiver) {
        if (userOutputReceiver != null) {
            return userOutputReceiver;
        } else {
            return fallbackUserOutputReceiver;
        }
    }

    private void validateConfiguration(boolean validateLocalConfig, boolean validateDownloadConfig) throws IOException {
        if (validateLocalConfig && !hasValidLocalConfiguration) {
            throw new IOException(
                "The instance management service is disabled or has no valid local configuration - "
                    + "cannot execute the requested operation");
        }
        if (validateDownloadConfig && !hasValidDownloadConfiguration) {
            throw new IOException(
                "The instance management service is disabled or has no valid download configuration - "
                    + "cannot execute the requested operation");
        }
    }

    private <T> boolean isUnique(Collection<T> idList) {
        Set<T> duplicateIdList = new LinkedHashSet<>();
        Set<T> uniqueIdList = new HashSet<T>();
        for (T t : idList) {
            if (!uniqueIdList.add(t)) {
                duplicateIdList.add(t);
            }
        }
        return duplicateIdList.isEmpty();
    }

    private boolean isIdValid(String id) {
        // TODO add reasonable maximum length check?
        if (!VALID_IDS_REGEXP_PATTERN.matcher(id).matches()) {
            return false;
        }
        return true;
    }

    private boolean isProfilePresent(String id) {
        File possibleProfile = new File(profilesRootDir + SLASH + id);
        return possibleProfile.exists();
    }

    private void validateInstallationId(String id) throws IOException {
        if (!isIdValid(id)) {
            // note: this assumes that even malformed ids are safe to print, as it should only affect the user
            // that issued the command anyway
            throw new IOException("Malformed id: " + id);
        }
    }

    private void validateInstanceId(List<String> idList, boolean forShutdown) throws IOException {
        if (!isUnique(idList)) {
            throw new IOException("Malformed command: multiple instances with identical id");
        }
        for (String id : idList) {
            if (!isIdValid(id)) {
                // note: this assumes that even malformed ids are safe to print, as it should only affect the user
                // that issued the command anyway
                throw new IOException("Malformed id: " + id);
            }
            if (forShutdown && !isProfilePresent(id)) {
                throw new IOException("Malformed command: tried to shutdown instance, which doesn't exist");
            }
        }
    }

    private void initProfileIdToInstallationMap() throws IOException {
        for (File instanceFile : profilesRootDir.listFiles()) {
            if (instanceFile.isDirectory()) {
                for (File fileInProfile : instanceFile.listFiles()) {
                    if (fileInProfile.getName().equals("installation")) {
                        try (BufferedReader br = new BufferedReader(new FileReader(fileInProfile))) {
                            synchronized (profileIdToInstallationIdMap) {
                                profileIdToInstallationIdMap.put(instanceFile.getName(), br.readLine());
                            }
                        }
                    }
                }
            }
        }
    }

    private void addInstanceToMap(List<String> instanceIds, String installationId) {
        for (String instanceId : instanceIds) {
            synchronized (profileIdToInstallationIdMap) {
                profileIdToInstallationIdMap.put(instanceId, installationId);
            }
        }
    }

    private void removeInstanceTopMap(List<String> instanceIds) throws IOException {
        for (Iterator<String> iterator = instanceIds.iterator(); iterator.hasNext();) {
            synchronized (profileIdToInstallationIdMap) {
                if (profileIdToInstallationIdMap.remove(iterator.next()) == null) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 
     * Adds all existing profiles to a given {@link Collection}. Note: this isn't a recursive search. There won't be a tree structure
     * containing all children as it is not needed in this particular case.
     * 
     * @param directory
     * @param running
     * @return
     * @throws IOException
     */
    private void addProfiles(Path directory, Collection<String> all) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
            for (Path child : ds) {
                all.add(child.getFileName().toString());
            }
        }
    }

    private File getConfiguredDirectory(final ConfigurationSegment configuration, String id) throws IOException {
        String configuredPath = configuration.getString(id);
        log.debug("Configuration value for property '" + id + "': " + configuredPath);
        if (configuredPath != null) {
            return new File(configuredPath).getAbsoluteFile();
        } else {
            return null;
        }
    }

    private void writeInstallationIdToFile(String installationId, final List<File> profileDirList, TextOutputReceiver userOutputReceiver)
        throws IOException,
        UnsupportedEncodingException, FileNotFoundException {
        for (File profile : profileDirList) {
            try (Writer writer =
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(profile.getAbsolutePath() + "/" + "installation"),
                    "utf-8"))) {
                writer.write(installationId);
            }
        }
    }

    @Override
    public void executeCommandOnInstance(String instanceId, String command, TextOutputReceiver userOutputReceiver) throws JSchException,
        SshParameterException, IOException, InterruptedException {
        if (isInstanceRunning(instanceId)) {
            Logger logger = JschSessionFactory.createDelegateLogger(log);
            Integer port = getSshPortForInstance(instanceId);
            String ip = getSshIpForInstance(instanceId);
            String passphrase = persistentSettingsService.readStringValue(InstanceManagementConstants.IM_MASTER_PASSPHRASE_KEY);
            if (passphrase != null && port != null && ip != null) {
                Session session =
                    JschSessionFactory.setupSession(ip, port,
                        InstanceManagementConstants.IM_MASTER_USER_NAME,
                        null, passphrase, logger);
                JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
                rceExecutor.start(command);
                try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
                    TextStreamWatcherFactory.create(stdoutStream, userOutputReceiver).start();
                    TextStreamWatcherFactory.create(stderrStream, userOutputReceiver).start();
                    rceExecutor.waitForTermination();
                }
                session.disconnect();
                userOutputReceiver.addOutput("Finished executing command " + command + " on instance " + instanceId);
            } else {
                userOutputReceiver.addOutput("Could not retrieve password and/or port for instance " + instanceId + ".");
            }
        } else {
            userOutputReceiver.addOutput("Cannot execute command on instance " + instanceId + " because it is not running.");
        }
    }

    /**
     * Retrieve SSH port from configuration.
     * 
     * @return the port
     * @throws IOException
     */
    private Integer getSshPortForInstance(String instanceId) throws IOException {
        File config = new File(new File(profilesRootDir, instanceId), CONFIGURATION_FILENAME);
        if (!config.exists()) {
            log.warn("No config file for instance " + instanceId + " exists.");
            return null;
        }
        InstanceConfigurationImpl configOperations;
        if (CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(config) == null) {
            configOperations = new InstanceConfigurationImpl(config);
        } else {
            configOperations = CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(config.getName());
        }
        return configOperations.getSshServerPort();
    }

    /**
     * Retrieve SSH ip from configuration.
     * 
     * @return the port
     * @throws IOException
     */
    private String getSshIpForInstance(String instanceId) throws IOException {
        File config = new File(new File(profilesRootDir, instanceId), CONFIGURATION_FILENAME);
        if (!config.exists()) {
            log.warn("No config file for instance " + instanceId + " exists.");
            return null;
        }
        InstanceConfigurationImpl configOperations;
        if (CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(config) == null) {
            configOperations = new InstanceConfigurationImpl(config);
        } else {
            configOperations = CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(config.getName());
        }
        return configOperations.getSshServerIp();
    }

}
