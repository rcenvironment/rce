/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.internal;

import java.io.File;

import de.rcenvironment.core.component.integration.CommonToolIntegratorComponent;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;

/**
 * Implementation of {@link ToolIntegrationContext} for the standard tool integration.
 * 
 * @author Sascha Zur
 */
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
    public String getComponentGroupId() {
        return ToolIntegrationConstants.DEFAULT_COMPONENT_GROUP_ID;
    }

    protected void bindConfigurationService(final ConfigurationService configServiceIn) {
        configService = configServiceIn;
    }

    @Override
    public String[] getDisabledIntegrationKeys() {
        return new String[] {};
    }

}
