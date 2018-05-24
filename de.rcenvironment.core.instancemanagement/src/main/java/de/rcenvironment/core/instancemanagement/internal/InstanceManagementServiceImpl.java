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
import java.net.URLDecoder;
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
import java.util.Objects;
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
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.instancemanagement.InstanceConfigurationOperationSequence;
import de.rcenvironment.core.instancemanagement.InstanceManagementConstants;
import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.toolkitbridge.transitional.TextStreamWatcherFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
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
 * @author Brigitte Boden
 */
public class InstanceManagementServiceImpl implements InstanceManagementService {

    private static final String INSTANCE_MANAGEMENT_DISABLED =
        "Local instance management is disabled due to missing or invalid configuration: ";

    private static final String ON_INSTANCE = " on instance ";

    private static final String ZIP = ".zip";

    private static final String SLASH = "/";

    private static final String TO = " to ";

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

    private volatile boolean instanceManagementStarted = false;

    private String reasonInstanceManagementNotStarted = "";

    private ConcurrentHashMap<String, String> profileIdToInstallationIdMap = new ConcurrentHashMap<>();

    private ConfigurationService configurationService;

    private PersistentSettingsService persistentSettingsService;

    // split into distinct classes to allow separate testing
    private final InstanceOperationsImpl instanceOperations = new InstanceOperationsImpl();

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
            reasonInstanceManagementNotStarted =
                "No '" + CONFIGURATION_SUBTREE_PATH + "' configuration segment found in configuration.json";
            return;
        }
        try {
            applyConfiguration(configuration);
            if (hasValidLocalConfiguration) {
                initProfileIdToInstallationMap();
                instanceManagementStarted = true;
            }
        } catch (IOException e) {
            // do not fail the activate() method; the failure is marked by the "hasValidConfiguration" flag being "false"
            log.error("Error while configuring " + getClass().getSimpleName(), e);
            reasonInstanceManagementNotStarted =
                INSTANCE_MANAGEMENT_DISABLED + e.getMessage();
        }

    }

    public boolean isInstanceManagementStarted() {
        return instanceManagementStarted;
    }

    public String getReasonInstanceManagementNotStarted() {
        return reasonInstanceManagementNotStarted;
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

        // Stop running instances
        if (!instancesForInstallation.isEmpty()) {
            stopInstance(instancesForInstallation, userOutputReceiver, timeout);
        }

        // Reinstall
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

        // Start instances with new installation
        if (!instancesForInstallation.isEmpty()) {
            startInstance(installationId, instancesForInstallation, userOutputReceiver, timeout, false);
        }
    }

    @Override
    public boolean isSpecialInstallationId(String input) {
        return MASTER_INSTANCE_SYMBOLIC_INSTALLATION_ID.equals(input)
            || input.startsWith(CUSTOM_LOCAL_INSTALLATION_PATH_INSTALLATION_ID_PREFIX);
    }

    @Override
    public InstanceConfigurationOperationSequence newConfigurationOperationSequence() {
        return new InstanceConfigurationOperationSequenceImpl();
    }

    @Override
    public void applyInstanceConfigurationOperations(String instanceId, InstanceConfigurationOperationSequence changeSequence,
        TextOutputReceiver userOutputReceiver) throws InstanceConfigurationException, IOException {

        validateConfiguration(true, false);
        final File destinationConfigFile = resolveRelativePathWithinProfileDirectory(instanceId, CONFIGURATION_FILENAME);
        createProfileWithEmptyConfigFileIfNotPresent(destinationConfigFile);

        List<InstanceConfigurationOperationDescriptor> changeEntries =
            ((InstanceConfigurationOperationSequenceImpl) changeSequence).getConfigurationSteps();
        if (changeEntries.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one configuration step to perform");
        }

        // perform commands "reset" or "apply template" that can only reasonably happen as the first step,
        // and before creating the in-memory configuration modification class
        InstanceConfigurationOperationDescriptor firstEntry = changeEntries.get(0);
        switch (firstEntry.getFlag()) {
        case InstanceManagementConstants.SUBCOMMAND_RESET:
            // FIXME backup?!
            writeEmptyConfigFile(destinationConfigFile);
            userOutputReceiver.addOutput("Clearing/resetting the configuration" + OF_INSTANCE + instanceId);
            break;
        case InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE:
            // FIXME backup?!
            userOutputReceiver
                .addOutput("Replacing configuration" + OF_INSTANCE + instanceId + " with template " + firstEntry.getSingleParameter());
            File template = resolveAndCheckTemplateDir(firstEntry.getSingleParameter() + SLASH + CONFIGURATION_FILENAME);
            Path src = template.toPath();
            Files.copy(src, destinationConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            break;
        default:
            // ignore standard commands here
        }

        applyChangeEntries(changeEntries, destinationConfigFile, instanceId, userOutputReceiver);
    }

    // TODO change exceptions types
    private void applyChangeEntries(List<InstanceConfigurationOperationDescriptor> changeEntries, final File destinationConfigFile,
        String instanceId, TextOutputReceiver userOutputReceiver) throws InstanceConfigurationException {
        final InstanceConfigurationImpl configOperations;
        if (CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(destinationConfigFile) == null) {
            configOperations = new InstanceConfigurationImpl(destinationConfigFile);
            CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.put(destinationConfigFile.getName(), configOperations);
        } else {
            configOperations = CONFIG_FILE_NAME_TO_CONFIG_STORE_MAP.get(destinationConfigFile.getName());
        }

        boolean isFirstCommand = true;
        for (InstanceConfigurationOperationDescriptor entry : changeEntries) {

            final Object[] parameters = entry.getParameters();

            switch (entry.getFlag()) {
            case InstanceManagementConstants.SUBCOMMAND_SET_NAME:
                configOperations.setInstanceName((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Setting instance name of instance " + instanceId + TO + entry.getSingleParameter());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION:
                if (!Boolean.TRUE.equals(entry.getSingleParameter()) && !Boolean.FALSE.equals(entry.getSingleParameter())) {
                    throw new InstanceConfigurationException(
                        "The parameter of the " + InstanceManagementConstants.SUBCOMMAND_SET_RELAY_OPTION
                            + " sub-command must be a boolean value, but is a " + entry.getSingleParameter().getClass());
                }
                configOperations.setRelayFlag((Boolean) entry.getSingleParameter());
                userOutputReceiver.addOutput("The relay flag" + OF_INSTANCE + instanceId + " is set to " + entry.getSingleParameter());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_COMMENT:
                String entryString = (String) entry.getSingleParameter();
                configOperations.setInstanceComment(entryString);
                userOutputReceiver.addOutput("Setting comment field " + OF_INSTANCE + instanceId + TO + entryString);
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_CONNECTION:
                final ConfigurationConnection connectionData = (ConfigurationConnection) entry.getSingleParameter();
                configOperations.addConnection(connectionData);
                userOutputReceiver.addOutput("Adding connection " + connectionData.getConnectionName() + " to instance " + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_CONNECTION:
                configOperations.removeConnection((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Removing connection " + entry.getSingleParameter() + " from instance " + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_SERVER_PORT:
                if (parameters.length != 3) {
                    throw new IllegalArgumentException("Wrong number of parameters.");
                }
                final String serverPortName = (String) parameters[0];
                final String serverPortIp = (String) parameters[1];
                final Integer serverPortNumber = (Integer) parameters[2];
                configOperations.addServerPort(serverPortName, serverPortIp, serverPortNumber);
                userOutputReceiver.addOutput("Adding server port " + serverPortName + TO_INSTANCE + instanceId + SUCCESS);
                break;
            case InstanceManagementConstants.SUBCOMMAND_DISABLE_SSH_SERVER:
                configOperations.disableSshServer();
                userOutputReceiver.addOutput("Disabling ssh server" + OF_INSTANCE + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_WORKFLOW_HOST_OPTION:
                configOperations.setWorkflowHostFlag((Boolean) entry.getSingleParameter());
                userOutputReceiver.addOutput("Set workflow host flag" + OF_INSTANCE + instanceId + TO + entry.getSingleParameter());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_TEMPDIR_PATH:
                configOperations.setTempDirectory((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Setting temp directory" + OF_INSTANCE + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_ENABLE_IM_SSH_ACCESS:
                configOperations.enableImSshAccess(((Integer) entry.getSingleParameter()), getHashedPassphrase());
                userOutputReceiver.addOutput("Configuring ssh access for IM on instance" + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_CONFIGURE_SSH_SERVER:
                configOperations.enableSshServer();
                configOperations.setSshServerIP((String) parameters[0]);
                configOperations.setSshServerPort((Integer) parameters[1]);
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_IP_FILTER_OPTION:
                configOperations.setIpFilterFlag((Boolean) entry.getSingleParameter());
                userOutputReceiver.addOutput("Set ip filter flag" + OF_INSTANCE + instanceId + TO + entry.getSingleParameter());
                break;
            case InstanceManagementConstants.SUBCOMMAND_RESET:
            case InstanceManagementConstants.SUBCOMMAND_APPLY_TEMPLATE:
                // these operations were already performed; only run a consistency check here
                if (!isFirstCommand) {
                    throw new InstanceConfigurationException("Resetting the configuration or applying a template "
                        + "must take place *before* applying any other configuration commands");
                }
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_REQUEST_TIMEOUT:
                configOperations.setRequestTimeout((Long) entry.getSingleParameter());
                userOutputReceiver.addOutput("Set request timeout" + OF_INSTANCE + instanceId + TO + entry.getSingleParameter());
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_FORWARDING_TIMEOUT:
                configOperations.setForwardingTimeout((Long) entry.getSingleParameter());
                userOutputReceiver.addOutput("Set forwarding timeout" + OF_INSTANCE + instanceId + TO + entry.getSingleParameter());
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_ALLOWED_INBOUND_IP:
                configOperations.addAllowedIp((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Added allowed IP " + entry.getSingleParameter() + TO_INSTANCE + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_ALLOWED_INBOUND_IP:
                configOperations.removeAllowedIp((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Removed allowed IP " + entry.getSingleParameter() + " from " + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_ADD_SSH_CONNECTION:
                final ConfigurationSshConnection sshConnectionData = (ConfigurationSshConnection) entry.getSingleParameter();
                configOperations.addSshConnection(sshConnectionData);
                userOutputReceiver.addOutput("Adding SSH connection " + sshConnectionData.getName() + TO_INSTANCE + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_REMOVE_SSH_CONNECTION:
                configOperations.removeSshConnection((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Removing SSH connection " + entry.getSingleParameter() + " from instance " + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_PUBLISH_COMPONENT:
                configOperations.publishComponent((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Published component " + entry.getSingleParameter() + ON_INSTANCE + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_UNPUBLISH_COMPONENT:
                configOperations.unPublishComponent((String) entry.getSingleParameter());
                userOutputReceiver.addOutput("Unpublished component " + entry.getSingleParameter() + ON_INSTANCE + instanceId);
                break;
            case InstanceManagementConstants.SUBCOMMAND_SET_BACKGROUND_MONITORING:
                String id = (String) entry.getParameters()[0];
                int interval = (Integer) entry.getParameters()[1];
                configOperations.setBackgroundMonitoring(id, interval);
                userOutputReceiver.addOutput("Setting background monitoring interval for instance" + instanceId + TO + interval);
                break;
            default:
                throw new InstanceConfigurationException("Unhandled configuration change request: " + entry.getFlag());
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

    @Override
    public File resolveRelativePathWithinProfileDirectory(String instanceId, final String relativePath) {
        return new File(new File(profilesRootDir, instanceId).getAbsoluteFile(), relativePath);
    }

    @Override
    public void startInstance(String installationId, List<String> instanceIdList,
        TextOutputReceiver userOutputReceiver, final long timeout, boolean startWithGui) throws InstanceOperationException, IOException {

        // the list may be modified, so replace it with a local copy
        instanceIdList = new ArrayList<>(instanceIdList);

        if (installationId == null || instanceIdList == null || installationId.isEmpty() || instanceIdList.isEmpty()) {
            throw new IOException("Malformed command: either no installation id or instance id defined.");
        }

        final File installationDir;
        if (InstanceManagementService.MASTER_INSTANCE_SYMBOLIC_INSTALLATION_ID.equals(installationId)) {
            installationDir = new File(getIMMasterInstallationPathAsInstallationId());
        } else if (installationId.startsWith(CUSTOM_LOCAL_INSTALLATION_PATH_INSTALLATION_ID_PREFIX)) {
            // cut away the prefix, expecting the remainder to point to an installation's directory
            installationDir = new File(installationId.substring(CUSTOM_LOCAL_INSTALLATION_PATH_INSTALLATION_ID_PREFIX.length()));
        } else {
            File possibleInstallation = new File(installationsRootDir + SLASH + installationId);
            if (!possibleInstallation.exists()) {
                throw new IOException("Installation with id: " + installationId + " does not exist");
            }
            validateInstallationId(installationId);
            installationDir = resolveAndCheckInstallationDir(installationId);
        }

        if (!installationDir.isDirectory()) {
            throw new IOException(
                "Resolved the given installation id to directory \"" + installationDir.getAbsolutePath() + "\", but it does not exist");
        }

        validateInstanceId(instanceIdList, false);
        validateConfiguration(true, false);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);

        for (String s : new ArrayList<String>(instanceIdList)) {
            if (isInstanceRunning(s)) {
                instanceIdList.remove(s);
                userOutputReceiver.addOutput("Profile with id: " + s + " is already running.");
            }
        }
        List<File> profileDirList = resolveAndCheckProfileDirList(instanceIdList);
        writeInstallationIdToFile(installationId, profileDirList, userOutputReceiver);
        try {
            instanceOperations.startInstanceUsingInstallation(profileDirList, installationDir, timeout, userOutputReceiver, startWithGui);
        } catch (InstanceOperationException e) {
            throw new IOException("An error occured on the startup process of some instances. Aborted with message: " + e.getMessage());
        }

        addInstanceToMap(instanceIdList, installationId);
    }

    @Override
    public void stopInstance(List<String> instanceIdList, TextOutputReceiver userOutputReceiver, final long timeout)
        throws InstanceOperationException, IOException {

        if (instanceIdList == null || instanceIdList.isEmpty()) {
            userOutputReceiver.addOutput("No instance to stop defined.. aborting.");
            return;
        }

        // the list may be modified, so replace it with a local copy
        instanceIdList = new ArrayList<>(instanceIdList);

        validateInstanceId(instanceIdList, true);
        validateConfiguration(true, false);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);
        final List<File> profileDirList = resolveAndCheckProfileDirList(instanceIdList);
        for (File profile : new ArrayList<>(profileDirList)) {
            if (!isInstanceRunning(profile.getName())) {
                userOutputReceiver.addOutput("Instance with id: " + profile.getName() + " is currently not running.");
                profileDirList.remove(profile);
            }
        }
        if (profileDirList.isEmpty()) {
            return;
        }
        try {
            instanceOperations.shutdownInstance(profileDirList, timeout, userOutputReceiver);
        } catch (IOException e) {
            checkAndRemoveInstanceLock(profileDirList, instanceIdList, e);
        } finally {
            checkAndRemoveInstanceLock(profileDirList, instanceIdList, null);
            removeInstanceTopMap(instanceIdList);
            removeInstallationFileIfProfileIsNotRunning(resolveAndCheckProfileDirList(instanceIdList));
        }
    }

    private void removeInstallationFileIfProfileIsNotRunning(List<File> profileDirList) throws IOException {
        for (File profile : profileDirList) {
            if (!isInstanceRunning(profile.getName())) {
                if (profile.isDirectory()) {
                    for (File file : profile.listFiles()) {
                        if (file.getName().equals(InstanceOperationsUtils.INSTALLATION_ID_FILE_NAME)) {
                            boolean success = file.delete();
                            if (!success) {
                                throw new IOException("Failed to delete installation id.");
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkAndRemoveInstanceLock(List<File> profileDirList, List<String> instanceIdList, IOException e) throws IOException {
        for (File profile : new ArrayList<>(profileDirList)) {
            if (!isInstanceRunning(profile.getName())) {
                profileDirList.remove(profile);
                if (profile.isDirectory()) {
                    for (File file : profile.listFiles()) {
                        if (file.getName().equals(Profile.PROFILE_DIR_LOCK_FILE_NAME)) {
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
        return InstanceOperationsUtils.isProfileLocked(profileDir);
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
                    if (fileInInstanceFolder.getName().equals(Profile.PROFILE_DIR_LOCK_FILE_NAME)) {
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
    public void startAllInstances(String installationId, TextOutputReceiver userOutputReceiver, final long timeout)
        throws InstanceOperationException, IOException {
        List<String> instanceIdList = new ArrayList<>();
        try {
            addProfiles(profilesRootDir.toPath(), instanceIdList);
        } catch (IOException e) {
            throw new InstanceOperationException("Failed to add profile. Aborted with message: " + e.getMessage());
        }
        startInstance(installationId, instanceIdList, userOutputReceiver, timeout, false);
    }

    @Override
    public void stopAllInstances(String installationId, TextOutputReceiver userOutputReceiver, final long timeout)
        throws InstanceOperationException, IOException {
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
            try {
                addProfiles(profilesRootDir.toPath(), instanceIdList);
            } catch (IOException e) {
                throw new InstanceOperationException("Failed to add profile. Aborted with message: " + e.getMessage());
            }
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
            log.error("Disabling local instance management due to missing or invalid configuration: " + e.getMessage());
            reasonInstanceManagementNotStarted =
                INSTANCE_MANAGEMENT_DISABLED + e.getMessage();
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
            reasonInstanceManagementNotStarted =
                INSTANCE_MANAGEMENT_DISABLED + e.getMessage();
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
        log.info(StringUtils.format("Identified version of remote installation package '%s': %s", urlQualifier, version));
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
        log.info(StringUtils.format("Identified version of remote installation package '%s': %s", urlQualifier, newVersion));

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
            log.info("Reinstalling with 'force new download' option set");
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
                        if (fileInProfile.isFile() && fileInProfile.getName().equals(Profile.PROFILE_DIR_LOCK_FILE_NAME)) {
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
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(profile.getAbsolutePath() + SLASH + "installation"),
                    "utf-8"))) {
                writer.write(installationId);
            }
        }
    }

    @Override
    public void executeCommandOnInstance(String instanceId, String command, TextOutputReceiver userOutputReceiver) throws JSchException,
        SshParameterException, IOException, InterruptedException {
        Objects.requireNonNull(instanceId); // sanity check
        if (isInstanceRunning(instanceId)) {
            Logger logger = JschSessionFactory.createDelegateLogger(log);
            Integer port = null;
            try {
                port = getSshPortForInstance(instanceId);
            } catch (InstanceConfigurationException e) {
                throw new IOException(e);
            }
            String ip = null;
            try {
                ip = getSshIpForInstance(instanceId);
            } catch (InstanceConfigurationException e) {
                throw new IOException(e);
            }
            String passphrase = persistentSettingsService.readStringValue(InstanceManagementConstants.IM_MASTER_PASSPHRASE_KEY);
            if (passphrase != null && port != null && ip != null) {
                Session session =
                    JschSessionFactory.setupSession(ip, port,
                        InstanceManagementConstants.IM_MASTER_USER_NAME,
                        null, passphrase, logger);
                JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
                rceExecutor.start(command);
                try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
                    TextStreamWatcher stdoutWatcher = TextStreamWatcherFactory.create(stdoutStream, userOutputReceiver);
                    TextStreamWatcher stderrWatcher = TextStreamWatcherFactory.create(stderrStream, userOutputReceiver);
                    stdoutWatcher.start();
                    stderrWatcher.start();
                    rceExecutor.waitForTermination();
                    stdoutWatcher.waitForTermination();
                    stderrWatcher.waitForTermination();
                }
                session.disconnect();
                userOutputReceiver.addOutput("Finished executing command " + command + ON_INSTANCE + instanceId);
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
     * @throws InstanceConfigurationException
     */
    private Integer getSshPortForInstance(String instanceId) throws IOException, InstanceConfigurationException {
        File config = resolveRelativePathWithinProfileDirectory(instanceId, CONFIGURATION_FILENAME);
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
     * @throws InstanceConfigurationException
     */
    private String getSshIpForInstance(String instanceId) throws InstanceConfigurationException {
        File config = resolveRelativePathWithinProfileDirectory(instanceId, CONFIGURATION_FILENAME);
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

    private String getIMMasterInstallationPathAsInstallationId() throws IOException {
        String runningIMInstallationPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        runningIMInstallationPath = runningIMInstallationPath.substring(0, runningIMInstallationPath.lastIndexOf(SLASH));
        runningIMInstallationPath = runningIMInstallationPath.substring(0, runningIMInstallationPath.lastIndexOf(SLASH));
        try {
            runningIMInstallationPath = URLDecoder.decode(runningIMInstallationPath, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            throw new IOException("Failed to decode installation path");
        }
        if (runningIMInstallationPath == null || runningIMInstallationPath.isEmpty()) {
            throw new IOException("Installation path of IM master instance is either null or empty");
        }
        return runningIMInstallationPath;
    }
}
