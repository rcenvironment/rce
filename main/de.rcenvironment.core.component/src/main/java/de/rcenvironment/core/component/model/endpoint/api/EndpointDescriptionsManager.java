/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Manages endpoint descriptions of a component.
 * 
 * @author Doreen Seider
 */
public class EndpointDescriptionsManager extends PropertiesChangeSupport implements Serializable {

    /** Property that is fired when an endpoint was changed. */
    public static final String PROPERTY_ENDPOINT = "de.rcenvironment.rce.component.endpoint.ComponentDescriptionsEndpoint";

    private static final long serialVersionUID = -5776007445203691291L;

    private final EndpointType endpointType;

    private final EndpointDefinitionsProvider endpointDefinitionsProvider;

    private final Map<String, EndpointDescription> staticEndpointDescriptions;

    private Map<String, EndpointDescription> dynamicEndpointDescriptions = new HashMap<>();

    /**
     * @param staticEndpointDescriptions all endpoint descriptions
     */
    public EndpointDescriptionsManager(EndpointDefinitionsProvider endpointDefinitionsProvider, EndpointType entdpointType) {
        this.endpointType = entdpointType;
        this.endpointDefinitionsProvider = endpointDefinitionsProvider;
        this.staticEndpointDescriptions = createStaticEndpointDescriptions(endpointDefinitionsProvider);
    }

    private Map<String, EndpointDescription> createStaticEndpointDescriptions(EndpointDefinitionsProvider provider) {

        Map<String, EndpointDescription> descs = new HashMap<>();

        for (EndpointDefinition definition : provider.getStaticEndpointDefinitions()) {
            descs.put(definition.getName(), new EndpointDescription(definition, endpointType));
        }

        return descs;
    }
    
    public Set<EndpointGroupDefinition> getEndpointGroupDefinitions() {
        return Collections.unmodifiableSet(endpointDefinitionsProvider.getEndpointGroups());
    }

    /**
     * @param groupName name of the group to get
     * @return {@link EndpointGroupDefinition} with given group name or <code>null</code> if there is none
     */
    public EndpointGroupDefinition getEndpointGroupDefnition(String groupName) {
        return endpointDefinitionsProvider.getEndpointGroup(groupName);
    }

    /**
     * Adds initial endpoints, if there are some declared.
     */
    public void addInitialDynamicEndpointDescriptions() {

        for (EndpointDefinition definition : endpointDefinitionsProvider.getDynamicEndpointDefinitions()) {
            for (InitialDynamicEndpointDefinition initialDefinition : definition
                .getInitialDynamicEndpointDefinitions()) {
                addDynamicEndpointDescription(definition.getIdentifier(), initialDefinition.getName(),
                    initialDefinition.getDataType(), new HashMap<String, String>());
            }
        }

    }

    /**
     * @return all {@link EndpointDescription}s
     */
    public Set<EndpointDescription> getEndpointDescriptions() {
        Map<String, EndpointDescription> endpointDescs = new HashMap<String, EndpointDescription>();
        endpointDescs.putAll(staticEndpointDescriptions);
        endpointDescs.putAll(dynamicEndpointDescriptions);
        return cloneEndpointDescriptionValues(endpointDescs.values());
    }
    
    public Set<EndpointDescription> getStaticEndpointDescriptions() {
        return cloneEndpointDescriptionValues(staticEndpointDescriptions.values());
    }

    public Set<EndpointDescription> getDynamicEndpointDescriptions() {
        return cloneEndpointDescriptionValues(dynamicEndpointDescriptions.values());
    }

    public Set<EndpointDefinition> getStaticEndpointDefinitions() {
        return Collections.unmodifiableSet(endpointDefinitionsProvider.getStaticEndpointDefinitions());
    }

    /**
     * @param name name of {@link EndpointDescription} to get
     * @return {@link EndpointDefinition} with given name
     */
    public EndpointDefinition getStaticEndpointDefinition(String name) {
        return endpointDefinitionsProvider.getStaticEndpointDefinition(name);
    }

    public Set<EndpointDefinition> getDynamicEndpointDefinitions() {
        return Collections.unmodifiableSet(endpointDefinitionsProvider.getDynamicEndpointDefinitions());
    }

