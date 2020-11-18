/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationServiceMessage;
import de.rcenvironment.core.configuration.ConfigurationServiceMessageEvent;
import de.rcenvironment.core.configuration.ConfigurationServiceMessageEventListener;
import de.rcenvironment.core.configuration.WritableConfigurationSegment;
import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLogFileBackend;
import de.rcenvironment.core.utils.common.AuditLogIds;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.VersionUtils;

/**
 * Implementation of the {@link ConfigurationService} using JSON as file format.
 * 
 * @author Heinrich Wendel
 * @author Tobias Menden
 * @author Robert Mischke
 * @author Christian Weiss
 * @author Doreen Seider
 * @author Kathrin Schaffert
 */
public class ConfigurationServiceImpl implements ConfigurationService {

    protected static final String CONFIGURATION_SUBDIRECTORY_PATH = "configuration";

    protected static final String INTEGRATION_SUBDIRECTORY_PATH = "integration";

    protected static final String RELATIVE_PATH_TO_STORAGE_ROOT = "storage";

    protected static final String RELATIVE_PATH_TO_INTERNAL_DATA_ROOT = "internal";

    protected static final String RELATIVE_PATH_TO_OUTPUT_ROOT = "output";

    protected static final String MAIN_CONFIGURATION_FILENAME = "configuration.json";

    protected static final String JDBC_SUBDIRECTORY_PATH = "extras/database_connectors";

    // debug option that writes/exports the active configuration to the profile's output folder
    private static final boolean AUTO_EXPORT_CONFIGURATION_ON_STARTUP = false;

    private static final String SPACE_CHARACTER = " ";

    private File parentTempDirectoryRoot;

    private final Map<ConfigurablePathId, File> configurablePathMap = new HashMap<>();

    private final Map<ConfigurablePathListId, List<File>> configurablePathListMap = new HashMap<>();

    private final ObjectMapper mapper; // reusable JSON mapper object

    /**
     * The merged map of key-value replacements; the namespace qualifier is merged into the map keys by the format "<namespace>:<plain key
     * value>".
     */
    private Map<String, String> substitutionProperties = new HashMap<>();

    // injected listeners
    private final List<ConfigurationServiceMessageEventListener> errorListeners = new LinkedList<>();

    private final Log log = LogFactory.getLog(getClass());

    private List<File> readableConfigurationDirs;

    private Profile profile;

    private boolean isUsingIntendedProfileDirectory;

    private BootstrapConfiguration bootstrapSettings;

    private ConfigurationStoreImpl configurationStore;

    private File configurationStoreFile;

    private ConfigurationSegment currentRootConfiguration;

    private GeneralSettings generalSettings;

    private String resolvedInstanceName;

    private boolean usingDefaultConfigurationValues = false;

    private UnpackedFilesDirectoryResolver unpackedFilesDirectoryResolver;

    public ConfigurationServiceImpl() {
        mapper = JsonUtils.getDefaultObjectMapper();
        // allow comments in JSON files
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    /**
     * OSGi-DS life cycle method. Note that due to global initialization calls, this should not be called from unit tests; use the mock
     * activator methods instead.
     * 
     * @param context the bundle context
     */
    public void activate(BundleContext context) {
        bootstrapSettings = BootstrapConfiguration.getInstance();

        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not implicitly initialize a profile and related settings in test environments; our testing
            // approach is that relevant tests should do this themselves in a controlled way instead.
            // if disabling this causes downstream problems, these need to be fixed on a case-by-case basis,
            // typically by checking for the test environment in the activator and exiting early.
            return;
        }

        initializeProfileDirFromBootstrapSettings();
        initializeConfigurablePaths();
        loadRootConfiguration(false);
        exportConfigIfConfigured(false);
        initializeGeneralSettings();

        initializeAuditLog();

        // initialize parent temp directory root
        initializeParentTempDirectoryRoot(generalSettings.getTempDirectoryOverride());

        initializeInstanceTempDirectoryRoot();

        unpackedFilesDirectoryResolver =
            new UnpackedFilesDirectoryResolver(context, getConfigurablePath(ConfigurablePathId.INSTALLATION_DATA_ROOT));
    }

