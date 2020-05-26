/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * Represents an instance (ie, a profile) managed by these test steps.
 * 
 * @author Robert Mischke
 * @author Marlon Schroeter 
 */
public final class ManagedInstance {

    private final String id; // the id of the instance/profile

    private String installationId; // the id of the installation to run this instance/profile with

    private Map<String, Integer> serverPorts = new HashMap<String, Integer>();

    private final List<String> configuredAutostartConnectionIds = new ArrayList<>();

    private String lastCommandOutput;

    // cache to avoid I/O on multiple check operations; only used while instance is stopped, and reset on startup
    private Map<String, String> cachedFileContent = new HashMap<>();

    private boolean potentiallyRunning;

    private final InstanceManagementService instanceManagementService;

    private final Log log = LogFactory.getLog(getClass());

    public ManagedInstance(String instanceId, String installationId, InstanceManagementService instanceManagementService) {
        this.id = instanceId;
        this.installationId = installationId;
        this.instanceManagementService = instanceManagementService;
    }

    @Override
    public String toString() {
        return getId();
    }

    public String getId() {
        return id;
    }

    public synchronized String getInstallationId() {
        return installationId;
    }

    @SuppressWarnings("unused") // for potential future use
    public synchronized void setInstallationId(String installationId) {
        this.installationId = installationId;
    }

    /**
     * @param connectionType one of the possible connection types
     * @param serverNumber number of the server of this connection type (not the serverPort)
     * @return the serverPort associated with connection type and serverNumber. Null if none is specified yet.
     */
    public synchronized Integer getServerPort(String connectionType, int serverNumber) {
        if (connectionType.equals(StepDefinitionConstants.CONNECTION_TYPE_SSH)
            || connectionType.equals(StepDefinitionConstants.CONNECTION_TYPE_UPLINK)) {
            // currently there is only one ssh server supported and the server for ssh and uplink is handled the same way.
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_SSH;
            serverNumber = 0;
        }
        return serverPorts.get(connectionType + serverNumber);
    }

    /**
     * @param connectionType one of the possible connection types
     * @param serverNumber number of the server of this connection type (not the serverPort)
     * @param serverPort server port to be set
     */
    public synchronized void setServerPort(String connectionType, int serverNumber, Integer serverPort) {
        serverPorts.put(connectionType + serverNumber, serverPort);
    }

    /**
     * @return the internal mutable list; not a copy!
     */
    public List<String> accessConfiguredAutostartConnectionIds() {
        return configuredAutostartConnectionIds; // the list reference itself is immutable
    }

    public synchronized String getLastCommandOutput() {
        return lastCommandOutput;
    }

    public synchronized void setLastCommandOutput(String lastCommandOutput) {
        this.lastCommandOutput = lastCommandOutput;
    }

    /**
     * Loads the content of a file location in a test instance's profile.
     * 
     * @param relativePath the relative path within the profile
     * @param forceReload whether to disable content caching
     * @return the file's content, or null if the file does not exist
     * @throws IOException on I/O errors; note that absence of the target file is not an error
     */
    public synchronized String getProfileRelativeFileContent(String relativePath, boolean forceReload) throws IOException {
        if (!potentiallyRunning) {
            if (cachedFileContent.containsKey(relativePath)) {
                return cachedFileContent.get(relativePath); // may be null if file does not exist
            }
        } else {
            log.warn("Requested file " + relativePath + " of running instance " + id + "; not using I/O cache");
        }

        final File fileLocation = instanceManagementService.resolveRelativePathWithinProfileDirectory(id, relativePath);
        final String content;
        if (!fileLocation.exists()) {
            content = null;
        } else {
            content = FileUtils.readFileToString(fileLocation, "UTF-8"); // no other information available; assume UTF8
        }

        if (!potentiallyRunning) {
            cachedFileContent.put(relativePath, content); // content may be null if file is missing
        }
        return content;
    }

    /**
     * @param relativePath A path relative to the profile directory of this instance
     * @return A file representing the absolute path to the given relative path
     */
    public synchronized File getAbsolutePathFromRelative(String relativePath) {
        return instanceManagementService.resolveRelativePathWithinProfileDirectory(id, relativePath);
    }

    /**
     * Must be called before starting the instance.
     */
    public synchronized void onStarting() {
        potentiallyRunning = true;
        cachedFileContent.clear();
    }

    /**
     * Must be called after stopping the instance.
     */
    public synchronized void onStopped() {
        potentiallyRunning = false;
    }

    public synchronized boolean getPotentiallyRunning() {
        return potentiallyRunning;
    }
}
