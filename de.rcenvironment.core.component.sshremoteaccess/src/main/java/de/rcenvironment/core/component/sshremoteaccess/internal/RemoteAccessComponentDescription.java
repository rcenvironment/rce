/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.sshremoteaccess.internal;

/**
 * A class containing the description of a remote access component.
 *
 * @author Brigitte Boden
 */
public class RemoteAccessComponentDescription {

    private String componentId;

    private String toolName;

    private String toolVersion;

    private String hostName;

    private String hostId;

    private String connectionId;

    private boolean isWorkflow;

    private String inputDefinitions;

    private String outputDefinitions;
    
    private String group;

    public RemoteAccessComponentDescription(String componentId, String toolName, String toolVersion, String hostName, String hostId,
        String connectionId, boolean isWorkflow, String inputDefinitions, String outputDefinitions, String group) {
        this.componentId = componentId;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.hostName = hostName;
        this.hostId = hostId;
        this.connectionId = connectionId;
        this.isWorkflow = isWorkflow;
        this.inputDefinitions = inputDefinitions;
        this.outputDefinitions = outputDefinitions;
        this.group = group;
    }

    
    public String getGroup() {
        return group;
    }

    
    public void setGroup(String group) {
        this.group = group;
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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public boolean isWorkflow() {
        return isWorkflow;
    }

    public void setWorkflow(boolean isAWorkflow) {
        this.isWorkflow = isAWorkflow;
    }

    public String getInputDefinitions() {
        return inputDefinitions;
    }

    public void setInputDefinitions(String inputDefinitions) {
        this.inputDefinitions = inputDefinitions;
    }

    public String getOutputDefinitions() {
        return outputDefinitions;
    }

    public void setOutputDefinitions(String outputDefinitions) {
        this.outputDefinitions = outputDefinitions;
    }
}
