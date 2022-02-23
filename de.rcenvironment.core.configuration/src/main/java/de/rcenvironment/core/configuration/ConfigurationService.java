/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.configuration.internal.UnpackedFilesDirectoryResolver;

/**
 * Service that can be used to retrieve configuration values, based on simple Java POJO mapping.
 * 
 * @author Heinrich Wendel
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Doreen Seider
 * @author Kathrin Schaffert
 */
public interface ConfigurationService {

    /**
     * A system property to override the location of installation-provided configuration and integration directories. The default value is
     * the parent directory of ${osgi.install.area}. Any override value must be an absolute path that points to an existing directory.
     */
    String SYSTEM_PROPERTY_INSTALLATION_DATA_ROOT_OVERRIDE = "rce.installationData.rootDir";

    /**
     * A system property to override the location of the optional system configuration and integration directories used by all users and
     * their instances. The default value is empty. Any override value must be an absolute path that points to an existing directory.
     */
    // TODO actually use this
    String SYSTEM_PROPERTY_SHARED_SETTINGS_ROOT_OVERRIDE = "rce.sharedSettings.rootDir";

    /**
     * A system property to override the location of the optional cross-instance user configuration and integration directories. The default
     * value is a directory named "common" inside the parent profile directory (see
     * {@link BootstrapConfiguration#SYSTEM_PROPERTY_PROFILES_PARENT_DIRECTORY_OVERRIDE}. Any override value must be an absolute path that
     * points to an existing directory.
     */
    String SYSTEM_PROPERTY_USER_SETTINGS_ROOT_OVERRIDE = "rce.userSettings.rootDir";

    /**
     * A placeholder in certain configuration properties that is resolved to the Java "user.name" property.
     */
    String CONFIGURATION_PLACEHOLDER_SYSTEM_USER_NAME = "${systemUser}";

    /**
     * A placeholder in certain configuration properties that is resolved to the local system's host name.
     */
    String CONFIGURATION_PLACEHOLDER_HOST_NAME = "${hostName}";

    /**
     * A placeholder in certain configuration properties that is resolved to the last part of the current profile's file system path.
     */
    String CONFIGURATION_PLACEHOLDER_PROFILE_NAME = "${profileName}";

    /**
     * A placeholder in certain configuration properties that is resolved to the build id.
     */
    String CONFIGURATION_PLACEHOLDER_VERSION = "${version}";

    /**
     * A placeholder in certain configuration properties that is resolved to the system's Java Version.
     */
    String CONFIGURATION_PLACEHOLDER_JAVA_VERSION = "${javaVersion}";

    /**
     * A placeholder in certain configuration properties that is resolved to the operating system name.
     */
    String CONFIGURATION_PLACEHOLDER_SYSTEM_NAME = "${systemName}";

    /**
     * Default value for configuration key "general/instanceName".
     */
    String DEFAULT_INSTANCE_NAME_VALUE = "Unnamed instance started by \"${systemUser}\" on ${hostName}";

    /**
     * Default path for the parent temp directory on Windows, relative to the system temp directory.
     */
    String DEFAULT_PARENT_TEMP_DIRECTORY_RELATIVE_PATH_WINDOWS = "rce-temp";

    /**
     * Default path for the parent temp directory on Linux, relative to the system temp directory. As /tmp is typically shared between
     * users, the current user' login name is added to avoid the problem that the first user creating the parent directory is set as its
     * owner - misc_ro, Nov 2014
     */
    String DEFAULT_PARENT_TEMP_DIRECTORY_RELATIVE_PATH_LINUX = "rce-temp-" + CONFIGURATION_PLACEHOLDER_SYSTEM_USER_NAME;

    /**
     * Standard Java "user.home" property.
     */
    String SYSTEM_PROPERTY_USER_HOME = "user.home";

    /**
     * Standard Java "user.name" property.
     */
    String SYSTEM_PROPERTY_USER_NAME = "user.name";

