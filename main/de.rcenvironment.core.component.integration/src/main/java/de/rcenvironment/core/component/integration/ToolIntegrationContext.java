/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;

/**
 * Stores information for the tool integration.
 * 
 * @author Sascha Zur
 */
public interface ToolIntegrationContext {

    /**
     * @return id for the information instance, which must be unique!
     */
    String getContextId();

    /**
     * 
     * @return the type of the context in the configuration files (e.g. "common")
     */
    String getContextType();

    /**
     * @return an absolute path to the directory in which the directory with all tools is.
     */
    String getRootPathToToolIntegrationDirectory();

    /**
     * @return name of the directory that has all tool integration directories.
     */
    String getNameOfToolIntegrationDirectory();

    /**
     * @return the prefix of the tool integration directories (e.g "Tool") that are searched for
     */
    String getToolDirectoryPrefix();

    /**
     * @return name of the files that have the actual json configuration for the integration
     */
    String getConfigurationFilename();

    /**
     * @return name of the class (with package structure!) that is used for executing the integrated
     *         tool
     */
    String getImplementingComponentClassName();

    /**
     * @return id prefix for all tools that are read or created with this information
     */
    String getPrefixForComponentId();

    /**
     * @return name of the component group in which the integrated components are published
     */
    String getComponentGroupId();

    /**
     * Some integration types don't need all common integration properties to be defined by the
     * user. Keys that are not necessary can be disabled in the wizard with this method.
     * 
     * @return array of disabled keys. if no keys should be disabled, the Arrays must be empty.
     */
    String[] getDisabledIntegrationKeys();

    /**
     * List of all paths for e.g. templates.
     * 
     * @return pathes
     */
    File[] getReadOnlyPathsList();
}
