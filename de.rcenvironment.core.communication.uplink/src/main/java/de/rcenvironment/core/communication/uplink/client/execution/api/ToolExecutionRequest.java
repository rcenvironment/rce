/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Represents the information that is transmitted between the initiator and the receiver of a request for a published tool's execution.
 *
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class ToolExecutionRequest implements Serializable {

    private static final long serialVersionUID = -4679081522852537880L;

    private String toolId;

    private String toolVersion;

    private String authGroupId;

    private String destinationId;

    private Set<String> nonRequiredInputs;

    private Set<Map<String, Object>> dynamicInputs;

    private Set<Map<String, Object>> dynamicOutputs;

    private Map<String, String> properties;

    private String isMockMode;

    // TODO add an authorization confirmation token etc.
    // TODO add caller information?

    public ToolExecutionRequest() {
        // for deserialization
    }

    public ToolExecutionRequest(String toolId, String toolVersion, String authGroupId, String destinationId, Set<String> nonRequiredInputs,
        Set<Map<String, Object>> dynamicInputs, Set<Map<String, Object>> dynamicOutputs, Map<String, String> properties,
        String isMockMode) {
        this.toolId = toolId;
        this.toolVersion = toolVersion;
        this.authGroupId = authGroupId;
        this.destinationId = destinationId;
        this.nonRequiredInputs = nonRequiredInputs;
        this.dynamicInputs = dynamicInputs;
        this.dynamicOutputs = dynamicOutputs;
        this.properties = properties;
        this.isMockMode = isMockMode;
    }

    /**
     * Cloning constructor.
     */
    public ToolExecutionRequest(ToolExecutionRequest source) {
        this.toolId = source.toolId;
        this.toolVersion = source.toolVersion;
        this.authGroupId = source.authGroupId;
        this.destinationId = source.destinationId;
        this.nonRequiredInputs = source.nonRequiredInputs;
        this.dynamicInputs = source.dynamicInputs;
        this.dynamicOutputs = source.dynamicOutputs;
        this.properties = source.properties;
    }

    public String getToolId() {
        return toolId;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public String getAuthGroupId() {
        return authGroupId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public Set<String> getNonRequiredInputs() {
        return nonRequiredInputs;
    }

    public Set<Map<String, Object>> getDynamicInputs() {
        return dynamicInputs;
    }

    public Set<Map<String, Object>> getDynamicOutputs() {
        return dynamicOutputs;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String isMockMode() {
        return isMockMode;
    }
}
