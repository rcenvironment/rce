/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.instancemanagement.internal.ConfigurationChangeSequence;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * A service to set up, start, query and stop RCE instances from a running instance. Intended for multi-instance deployments (either in
 * production or testing), and automated test setups.
 * 
 * @author Robert Mischke
 * @author David Scholz
 */
public interface InstanceManagementService {

    /**
     * Specifies the behavior of the "install" operation regarding version checking, downloading and/or reinstallation.
     * 
     * @author Robert Mischke
     */
    // TODO split into download and installation policy enums?
    enum InstallationPolicy {
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
     * 
     * Maps config flag.
     * 
     * @author David Scholz
     */
    enum ConfigurationFlag {

        // TODO decide what to do with the parameter strings in this enum; they differ from the updated commands

        DUMMY(""),

        SET_NAME("--name"),

        ENABLE_RELAY("--enable-relay"),

        DISABLE_RELAY("--disable-relay"),

        ENABLE_WORKFLOWHOST("--enable-workflow-host"),

        DISABLE_WORKFLOWHOST("--disable-workflow-host"),

        SET_COMMENT("--add-comment"),

        TEMP_DIR("--set-temp-dir"),

        ENABLE_DEP_INPUT_TAB("--enable-deprecated-input-tab"),

        DISABLE_DEP_INPUT_TAB("--disable-deprecated-input-tab"),

        ENABLE_IP_FILTER("--enable-ip-filter"),

        DISABLE_IP_FILTER("--disable-ip-filter"),

        REQUEST_TIMEOUT("--set-request-timeout"),

        FORWARDING_TIMEOUT("--set-forwarding-timeout"),

        ADD_CONNECTION("--add-connection"),

        REMOVE_CONNECTION("--remove-connection"),

        ADD_SERVER_PORT("--add-server-port"),

        REMOVE_SERVER_PORT("--remove-server-port"),

        ADD_ALLOWED_IP("--add-allowed-ip"),

        REMOVE_ALLOWED_IP("--remove-connection"),

        ADD_SSH_CONNECTION("--add-ssh-connection"),

        REMOVE_SSH_CONNECTION("--remove-ssh-connection"),

        PUBLISH_COMPONENT("--publish-component"),

        UNPUBLISH_COMPONENT("--unpublish-component"),

        ENABLE_SSH_SERVER("--enable-ssh-server"),

        DISABLE_SSH_SERVER("--disable-ssh-server"),

        SET_SSH_SERVER_IP("--set-ssh-server-ip"),

        SET_SSH_SERVER_PORT("--set-ssh-server-port"),

        SET_BACKGROUND_MONITORING("--set-background-monitoring"),

        ENABLE_IM_SSH_ACCESS("--enable-im-ssh-access"),

        RESET_CONFIGURATION("--reset"),

        APPLY_TEMPLATE("--use-template");

        private final String flag;

        ConfigurationFlag(String flag) {
            this.flag = flag;
        }

        public String getFlag() {
            return flag;
        }

        public List<String> getAllFlags() {
            List<String> l = new ArrayList<String>();
            for (ConfigurationFlag possibleValue : this.getDeclaringClass().getEnumConstants()) {
                l.add(possibleValue.getFlag());
            }
            return l;
        }
    }

    /**
     * Configures the given instance (ie, profile) using the provided property map for configuration.
     * 
     * TODO check whether a property map is the most useful format here
     * 
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to use
     * @param changeSequence the sequence to append changes to.
     * @param userOutputReceiver the output receiver.
     * @throws IOException on configuration or I/O errors
     */
    void configureInstance(String instanceId, ConfigurationChangeSequence changeSequence, TextOutputReceiver userOutputReceiver)
        throws IOException;

    /**
     * Attempts to start the profile specified by "instanceId" with the installation specified by "installationId".
     * 
     * @param installationId the sub-folder of the root installation folder that contains the installation to use
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to use
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @param startWithGui true if the instance shall be started with GUI.
     * @throws IOException on startup failure
     */
    void startInstance(String installationId, List<String> instanceId, TextOutputReceiver userOutputReceiver, long timeout, 
        boolean startWithGui)
        throws IOException;

    /**
     * Attempts to stop/shutdown the profile specified by "instanceId".
     * 
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to shut down
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * 
     * @throws IOException on shutdown failure
     */
    void stopInstance(List<String> instanceId, TextOutputReceiver userOutputReceiver, long timeout) throws IOException;

    /**
     * 
     * Attempts to start all instance with a specified profile with the installation specified by "installationId".
     * 
     * @param installationId the sub-folder of the root installation folder that contains the installation to use
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @throws IOException if an instance has a startup failure
     */
    void startAllInstances(String installationId, TextOutputReceiver userOutputReceiver, long timeout) throws IOException;

    /**
     * 
     * Attempts to stop/shutdown all running instances.
     * 
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param installationId the sub-folder of the root installation folder that contains the installation. this optional paramater causes
     *        all running instances using this installation to shutdown.
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @throws IOException on shutdown failure
     */
    void stopAllInstances(String installationId, TextOutputReceiver userOutputReceiver, long timeout) throws IOException;

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
     * @param timeout timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @throws IOException on I/O errors
     */
    void setupInstallationFromUrlQualifier(String installationId, String urlQualifier, InstallationPolicy installationPolicy,
        TextOutputReceiver userOutputReceiver, long timeout) throws IOException;
    
    /**
     * Stops all instances running the given installation, replaces the installation and restarts the instances.
     * 
     * @param installationId the installation to (re-)install
     * @param urlQualifier the version part to insert into the configured URL template
     * @param installationPolicy the download/installation policy
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @throws IOException on I/O errors
     */
    void reinstallFromUrlQualifier(String installationId, String urlQualifier, InstallationPolicy installationPolicy,
        TextOutputReceiver userOutputReceiver, long timeout) throws IOException;

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

    /**
     * Attempts to execute a command on a managed instance via SSH.
     * 
     * @param instanceId The instance on which the command will be executed
     * @param command The command to execute
     * @param userOutputReceiver {@link TextOutputReceiver} where the output from the instance will be forwarded
     * @throws SshParameterException on invalid SSH parameters
     * @throws JSchException on SSH command execution errors
     * @throws IOException on on SSH command execution errors
     * @throws InterruptedException on SSH command execution errors
     */
    void executeCommandOnInstance(String instanceId, String command, TextOutputReceiver userOutputReceiver) throws JSchException,
        SshParameterException, IOException, InterruptedException;

    // map: state -> list of instanceIds
    // Map<String, List<String>> listinstances(boolean onlyRunning) throws IOException;

    // profile, template, installation ids
    // boolean validateId(String id);
}