    private synchronized void loadRootConfiguration(boolean isReload) {

        if (!isReload) {
            configurationStoreFile = new File(getProfileDirectory(), MAIN_CONFIGURATION_FILENAME);
            if (!configurationStoreFile.exists()) {
                File sampleFileLocation = new File(getConfigurablePath(ConfigurablePathId.INSTALLATION_DATA_ROOT),
                    "examples/configuration/configuration.json.default_configuration.sample");
                log.info("No configuration file found; creating a new one at " + configurationStoreFile.getAbsolutePath());
                if (sampleFileLocation.isFile()) {
                    try {
                        FileUtils.copyFile(sampleFileLocation, configurationStoreFile);
                        log.info("Successfully created a default configuration file at " + configurationStoreFile.getAbsolutePath());
                    } catch (IOException e) {
                        log.error("Failed to copy sample configuration file from " + sampleFileLocation.getAbsolutePath() + " to "
                            + configurationStoreFile.getAbsolutePath());
                    }
                } else {
                    log.error("Expected configuration sample file not found at " + sampleFileLocation.getAbsolutePath());
                }
            }
            // proceed in any case; if there was an error, the file may not exist
            configurationStore = new ConfigurationStoreImpl(configurationStoreFile);
        }

        try {
            currentRootConfiguration = configurationStore.getSnapshotOfRootSegment();
        } catch (IOException e) {
            // log without (usually irrelevant) stacktrace
            log.error(StringUtils.format("Failed to load configuration file %s: %s",
                configurationStoreFile.getAbsolutePath(), e.toString()));
        }

        if (currentRootConfiguration == null) {
            log.error(StringUtils.format("No configuration file found, or it could not be loaded; using default values"));
            currentRootConfiguration = configurationStore.createEmptyPlaceholder();
            usingDefaultConfigurationValues = true;
        }

        log.debug("(Re-)loaded root configuration");
    }

    @SuppressWarnings("unused")
    private void exportConfigIfConfigured(boolean isReload) {
        if (!isReload && AUTO_EXPORT_CONFIGURATION_ON_STARTUP) {
            try {
                configurationStore.exportToFile(currentRootConfiguration,
                    new File(getProfileDirectory(), "output/autoExportOnStartup.json"));
            } catch (IOException e) {
                log.error(e.toString());
            }
        }
    }

    @Override
    public File getProfileDirectory() {
        return profile.getProfileDirectory();
    }

    @Override
    public File getProfileConfigurationFile() {
        // FIXME 6.0.0 this path will change; adapt
        return new File(getConfigurablePath(ConfigurablePathId.PROFILE_ROOT), MAIN_CONFIGURATION_FILENAME);
    }

    @Override
    public boolean isUsingIntendedProfileDirectory() {
        return isUsingIntendedProfileDirectory;
    }

    @Override
    public boolean isIntendedProfileDirectorySuccessfullyLocked() {
        return bootstrapSettings.getOriginalProfile().isLocked();
    }

    @Override
    public boolean hasIntendedProfileDirectoryValidVersion() {
        final Profile originalProfile = bootstrapSettings.getOriginalProfile();
        try {
            return originalProfile.hasCurrentVersion();
        } catch (ProfileException e) {
            log.error(String.format("Could not determine version of profile \"%s\".", originalProfile.getName()), e);
            return false;
        }
    }

    private void initializeProfileDirFromBootstrapSettings() {
        profile = bootstrapSettings.getProfile();
        isUsingIntendedProfileDirectory =
            this.isIntendedProfileDirectorySuccessfullyLocked() && this.hasIntendedProfileDirectoryValidVersion();
        // initializeInstanceDataDirectory();
    }

