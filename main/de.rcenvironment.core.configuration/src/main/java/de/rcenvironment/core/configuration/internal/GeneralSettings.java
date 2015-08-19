/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;

/**
 * Common instance settings; typically read from the "general/" configuration subtree.
 * 
 * @author Robert Mischke
 */
class GeneralSettings {

    private String instanceName;

    private boolean isWorkflowHost;

    private boolean isRelay;

    private String tempDirectoryOverride;

    public GeneralSettings(ConfigurationSegment configurationSegment) {
        instanceName = configurationSegment.getString("instanceName", ConfigurationService.DEFAULT_INSTANCE_NAME_VALUE);
        isWorkflowHost = configurationSegment.getBoolean("isWorkflowHost", false);
        isRelay = configurationSegment.getBoolean("isRelay", false);
        tempDirectoryOverride = configurationSegment.getString("tempDirectory");
        resolveInstanceNamePlaceholders();
    }

    public String getInstanceName() {
        return instanceName;
    }

    public boolean getIsWorkflowHost() {
        return isWorkflowHost;
    }

    public boolean getIsRelay() {
        return isRelay;
    }

    public String getTempDirectoryOverride() {
        return tempDirectoryOverride;
    }

    private void resolveInstanceNamePlaceholders() {
        instanceName = instanceName.replace(ConfigurationService.CONFIGURATION_PLACEHOLDER_SYSTEM_USER_NAME,
            System.getProperty(ConfigurationServiceImpl.SYSTEM_PROPERTY_USER_NAME));

        if (instanceName.contains("${hostName}")) {
            try {
                instanceName = instanceName.replace(ConfigurationService.CONFIGURATION_PLACEHOLDER_HOST_NAME,
                    InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                LogFactory.getLog(getClass()).warn("Failed to determine the local host name", e);
            }
        }
    }

}
