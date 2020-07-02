/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.util.Map;
import java.util.Set;

/**
 * Provides all required data and data sources to initiate a remote tool execution.
 *
 * @author Robert Mischke
 */
public final class ToolExecutionClientSideSetup {

    private final ToolExecutionRequest executionRequest;

    /**
     * A builder interface to configure and create immutable {@link ToolExecutionClientSideSetup}s.
     *
     * @author Robert Mischke
     */
    public static final class Builder {

        private String toolId;

        private String toolVersion;

        private String authGroupId;

        private String destinationId;

        private Set<String> nonRequiredInputs;

        private Set<Map<String, Object>> dynamicInputs;

        private Set<Map<String, Object>> dynamicOutputs;

        private Map<String, String> properties;

        /**
         * Transfers all relevant values from an existing {@link ToolMetadata} object.
         * 
         * @param value the {@link ToolMetadata} to copy from
         * @return the builder instance (for call chaining)
         */
        //TODO Check if this method is actually needed; if yes, we have to think about how to handle the auth groups here
        //(need to choose one from the list)
        /*  public Builder copyFrom(ToolMetadata value) {
            this.toolId = value.getToolId();
            this.toolVersion = value.getToolVersion();
            this.authGroupId = value.getAuthGroupIds();
            return this;
        }*/

        /**
         * Sets the external id (e.g. "MyTool") of the tool to execute. TODO define handling/mapping of 9.0+ tool prefixes.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder toolId(String value) {
            this.toolId = value;
            return this;
        }

        /**
         * Sets the version string of the tool to execute.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder toolVersion(String value) {
            this.toolVersion = value;
            return this;
        }

        /**
         * Sets the full id ("<user-given name>:<disambiguation suffix>") of the authorization group to use for this tool's invocation.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder authGroupId(String value) {
            this.authGroupId = value;
            return this;
        }

        /**
         * Sets the destination id (following the pattern of a LogicalNodeId string) to use for this tool's invocation.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder destinationId(String value) {
            this.destinationId = value;
            return this;
        }

        /**
         * Sets the set of non required inputs.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder nonRequiredInputs(Set<String> value) {
            this.nonRequiredInputs = value;
            return this;
        }

        /**
         * Sets the set of dynamic inputs.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder dynamicInputs(Set<Map<String, Object>> value) {
            this.dynamicInputs = value;
            return this;
        }

        /**
         * Sets the set of dynamic outputs.
         * 
         * @param value the new value
         * @return the builder instance (for call chaining)
         */
        public Builder dynamicOutputs(Set<Map<String, Object>> value) {
            this.dynamicOutputs = value;
            return this;
        }

        /**
         * Sets the tool properties.
         * 
         * @param value the properties map
         * @return the builder instance (for call chaining)
         */
        public Builder properties(Map<String, String> value) {
            this.properties = value;
            return this;
        }

        /**
         * Creates the configured {@link ToolExecutionClientSideSetup} instance.
         * 
         * @return a new {@link ToolExecutionClientSideSetup}
         */
        public ToolExecutionClientSideSetup build() {
            return new ToolExecutionClientSideSetup(this);
        }



    }

    private ToolExecutionClientSideSetup(Builder builder) {
        this.executionRequest = new ToolExecutionRequest(builder.toolId, builder.toolVersion, builder.authGroupId, builder.destinationId,
            builder.nonRequiredInputs, builder.dynamicInputs, builder.dynamicOutputs, builder.properties);
    }

    /**
     * Creates a new {@link Builder} instance.
     * 
     * @return the new instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public ToolExecutionRequest getExecutionRequest() {
        return executionRequest;
    }

    public String getToolId() {
        return executionRequest.getToolId();
    }

    public String getToolVersion() {
        return executionRequest.getToolVersion();
    }

    public String getAuthGroupId() {
        return executionRequest.getAuthGroupId();
    }

    public String getDestinationId() {
        return executionRequest.getDestinationId();
    }

}
