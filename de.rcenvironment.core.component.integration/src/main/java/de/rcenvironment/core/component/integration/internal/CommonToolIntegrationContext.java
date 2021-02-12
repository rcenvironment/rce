/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.component.integration.CommonToolIntegratorComponent;
import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;

/**
 * Implementation of {@link ToolIntegrationContext} for the standard tool integration.
 * 
 * @author Sascha Zur
 * @author Alexander Weinert (OSGI annotations)
 */
@Component
public final class CommonToolIntegrationContext implements ToolIntegrationContext {

    private static ConfigurationService configService;

    @Override
    public String getContextId() {
        return ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID;
    }

    @Override
    public String getContextType() {
        return ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_TYPE;
    }

    @Override
    public String getRootPathToToolIntegrationDirectory() {
        return configService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath();
    }

    @Override
    public File[] getReadOnlyPathsList() {
        return configService.getConfigurablePathList(ConfigurablePathListId.READABLE_INTEGRATION_DIRS);
    }

    @Override
    public String getNameOfToolIntegrationDirectory() {
        return "tools" + File.separator + "common";
    }

    @Override
    public String getToolDirectoryPrefix() {
        return "";
    }

    @Override
    public String getConfigurationFilename() {
        return "configuration.json";
    }

    @Override
    public String getImplementingComponentClassName() {
        return CommonToolIntegratorComponent.class.getCanonicalName();
    }

    @Override
    public String getPrefixForComponentId() {
        return ToolIntegrationConstants.STANDARD_COMPONENT_ID_PREFIX;
    }

    @Override
    public String getDefaultComponentGroupId() {
        return ToolIntegrationConstants.DEFAULT_COMPONENT_GROUP_ID;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected void bindConfigurationService(final ConfigurationService configServiceIn) {
        configService = configServiceIn;
    }

    @Override
    public String[] getDisabledIntegrationKeys() {
        return new String[] {};
    }

    @Override
    public Optional<ConfigurationMap> parseConfigurationMap(Map<String, Object> rawConfigurationMap) {
        return Optional.ofNullable(ConfigurationMap.fromMap(rawConfigurationMap));
    }

}
