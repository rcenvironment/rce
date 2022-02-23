/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.instancemanagement.internal.InstanceConfigurationException;
import de.rcenvironment.core.instancemanagement.internal.InstanceOperationException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * A service to set up, start, query and stop RCE instances from a running instance. Intended for multi-instance deployments (either in
 * production or testing), and automated test setups.
 * 
 * @author Robert Mischke
 * @author David Scholz
 * @author Brigitte Boden
 * @author Lukas Rosenbach
 * @author Marlon Schroeter
 */
public interface InstanceManagementService {

    /**
     * A special installation id denoting that the currently running ("master"/"host") installation should be used for the given operation.
     */
    String MASTER_INSTANCE_SYMBOLIC_INSTALLATION_ID = ":self";

    /**
     * An installation id prefix denoting that the rest of the installation id string denotes a path pointing to an unpacked installation
     * that should be used, instead of a previously registered installation.
     */
    String CUSTOM_LOCAL_INSTALLATION_PATH_INSTALLATION_ID_PREFIX = "local:";

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
     * Creates a new {@link InstanceConfigurationOperationSequence} to collect configuration change steps. Can be applied using
     * {@link #applyInstanceConfigurationOperations(String, InstanceConfigurationOperationSequence, TextOutputReceiver)} when done.
     * 
     * @return the new instance
     */
    InstanceConfigurationOperationSequence newConfigurationOperationSequence();

    /**
     * Configures the given instance (ie, profile) using the provided property map for configuration.
     * 
     * @param instanceId the sub-folder of the root profiles folder that contains the profile to use
     * @param changeSequence the sequence to append changes to.
     * @param userOutputReceiver the output receiver.
     * @throws InstanceConfigurationException on configuration specific failures.
     * @throws IOException on I/O specific failures.
     */
    void applyInstanceConfigurationOperations(String instanceId, InstanceConfigurationOperationSequence changeSequence,
        TextOutputReceiver userOutputReceiver) throws InstanceConfigurationException, IOException;

    /**
     * Attempts to start the profile specified by "instanceId" with the installation specified by "installationId".
     * 
     * @param installationId the sub-folder of the root installation folder that contains the installation to use
     * @param instanceIds the sub-folders of the root profile folders that contains the profiles to use
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @param startWithGui true if the instance shall be started with GUI.
     * @param commandArguments console arguments that are used to start the instance.
     * @throws InstanceOperationException on startup failure
     * @throws IOException on I/O specific failures.
     */
    void startInstance(String installationId, List<String> instanceIds, TextOutputReceiver userOutputReceiver, long timeout,
        boolean startWithGui, String commandArguments)
        throws InstanceOperationException, IOException;

    /**
     * Attempts to stop/shutdown the profile specified by "instanceId".
     * 
     * @param instanceIds the sub-folders of the root profile folders that contains the profiles to shut down
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * 
     * @throws InstanceOperationException on shutdown failure
     * @throws IOException on I/O specific failures.
     */
    void stopInstance(List<String> instanceIds, TextOutputReceiver userOutputReceiver, long timeout)
        throws InstanceOperationException, IOException;

    /**
     * 
     * Attempts to start all instance with a specified profile with the installation specified by "installationId".
     * 
     * @param installationId the sub-folder of the root installation folder that contains the installation to use
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @param commandArguments optional arguments that will be used to start the instances.
     * @throws InstanceOperationException if an instance has a startup failure
     * @throws IOException on I/O specific failures.
     */
    void startAllInstances(String installationId, TextOutputReceiver userOutputReceiver, long timeout, String commandArguments)
        throws InstanceOperationException, IOException;

    /**
     * 
     * Attempts to stop/shutdown all running instances.
     * 
     * @param userOutputReceiver an optional {@link TextOutputReceiver} to send user progress information to
     * @param installationId the sub-folder of the root installation folder that contains the installation. this optional paramater causes
     *        all running instances using this installation to shutdown.
     * @param timeout optional time for the command to be blocked. command will be canceled if time exceeds.
     * @throws InstanceOperationException on shutdown failure.
     * @throws IOException on I/O specific failures.
     */
    void stopAllInstances(String installationId, TextOutputReceiver userOutputReceiver, long timeout)
        throws InstanceOperationException, IOException;

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

    /**
     * Attempts to start a workflow on a managed instance via SSH. Will not wait for completion of the workflow.
     * {@link #executeCommandOnInstance(String, String, TextOutputReceiver) executeCommandOnInstance} it does not wait for the command to be
     * executed.
     * 
     * @param instanceId The instance on which the workflow will be started
     * @param workflowFileLocation path to the workflow file to be started
     * @param userOutputReceiver {@link TextOutputReceiver} where the output from the instance will be forwarded
     * @throws SshParameterException on invalid SSH parameters
     * @throws JSchException on SSH command execution errors
     * @throws IOException on on SSH command execution errors
     * @throws InterruptedException on SSH command execution errors
     * @return string array, containing info about started workflow. [0]:workflowName [1]:workflowLogDir [2]:workflowId
     */
    String[] startWorkflowOnInstance(String instanceId, Path workflowFileLocation, CapturingTextOutReceiver userOutputReceiver)
        throws JSchException, SshParameterException, IOException, InterruptedException;

    /**
     * Attempts to start a workflow using a placeholder file on a managed instance via SSH. Will not wait for completion of the workflow.
     * {@link #executeCommandOnInstance(String, String, TextOutputReceiver) executeCommandOnInstance} it does not wait for the command to be
     * executed.
     * 
     * @param instanceId The instance on which the workflow will be started
     * @param workflowFileLocation path to the workflow file to be started
     * @param placeholderFileLocation path to the placeholder file
     * @param userOutputReceiver {@link TextOutputReceiver} where the output from the instance will be forwarded
     * @throws SshParameterException on invalid SSH parameters
     * @throws JSchException on SSH command execution errors
     * @throws IOException on on SSH command execution errors
     * @throws InterruptedException on SSH command execution errors
     * @return string array, containing info about started workflow. [0]:workflowName [1]:workflowLogDir [2]:workflowId
     */
    String[] startWorkflowOnInstance(String instanceId, Path workflowFileLocation, Path placeholderFileLocation,
        CapturingTextOutReceiver userOutputReceiver) throws JSchException, SshParameterException, IOException, InterruptedException;

    /**
     * @param input the string to test
     * @return true if the given string is either a symbolic or otherwise special installation id, e.g.
     *         {@link #MASTER_INSTANCE_SYMBOLIC_INSTALLATION_ID} or beginning with
     *         {@link #CUSTOM_LOCAL_INSTALLATION_PATH_INSTALLATION_ID_PREFIX}
     */
    boolean isSpecialInstallationId(String input);

    /**
     * Checks if the instance management service was correctly started.
     * 
     * @return true if the instance management was correctly initiliazed (i.e. the config was correct)
     */
    boolean isInstanceManagementStarted();

    /**
     * @return Error message in case of not started instance management.
     */
    String getReasonInstanceManagementNotStarted();

    /**
     * @param instanceId the profile to use
     * @param relativePath the relative path within the profile directory
     * @return the absolute {@link File} pointing to the relative path within the given instance's profile directory; may or may not exist
     */
    File resolveRelativePathWithinProfileDirectory(String instanceId, String relativePath);

    /**
     * 
     * @param installationId the profile to use
     * @return version string of given installation
     * @throws IOException on I/O failure
     */
    String getVersionOfInstallation(String installationId) throws IOException;

    /**
     * @return root directory of IM installations
     */
    File getInstallationsRootDir();

    /**
     * @return directory in which IM downloads are cached
     */
    File getDownloadsCacheDir();

    // map: state -> list of instanceIds
    // Map<String, List<String>> listinstances(boolean onlyRunning) throws IOException;

    // profile, template, installation ids
    // boolean validateId(String id);
}
