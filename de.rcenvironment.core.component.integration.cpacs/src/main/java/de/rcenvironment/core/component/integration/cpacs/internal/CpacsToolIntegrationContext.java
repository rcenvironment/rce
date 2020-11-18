/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.cpacs.internal;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.component.integration.cpacs.CpacsToolIntegrationConstants;
import de.rcenvironment.core.component.integration.cpacs.CpacsToolIntegratorComponent;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;

/**
 * Implementation of {@link ToolIntegrationContext} for the CPACS tool integration.
 * 
 * @author Jan Flink
 * @author Alexander Weinert (OSGI annotations)
 */
@Component
public final class CpacsToolIntegrationContext implements ToolIntegrationContext {

    private static ConfigurationService configService;

    @Override
    public String getContextId() {
        return CpacsToolIntegrationConstants.CPACS_TOOL_INTEGRATION_CONTEXT_UID;
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
        return "tools" + File.separator + "cpacs";
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
        return CpacsToolIntegratorComponent.class.getCanonicalName();
    }

    @Override
    public String getPrefixForComponentId() {
        return CpacsToolIntegrationConstants.CPACS_COMPONENT_ID_PREFIX;
    }

    @Override
    public String getDefaultComponentGroupId() {
        return ToolIntegrationConstants.DEFAULT_COMPONENT_GROUP_ID;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, name = "Configuration Service")
    protected void bindConfigurationService(final ConfigurationService configServiceIn) {
        configService = configServiceIn;
    }

    @Override
    public String getContextType() {
        return "CPACS";
    }

    @Override
    public String[] getDisabledIntegrationKeys() {
        return new String[] { ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER,
            ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR };
    }

    @Override
    public Optional<ConfigurationMap> parseConfigurationMap(Map<String, Object> rawConfigurationMap) {
        return Optional.ofNullable(ConfigurationMap.fromMap(rawConfigurationMap));
    }

}