    /**
     * @param id identifier of {@link EndpointDefinition} to get
     * @return {@link EndpointDefinition} with given id
     */
    public EndpointDefinition getDynamicEndpointDefinition(String id) {
        return endpointDefinitionsProvider.getDynamicEndpointDefinition(id);
    }

    private Set<EndpointDescription> cloneEndpointDescriptionValues(Collection<EndpointDescription> endpointDescriptions) {
        Set<EndpointDescription> clonedDescs = new HashSet<EndpointDescription>();
        for (EndpointDescription desc : endpointDescriptions) {
            clonedDescs.add(desc.clone());
        }
        return clonedDescs;
    }

    /**
     * @param name name of {@link EndpointDescription} to get
     * @return {@link EndpointDescription} object with given name or <code>null</code> if given name
     *         doesn't exist
     */
    public EndpointDescription getEndpointDescription(String name) {
        EndpointDescription endpointDesc = getNotClonedEndpointDescription(name);
        if (endpointDesc != null) {
            return endpointDesc.clone();
        }
        return null;
    }

    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as
     *        the underlying {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData) throws IllegalArgumentException {

        return addDynamicEndpointDescription(endpointId, name, dataType, metaData, UUID.randomUUID().toString());
    }
    
    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as
     *        the underlying {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @param identifier identifier of the endpoint
     * @param checkIfDeclared perform check if dynamic endpoint is declared
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData, String identifier, boolean checkIfDeclared) throws IllegalArgumentException {
        
        if (checkIfDeclared && (!isDynamicEndpointDefinitionDeclared(endpointId) || !isValidEndpointName(name))) {
            String message;
            if (!isDynamicEndpointDefinitionDeclared(endpointId)) {
                message = "no dynamic endpoint description with id '" + endpointId + "' declared";
            } else {
                message = "desired endpoint name already exists: " + name;
            }
            throw new IllegalArgumentException(message);
        }
        EndpointDescription desc = new EndpointDescription(endpointDefinitionsProvider
            .getDynamicEndpointDefinition(endpointId), endpointType, identifier);

        desc.setName(name);
        desc.setDynamicEndpointIdentifier(endpointId);
        desc.setDataType(dataType);
        for (String key : metaData.keySet()) {
            desc.setMetaDataValue(key, metaData.get(key));
        }

        dynamicEndpointDescriptions.put(name, desc);

        firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Added, desc, null));
        
        return desc;
    }
    
    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as
     *        the underlying {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @param identifier identifier of the endpoint
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData, String identifier) throws IllegalArgumentException {
        return addDynamicEndpointDescription(endpointId, name, dataType, metaData, identifier, true);
    }
    
    private boolean isDynamicEndpointDefinitionDeclared(String endpointId) {
        return endpointDefinitionsProvider.getDynamicEndpointDefinition(endpointId) != null;
    }

    /**
     * Removes dynamic {@link EndpointDescription} with given name.
     * 
     * @param name name of {@link EndpointDescription} to remove
     * @return {@link EndpointDescription} object removed
     */
    public EndpointDescription removeDynamicEndpointDescription(String name) {
        EndpointDescription desc = dynamicEndpointDescriptions.remove(name);

        firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Removed, null, desc));

        return desc;
    }
    
    /**
     * Removes dynamic {@link EndpointDescription} with given name without fireing a property change event.
     * 
     * @param name name of {@link EndpointDescription} to remove
     * @return {@link EndpointDescription} object removed
     */
    public EndpointDescription removeDynamicEndpointDescriptionQuietely(String name) {
        return dynamicEndpointDescriptions.remove(name);
    }

    /**
     * Edits a dynamic {@link EndpointDescription}.
     * 
     * @param oldName old name of {@link EndpointDescription}
     * @param newName new name of {@link EndpointDescription}
     * @param newDataType new {@link DataType}
     * @param newMetaData new meta data {@link Map}
     * @param newDynEndpointId new name of endpoint the input belongs to
     * @return {@link EndpointDescription} edited
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists or new name is invalid
     */
    public synchronized EndpointDescription editDynamicEndpointDescription(String oldName, String newName, DataType newDataType,
        Map<String, String> newMetaData, String newDynEndpointId) throws IllegalArgumentException {

        EndpointDescription description = dynamicEndpointDescriptions.remove(oldName);
        if (description == null) {
            throw new IllegalArgumentException("dynamic endpoint description with name '" + oldName + "' doesn't exist");
        }
        EndpointDescription oldDescription = description.clone();
        try {
            if (!isValidEndpointName(newName)) {
                throw new IllegalArgumentException("desired endpoint name already exists: " + newName);
            }
            description.setName(newName);
            description.setDataType(newDataType);
            description.setMetaData(newMetaData);
            if (newDynEndpointId != null) {
                description.setDynamicEndpointIdentifier(newDynEndpointId);
            }
        } catch (IllegalArgumentException e) {
            dynamicEndpointDescriptions.put(oldName, description);
            throw e;
        }

        dynamicEndpointDescriptions.put(newName, description);
        
        firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Modified, description, oldDescription));

        return description;
    }
    
    /**
     * Edits a dynamic {@link EndpointDescription}.
     * 
     * @param oldName old name of {@link EndpointDescription}
     * @param newName new name of {@link EndpointDescription}
     * @param newDataType new {@link DataType}
     * @param newMetaData new meta data {@link Map}
     * @return {@link EndpointDescription} edited
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists or new name is invalid
     */
    public synchronized EndpointDescription editDynamicEndpointDescription(String oldName, String newName, DataType newDataType,
        Map<String, String> newMetaData) throws IllegalArgumentException {
        return editDynamicEndpointDescription(oldName, newName, newDataType, newMetaData, null);
    }
    
    /**
     * Adds a static {@link EndpointDescription}.
     * 
     * @param description {@link EndpointDescription} to add
     */
    public synchronized void addStaticEndpointDescription(EndpointDescription description) {
        staticEndpointDescriptions.put(description.getName(), description);
    }

    /**
     * Edits a static {@link EndpointDescription}.
     * 
     * @param name name of {@link EndpointDescription} to edit
     * @param newDataType new {@link DataType}
     * @param newMetaData new meta data {@link Map}
     * @return {@link EndpointDescription} edited
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists
     */
    public synchronized EndpointDescription editStaticEndpointDescription(String name, DataType newDataType,
        Map<String, String> newMetaData) throws IllegalArgumentException {

        EndpointDescription description = staticEndpointDescriptions.get(name);
        if (description == null) {
            throw new IllegalArgumentException("static endpoint description with name '" + name + "' doesn't exist");
        }
        
        EndpointDescription oldDescription = description.clone();
        
        description.setDataType(newDataType);
        description.setMetaData(newMetaData);
        
        firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Modified, description, oldDescription));
        
        return description;
    }
    
    /**
     * @param name name to validate
     * @return <code>true</code> if no other endpoint with given name exists, else
     *         <code>false</code>
     */
    public boolean isValidEndpointName(String name) {
        return name != null && !name.isEmpty() && !staticEndpointDescriptions.containsKey(name)
            && !dynamicEndpointDescriptions.containsKey(name);
    }

    /**
     * @param endpointName name affected {@link EndpointDescription}
     * @param dataType {@link DataType} to add as a connected one
     */
    public void addConnectedDataType(String endpointName, DataType dataType) {
        EndpointDescription endpointDesc = getNotClonedEndpointDescription(endpointName);
        if (endpointDesc != null) {
            endpointDesc.addConnectedDataType(dataType);
        }
    }

    /**
     * @param endpointName name affected {@link EndpointDescription}
     * @param dataType {@link DataType} to remove as a connected one
     */
    public void removeConnectedDataType(String endpointName, DataType dataType) {
        EndpointDescription endpointDesc = getNotClonedEndpointDescription(endpointName);
        if (endpointDesc != null) {
            endpointDesc.removeConnectedDataType(dataType);
        }
    }

    public EndpointType getManagedEndpointType() {
        return endpointType;
    }

    private EndpointDescription getNotClonedEndpointDescription(String name) {
        EndpointDescription endpointDesc = staticEndpointDescriptions.get(name);
        if (endpointDesc == null) {
            return dynamicEndpointDescriptions.get(name);
        }
        return endpointDesc;
    }

}