    /**
     * Standard Java "java.version" property.
     */
    String SYSTEM_PROPERTY_JAVA_VERSION = "java.version";

    /**
     * Standard Java "os.name" property.
     */
    String SYSTEM_PROPERTY_OS_NAME = "os.name";

    /**
     * Identifies various directories/locations that are affected by the chosen profile directory, or other configuration settings.
     * 
     * Use {@link ConfigurationService#getConfigurablePath(ConfigurablePathId)} to resolve them to actual locations.
     * 
     * @author Robert Mischke
     */
    enum ConfigurablePathId {
        /**
         * The directory where installation-provided files (including possible "configuration" and "integration" directories) are located.
         */
        INSTALLATION_DATA_ROOT,

        /**
         * The directory where the optional cross-instance user "configuration" and "integration" directories may be located.
         */
        SHARED_USER_SETTINGS_ROOT,

        /**
         * The current instance's root profile directory; for the default profile, this is "~/.rce/default".
         */
        PROFILE_ROOT,

        /**
         * The configuration directory of the current instance/profile; default: "<PROFILE_ROOT>/configuration".
         */
        PROFILE_CONFIGURATION_DATA,

        /**
         * The integration directory of the current instance/profile; default: "<PROFILE_ROOT>/integration".
         */
        PROFILE_INTEGRATION_DATA,

        /**
         * The data management storage root directory of the current instance/profile; default: "<PROFILE_ROOT>/storage".
         */
        PROFILE_DATA_MANAGEMENT,

        /**
         * The current instance's root directory for internal data (which is not supposed to be edited or inspected by the user, and
         * therefore does not need to be externally documented); default: "<PROFILE_ROOT>/internal".
         */
        PROFILE_INTERNAL_DATA,

        /**
         * The output directory of the current instance/profile; default: "<PROFILE_ROOT>/output".
         */
        PROFILE_OUTPUT,

        /**
         * The current instance's default location to write configuration files to. Currently always an alias for
         * {@link #PROFILE_CONFIGURATION_DIR}.
         */
        // TODO >= 6.0.0 review: what is this actually used for anymore? - misc_ro
        DEFAULT_WRITEABLE_CONFIGURATION_ROOT,

        /**
         * The current instance's default location to write integration files to. Currently always an alias for
         * {@link #PROFILE_INTEGRATION_DIR}.
         */
        DEFAULT_WRITEABLE_INTEGRATION_ROOT,

        /**
         * The location where the example configuration.json.*.sample files are stored.
         */
        CONFIGURATION_SAMPLES_LOCATION
    }

    /**
     * Identifies lists of directories/locations that are affected by the chosen profile directory, or other configuration settings.
     * 
     * Use {@link ConfigurationService#getConfigurablePathList(ConfigurablePathListId)} to resolve them to actual locations.
     * 
     * @author Robert Mischke
     */
    enum ConfigurablePathListId {
        /**
         * The current instance's existing locations for reading configuration data from.
         */
        READABLE_CONFIGURATION_DIRS,

        /**
         * The current instance's existing locations for reading integration data from.
         */
        READABLE_INTEGRATION_DIRS,

        /**
         * The current instance's existing locations for reading JDBC driver jars from.
         */
        JDBC_DRIVER_DIRS,
    }

    /**
     * Registers key-value pairs for variable substitution in configuration data (usually, configuration files). The given properties are
     * added to previously-registered properties. All subsequent {@link #getConfiguration(String, Class)} calls will use the registered
     * properties, usually by applying them to the configuration data before the calling bundle processes its content. Therefore, all
     * variables must be registered before the target bundle is configured.
     * 
     * @param namespace a qualifier for the given properties; how this qualifier is used depends on the concrete
     *        {@link ConfigurationService} implementation
     * @param properties the property map to add to the existing set of properties
     */
    void addSubstitutionProperties(String namespace, Map<String, String> properties);

