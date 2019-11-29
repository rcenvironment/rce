/*
 * Copyright (C) 2006-2019 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import de.rcenvironment.core.component.api.DistributedNodeComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;


/**
 * Default implementation of {@link DistributedNodeComponentKnowledge}.
 *
 * @author Alexander Weinert
 */
public final class DistributedNodeComponentKnowledgeImpl implements DistributedNodeComponentKnowledge {

    private Map<String, DistributedComponentEntry> accessibleComponents;
    private Map<String, DistributedComponentEntry> inaccessibleComponents;
    
    private DistributedNodeComponentKnowledgeImpl(Map<String, DistributedComponentEntry> accessibleComponentsParam,
        Map<String, DistributedComponentEntry> inaccessibleComponentsParam) {
        this.accessibleComponents = new HashMap<>(accessibleComponentsParam);
        this.inaccessibleComponents = new HashMap<>(inaccessibleComponentsParam);
    }
    
    /**
     * Factory method.
     * 
     * @return An instance of {@link DistributedNodeComponentKnowledge} that contains no knowledge about any remote components.
     */
    public static DistributedNodeComponentKnowledge createEmpty() {
        return new DistributedNodeComponentKnowledgeImpl(new HashMap<>(), new HashMap<>());
    }
    
    /**
     * Factory method. The constructed object holds no references to the given maps, albeit it does share the contained
     * {@link DistributedComponentEntry}s.
     * 
     * @param accessibleComponentsParam   A map mapping ids of accessible components to their corresponding
     *                                    {@link DistributedComponentEntry}.
     * @param inaccessibleComponentsParam A map mapping ids of inaccessible components to their corresponding
     *                                    {@link DistributedComponentEntry}.
     * @return A newly created instance of {@link DistributedNodeComponentKnowledge} that represents the information stored in the given
     *         map.
     */
    public static DistributedNodeComponentKnowledge fromMap(Map<String, DistributedComponentEntry> accessibleComponentsParam,
        Map<String, DistributedComponentEntry> inaccessibleComponentsParam) {
        return new DistributedNodeComponentKnowledgeImpl(accessibleComponentsParam, inaccessibleComponentsParam);
    }

    @Override
    public DistributedNodeComponentKnowledge putAccessibleComponent(String componentId, DistributedComponentEntry entry) {
        final Map<String, DistributedComponentEntry> newInaccessibleComponents = new HashMap<>(this.inaccessibleComponents);
        if (newInaccessibleComponents.containsKey(componentId)) {
            newInaccessibleComponents.remove(componentId);
        }

        final Map<String, DistributedComponentEntry> newAccessibleComponents = new HashMap<>(this.accessibleComponents);
        newAccessibleComponents.put(componentId, entry);

        return new DistributedNodeComponentKnowledgeImpl(newAccessibleComponents, newInaccessibleComponents);
    }

    @Override
    public DistributedNodeComponentKnowledge putInaccessibleComponent(String componentId, DistributedComponentEntry entry) {
        final Map<String, DistributedComponentEntry> newAccessibleComponents = new HashMap<>(this.accessibleComponents);
        if (newAccessibleComponents.containsKey(componentId)) {
            newAccessibleComponents.remove(componentId);
        }

        final Map<String, DistributedComponentEntry> newInaccessibleComponents = new HashMap<>(this.inaccessibleComponents);
        newInaccessibleComponents.put(componentId, entry);

        return new DistributedNodeComponentKnowledgeImpl(newAccessibleComponents, newInaccessibleComponents);
    }
    
    @Override
    public DistributedComponentEntry getAccessibleComponent(String componentId) {
        return this.accessibleComponents.get(componentId);
    }
    
    @Override
    public DistributedComponentEntry getInaccessibleComponent(String componentId) {
        return this.inaccessibleComponents.get(componentId);
    }
    
    @Override
    public DistributedNodeComponentKnowledge removeComponent(String componentId) {
        final Map<String, DistributedComponentEntry> newAccessibleComponents = new HashMap<>(this.accessibleComponents);
        newAccessibleComponents.remove(componentId);

        final Map<String, DistributedComponentEntry> newInaccessibleComponents = new HashMap<>(this.inaccessibleComponents);
        newInaccessibleComponents.remove(componentId);
        
        return new DistributedNodeComponentKnowledgeImpl(newAccessibleComponents, newInaccessibleComponents);
    }
    
    @Override
    public boolean isComponentAccessible(String componentId) {
        return this.accessibleComponents.containsKey(componentId);
    }
    
    @Override
    public boolean isComponentInaccessible(String componentId) {
        return this.inaccessibleComponents.containsKey(componentId);
    }
    
    @Override
    public Collection<DistributedComponentEntry> getComponents() {
        // We override the default method in order to not needlessly create additional hashsets by calling getAccessibleComponents and
        // getInaccessibleComponents just to discard the result immediately. This may be premature optimization, but does not incur any
        // significant computational or organizational cost.
        final Collection<DistributedComponentEntry> returnValue = new HashSet<>();
        returnValue.addAll(this.accessibleComponents.values());
        returnValue.addAll(this.inaccessibleComponents.values());
        return returnValue;
    }
    
    @Override
    public Collection<DistributedComponentEntry> getAccessibleComponents() {
        return new HashSet<>(this.accessibleComponents.values());
    }
    
    @Override
    public Collection<DistributedComponentEntry> getInaccessibleComponents() {
        return new HashSet<>(this.inaccessibleComponents.values());
    }

    @Override
    public Map<String, DistributedComponentEntry> getAccessibleComponentMap() {
        return new HashMap<>(this.accessibleComponents);
    }

    @Override
    public Map<String, DistributedComponentEntry> getInaccessibleComponentMap() {
        return new HashMap<>(this.inaccessibleComponents);
    }
}
