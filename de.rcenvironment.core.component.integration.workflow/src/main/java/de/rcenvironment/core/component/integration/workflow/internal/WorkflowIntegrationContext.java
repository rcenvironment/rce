/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration.workflow.internal;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.IntegrationContext;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.component.integration.workflow.WorkflowIntegratorComponent;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathListId;

/**
 * A {@link IntegrationContext} for integrating workflows as components.
 * 
 * @author Alexander Weinert
 */
@Component
public final class WorkflowIntegrationContext implements IntegrationContext {

    private ConfigurationService configService; // Initialized by OSGI-DS

    @Override
    public String getContextId() {
        return UUID.randomUUID().toString() + "_WORKFLOW";
    }

    @Override
    public IntegrationContextType getContextType() {
        return IntegrationContextType.WORKFLOW;
    }

    @Override
    public String getContextTypeString() {
        return IntegrationContextType.WORKFLOW.toString();
    }

    @Override
    public String getRootPathToToolIntegrationDirectory() {
        return configService.getConfigurablePath(ConfigurablePathId.DEFAULT_WRITEABLE_INTEGRATION_ROOT).getAbsolutePath();
    }

    @Override
    public String getNameOfToolIntegrationDirectory() {
        return String.join(File.separator, "tools", "workflow");
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
        return WorkflowIntegratorComponent.class.getCanonicalName();
    }

    @Override
    public String getPrefixForComponentId() {
        return "de.rcenvironment.integration.workflow.";
    }

    @Override
    public String getDefaultComponentGroupId() {
        return WorkflowIntegrationConstants.DEFAULT_GROUP_ID;
    }

    @Override
    public String[] getDisabledIntegrationKeys() {
        return new String[] {};
    }

    @Override
    public File[] getReadOnlyPathsList() {
        return configService.getConfigurablePathList(ConfigurablePathListId.READABLE_INTEGRATION_DIRS);
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected void bindConfigurationService(final ConfigurationService service) {
        this.configService = service;
    }

    @Override
    public Optional<ConfigurationMap> parseConfigurationMap(Map<String, Object> rawConfigurationMap) {
        return Optional.ofNullable(ConfigurationMap.fromMap(rawConfigurationMap));
    }


}
