/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.uplinktoolaccess.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class containing the description of a remote access component.
 *
 * @author Brigitte Boden
 */
public class UplinkToolAccessComponentDescription {

    private String componentId;

    private String toolName;

    private String toolVersion;

    private Set<Map<String, Object>> inputDefinitions;

    private Set<Map<String, Object>> outputDefinitions;

    private List<Object> configurationValues;

    private List<Object> configurationMetaData;

    private String paletteGroup;

    private String toolDocumentationHash;
    
    private Map<String, String> readOnlyConfig;

    // Default constructor, required by Jackson for deserialization
    public UplinkToolAccessComponentDescription() {}

    public UplinkToolAccessComponentDescription(String componentId, String toolName, String toolVersion,
        Set<Map<String, Object>> inputDefinitions, Set<Map<String, Object>> outputDefinitions, List<Object> configurationValues,
        List<Object> configurationMetaData, String paletteGroup, String toolDocumentationHash, Map<String, String> readOnlyConfig) {
        this.componentId = componentId;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.inputDefinitions = inputDefinitions;
        this.outputDefinitions = outputDefinitions;
        this.configurationValues = configurationValues;
        this.configurationMetaData = configurationMetaData;
        this.paletteGroup = paletteGroup;
        this.toolDocumentationHash = toolDocumentationHash;
        this.readOnlyConfig = readOnlyConfig;
    }

    public String getPaletteGroup() {
        return paletteGroup;
    }

    public void setPaletteGroup(String group) {
        this.paletteGroup = group;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public void setToolVersion(String toolVersion) {
        this.toolVersion = toolVersion;
    }

    public Set<Map<String, Object>> getInputDefinitions() {
        return inputDefinitions;
    }

    public void setInputDefinitions(Set<Map<String, Object>> inputDefinitions) {
        this.inputDefinitions = inputDefinitions;
    }

    public Set<Map<String, Object>> getOutputDefinitions() {
        return outputDefinitions;
    }

    public void setOutputDefinitions(Set<Map<String, Object>> outputDefinitions) {
        this.outputDefinitions = outputDefinitions;
    }

    public String getToolDocumentationHash() {
        return toolDocumentationHash;
    }

    public void setToolDocumentationHash(String toolDocumentationHash) {
        this.toolDocumentationHash = toolDocumentationHash;
    }

    public List<Object> getConfigurationMetaData() {
        return configurationMetaData;
    }

    public void setConfigurationMetaData(List<Object> properties) {
        this.configurationMetaData = properties;
    }

    public List<Object> getConfigurationValues() {
        return configurationValues;
    }

    public void setConfigurationValues(List<Object> configurationValues) {
        this.configurationValues = configurationValues;
    }

    /**
     * @return Hash value for this object.
     */
    public String createHashString() {
        String toHash = componentId + toolName + toolVersion + inputDefinitions.toString() + outputDefinitions.toString()
            + configurationValues.toString() + configurationMetaData.toString() + paletteGroup + toolDocumentationHash;
        return Integer.toString(toHash.hashCode());
    }

    
    public Map<String, String> getReadOnlyConfig() {
        return readOnlyConfig;
    }

    
    public void setReadOnlyConfig(Map<String, String> readOnlyConfig) {
        this.readOnlyConfig = readOnlyConfig;
    }
    
}
