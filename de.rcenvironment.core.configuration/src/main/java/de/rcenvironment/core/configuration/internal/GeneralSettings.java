/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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

    private double[] location;

    private String locationName;

    private String contact;

    private String additionalInformation;

    private String tempDirectoryOverride;

    GeneralSettings(ConfigurationSegment configurationSegment) {
        instanceName = configurationSegment.getString("instanceName", ConfigurationService.DEFAULT_INSTANCE_NAME_VALUE);
        isWorkflowHost = configurationSegment.getBoolean("isWorkflowHost", false);
        isRelay = configurationSegment.getBoolean("isRelay", false);
        tempDirectoryOverride = configurationSegment.getString("tempDirectory");
        location = new double[] { configurationSegment.getSubSegment("coordinates").getDouble("lat", 0.0),
            configurationSegment.getSubSegment("coordinates").getDouble("long", 0.0) };
        locationName = configurationSegment.getString("locationName", "");
        contact = configurationSegment.getString("contact", "");
        additionalInformation = configurationSegment.getString("information", "");
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

    public double[] getLocation() {
        return location;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getContact() {
        return contact;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

}
