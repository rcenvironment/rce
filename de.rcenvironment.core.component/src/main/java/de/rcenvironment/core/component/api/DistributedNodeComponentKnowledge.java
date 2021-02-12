/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import de.rcenvironment.core.component.management.api.DistributedComponentEntry;

/**
 * Immutable holder for a set of {@link DistributedComponentEntry}s on a given remote node. DistributedComponentEntries are identified by
 * their componentId. Each known componentId is marked as being either accessible or inaccessible, no componentId is both accessible and
 * inaccessible.
 * 
 * In contrast to {@link DistributedComponentKnowledge}, this class only holds the installation for a single logical node instead of all
 * installations in the complete known network.
 *
 * @author Alexander Weinert
 */
public interface DistributedNodeComponentKnowledge {

    /**
     * If there already exists an accessible or inaccessible entry for the given component id, that entry is discarded and only the new,
     * given entry is stored.
     * 
     * @param componentId The id of the newly stored component.
     * @param entry       The DistributedComponentEntry describing the newly stored component.
     * @return A new instance of {@link DistributedNodeComponentKnowledge} that marks the given componentId as accessible and associates it
     *         with the given DistributedComponentEntry.
     */
    DistributedNodeComponentKnowledge putAccessibleComponent(String componentId, DistributedComponentEntry entry);

    /**
     * If there already exists an accessible or inaccessible entry for the given component id, that entry is discarded and only the new,
     * given entry is stored.
     * 
     * @param componentId The id of the newly stored component.
     * @param entry       The DistributedComponentEntry describing the newly stored component.
     * @return A new instance of {@link DistributedNodeComponentKnowledge} that marks the given componentId as inaccessible and associates
     *         it with the given DistributedComponentEntry.
     */
    DistributedNodeComponentKnowledge putInaccessibleComponent(String componentId, DistributedComponentEntry entry);

    /**
     * Convenience method.
     * 
     * @param componentId The id for which the associated DistributedComponentEntry shall be returned.
     * @return The DistributedComponentEntry associated with the given componentId. May be null.
     */
    default DistributedComponentEntry getComponent(String componentId) {
        final DistributedComponentEntry accessibleEntry = getAccessibleComponent(componentId);
        if (accessibleEntry != null) {
            return accessibleEntry;
        } else {
            return getInaccessibleComponent(componentId);
        }
    }

    /**
     * This method returns null if either there is no DistributedComponentEntry associated with the given id, or if there is such an entry,
     * but it is marked as being inaccessible.
     * 
     * @param componentId The id for which the associated DistributedComponentEntry shall be returned.
     * @return The DistributedComponentEntry associated with the given componentId. May be null.
     */
    DistributedComponentEntry getAccessibleComponent(String componentId);

    /**
     * This method returns null if either there is no DistributedComponentEntry associated with the given id, or if there is such an entry,
     * but it is marked as being accessible.
     * 
     * @param componentId The id for which the associated DistributedComponentEntry shall be returned.
     * @return The DistributedComponentEntry associated with the given componentId. May be null.
     */
    DistributedComponentEntry getInaccessibleComponent(String componentId);

    /**
     * @param componentId The id of the component to be removed
     * @return A new {@link DistributedNodeComponentKnowledge} that is identical to this one, but does not contain any association for the
     *         given componentId.
     */
    DistributedNodeComponentKnowledge removeComponent(String componentId);

    /**
     * Convenience method.
     * 
     * @param componentId The componentId for which the existence of a DistributedComponentEntry is to be determined.
     * @return True if the given componentId is associated with a DistributedComponentEntry. False otherwise.
     */
    default boolean componentExists(String componentId) {
        return isComponentAccessible(componentId) || isComponentInaccessible(componentId);
    }

    /**
     * @param componentId The componentId for which the existence of a DistributedComponentEntry is to be determined.
     * @return True if the given componentId is accessible and associated with a DistributedComponentEntry. False otherwise.
     */
    boolean isComponentAccessible(String componentId);

    /**
     * @param componentId The componentId for which the existence of a DistributedComponentEntry is to be determined.
     * @return True if the given componentId is inaccessible and associated with a DistributedComponentEntry. False otherwise.
     */
    boolean isComponentInaccessible(String componentId);

    /**
     * The returned collection does not back the {@link DistributedNodeComponentKnowledge}.
     * 
     * @return A collection of all known components. Is never null.
     */
    default Collection<DistributedComponentEntry> getComponents() {
        final Collection<DistributedComponentEntry> returnValue = new HashSet<>();
        returnValue.addAll(getAccessibleComponents());
        returnValue.addAll(getInaccessibleComponents());
        return returnValue;
    }

    /**
     * The returned collection does not back the {@link DistributedNodeComponentKnowledge}.
     * 
     * @return A collection of all known accessible components. Is never null.
     */
    Collection<DistributedComponentEntry> getAccessibleComponents();

    /**
     * The returned collection does not back the {@link DistributedNodeComponentKnowledge}.
     * 
     * @return A collection of all known inaccessible components. Is never null.
     */
    Collection<DistributedComponentEntry> getInaccessibleComponents();
    
    /**
     * @deprecated Instead of the maps backing this object, the object itself should be used, as it exposes the methods of the maps and adds
     *             consistency checks.
     * @return A map mapping componentIds of accessible components to their respective entry.
     */
    @Deprecated
    Map<String, DistributedComponentEntry> getAccessibleComponentMap();
    
    /**
     * @deprecated Instead of the maps backing this object, the object itself should be used, as it exposes the methods of the maps and adds
     *             consistency checks.
     * @return A map mapping componentIds of inaccessible components to their respective entry.
     */
    @Deprecated
    Map<String, DistributedComponentEntry> getInaccessibleComponentMap();
}
