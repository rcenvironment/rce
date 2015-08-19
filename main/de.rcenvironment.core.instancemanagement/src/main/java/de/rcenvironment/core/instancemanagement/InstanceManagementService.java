/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.IOException;
import java.util.Map;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A service to set up, start, query and stop RCE instances from a running instance. Intended for multi-instance deployments (either in
 * production or testing), and automated test setups.
 * 
 * @author Robert Mischke
 */
public interface InstanceManagementService {

    /**
     * Specifies the behavior of the "install" operation regarding version checking, downloading and/or reinstallation.
     * 
     * @author Robert Mischke
     */
    // TODO split into download and installation policy enums?
    public enum InstallationPolicy {
        /**
         * If an installation exists at the given location, do nothing.
         */
        ONLY_INSTALL_IF_NOT_PRESENT,
        /**
         * If an installation exists at the given location, check which version the given URL points to, and reinstall if it is different
         * than the version of the installation.
         */
        IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT,
        /**
         * Check which version the given URL points to, download it if it is not already in the cache, and then always (re-)install it,
         * regardless of the installed version.
         */
        FORCE_REINSTALL,
        /**
         * Always download the installation package (even if it exists in the cache) and (re-)install it.
         */
        FORCE_NEW_DOWNLOAD_AND_REINSTALL
    }

    /**
     * Configures the given instance (ie, profile) from the template specified by "templateId", using the provided property map for
     * configuration.
     * 
     * TODO check whether a property map is the most useful format here
     * 
     * @param templateId the sub-folder of the root templates folder that contains the profile template to use
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to use
     * @param properties the substitution properties
     * @param deleteOtherFiles is true, the instance/profile directory will be cleaned before writing the new configuration
     * @throws IOException on configuration or I/O errors
     */
    void configureInstanceFromTemplate(String templateId, String instanceId, Map<String, String> properties, boolean deleteOtherFiles)
        throws IOException;

    /**
     * Attempts to start the profile specified by "instanceId" with the installation specified by "installationId".
     * 
     * @param installationId the sub-folder of the root installation folder that contains the installation to use
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to use
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @throws IOException on startup failure
     */
    void startinstance(String installationId, String instanceId, TextOutputReceiver userOutputReceiver) throws IOException;

    /**
     * Attempts to stop/shutdown the profile specified by "instanceId".
     * 
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to shut down
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * 
     * @throws IOException on shutdown failure
     */
    void stopInstance(String instanceId, TextOutputReceiver userOutputReceiver) throws IOException;
    
    
    /**
     * List information about instances, installations or templates.
     * 
     * @param scope the scope of the command, i.e. instances, installations or templates
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * 
     * @throws IOException on information retrieving failure
     */
    void listInstanceManagementInformation(String scope, TextOutputReceiver userOutputReceiver) throws IOException;

    /**
     * Tests whether the the profile specified by "instanceId" is locked, ie in use.
     * 
     * @return true if the profile is locked/in use
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to check
     * @throws IOException on test/query failure
     */
    boolean isInstanceRunning(String instanceId) throws IOException;

    /**
     * Performs all supported types of download-and-install operations, configured by the "policy" parameter(s).
     * 
     * @param installationId the installation to (re-)install
     * @param urlQualifier the version part to insert into the configured URL template
     * @param installationPolicy the download/installation policy
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @throws IOException on I/O errors
     */
    void setupInstallationFromUrlQualifier(String installationId, String urlQualifier, InstallationPolicy installationPolicy,
        TextOutputReceiver userOutputReceiver) throws IOException;

    /**
     * Attempts to dispose the profile specified by "instanceId".
     * 
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to dispose
     * @param outputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * 
     * @throws IOException on disposal failure
     */
    void disposeInstance(String instanceId, TextOutputReceiver outputReceiver) throws IOException;
    
    
    /**
     * Provides information about the instance management.
     * 
     * @param outputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     */
    void showInstanceManagementInformation(TextOutputReceiver outputReceiver);

    // map: state -> list of instanceIds
    // Map<String, List<String>> listinstances(boolean onlyRunning) throws IOException;

    // profile, template, installation ids
    // boolean validateId(String id);
}
