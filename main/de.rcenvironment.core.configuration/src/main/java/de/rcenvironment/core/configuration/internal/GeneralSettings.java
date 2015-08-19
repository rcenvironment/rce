/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

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
    }

    public String getRawInstanceName() {
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

}