    private void initializeAuditLog() {
        try {
            Path profileRootPath = getConfigurablePath(ConfigurablePathId.PROFILE_ROOT).toPath();
            AuditLog.initialize(new AuditLogFileBackend(profileRootPath.resolve("event.log")));

            // log a separate event for visual separation
            AuditLog.append(AuditLogIds.APPLICATION_START, "", AuditLogIds.SEPARATOR_LINE_VALUE);

            AuditLog.append(AuditLog.newEntry(AuditLogIds.APPLICATION_SESSION_STARTING)
                .set("profile_location", profileRootPath.toString()) // e.g. for logging to non-default locations
                .set("os_name", System.getProperty("os.name") + "; " + System.getProperty("os.version"))
                .set("user_name", System.getProperty("user.name"))
                .set("user_home", System.getProperty("user.home"))
                .set("work_dir", System.getProperty("user.dir")));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // dummy data as visual separator
                AuditLog.append(AuditLogIds.APPLICATION_TERMINATING, null);
                AuditLog.close();
            }));
        } catch (IOException e) {
            log.error("Failed to initialize audit log: " + e.toString());
        }
    }

    /**
     * Unit test initialization method.
     */
    protected void mockActivate() {
        this.bootstrapSettings = BootstrapConfiguration.getInstance();

        initializeProfileDirFromBootstrapSettings();
        initializeConfigurablePaths();
        loadRootConfiguration(false);
        initializeGeneralSettings();
    }

    @Override
    public void addSubstitutionProperties(String namespace, Map<String, String> properties) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace must not be null");
        }
        // copy all entries with the namespace and a colon as an added key prefix
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            substitutionProperties.put(namespace + ":" + entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Deprecated
    public <T> T getConfiguration(String identifier, Class<T> clazz) {
        log.warn("Using a legacy method to load configuration data; id = " + identifier);
        T configObject = null;
        String errorMessage = null;
        Throwable exception = null;
        File configFile;
        String fileName = identifier + ".json";
        configFile = locateConfigurationFile(fileName);
        if (configFile != null) {
            log.debug("Loading configuration file " + configFile);
            try {
                configObject = parseConfigurationFile(clazz, configFile);
            } catch (JsonParseException e) {
                errorMessage = Messages.bind(Messages.parsingError, configFile);
                exception = e;
            } catch (JsonMappingException e) {
                errorMessage = Messages.bind(Messages.mappingError, configFile);
                exception = e;
            } catch (IOException e) {
                errorMessage = Messages.bind(Messages.couldNotOpen, configFile);
                exception = e;
            }
        } else {
            log.debug("No " + fileName + " found in any of the configuration directories "
                + readableConfigurationDirs + "; using default values");
        }

        // broadcast error if parsing failed
        if (errorMessage != null) {
            log.warn(errorMessage, exception);
            final ConfigurationServiceMessage error = new ConfigurationServiceMessage(errorMessage);
            fireErrorEvent(error);
        }
        // create a new configuration instance if parsing did not succeed
        if (configObject == null) {
            try {
                configObject = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Error creating configuration object", e);
                throw new RuntimeException(e);
            }
        }
        return configObject;
    }

    @Override
    public ConfigurationSegment getConfigurationSegment(String relativePath) {
        return currentRootConfiguration.getSubSegment(relativePath);
    }

    @Override
    public WritableConfigurationSegment getOrCreateWritableConfigurationSegment(String path) throws ConfigurationException {
        return currentRootConfiguration.getOrCreateWritableSubSegment(path);
    }

    @Override
    public void writeConfigurationChanges() throws ConfigurationException, IOException {
        configurationStore.update(currentRootConfiguration);
    }

    @Override
    public void reloadConfiguration() {
        loadRootConfiguration(true);
    }

    @Override
    public ConfigurationSegment loadCustomConfigurationFile(Path path) throws IOException {
        return new ConfigurationStoreImpl(path.toFile()).getSnapshotOfRootSegment();
    }

    protected <T> T parseConfigurationFile(Class<T> clazz, File configFile) throws IOException, JsonParseException, JsonMappingException {
        T configObject;
        if (configFile.exists()) {
            String fileContent = FileUtils.readFileToString(configFile);
            if (fileContent.equals("")) {
                return null;
            }
            try {
                configObject = mapper.readValue(fileContent, clazz);
            } catch (JsonParseException e) {
                String errorMessage = Messages.bind(Messages.parsingError, configFile.getAbsolutePath());
                final ConfigurationServiceMessage error = new ConfigurationServiceMessage(errorMessage);
                fireErrorEvent(error);
                configObject = null;
            }
            return configObject;
        } else {
            return null;
        }
    }

    @Override
    public String resolveBundleConfigurationPath(String identifier, String path) {
        final String absolutePath;
        // TODO 5.0 review: should this accept absolute paths at all? what is the use case? -
        // misc_ro
        if (new File(path).isAbsolute()) {
            absolutePath = path;
        } else {
            absolutePath = bootstrapSettings.getInstallationDir().getAbsolutePath() + File.separator + identifier + File.separator + path;
        }
        return absolutePath;
    }

    @Override
    public File getConfigurablePath(ConfigurablePathId pathId) {
        File path = configurablePathMap.get(pathId);
        if (path == null) {
            throw new IllegalStateException("Internal error: Unconfigured path requested, id = " + pathId);
        }
        return path;
    }

    @Override
    public File[] getConfigurablePathList(ConfigurablePathListId pathListId) {
        List<File> pathList = configurablePathListMap.get(pathListId);
        if (pathList == null) {
            throw new IllegalStateException("Internal error: Unconfigured path list requested, id = " + pathListId);
        }
        return pathList.toArray(new File[pathList.size()]);
    }

    @Override
    public File initializeSubDirInConfigurablePath(ConfigurablePathId pathId, String relativePath) {
        File subDir = new File(getConfigurablePath(pathId), relativePath);
        subDir.mkdirs();
        if (!subDir.isDirectory()) {
            throw new RuntimeException(new IOException("Failed to create configuration sub-directory " + subDir.getAbsolutePath()));
        } else {
            return subDir;
        }
    }

    @Override
    public File getStandardImportDirectory(String subdir) {
        // TODO could get its individual CPId
        return new File(getConfigurablePath(ConfigurationService.ConfigurablePathId.PROFILE_ROOT), "import/" + subdir);
    }

    @Override
    public File getOriginalProfileDirectory() {
        return bootstrapSettings.getOriginalProfile().getProfileDirectory();
    }

    @Override
    public File getUnpackedFilesLocation(String filesetId) throws ConfigurationException {
        return unpackedFilesDirectoryResolver.resolveIdToUnpackedFilesDirectory(filesetId);
    }

    @Override
    public String getInstanceName() {
        return resolvedInstanceName;
    }

    @Override
    public boolean getIsWorkflowHost() {
        return generalSettings.getIsWorkflowHost();
    }

    @Override
    public boolean getIsRelay() {
        return generalSettings.getIsRelay();
    }

    @Override
    public File getParentTempDirectoryRoot() {
        return parentTempDirectoryRoot;
    }

    /**
     * Adds a {@link ConfigurationServiceMessageEventListener} to this {@link ConfigurationService}.
     * 
     * @param listener the listener to add
     */
    public void addErrorListener(ConfigurationServiceMessageEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        errorListeners.add(listener);
        log.debug(StringUtils.format("Added instance of type '%s' to the configuration service error listeners.", listener.getClass()));
    }

    /**
     * Removes the given {@link ConfigurationServiceMessageEventListener} from this {@link ConfigurationService}.
     * 
     * @param listener the listener to remove
     */
    public void removeErrorListener(ConfigurationServiceMessageEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        errorListeners.remove(listener);
        log.debug(StringUtils.format("Removed instance of type '%s' to the configuration service error listeners.", listener.getClass()));
    }

    protected void fireErrorEvent(final ConfigurationServiceMessage error) {
        final ConfigurationServiceMessageEvent event = new ConfigurationServiceMessageEvent(this, error);
        RuntimeException exception = null;
        for (final ConfigurationServiceMessageEventListener listener : errorListeners) {
            try {
                listener.handleConfigurationServiceError(event);
            } catch (RuntimeException e) {
                // only cache first exception
                if (exception == null) {
                    exception = e;
                }
            }
        }
        // re-throw first exception
        if (exception != null) {
            throw exception;
        }
    }

    private void initializeParentTempDirectoryRoot(String parentTempDirectoryOverride) {
        File parentTempDir = null;
        if (parentTempDirectoryOverride != null && !parentTempDirectoryOverride.trim().isEmpty()) {
            if (parentTempDirectoryOverride.contains(SPACE_CHARACTER)) {
                log.warn("Failed to initialize custom temp directory. Path '" + parentTempDirectoryOverride
                    + "' contains whitespace(s) - trying default directory");
            } else {
                parentTempDir = new File(resolvePlaceholdersInTempDirSetting(parentTempDirectoryOverride));
                parentTempDir.mkdirs();
                if (!parentTempDir.isDirectory()) {
                    log.warn("Failed to initialize custom temp directory " + parentTempDir.getAbsolutePath()
                        + " - trying default directory");
                    // clear the field to use same code path as when no custom root dir was given in
                    // the first place
                    parentTempDir = null;
                }
            }
        }
        if (parentTempDir == null) {
            String relativePath;
            if (OSFamily.isWindows()) {
                relativePath = DEFAULT_PARENT_TEMP_DIRECTORY_RELATIVE_PATH_WINDOWS;
            } else {
                // Linux, and also the fallback for unrecognized OS values
                relativePath = DEFAULT_PARENT_TEMP_DIRECTORY_RELATIVE_PATH_LINUX;
            }
            parentTempDir = new File(getSystemTempDir(), resolvePlaceholdersInTempDirSetting(relativePath));
            parentTempDir.mkdirs();
            if (!parentTempDir.isDirectory()) {
                // TODO >6.0.0: delegate failure handling to validator?
                throw new RuntimeException("Failed to initialize default temp directory "
                    + parentTempDir.getAbsolutePath());
            }
        }
        log.debug("Using parent temporary file directory " + parentTempDir.getAbsolutePath());
        parentTempDirectoryRoot = parentTempDir;

    }

    private String resolvePlaceholdersInTempDirSetting(String value) {
        return value.replace(ConfigurationService.CONFIGURATION_PLACEHOLDER_SYSTEM_USER_NAME,
            System.getProperty(ConfigurationServiceImpl.SYSTEM_PROPERTY_USER_NAME));
    }

    private void initializeGeneralSettings() {
        ConfigurationSegment configurationSegment = getConfigurationSegment("general");
        generalSettings = new GeneralSettings(configurationSegment);
        resolvedInstanceName = resolvePlaceholdersInInstanceName(generalSettings.getRawInstanceName());
        log.debug("Resolved instance name: " + resolvedInstanceName);
    }

    private String resolvePlaceholdersInInstanceName(String instanceName) {
        instanceName = instanceName.replace(CONFIGURATION_PLACEHOLDER_SYSTEM_USER_NAME,
            System.getProperty(SYSTEM_PROPERTY_USER_NAME));
        instanceName = instanceName.replace(CONFIGURATION_PLACEHOLDER_PROFILE_NAME,
            profile.getLocationDependentName());
        instanceName = instanceName.replace(CONFIGURATION_PLACEHOLDER_VERSION,
            StringUtils.nullSafeToString(VersionUtils.getVersionOfProduct(), "<unknown>"));
        instanceName = instanceName.replace(CONFIGURATION_PLACEHOLDER_JAVA_VERSION,
            System.getProperty(SYSTEM_PROPERTY_JAVA_VERSION));
        instanceName = instanceName.replace(CONFIGURATION_PLACEHOLDER_SYSTEM_NAME,
            System.getProperty(SYSTEM_PROPERTY_OS_NAME));

        // only determine the host name if actually necessary
        if (instanceName.contains(CONFIGURATION_PLACEHOLDER_HOST_NAME)) {
            try {
                instanceName = instanceName.replace(CONFIGURATION_PLACEHOLDER_HOST_NAME,
                    InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                LogFactory.getLog(getClass()).warn("Failed to determine the local host name", e);
            }
        }

        return instanceName;
    }

    private void initializeInstanceTempDirectoryRoot() {
        String instanceTempDirectoryPrefix;
        // TODO this uses the last part of the instance data dir for identification - sufficient?
        instanceTempDirectoryPrefix = profile.getName();
        try {
            TempFileServiceAccess.setupLiveEnvironment(parentTempDirectoryRoot, instanceTempDirectoryPrefix);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to initialize the temporary file manager after the parent root directory was successfully initialized; prefix="
                    + instanceTempDirectoryPrefix,
                e);
        }
        // TODO log instance root dir?
    }

    private void initializeConfigurablePaths() {

        // INSTALLATION_DATA_ROOT (default: "${osgi.install.area}/..")
        if (!configurePathFromOverridePropertyIfSet(ConfigurablePathId.INSTALLATION_DATA_ROOT,
            SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE)) {
            File installationDirectory = BootstrapConfiguration.getInstallationDir();
            log.info("Using installation data root directory " + installationDirectory.getAbsolutePath());
            configurablePathMap.put(ConfigurablePathId.INSTALLATION_DATA_ROOT, installationDirectory);
        }
        // get final value for internal usage
        final File installationDataRoot = configurablePathMap.get(ConfigurablePathId.INSTALLATION_DATA_ROOT);

        // USER_SETTINGS_ROOT (default: <PROFILES_PARENT_DIR>/common)
        if (!configurePathFromOverridePropertyIfSet(ConfigurablePathId.SHARED_USER_SETTINGS_ROOT,
            SYSTEM_PROPERTY_USER_SETTINGS_ROOT_OVERRIDE)) {
            configurablePathMap.put(ConfigurablePathId.SHARED_USER_SETTINGS_ROOT, new File(System.getProperty(SYSTEM_PROPERTY_USER_HOME),
                ".rce/common").getAbsoluteFile());
        }

        // PROFILE_ROOT
        configurablePathMap.put(ConfigurablePathId.PROFILE_ROOT, profile.getProfileDirectory());

        // profile subdirectories
        // initializeRelativeProfilePath(ConfigurablePathId.PROFILE_CONFIGURATION_DATA,
        // CONFIGURATION_SUBDIRECTORY_PATH);
        initializeRelativeProfilePath(ConfigurablePathId.PROFILE_INTEGRATION_DATA, INTEGRATION_SUBDIRECTORY_PATH);
        initializeRelativeProfilePath(ConfigurablePathId.PROFILE_OUTPUT, RELATIVE_PATH_TO_OUTPUT_ROOT);
        initializeRelativeProfilePath(ConfigurablePathId.PROFILE_DATA_MANAGEMENT, RELATIVE_PATH_TO_STORAGE_ROOT);
        initializeRelativeProfilePath(ConfigurablePathId.PROFILE_CONFIGURATION_DATA, CONFIGURATION_SUBDIRECTORY_PATH);
        initializeRelativeProfilePath(ConfigurablePathId.PROFILE_INTERNAL_DATA, RELATIVE_PATH_TO_INTERNAL_DATA_ROOT);

        // definePathAlias(ConfigurablePathId.DEFAULT_WRITEABLE_CONFIGURATION_ROOT,
        // ConfigurablePathId.PROFILE_CONFIGURATION_DATA);
        definePathAlias(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT, ConfigurablePathId.PROFILE_INTEGRATION_DATA);

        readableConfigurationDirs = new ArrayList<>();
        addDirectoryIfPresent(readableConfigurationDirs, new File(installationDataRoot, CONFIGURATION_SUBDIRECTORY_PATH));
        // TODO add system settings
        // TODO add user settings
        // addDirectoryIfPresent(readableConfigurationDirs,
        // getConfigurablePath(ConfigurablePathId.PROFILE_CONFIGURATION_DATA));
        configurablePathListMap.put(ConfigurablePathListId.READABLE_CONFIGURATION_DIRS, readableConfigurationDirs);

        List<File> jdbcDriverDirs = new ArrayList<>();
        addDirectoryIfPresent(jdbcDriverDirs, new File(installationDataRoot, JDBC_SUBDIRECTORY_PATH));
        configurablePathListMap.put(ConfigurablePathListId.JDBC_DRIVER_DIRS, jdbcDriverDirs);

        List<File> readableIntegrationDirs = new ArrayList<>();
        addDirectoryIfPresent(readableIntegrationDirs, new File(installationDataRoot, INTEGRATION_SUBDIRECTORY_PATH));
        // TODO add system settings
        // TODO add user settings
        addDirectoryIfPresent(readableIntegrationDirs, getConfigurablePath(ConfigurablePathId.PROFILE_INTEGRATION_DATA));
        configurablePathListMap.put(ConfigurablePathListId.READABLE_INTEGRATION_DIRS, readableIntegrationDirs);

        configurablePathMap
            .put(ConfigurablePathId.CONFIGURATION_SAMPLES_LOCATION, new File(installationDataRoot, "examples/configuration"));

        log.debug("Configured paths: " + configurablePathMap);
        log.debug("Configured path lists: " + configurablePathListMap);
        log.debug("Using instance output directory " + getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT));
    }

    private void definePathAlias(ConfigurablePathId aliasKey, ConfigurablePathId existingKey) {
        configurablePathMap.put(aliasKey, configurablePathMap.get(existingKey));
    }

    private void addDirectoryIfPresent(List<File> pathList, File path) {
        path = path.getAbsoluteFile();
        if (path.isDirectory()) {
            pathList.add(path);
        }
    }

    private boolean configurePathFromOverridePropertyIfSet(ConfigurablePathId pathId,
        String overrideProperty) {
        String value = System.getProperty(overrideProperty);
        if (value == null) {
            return false;
        }
        File path = new File(value);
        if (!path.isAbsolute()) {
            log.warn(StringUtils.format("Value '%s' for path override setting '%s' will be ignored as it is not an absolute path",
                value, overrideProperty));
            return false;
        }
        if (!path.isDirectory()) {
            log.warn(StringUtils.format(
                "Value '%s' for path override setting '%s' will be ignored as it does not point to an existing directory",
                value, overrideProperty));
            return false;
        }
        configurablePathMap.put(pathId, path);
        return true;
    }

    private void initializeRelativeProfilePath(ConfigurablePathId key, String relativePath) {
        File subDir = new File(profile.getProfileDirectory(), relativePath).getAbsoluteFile();
        subDir.mkdirs();
        if (!subDir.isDirectory()) {
            // TODO realistic enough to throw a checked exception instead?
            throw new RuntimeException("Unexpected state: Failed to initialize profile sub-directory " + subDir.getAbsolutePath());
        }
        configurablePathMap.put(key, subDir);
    }

    private File locateConfigurationFile(String fileName) {
        for (File configDir : readableConfigurationDirs) {
            File file = new File(configDir, fileName);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private String performSubstitutions(String input, File originFile) {
        // shortcut if no substitution is configured
        if (substitutionProperties.isEmpty()) {
            return input;
        }
        // construct pattern to detect "${namespace:key}" and extract the "namespace:key" part
        Pattern pattern = Pattern.compile("\\$\\{(\\w+:\\w+)\\}");
        // note: the Matcher class enforces use of StringBuffer (instead of StringBuilder)
        StringBuffer buffer = new StringBuffer(input.length());
        // perform substitution
        Matcher m = pattern.matcher(input);
        while (m.find()) {
            String key = m.group(1);
            String value = substitutionProperties.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Missing configuration value for \"" + key + "\" in file "
                    + originFile.getAbsolutePath());
            }
            m.appendReplacement(buffer, value);
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    private String getSystemTempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    @Override
    public boolean isUsingDefaultConfigurationValues() {
        return usingDefaultConfigurationValues;
    }

    @Override
    public double[] getLocationCoordinates() {
        return generalSettings.getLocation();
    }

    @Override
    public String getLocationName() {
        return generalSettings.getLocationName();
    }

    @Override
    public String getInstanceContact() {
        return generalSettings.getContact();
    }

    @Override
    public String getInstanceAdditionalInformation() {
        return generalSettings.getAdditionalInformation();
    }

}
