/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.model.api.ComponentInterface;

/**
 * Service for registering new user integrated tools and read the already integrated from the
 * configuration.
 * 
 * @author Sascha Zur
 */
public interface ToolIntegrationService {

    /**
     * Dynamically adds a new {@link ComponentInterface} based on a configuration.
     * 
     * @param configurationMap : information for the interface
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
     * Reads all previously added {@link ComponentInterface}s.
     * 
     * @param information about the tools to be integrated.
     */
    void readAndIntegratePersistentTools(ToolIntegrationContext information);

    /**
     * Writes a new description of a component to the configuration folder of the local RCE.
     * 
     * @param configurationMap : information about the component
     * @param integrationInformation about the tool e.g. the component id prefix and all location
     *        information
     */
    void writeToolIntegrationFile(Map<String, Object> configurationMap, ToolIntegrationContext integrationInformation);

    /**
     * Writes a new description of a component to the folder that was selected.
     * 
     * @param folder : path to the folder where the config should be stored
     * @param configurationMap : information about the component
     * @param integrationInformation about the tool e.g. the component id prefix and all location
     *        information
     */
    void writeToolIntegrationFileToSpecifiedFolder(String folder, Map<String, Object> configurationMap,
        ToolIntegrationContext integrationInformation);

    /**
     * Returns the read in configuration of toolId, which has all information about the tool and for
     * the component.
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
     * @return all as published marked integrated components.
     */
    Set<String> getPublishedComponents();

    /**
     * Returns the absolute path for the given componet id.
     * 
     * @param id of the component
     * @return path
     */
    String getPathOfComponentID(String id);

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
     * Reads the given configuration file and integrated it as a tool.
     * 
     * @param parentFile configuration json file
     * @param context used for integration
     */
    void readToolDirectory(File parentFile, ToolIntegrationContext context);

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
     * Saves the current published components list for the given context.
     * 
     * @param context to save the components
     */
    void savePublishedComponents(ToolIntegrationContext context);

    /**
     * add the given tool to the published components.
     * 
     * @param toolName of the tool to publish
     */
    void addPublishedTool(String toolName);

    /**
     * removes the given tool from the published components.
     * 
     * @param toolPath to remove
     */
    void unpublishTool(String toolPath);
}
