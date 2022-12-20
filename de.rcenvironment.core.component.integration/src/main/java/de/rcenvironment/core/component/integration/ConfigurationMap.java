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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ComponentConfigurationModelFactory;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * @author Alexander Weinert
 * @author Kathrin Schaffert (#17480)
 * @author Jan Flink
 */
public class ConfigurationMap {

    protected Map<String, Object> rawConfigurationMap;

    public ConfigurationMap(Map<String, Object> rawConfigurationMap) {
        this.rawConfigurationMap = rawConfigurationMap;
    }

    public ConfigurationMap() {
        this(new HashMap<>());
    }

    /**
     * @param configurationMap A map of configuration key-value-pairs.
     * @return A {@link ConfigurationMap} object.
     */
    public static ConfigurationMap fromMap(Map<String, Object> configurationMap) {
        return new ConfigurationMap(configurationMap);
    }

    /**
     * @param migration Some migration that is to be applied to the map backing this configuration.
     */
    public void applyMigration(ConfigurationMapMigration migration) {
        migration.migrate(this.rawConfigurationMap);
    }

    public String getToolName() {
        return (String) rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_COMPONENT_NAME, "");
    }

    public String getToolVersion() {
        return getFirstLaunchSettings().get(IntegrationConstants.KEY_VERSION);
    }

    public Map<String, String> getFirstLaunchSettings() {
        List<Object> allLaunchSettings = checkedListCast(rawConfigurationMap.get(IntegrationConstants.KEY_LAUNCH_SETTINGS));
        return checkedMapCast(allLaunchSettings.get(0), String.class, String.class);
    }

    public String getGroupPath() {
        return (String) rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_GROUPNAME, "");
    }

    public List<Map<String, String>> getStaticInputs() {
        return checkedListOfMapCast(rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_ENDPOINT_INPUTS,
            new LinkedList<>()), String.class, String.class);
    }

    public boolean containsStaticInputs() {
        return rawConfigurationMap.containsKey(IntegrationConstants.KEY_ENDPOINT_INPUTS);
    }

    public boolean containsDynamicInputs() {
        return rawConfigurationMap.containsKey(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_INPUTS);
    }

    public List<Map<String, Object>> getDynamicInputs() {
        return checkedListOfMapCast(
            rawConfigurationMap.getOrDefault(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_INPUTS, new LinkedList<>()), String.class,
            Object.class);
    }



    public List<Map<String, String>> getStaticOutputs() {
        return checkedListOfMapCast(rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_ENDPOINT_OUTPUTS, new LinkedList<>()),
            String.class, String.class);
    }

    public List<Map<String, Object>> getDynamicOutputs() {
        return checkedListOfMapCast(
            rawConfigurationMap.getOrDefault(ToolIntegrationConstants.KEY_ENDPOINT_DYNAMIC_OUTPUTS, new LinkedList<>()), String.class,
            Object.class);
    }

    public String getExecutionCountLimit() {
        Map<String, String> firstLaunchSettings = this.getFirstLaunchSettings();
        final String newExecutionCountLimit = firstLaunchSettings.get(IntegrationConstants.KEY_LIMIT_INSTANCES);
        if (newExecutionCountLimit != null) {
            return newExecutionCountLimit;
        } else {
            // Ensure backward compatibility.
            return firstLaunchSettings.get(ToolIntegrationConstants.KEY_LIMIT_INSTANCES_OLD);
        }
    }

    public String getMaxParallelCount() {
        return this.getFirstLaunchSettings().get(IntegrationConstants.KEY_LIMIT_INSTANCES_COUNT);
    }

    public Optional<Boolean> isActive() {
        return Optional.ofNullable((Boolean) this.rawConfigurationMap.get(IntegrationConstants.IS_ACTIVE));
    }

    public String getIconPath() {
        return (String) this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_ICON_PATH, "");
    }

    public Long getIconModificationDate() {
        return (Long) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_ICON_MODIFICATION_DATE);
    }

    public String getIconHash() {
        return (String) this.rawConfigurationMap.get(ToolIntegrationConstants.KEY_ICON_HASH);
    }

    public Boolean shouldUploadIcon() {
        return (Boolean) this.rawConfigurationMap.get(IntegrationConstants.KEY_COPY_ICON);
    }

    public void setIconHash(String md5Hash) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_ICON_HASH, md5Hash);

    }

    public void setIconModificationDate(long lastModified) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_ICON_MODIFICATION_DATE, lastModified);
    }

    public void doNotUploadIcon() {
        this.rawConfigurationMap.remove(IntegrationConstants.KEY_COPY_ICON);
    }

    public void setIconPath(String path) {
        this.rawConfigurationMap.put(IntegrationConstants.KEY_ICON_PATH, path);
    }

    /**
     * Generates the components configuration options as shown in the components properties tab.
     * 
     * @return A {@link ConfigurationDefinition} describing the options and layout of the configurable component properties.
     */
    public ConfigurationDefinition generateConfiguration() {
        List<Object> configuration = new LinkedList<>();
        List<Object> configurationMetadata = new LinkedList<>();
        this.readConfigurationWithMetaDataToLists(configuration, configurationMetadata);
        Map<String, String> readOnlyConfiguration = this.createReadOnlyConfiguration();
        return ComponentConfigurationModelFactory.createConfigurationDefinition(configuration, new LinkedList<>(),
            configurationMetadata, readOnlyConfiguration);
    }


    private void readConfigurationWithMetaDataToLists(List<Object> configuration, List<Object> configurationMetadata) {
        Map<String, Object> properties = checkedMapCast(
                this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_PROPERTIES, new HashMap<String, Object>()), String.class,
                Object.class);

        for (Entry<String, Object> groupEntry : properties.entrySet()) {
            Map<String, Object> group = checkedMapCast(groupEntry.getValue(), String.class, Object.class);
            String configFileName = null;
            if (group.get(IntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE) != null
                && (Boolean) group.get(IntegrationConstants.KEY_PROPERTY_CREATE_CONFIG_FILE)) {
                configFileName = (String) group.get(ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME);
            }
            int i = 0;
            for (Entry<String, Object> propertyOrConfigfile : group.entrySet()) {
                if (!(propertyOrConfigfile.getValue() instanceof String || propertyOrConfigfile.getValue() instanceof Boolean)) {
                    Map<String, String> property = checkedMapCast(propertyOrConfigfile.getValue(), String.class, String.class);
                    Map<String, String> config = new HashMap<>();
                    config.put(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY,
                        property.get(IntegrationConstants.KEY_PROPERTY_KEY));
                    config.put(ComponentConstants.KEY_DEFAULT_VALUE,
                        property.get(IntegrationConstants.KEY_PROPERTY_DEFAULT_VALUE));
                    configuration.add(config);
                    Map<String, String> configMetadata = new HashMap<>();
                    configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_NAME,
                        property.get(IntegrationConstants.KEY_PROPERTY_DISPLAYNAME));
                    configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_COMMENT,
                        property.get(IntegrationConstants.KEY_PROPERTY_COMMENT));
                    if (configFileName != null) {
                        configMetadata.put(ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME, configFileName);
                    }
                    configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_GROUP_NAME, groupEntry.getKey());
                    configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_GUI_POSITION, "" + i++);
                    configMetadata.put(ConfigurationDefinitionConstants.KEY_METADATA_CONFIG_KEY,
                        property.get(IntegrationConstants.KEY_PROPERTY_KEY));
                    configurationMetadata.add(configMetadata);
                }
            }
        }

        Map<String, String> historyConfig = new HashMap<>();
        historyConfig.put(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY,
            ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM);
        historyConfig.put(ComponentConstants.KEY_DEFAULT_VALUE, "" + false);
        configuration.add(historyConfig);
    }

    private Map<String, String> createReadOnlyConfiguration() {
        final Map<String, String> configuration = new HashMap<>();
        for (Entry<String, Object> entry : this.rawConfigurationMap.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof String) {
                configuration.put(entry.getKey(), (String) value);
            } else if (value instanceof Boolean) {
                configuration.put(entry.getKey(), ((Boolean) value).toString());
            }
        }
        configuration.put(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY,
            this.getFirstLaunchSettings().get(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY));

        configuration.put(ToolIntegrationConstants.KEY_TOOL_DIRECTORY,
            this.getFirstLaunchSettings().get(ToolIntegrationConstants.KEY_TOOL_DIRECTORY));

        return configuration;
    }

    public boolean hasIntegrationVersion() {
        return this.rawConfigurationMap.containsKey(ToolIntegrationConstants.KEY_TOOL_INTEGRATION_VERSION);
    }

    public void setIntegrationVersion(int currentToolintegrationVersion) {
        this.rawConfigurationMap.put(ToolIntegrationConstants.KEY_TOOL_INTEGRATION_VERSION,
            currentToolintegrationVersion);
    }

    public boolean containsDocFilePath() {
        return this.rawConfigurationMap.get(IntegrationConstants.KEY_DOC_FILE_PATH) != null && !getDocFilePath().isEmpty();
    }

    public String getDocFilePath() {
        return (String) this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_DOC_FILE_PATH, "");
    }

    public void setDocFilePath(String path) {
        this.rawConfigurationMap.put(IntegrationConstants.KEY_DOC_FILE_PATH, path);
    }

    @SuppressWarnings("unchecked")
    private <K, V> List<Map<K, V>> checkedListOfMapCast(Object o, Class<K> clazzK, Class<V> clazzV) {
        if (!(o instanceof List)) {
            throw new ClassCastException(StringUtils.format("Tool integration configuration error. %s is not of type 'List'", o));
        }
        boolean castable = Stream.of(o).filter(Map.class::isInstance).map(Map.class::cast)
            .allMatch(entry -> entry.keySet().stream().allMatch(clazzK::isInstance)
                && entry.values().stream().allMatch(clazzV::isInstance));
        if (!castable) {
            throw new ClassCastException(StringUtils
                .format("Tool integration configuration error. At least one list entry is not of type Map<%s, %s>.", clazzK, clazzV));
        }
        return (List<Map<K, V>>) o;
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> checkedMapCast(Object o, Class<K> clazzK, Class<V> clazzV) {
        if (!(o instanceof Map)) {
            throw new ClassCastException(StringUtils.format("Tool integration configuration error. %s is not of type 'Map'", o));
        }
        Map<K, V> map = (Map<K, V>) o;
        boolean castable = map.keySet().stream().allMatch(clazzK::isInstance)
            && map.values().stream().allMatch(clazzV::isInstance);
        if (!castable) {
            throw new ClassCastException(
                StringUtils.format("Tool integration configuration error. Unable to cast to Map<%s, %s>.", clazzK, clazzV));
        }
        return (Map<K, V>) o;
    }

    @SuppressWarnings("unchecked")
    private List<Object> checkedListCast(Object o) {
        if (!(o instanceof List)) {
            throw new ClassCastException(StringUtils.format("Tool integration configuration error. %s is not of type 'List'", o));
        }
        return (List<Object>) o;
    }

    /**
     * Generates a copy of the configuration map object.
     * 
     * @return A copy of the configuration map.
     */
    public Map<String, Object> getShallowClone() {
        final Map<String, Object> returnValue = new TreeMap<>();
        returnValue.putAll(rawConfigurationMap);
        return returnValue;
    }

    public String getToolDescription() {
        return (String) this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_DESCRIPTION, "");
    }

    public String getIntegratorName() {
        return (String) this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_INTEGRATOR_NAME, "");
    }

    public String getIntegratorEmail() {
        return (String) this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_INTEGRATOR_EMAIL, "");
    }

    public boolean isCopyIcon() {
        return (Boolean) this.rawConfigurationMap.getOrDefault(IntegrationConstants.KEY_COPY_ICON, false);
    }

    public boolean isLimitInstance() {
        Map<String, String> firstLaunchSettings = this.getFirstLaunchSettings();
        return Boolean.parseBoolean((String) firstLaunchSettings.getOrDefault(IntegrationConstants.KEY_LIMIT_INSTANCES, "false"));
    }

    public Map<String, Object> getRawConfigurationMap() {
        return rawConfigurationMap;
    }

    public void setRawConfigurationMap(Map<String, Object> rawConfigurationMap) {
        this.rawConfigurationMap = rawConfigurationMap;
    }

}