    /**
     * Retrieves the configuration for the given identifier as Java type of clazz. Usage: CustomType config =
     * getConfiguration("de.rcenvironment.bundle", CustomType.class);
     * 
     * @param identifier The identifier to retrieve the configuration for.
     * @param clazz Type of the object to return.
     * @param <T> Type of the object to return.
     * @return A custom configuration object.
     */
    @Deprecated
    <T> T getConfiguration(String identifier, Class<T> clazz);

    /**
     * Fetches a part or all of the current configuration data.
     * 
     * @param relativePath a path that defines the sub-tree to fetch, with hierarchy levels separated by slashes (example:
     *        "network/ipFilter"); an empty string fetches the whole configuration
     * @return the defined {@link ConfigurationSegment}, or null if there is no configuration data at the given path
     */
    ConfigurationSegment getConfigurationSegment(String relativePath);

    /**
     * Retrieves, and if it does not exist yet, creates the object node at the given configuration path. The returned
     * {@link WritableConfigurationSegment} can then be used to add, edit, or remove properties or elements.
     * 
     * @param relativePath a path that defines the sub-tree to fetch, with hierarchy levels separated by slashes (example:
     *        "network/ipFilter"); an empty string fetches the whole configuration
     * @return the {@link WritableConfigurationSegment}
     * @throws ConfigurationException if there was an error fetching or creating the object node
     */
    WritableConfigurationSegment getOrCreateWritableConfigurationSegment(String relativePath) throws ConfigurationException;

    /**
     * Writes all changes made since the last {@link #reloadConfiguration()} call to the configuration file.
     * 
     * @throws ConfigurationException on configuration data errors
     * @throws IOException on file I/O errors (e.g. a read-only configuration file)
     */
    void writeConfigurationChanges() throws ConfigurationException, IOException;

    /**
     * Temporary method for 6.0.0 to force reloading, until configuration writing and file watching (for external changes) is implemented.
     */
    void reloadConfiguration();

    /**
     * Allows custom code to read JSON configuration files using the standard {@link ConfigurationSegment} API..
     * 
     * @param path the {@link Path} to the file to read
     * @return the JSON contents of the file, parsed into a {@link ConfigurationSegment}
     * @throws IOException on I/O errors, or if the file does not exist
     */
    ConfigurationSegment loadCustomConfigurationFile(Path path) throws IOException;

    // Object getConfigurationWriteLock();

    // WritableConfigurationSegment getWritableConfigurationSegment(String relativePath);

    // void updateConfiguration(WritableConfigurationSegment configurationSegment);

    /**
     * Resolves a path relative to the configuration folder of this bundle to an absolute one. If it is already an absolute one it will be
     * returned as it is.
     * 
     * @param identifier The bundleSymbolicName
     * @param path The path to convert.
     * @return The absolute path.
     */
    String resolveBundleConfigurationPath(String identifier, String path);

    /**
     * Returns the configured display name of the local node.
     * 
     * @return the host.
     */
    String getInstanceName();

    /**
     * Determines whether this node may act as a "server", which means that it may provide components, act as a workflow controller, allow
     * remote access etc.
     * 
     * @return true if this node is configured to be a workflow host
     */
    boolean getIsWorkflowHost();

    /**
     * Returns the configured location coordinates, which must be latitude and longitude values.
     * 
     * @return 2 dim array of [lat, long]
     */
    double[] getLocationCoordinates();

    /**
     * Returns the name of the location the instance is at.
     * 
     * @return name, e.g. the institute or city.
     */
    String getLocationName();

    /**
     * Returns contact information for this instance.
     * 
     * @return contact
     */
    String getInstanceContact();

    /**
     * 
     * Returns some addition information of this instance.
     * 
     * @return information text.
     */
    String getInstanceAdditionalInformation();

    /**
     * Determines whether this node acts as a "relay", which means that when other nodes connect to this node, they are connected to a
     * single logical network through this node; in other words, their logical networks are connected to one, larger network.
     * 
     * @return true if this node is configured to be a relay
     */
    boolean getIsRelay();

