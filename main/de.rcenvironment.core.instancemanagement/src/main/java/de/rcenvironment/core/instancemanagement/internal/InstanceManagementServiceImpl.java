/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.AbstractTextOutputReceiver;
import de.rcenvironment.core.utils.incubator.FileSystemOperations;

/**
 * Default {@link InstanceManagementService} implementation.
 * 
 * @author Robert Mischke
 */
public class InstanceManagementServiceImpl implements InstanceManagementService {

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

    private File dataRootDir;

    private File installationsRootDir;

    private File profilesRootDir;

    private File templatesRootDir;

    private File downloadsCacheDir;

    private volatile boolean hasValidLocalConfiguration = false;

    private volatile boolean hasValidDownloadConfiguration = false;

    private ConfigurationService configurationService;

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
            return;
        }
        try {
            applyConfiguration(configuration);
        } catch (IOException e) {
            // do not fail the activate() method; the failure is marked by the "hasValidConfiguration" flag being "false"
            log.error("Error while configuring " + getClass().getSimpleName(), e);
        }

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
        if (downloadSourceFolderUrlPattern.length() > 0 && !downloadSourceFolderUrlPattern.endsWith("/")) {
            downloadSourceFolderUrlPattern = downloadSourceFolderUrlPattern + "/";
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

    /**
     * OSGi-DS lifecycle method; made public for unit testing.
     */
    public void deactivate() {
        hasValidLocalConfiguration = false;
        hasValidDownloadConfiguration = false;
    }

    @Override
    public void setupInstallationFromUrlQualifier(String installationId, String urlQualifier, InstallationPolicy installationPolicy,
        TextOutputReceiver userOutputReceiver) throws IOException {
        validateId(installationId);
        validateConfiguration(true, true);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);
        deploymentOperations.setUserOutputReceiver(userOutputReceiver);

        // FIXME validate installationId!

        // TODO support other policies
        if (installationPolicy == InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT) {
            // get remote version
            userOutputReceiver.addOutput("Fetching remote version information");
            String newVersion = fetchVersionInformationFromDownloadSourceFolder(urlQualifier);
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

            // download installation package if not already present
            File zipFile = fetchProductZipIfNecessary(urlQualifier, newVersion);

            File installationDir = new File(installationsRootDir, installationId);
            if (installationDir.exists()) {
                userOutputReceiver.addOutput("Deleting old installation " + installationId);
                deploymentOperations.deleteInstallation(installationDir);
            }
            userOutputReceiver.addOutput("Setting up new installation " + installationId);
            deploymentOperations.installFromProductZip(zipFile, installationDir);

            log.debug("Writing version information for installation " + installationId);
            storeVersionOfInstallation(installationId, newVersion);
        } else {
            throw new IOException("Not supported yet: " + installationPolicy);
        }
    }

    @Override
    public void configureInstanceFromTemplate(String templateId, String instanceId, Map<String, String> properties,
        boolean deleteOtherFiles) throws IOException {
        validateConfiguration(true, false);

        final File templateDir = resolveAndCheckTemplateDir(templateId);
        File templateConfigurationFile = new File(templateDir, CONFIGURATION_FILENAME);
        final File profileDir = resolveAndCheckProfileDir(instanceId);
        File profileConfigurationFile = new File(profileDir, CONFIGURATION_FILENAME);

        // for now, simply copy the template file
        FileUtils.copyFile(templateConfigurationFile, profileConfigurationFile);
    }

    @Override
    public void startinstance(String installationId, String instanceId, TextOutputReceiver userOutputReceiver) throws IOException {
        validateId(installationId);
        validateId(instanceId);
        validateConfiguration(true, false);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);

        final File installationDir = resolveAndCheckInstallationDir(installationId);
        final File profileDir = resolveAndCheckProfileDir(instanceId);

        instanceOperations.startInstanceUsingInstallation(profileDir, installationDir);
    }

    @Override
    public void stopInstance(String instanceId, TextOutputReceiver userOutputReceiver) throws IOException {
        validateId(instanceId);
        validateConfiguration(true, false);
        userOutputReceiver = ensureUserOutputReceiverDefined(userOutputReceiver);

        final File profileDir = resolveAndCheckProfileDir(instanceId);
        instanceOperations.shutdownInstance(profileDir);
    }

    @Override
    public boolean isInstanceRunning(String instanceId) throws IOException {
        validateConfiguration(true, false);
        final File profileDir = resolveAndCheckProfileDir(instanceId);
        return instanceOperations.isProfileLocked(profileDir);
    }

    protected void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
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

    private void validateId(String id) throws IOException {
        // TODO add reasonable maximum length check?
        if (!VALID_IDS_REGEXP_PATTERN.matcher(id).matches()) {
            // note: this assumes that even malformed ids are safe to print, as it should only affect the user
            // that issued the command anyway
            throw new IOException("Malformed id: " + id);
        }
    }

    private File resolveAndCheckInstallationDir(String installationId) throws IOException {
        final File installationDir = new File(installationsRootDir, installationId);
        prepareAndValidateDirectory("installation " + installationId, installationDir);
        return installationDir;
    }

    private File resolveAndCheckProfileDir(String instanceId) throws IOException {
        final File profileDir = new File(profilesRootDir, instanceId);
        prepareAndValidateDirectory("instance " + instanceId, profileDir);
        return profileDir;
    }

    private File resolveAndCheckTemplateDir(String templateId) throws IOException {
        final File templateDir = new File(templatesRootDir, templateId);
        prepareAndValidateDirectory("template " + templateId, templateDir);
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

    private File getConfiguredDirectory(final ConfigurationSegment configuration, String id) throws IOException {
        String configuredPath = configuration.getString(id);
        log.debug("Configuration value for property '" + id + "': " + configuredPath);
        if (configuredPath != null) {
            return new File(configuredPath).getAbsoluteFile();
        } else {
            return null;
        }
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

    private File fetchProductZipIfNecessary(String urlQualifier, String remoteVersion) throws IOException {
        File downloadFile = new File(downloadsCacheDir, remoteVersion + ".zip");
        if (downloadFile.exists()) {
            log.info("Version " + remoteVersion + " is already present in downloads cache, not downloading");
        } else {
            downloadInstallationPackage(urlQualifier, remoteVersion, downloadFile);
        }
        return downloadFile;
    }

    private TextOutputReceiver ensureUserOutputReceiverDefined(TextOutputReceiver userOutputReceiver) {
        if (userOutputReceiver != null) {
            return userOutputReceiver;
        } else {
            return fallbackUserOutputReceiver;
        }
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
        } else {
            outputReceiver.addOutput("Download cache directory not defined.");
        }
        if (downloadsCacheDir.listFiles().length > 0) {
            outputReceiver.addOutput("Downloads cached: ");
            for (File cachedDownloadFile : downloadsCacheDir.listFiles()) {
                outputReceiver.addOutput(INDENT + cachedDownloadFile.getName().replace(".zip", ""));
            }
        } else {
            outputReceiver.addOutput("No download cached.");
        }
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
                    if (fileInProfile.isFile() && fileInProfile.getName().equals(BootstrapConfiguration.PROFILE_DIR_LOCK_FILE_NAME)) {
                        runningState = "Running";
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
}
