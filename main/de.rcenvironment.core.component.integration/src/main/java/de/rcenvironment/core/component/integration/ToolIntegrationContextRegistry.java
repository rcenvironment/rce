/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.util.Collection;

/**
 * Registry for all {@link ToolIntegrationContext}. After the registration, the tools that are
 * stored in the given place are read and registered as workflow components.
 * 
 * @author Sascha Zur
 */
public interface ToolIntegrationContextRegistry {

    /**
     * Adds the given {@link ToolIntegrationContext} for integration. Directly registers them as an
     * workflow component.
     * 
     * @param information new information
     */
    void addToolIntegrationContext(ToolIntegrationContext information);

    /**
     * Removes the {@link ToolIntegrationContext} with the given ID from the list of all information
     * registered. Does not unregister the workflow component.
     * 
     * @param contextID id to remove
     */
    void removeToolIntegrationContext(String contextID);

    /**
     * Removes the given {@link ToolIntegrationContext} from the list of all information registered.
     * Does not unregister the workflow component.
     * 
     * @param context to remove
     */
    void removeToolIntegrationContext(ToolIntegrationContext context);

    /**
     * 
     * @param informationID that is searched for
     * @return the {@link ToolIntegrationContext} for the given id for further use. null if not
     *         available.
     * 
     */
    ToolIntegrationContext getToolIntegrationContext(String informationID);

    /**
     * Checks whether there is a context registered with the given id.
     * 
     * @param informationID to check
     * @return true, if id exists
     */
    boolean hasId(String informationID);

    /**
     * @return all registered contexts.
     */
    Collection<ToolIntegrationContext> getAllIntegrationContexts();

}
