/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;

/**
 * Creates component configuration model objects from raw (JSON) data.
 * 
 * @author Doreen Seider
 */
public final class ComponentConfigurationModelFactory {

    private ComponentConfigurationModelFactory() {}

    /**
     * @param rawConfigurationDef raw configuration definition information
     * @param rawPlaceholderMetaDataDef raw placeholder meta data information
     * @param rawConfigurationMetaDataDef raw configuration meta data information
     * @param readOnlyConfiguration read-only configuration of the component
     * @return {@link ConfigurationDefinition} object
     */
    public static ConfigurationDefinition createConfigurationDefinition(List<Object> rawConfigurationDef,
        List<Object> rawPlaceholderMetaDataDef, List<Object> rawConfigurationMetaDataDef, Map<String, String> readOnlyConfiguration) {
        ConfigurationDefinitionImpl configurationDefinition = new ConfigurationDefinitionImpl();
        configurationDefinition.setRawConfigurationDefinition(rawConfigurationDef);
        configurationDefinition.setRawPlaceholderMetaDataDefinition(rawPlaceholderMetaDataDef);
        configurationDefinition.setRawConfigurationMetaDataDefinition(rawConfigurationMetaDataDef);
        configurationDefinition.setRawReadOnlyConfiguration(readOnlyConfiguration);
        return configurationDefinition;
    }

    /**
     * @param rawConfigurationDef raw configuration definition information
     * @param rawPlaceholdersDef raw placeholder information
     * @param rawActivationFilter raw activation filter information
     * @return {@link ConfigurationExtensionDefinition} object
     */
    public static ConfigurationExtensionDefinition createConfigurationExtensionDefinition(List<Object> rawConfigurationDef,
        List<Object> rawPlaceholdersDef, Map<String, Object> rawActivationFilter) {
        ConfigurationExtensionDefinitionImpl configurationDefinition = new ConfigurationExtensionDefinitionImpl();
        configurationDefinition.setRawConfigurationDefinition(rawConfigurationDef);
        configurationDefinition.setRawPlaceholderMetaDataDefinition(rawPlaceholdersDef);
        configurationDefinition.setRawActivationFilter(rawActivationFilter);
        return configurationDefinition;
    }

}
