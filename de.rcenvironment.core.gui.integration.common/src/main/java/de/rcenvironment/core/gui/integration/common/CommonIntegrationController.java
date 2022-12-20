/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.common;

import java.util.Map;

import de.rcenvironment.core.component.integration.ConfigurationMap;
import de.rcenvironment.core.component.integration.ToolIntegrationContextRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Controller class to handle all common configurations for the integration of tools and workflows.
 * 
 * @author Kathrin Schaffert
 */
public class CommonIntegrationController {


    protected ConfigurationMap configurationModel;

    protected ToolIntegrationContextRegistry integrationContextRegistry;

    public CommonIntegrationController() {
        integrationContextRegistry = ServiceRegistry.createAccessFor(this).getService(ToolIntegrationContextRegistry.class);
        instantiateModel();
    }

    public void instantiateModel() {
        this.configurationModel = new ConfigurationMap();
    }

    public ConfigurationMap getConfigurationModel() {
        return configurationModel;
    }

    public void setConfigurationModel(ConfigurationMap configurationModel) {
        this.configurationModel = configurationModel;
    }

    public Map<String, Object> getConfigurationMap() {
        return configurationModel.getRawConfigurationMap();
    }

}