    /**
     * Returns the directory representing the home of the local RCE instance; by default, this is created inside of "${user.home}/.rce".
     * 
     * @return the directory
     */
    File getProfileDirectory();

    /**
     * Returns the location of the current profile's main configuration file, for example for presenting it to the user (e.g. for manual
     * editing), or exporting it.
     * 
     * @return the configuration file's location; usually, it should exist, but this is not strictly guaranteed
     */
    File getProfileConfigurationFile();

    /**
     * Determines if the configured "instance data" directory was used.
     * 
     * @return <code>true</code> if {@link #isIntendedProfileDirectorySuccessfullyLocked()} and
     *         {@link #hasIntendedProfileDirectoryValidVersion()} return <code>true</code>
     */
    boolean isUsingIntendedProfileDirectory();

    /**
     * Determines whether the version of the configured "instance data" directory is valid, i.e., it is <= the current one.
     * 
     * @return <code>true</code> if the configured "instance data" has a version number <= the current one
     */
    boolean hasIntendedProfileDirectoryValidVersion();

    /**
     * Determines if the configured "instance data" directory was successfully locked; if this is the case, it is reserved for exclusive
     * usage by this current instance.
     * 
     * @return <code>true</code> if the configured "instance data" was successfully locked
     */
    boolean isIntendedProfileDirectorySuccessfullyLocked();

    /**
     * Returns the directory that was original configured to be used as the profile directory.
     * 
     * IMPORTANT: note that this MAY NOT be the directory that is actually being used, as it may have been locked by another instance! Use
     * {@link #getProfileDirectory()} to get the active directory for this instance instead.
     * 
     * @return the directory
     */
    File getOriginalProfileDirectory();

    /**
     * @return the actual location for the given symbolic path/directory id
     * 
     * @param pathId the path identifier to resolve
     */
    File getConfigurablePath(ConfigurablePathId pathId);

    /**
     * @return a list of resolved paths for the given symbolic id; typically used for locating read-only files that can be specified at the
     *         system, user or instance level
     * 
     * @param pathListId the id specifying the list to return
     */
    File[] getConfigurablePathList(ConfigurablePathListId pathListId);

    /**
     * Attempts to create and return a sub-directory of the given configurable path. To indicate unexpected failure, this method can throw a
     * {@link RuntimeException}, as this is not expected to happen normally (ie, usually because of a coding or validation error).
     * 
     * @return the initialized sub-directory; guaranteed to exist if no exception was thrown
     * 
     * @param pathId the path identifier to start from
     * @param relativePath the relative path from the resolved configurable path id
     */
    File initializeSubDirInConfigurablePath(ConfigurablePathId pathId, String relativePath);

    /**
     * @param subdir the requested subdirectory within the profile's import directory
     * @return the path of the profile's standard import directory for the given subdir part (currently, this is &lt;profile>/import/xy for
     *         subdir value "xy"); no check is made whether this directory actually exists
     */
    File getStandardImportDirectory(String subdir);

    /**
     * Returns the *parent* temporary directory root. Inside of this, the *instance* temporary directory root is created on startup.
     * <p>
     * NOTE: This method is only intended for the central management and cleanup of temporary directories. It should NOT be used by other
     * code.
     * 
     * @return the parent root; it is guaranteed to exist when this service is activated
     */
    File getParentTempDirectoryRoot();

    /**
     * 
     * Determines if the configuration file could not be found, or it could not be loaded and instead the default configuration file is
     * loaded.
     * 
     * @return true if the default configuration file is used.
     */
    boolean isUsingDefaultConfigurationValues();

    /**
     * Attempts to resolve the given fileset id to a directory containing unpacked files (which should always be considered read-only).
     * Refer to the {@link UnpackedFilesDirectoryResolver} JavaDoc for details of the resolution process.
     * 
     * @param filesetId the id of the fileset to find
     * @return the (potentially read-only) directory containing the unpacked files on lookup success
     * @throws ConfigurationException on lookup failure
     */
    File getUnpackedFilesLocation(String filesetId) throws ConfigurationException;

}
