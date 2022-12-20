/*
 * Copyright 2006-2022 DLR, Germany
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

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.CommonToolIntegratorComponent;
import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;

/**
 * Implementation of {@link IntegrationContext} for the standard tool integration.
 * 
 * @author Sascha Zur
 * @author Alexander Weinert (OSGI annotations)
 */
@Component
public final class CommonToolIntegrationContext implements IntegrationContext {

    private static ConfigurationService configService;

    @Override
    public String getContextId() {
        return ToolIntegrationConstants.COMMON_TOOL_INTEGRATION_CONTEXT_UID;
    }

    @Override
    public IntegrationContextType getContextType() {
        return IntegrationContextType.COMMON;
    }

    @Override
    public String getContextTypeString() {
        return IntegrationContextType.COMMON.toString();
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
        return ComponentConstants.COMMON_INTEGRATED_COMPONENT_ID_PREFIX;
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
