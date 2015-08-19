/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.component.integration.internal;

import java.io.File;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationContext;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;
import de.rcenvironment.cpacs.component.integration.CpacsToolIntegrationConstants;
import de.rcenvironment.cpacs.component.integration.CpacsToolIntegratorComponent;

/**
 * Implementation of {@link ToolIntegrationContext} for the CPACS tool integration.
 * 
 * @author Jan Flink
 */
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
    public String getComponentGroupId() {
        return ToolIntegrationConstants.DEFAULT_COMPONENT_GROUP_ID;
    }

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

}
