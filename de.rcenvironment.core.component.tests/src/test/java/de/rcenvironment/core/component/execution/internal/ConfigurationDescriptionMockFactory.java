/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import org.easymock.EasyMock;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;

/**
 * Create {@link ConfigurationDescription} mocks.
 * 
 * @author Doreen Seider
 */
public final class ConfigurationDescriptionMockFactory {

    private ConfigurationDescriptionMockFactory() {}

    /**
     * Creates mock for a {@link ConfigurationDescription} instance that only contains a configuration value regarding manual output
     * approval (verification).
     * 
     * @param outputApprovalRequired <code>true</code> if manual output approval is required, otherwise <code>false</code>
     * @param imitationMode <code>true</code> if in tool run imitation mode, otherwise <code>false</code>
     * @return {@link ConfigurationDescription} mock
     */
    public static ConfigurationDescription createConfigurationDescriptionMock(boolean outputApprovalRequired, boolean imitationMode) {
        ReadOnlyConfiguration readOnlyConfigMock = EasyMock.createStrictMock(ReadOnlyConfiguration.class);
        EasyMock.expect(readOnlyConfigMock.getValue(ComponentConstants.COMPONENT_CONFIG_KEY_REQUIRES_OUTPUT_APPROVAL))
            .andStubReturn(String.valueOf(outputApprovalRequired));
        EasyMock.replay(readOnlyConfigMock);

        ConfigurationDefinition configDefMock = EasyMock.createStrictMock(ConfigurationDefinition.class);
        EasyMock.expect(configDefMock.getReadOnlyConfiguration()).andStubReturn(readOnlyConfigMock);
        EasyMock.replay(configDefMock);

        ConfigurationDescription configDescMock = EasyMock.createStrictMock(ConfigurationDescription.class);
        EasyMock.expect(configDescMock.getConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE))
            .andStubReturn(Boolean.toString(imitationMode));
        EasyMock.expect(configDescMock.getComponentConfigurationDefinition()).andStubReturn(configDefMock);
        EasyMock.replay(configDescMock);
        return configDescMock;
    }
}
