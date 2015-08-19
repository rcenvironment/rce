/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import java.io.File;
import java.util.Map;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;

/**
 * Service that can be used to retrieve configuration values, based on simple Java POJO mapping.
 * 
 * @author Heinrich Wendel
 * @author Robert Mischke
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
     * Standard OSGi "osgi.install.area" property.
     * 
     * Note that (contrary to what the name suggests) this is *not* the actual installation directory which, for example, the /plugin and
     * /feature directories are located in. Instead, this property points to the /configuration directory inside that installation
     * directory.
     */
    String SYSTEM_PROPERTY_OSGI_INSTALL_AREA = "osgi.install.area";

    /**
     * Identifies various directories/locations that are affected by the chosen profile directory, or other configuration settings.
     * 
     * Use {@link ConfigurationService#getConfigurablePath(ConfigurablePathId)} to resolve them to actual locations.
     * 
     * @author Robert Mischke
     */
    public enum ConfigurablePathId {
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
        // TODO >= 6.0.0 review: what is this actually used for anymore? - misc_ro
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
    public enum ConfigurablePathListId {
        /**
         * The current instance's existing locations for reading configuration data from.
         */
        READABLE_CONFIGURATION_DIRS,

        /**
         * The current instance's existing locations for reading integration data from.
         */
        READABLE_INTEGRATION_DIRS,
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
     * Temporary method for 6.0.0 to force reloading, until configuration writing and file watching (for external changes) is implemented.
     */
    void reloadConfiguration();

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
     * Determines if the configured "instance data" directory was successfully locked; if this is the case, it is reserved for exclusive
     * usage by this current instance.
     * 
     * @return true if the configured "instance data" was successfully locked
     */
    boolean isUsingIntendedProfileDirectory();

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
     * Returns the *parent* temporary directory root. Inside of this, the *instance* temporary directory root is created on startup.
     * <p>
     * NOTE: This method is only intended for the central management and cleanup of temporary directories. It should NOT be used by other
     * code.
     * 
     * @return the parent root; it is guaranteed to exist when this service is activated
     */
    File getParentTempDirectoryRoot();

}
