/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Default implementation of {@link ToolIntegrationService} for tests.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 */
public class DefaultToolIntegrationServiceStub implements ToolIntegrationService {

    @Override
    public void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context) {}

    @Override
    public void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context, boolean savePublished) {}

    @Override
    public void removeTool(String toolName, ToolIntegrationContext information) {}

    @Override
    public void writeToolIntegrationFile(Map<String, Object> configurationMap, ToolIntegrationContext integrationInformation)
        throws IOException {}

    @Override
    public void writeToolIntegrationFileToSpecifiedFolder(String folder, Map<String, Object> configurationMap,
        ToolIntegrationContext integrationInformation) throws IOException {}

    @Override
    public Map<String, Object> getToolConfiguration(String toolId) {
        return null;
    }

    @Override
    public Set<String> getIntegratedComponentIds() {
        return null;
    }

    @Override
    public String getPathOfComponentID(String id, ToolIntegrationContext context) {
        return null;
    }

    @Override
    public Set<String> getActiveComponentIds() {
        return null;
    }

    @Override
    public boolean isToolIntegrated(Map<String, Object> configurationMap, ToolIntegrationContext integrationContext) {
        return false;
    }

    @Override
    public String getToolNameToPath(String path) {
        return null;
    }

    @Override
    public void putToolNameToPath(String toolName, File parentFile) {

    }

    @Override
    public void updatePublishedComponents(ToolIntegrationContext context) {

    }

    @Override
    public byte[] getToolDocumentation(String identifier) throws RemoteOperationException {
        return null;
    }

    @Override
    public void setFileWatcherActive(boolean value) {}

    @Override
    public void unregisterIntegration(String previousToolName, ToolIntegrationContext integrationContext) {}

    @Override
    public void registerRecursive(String toolName, ToolIntegrationContext integrationContext) {}

}
