/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.util.Collection;

/**
 * Registry for all {@link ToolIntegrationContext}s. After the registration, the tools that are stored in the given place are read and
 * registered as workflow components.
 * 
 * @author Sascha Zur
 * @author Robert Mischke (rework/cleanup)
 */
public interface ToolIntegrationContextRegistry {

    /**
     * @param contextId the context id to look for; case sensitive
     * @return the {@link ToolIntegrationContext} for the given id; null if not available.
     */
    ToolIntegrationContext getToolIntegrationContextById(String contextId);

    /**
     * @param type the type (e.g. "common") to look for; not case sensitive
     * @return the {@link ToolIntegrationContext} for the given type; null if not available.
     */
    ToolIntegrationContext getToolIntegrationContextByType(String type);

    /**
     * Checks whether there is a context matching the prefix of the given tool id. (Note: apparently, this method has undergone semantic
     * changes over time; all uses should be checked for correctness.)
     * 
     * @param informationID to check
     * @return true, if id exists
     */
    boolean hasTIContextMatchingPrefix(String informationID);

    /**
     * @return all registered contexts.
     */
    Collection<ToolIntegrationContext> getAllIntegrationContexts();

    /**
     * @return the next unitialized {@link ToolIntegrationContext}, or null if no further context is queued; the element is removed from the
     *         internal global queue
     */
    ToolIntegrationContext fetchNextUninitializedToolIntegrationContext();

}
