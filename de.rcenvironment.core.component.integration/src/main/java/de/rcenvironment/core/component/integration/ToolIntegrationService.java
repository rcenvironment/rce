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

import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Service for registering new user integrated tools and read the already integrated from the configuration.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (removed obsolete methods)
 */
public interface ToolIntegrationService {

    /**
     * Dynamically adds a new {@link ComponentInterface} based on a configuration.
     * 
     * @param configurationMap Information for the interface
     * @param context about the component such as the prefix for the component id.
     */
    void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context);

    /**
     * See method integrateTool but with option to not save published components.
     * 
     * @param configurationMap : information for the interface
     * @param context about the component such as the prefix for the component id.
     * @param savePublished if true, published files are written.
     */
    void integrateTool(Map<String, Object> configurationMap, ToolIntegrationContext context, boolean savePublished);

    /**
     * Dynamically removes a {@link ComponentInterface} based on its id.
     * 
     * @param toolName of the component to remove
     * @param information about the component such as the prefix for the component id.
     */
    void removeTool(String toolName, ToolIntegrationContext information);

    /**
     * Writes a new description of a component to the configuration folder of the local RCE.
     * 
     * @param configurationMap : information about the component
     * @param integrationInformation about the tool e.g. the component id prefix and all location information
     * @throws IOException if writing tool fails
     */
    void writeToolIntegrationFile(Map<String, Object> configurationMap, ToolIntegrationContext integrationInformation) throws IOException;

    /**
     * Writes a new description of a component to the folder that was selected.
     * 
     * @param folder : path to the folder where the config should be stored
     * @param configurationMap : information about the component
     * @param integrationInformation about the tool e.g. the component id prefix and all location information
     * @throws IOException if writing tool fails
     */
    void writeToolIntegrationFileToSpecifiedFolder(String folder, Map<String, Object> configurationMap,
        ToolIntegrationContext integrationInformation) throws IOException;

    /**
     * Returns the read in configuration of toolId, which has all information about the tool and for the component.
     * 
     * @param toolId unique id from the component.
     * @return configuration
     */
    Map<String, Object> getToolConfiguration(String toolId);

    /**
     * Returns all component ids that were integrated dynamically.
     * 
     * @return a set of all integrated ids.
     */
    Set<String> getIntegratedComponentIds();

    /**
     * Returns the absolute path for the given componet id.
     * 
     * @param id of the component
     * @param context of the tool
     * @return path
     */
    String getPathOfComponentID(String id, ToolIntegrationContext context);

    /**
     * @return ids of all currently active components.
     */
    Set<String> getActiveComponentIds();

    /**
     * 
     * @param configurationMap of the given tool
     * @param integrationContext for the given tool
     * @return true, if tool is already integrated.
     */
    boolean isToolIntegrated(Map<String, Object> configurationMap, ToolIntegrationContext integrationContext);

    /**
     * Returns the tool name to a given path, if its known.
     * 
     * @param path to search for
     * @return tool name or null, if unknown
     */
    String getToolNameToPath(String path);

    /**
     * @param toolName of the new integrated tool.
     * @param parentFile path to the configuration of the tool.
     */
    void putToolNameToPath(String toolName, File parentFile);

    /**
     * Reads the published.conf and updates the published components.
     * 
     * @param context .
     */
    void updatePublishedComponents(ToolIntegrationContext context);

    /**
     * @param identifier with component id and version
     * 
     * @return documentation of the tool as zipped byte array
     * 
     * @exception RemoteOperationException if the remote operation fails.
     */
    byte[] getToolDocumentation(String identifier) throws RemoteOperationException;

    /**
     * (De-)activate all filewatcher.
     * 
     * @param value true, if they should be active, else false.
     */
    void setFileWatcherActive(boolean value);

    /**
     * Unregister a complete tool directory.
     * 
     * @param previousToolName to unregister
     * @param integrationContext of the tool
     */
    void unregisterIntegration(String previousToolName, ToolIntegrationContext integrationContext);

    /**
     * Register a complete tool directory.
     * 
     * @param toolName to unregister
     * @param integrationContext of the tool
     */
    void registerRecursive(String toolName, ToolIntegrationContext integrationContext);
    
    

}
