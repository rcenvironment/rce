/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cluster.execution;

import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * 
 * Provides configuration of this bundle and initializes default configuration.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ClusterComponentConfiguration {

    private static final int DEFAULT_MAX_CHANNELS = 8;

    private int maxChannels = DEFAULT_MAX_CHANNELS;

    public ClusterComponentConfiguration(ConfigurationSegment configurationSegment) {
        // TODO 6.0.0 review property name
        maxChannels = configurationSegment.getInteger("maximumChannels", DEFAULT_MAX_CHANNELS);
    }

    public int getMaxChannels() {
        return maxChannels;
    }

    public void setMaxChannels(int maxChannels) {
        this.maxChannels = maxChannels;
    }

}
