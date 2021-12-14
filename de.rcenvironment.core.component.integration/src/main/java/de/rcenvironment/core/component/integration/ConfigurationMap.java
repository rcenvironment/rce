/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.integration.internal.ToolIntegrationServiceImpl;
import de.rcenvironment.core.component.model.configuration.api.ComponentConfigurationModelFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;

/**
 * @author Alexander Weinert
 * @author Kathrin Schaffert (#17480)
 */
public class ConfigurationMap {

    private final Map<String, Object> rawConfigurationMap;

    ConfigurationMap(Map<String, Object> rawConfigurationMap) {
        this.rawConfigurationMap = rawConfigurationMap;
    }

    /**
     * @param configurationMap TODO
     * @return TODO
     */
    public static ConfigurationMap fromMap(Map<String, Object> configurationMap) {
        return new ConfigurationMap(configurationMap);
    }

    /**
     * @param migration Some migration that is to be applied to the map backing this configuration
     */
    public void applyMigration(ConfigurationMapMigration migration) {
        migration.migrate(this.rawConfigurationMap);
    }

    public String getToolName() {
        return (String) rawConfigurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME);
    }

    /**
     * @return TODO
     */
    public String getToolVersion() {
        Map<String, String> firstLaunchSettings = getFirstLaunchSettings();
        return firstLaunchSettings.get(ToolIntegrationConstants.KEY_VERSION);
    }

    private Map<String, String> getFirstLaunchSettings() {
        @SuppressWarnings("unchecked") List<Object> allLaunchSettings =
            (List<Object>) rawConfigurationMap.get(ToolIntegrationConstants.KEY_LAUNCH_SETTINGS);
        @SuppressWarnings("unchecked") Map<String, String> firstLaunchSettings = (Map<String, String>) allLaunchSettings.get(0);
        return firstLaunchSettings;
    }

    public String getGroupPath() {
        return (String) rawConfigurationMap.get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME);
    }

    /**
     * @return A list of the static inputs of this tool. May be empty, but is never null
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getStaticInputs() {
        return (List<Map<String, String>>) rawConfigurationMap.getOrDefault(ToolIntegrationConstants.KEY_ENDPOINT_INPUTS,
            new LinkedList<>());
    }

    /**
     * @return TODO
     */
    public boolean containsStaticInputs() {
        return rawConfigurationMap.containsKey(ToolIntegrationConstants.KEY_ENDPOINT_INPUTS);
    }

    /**
     * @return TODO
     */
    public boolean containsDynamicInputs() {
        return rawConfigurationMap.containsKey(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_INPUTS);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDynamicInputs() {
        return (List<Map<String, Object>>) rawConfigurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_INPUTS);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getStaticOutputs() {
        return (List<Map<String, String>>) rawConfigurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_OUTPUTS);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDynamicOutputs() {
        return (List<Map<String, Object>>) rawConfigurationMap.get(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_OUTPUTS);
    }

    /**
     * @return TODO
     */
    public String getExecutionCountLimit() {
        Map<String, String> firstLaunchSettings = this.getFirstLaunchSettings();
        final String newExecutionCountLimit = firstLaunchSettings.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES);
        if (newExecutionCountLimit != null) {
            return newExecutionCountLimit;
        } else {
            return firstLaunchSettings.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        }
    }

    public String getMaxParallelCount() {
        return this.getFirstLaunchSettings().get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_COUNT);
    }

    public Optional<Boolean> isActive() {
        return Optional.ofNullable((Boolean) this.rawConfigurationMap.get(ToolIntegrationConstants.IS_ACTIVE));
    }

    public String getIconPath() {
        return (String) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_TOOL_ICON_PATH);
    }

    public Long getIconModificationDate() {
        return (Long) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_ICON_MODIFICATION_DATE);
    }

    public String getIconHash() {
        return (String) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_ICON_HASH);
    }

    /**
     * @return TODO
     */
    public Boolean shouldUploadIcon() {
        return (Boolean) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_UPLOAD_ICON);
    }

    /**
     * @param md5Hash TODO
     */
    public void setIconHash(String md5Hash) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_ICON_HASH, md5Hash);

    }

    /**
     * @param lastModified TODO
     */
    public void setIconModificationDate(long lastModified) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_ICON_MODIFICATION_DATE, lastModified);
    }

    /**
     * TODO.
     */
    public void doNotUploadIcon() {
        this.rawConfigurationMap.remove(ToolIntegrationConstants.KEY_UPLOAD_ICON);
    }

    /**
     * @param path TODO
     */
    public void setIconPath(String path) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_TOOL_ICON_PATH, path);
    }

    /**
     * 
     * @param toolIntegrationServiceImpl TODO
     * @return TODO
     */
    public ConfigurationDefinition generateConfiguration(ToolIntegrationServiceImpl toolIntegrationServiceImpl) {
        List<Object> configuration = new LinkedList<>();
        List<Object> configurationMetadata = new LinkedList<>();
        this.readConfigurationWithMetaDataToLists(configuration, configurationMetadata);
        Map<String, String> readOnlyConfiguration = this.createReadOnlyConfiguration();
        return ComponentConfigurationModelFactory.createConfigurationDefinition(configuration, new LinkedList<>(),
            configurationMetadata, readOnlyConfiguration);
    }

    @SuppressWarnings("unchecked")
    private void readConfigurationWithMetaDataToLists(List<Object> configuration, List<Object> configurationMetadata) {
        Map<String, Object> properties = (Map<String, Object>) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_PROPERTIES);

        if (properties != null) {
            for (String groupKey : properties.keySet()) {
                Map<String, Object> group = (Map<String, Object>) properties.get(groupKey);
                String configFileName = null;
                if (group.get(ToolIntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE) != null
                    && (Boolean) group.get(ToolIntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE)) {
                    configFileName = (String) group.get(ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME);
                }
                for (String propertyOrConfigfile : group.keySet()) {
                    int i = 0;
                    if (!(group.get(propertyOrConfigfile) instanceof String
                        || group.get(propertyOrConfigfile) instanceof Boolean)) {
                        Map<String, String> property = (Map<String, String>) group.get(propertyOrConfigfile);
                        Map<String, String> config = new HashMap<>();
                        config.put(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_KEY));
                        config.put(ComponentConstants.KEY_DEFAULT_VALUE,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_DEFAULT_VALUE));
                        configuration.add(config);
                        Map<String, String> configMetadata = new HashMap<>();
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_NAME,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME));
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_COMMENT,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_COMMENT));
                        if (configFileName != null) {
                            configMetadata.put(ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME, configFileName);
                        }
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_GROUP_NAME, groupKey);
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_POSITION, "" + i++);
                        configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_CONFIG_KEY,
                            property.get(ToolIntegrationConstants.KEY_PROPERTY_KEY));
                        configurationMetadata.add(configMetadata);
                    }
                }
            }
        }

        Map<String, String> historyConfig = new HashMap<>();
        historyConfig.put(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY,
            ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM);
        historyConfig.put(ComponentConstants.KEY_DEFAULT_VALUE, "" + false);
        configuration.add(historyConfig);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> createReadOnlyConfiguration() {
        final Map<String, String> configuration = new HashMap<>();
        for (String key : this.rawConfigurationMap.keySet()) {
            final Object value = this.rawConfigurationMap.get(key);
            if (value instanceof String) {
                configuration.put(key, (String) value);
            } else if (value instanceof Boolean) {
                configuration.put(key, ((Boolean) value).toString());
            }
        }
        configuration.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY,
            this.getFirstLaunchSettings().get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY));

        configuration.put(ToolIntegrationConstants.KEY_TOOL_DIRECTORY,
            this.getFirstLaunchSettings().get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY));

        return configuration;
    }

    /**
     * @return TODO
     */
    public boolean hasIntegrationVersion() {
        return this.rawConfigurationMap.containsKey(ToolIntegrationConstants.KEY_TOOL_INTEGRATION_VERSION);
    }

    /**
     * @param currentToolintegrationVersion TODO
     */
    public void setIntegrationVersion(int currentToolintegrationVersion) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_TOOL_INTEGRATION_VERSION,
            ToolIntegrationConstants.CURRENT_TOOLINTEGRATION_VERSION);

    }

    /**
     * @return TODO
     */
    public boolean containsDocFilePath() {
        return this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH) != null;
    }

    public String getDocFilePath() {
        return (String) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_DOC_FILE_PATH);
    }

    /**
     * @param path TODO
     */
    public void setDocFilePath(String path) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_DOC_FILE_PATH, path);
    }

    /**
     * @return TODO
     */
    public Map<String, Object> getShallowClone() {
        final Map<String, Object> returnValue = new TreeMap<>();
        returnValue.putAll(rawConfigurationMap);
        return returnValue;
    }
}
