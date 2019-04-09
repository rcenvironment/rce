/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.testutils;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;

/**
 * A stub implementation of {@link ConfigurationService} that provides typical test use cases, e.g. providing a test configuration to a
 * service that expects a {@link ConfigurationService} as an injected dependency.
 * 
 * @author Robert Mischke
 */
public class TestConfigurationProvider extends ConfigurationServiceDefaultStub {

    private final Map<String, ConfigurationSegment> configurationSegments = new HashMap<>();

    /**
     * Adds a {@link ConfigurationSegment} to return from {@link #getConfigurationSegment(String)} for the same id. Useful for providing a
     * test configuration to a service that reads its configuration using the given configuration id.
     * 
     * @param relativePath the relative path within the configuration tree
     * @param segment the {@link ConfigurationSegment} to return; it is stored as a reference, ie no cloning is performed
     */
    public void setConfigurationSegment(String relativePath, ConfigurationSegment segment) {
        configurationSegments.put(relativePath, segment);
    }

    @Override
    public ConfigurationSegment getConfigurationSegment(String relativePath) {
        return configurationSegments.get(relativePath);
    }

}
