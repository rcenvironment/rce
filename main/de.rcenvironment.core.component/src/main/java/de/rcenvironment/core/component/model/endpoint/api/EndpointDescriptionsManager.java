/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Manages endpoint descriptions of a component. This class is not thread-safe. It is intended that this class is primary called by the GUI
 * thread.
 * 
 * @author Doreen Seider
 * 
 * FIXME: Use a factory to create {@link EndpointDescription} instances and just provide add, delete, etc. for
 * {@link EndpointDescription} instances --seid_do
 */
public class EndpointDescriptionsManager extends PropertiesChangeSupport implements Serializable {

    /** Property that is fired when an endpoint was changed. */
    public static final String PROPERTY_ENDPOINT = "de.rcenvironment.rce.component.endpoint.ComponentDescriptionsEndpoint";

    private static final String MESSAGE_DYNAMIC_ENDPOINT_DESCRIPTION_DOESNT_EXIST =
        "Dynamic endpoint description with name '%s' doesn't exist";

    private static final String NO_DYNAMIC_ENDPOINT_DEFINITION_WITH_ID_S_DECLARED = "No dynamic endpoint definition with id '%s' declared";

    private static final String MESSAGE_DESIRED_ENDPOINT_NAME_ALREADY_EXISTS = "Desired endpoint name already exists: ";

    private static final long serialVersionUID = -5776007445203691291L;

    private final EndpointType endpointType;

    private final EndpointDefinitionsProvider endpointDefinitionsProvider;

    private final Map<String, EndpointDescription> staticEndpointDescriptions;

    private Map<String, EndpointDescription> dynamicEndpointDescriptions = new HashMap<>();

    private final Map<String, EndpointGroupDescription> staticEndpointGroupDescriptions;

    private Map<String, EndpointGroupDescription> dynamicEndpointGroupDescriptions = new HashMap<>();

    public EndpointDescriptionsManager(EndpointDefinitionsProvider endpointDefinitionsProvider, EndpointType entdpointType) {
        this.endpointType = entdpointType;
        this.endpointDefinitionsProvider = endpointDefinitionsProvider;
        this.staticEndpointDescriptions = createStaticEndpointDescriptions(endpointDefinitionsProvider);
        this.staticEndpointGroupDescriptions = createStaticEndpointGroupDescriptions(endpointDefinitionsProvider);
    }

    private Map<String, EndpointDescription> createStaticEndpointDescriptions(EndpointDefinitionsProvider provider) {
        Map<String, EndpointDescription> descs = new HashMap<>();
        for (EndpointDefinition definition : provider.getStaticEndpointDefinitions()) {
            descs.put(definition.getName(), new EndpointDescription(definition, endpointType));
        }
        return descs;
    }

    private Map<String, EndpointGroupDescription> createStaticEndpointGroupDescriptions(EndpointDefinitionsProvider provider) {
        Map<String, EndpointGroupDescription> descs = new HashMap<>();
        for (EndpointGroupDefinition definition : provider.getStaticEndpointGroupDefinitions()) {
            descs.put(definition.getName(), new EndpointGroupDescription(definition));
        }
        return descs;
    }

    /**
     * Adds a dynamic {@link EndpointGroupDescription}.
     * 
     * @param endpointGroupId identifier of endpoint blueprint
     * @param name name of the {@link EndpointGroupDescription} to create
     */
    public void addDynamicEndpointGroupDescription(String endpointGroupId, String name) {
        addDynamicEndpointGroupDescription(endpointGroupId, name, true);
    }

    /**
     * Adds a dynamic {@link EndpointGroupDescription}.
     * 
     * @param endpointGroupId identifier of endpoint blueprint
     * @param name name of the {@link EndpointGroupDescription} to create
     * @param checkIfDeclared <code>true</code> if it should be checked that dynamic
     *        {@link EndpointDefinition} exists, otherwise <code>false</code>
     * @return {@link EndpointGroupDescription} added
     */
    public EndpointGroupDescription addDynamicEndpointGroupDescription(String endpointGroupId, String name, boolean checkIfDeclared) {

        if (checkIfDeclared && (!isDynamicEndpointGroupDefinitionDeclared(endpointGroupId) || !isValidEndpointGroupName(name))) {
            String message;
            if (!isDynamicEndpointGroupDefinitionDeclared(endpointGroupId)) {
                message = "No dynamic endpoint group definition with id '" + endpointGroupId + "' declared";
            } else {
                message = "Desired endpoint group name already exists: " + name;
            }
            throw new IllegalArgumentException(message);
        }
        EndpointGroupDescription desc = new EndpointGroupDescription(endpointDefinitionsProvider
            .getDynamicEndpointGroupDefinition(endpointGroupId));
        desc.setName(name);
        desc.setDynamicEndpointIdentifier(endpointGroupId);
        dynamicEndpointGroupDescriptions.put(name, desc);
        return desc;
    }

    /**
     * Removes dynamic {@link EndpointGroupDescription} with given name.
     * 
     * @param name name of {@link EndpoinGrouptDescription} to remove
     * @return {@link EndpointGroupDescription} object removed
     */
    public EndpointGroupDescription removeDynamicEndpointGroupDescription(String name) {
        return dynamicEndpointGroupDescriptions.remove(name);
    }

    /**
     * @return all dynamic {@link EndpointGroupDescription}s
     */
    public Set<EndpointGroupDescription> getEndpointGroupDescriptions() {
        Set<EndpointGroupDescription> descs = new HashSet<>();
        descs.addAll(staticEndpointGroupDescriptions.values());
        descs.addAll(dynamicEndpointGroupDescriptions.values());
        return Collections.unmodifiableSet(descs);
    }

    public Set<EndpointGroupDescription> getStaticEndpointGroupDescriptions() {
        return Collections.unmodifiableSet(new HashSet<>(staticEndpointGroupDescriptions.values()));
    }

    public Set<EndpointGroupDescription> getDynamicEndpointGroupDescriptions() {
        return Collections.unmodifiableSet(new HashSet<>(dynamicEndpointGroupDescriptions.values()));
    }

    /**
     * @param name name of the {@link EndpointGroupDescription} to get
     * @return {@link EndpointGroupDescription} with given group name or <code>null</code> if there
     *         is none
     */
    public EndpointGroupDescription getEndpointGroupDescription(String name) {
        EndpointGroupDescription desc = staticEndpointGroupDescriptions.get(name);
        if (desc == null) {
            desc = dynamicEndpointGroupDescriptions.get(name);
        }
        return desc;
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
     * @param names names of {@link EndpointDefinition}s to get
     * @return list of {@link EndpointDefinition}s for given names
     */
    public Set<EndpointDefinition> getStaticEndpointDefinitions(List<String> names) {
        Set<EndpointDefinition> endpointDefinitions = new HashSet<>();
        for (String name : names) {
            endpointDefinitions.add(getStaticEndpointDefinition(name));
        }
        return endpointDefinitions;
    }

    /**
     * @param name name of {@link EndpointDefinition} to get
     * @return {@link EndpointDefinition} with given name
     */
    public EndpointDefinition getStaticEndpointDefinition(String name) {
        return endpointDefinitionsProvider.getStaticEndpointDefinition(name);
    }

    public Set<EndpointDefinition> getDynamicEndpointDefinitions() {
        return Collections.unmodifiableSet(endpointDefinitionsProvider.getDynamicEndpointDefinitions());
    }
    
    /**
     * @param ids identifiers of {@link EndpointDefinition}s to get
     * @return list of {@link EndpointDefinition}s for given identifiers
     */
    public Set<EndpointDefinition> getDynamicEndpointDefinitions(List<String> ids) {
        Set<EndpointDefinition> endpointDefinitions = new HashSet<>();
        for (String id : ids) {
            endpointDefinitions.add(getDynamicEndpointDefinition(id));
        }
        return endpointDefinitions;
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
            clonedDescs.add(EndpointDescription.copy(desc));
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
            return EndpointDescription.copy(endpointDesc);
        }
        return null;
    }

    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as the underlying
     *        {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already
     *         exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData) throws IllegalArgumentException {
        if (!isDynamicEndpointDefinitionDeclared(endpointId)) {
            throw new IllegalArgumentException(StringUtils.format(NO_DYNAMIC_ENDPOINT_DEFINITION_WITH_ID_S_DECLARED, endpointId));
        }
        return addDynamicEndpointDescription(endpointId, name, dataType, metaData, UUID.randomUUID().toString(),
            getDynamicEndpointDefinition(endpointId).getParentGroupName(), true);
    }

    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as the underlying
     *        {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @param checkIfDeclared perform check if dynamic endpoint is declared
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already
     *         exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData, boolean checkIfDeclared) throws IllegalArgumentException {
        if (isDynamicEndpointDefinitionDeclared(endpointId)) {
            return addDynamicEndpointDescription(endpointId, name, dataType, metaData, UUID.randomUUID().toString(),
                getDynamicEndpointDefinition(endpointId).getParentGroupName(), checkIfDeclared);
        } else if (checkIfDeclared) {
            throw new IllegalArgumentException(StringUtils.format(NO_DYNAMIC_ENDPOINT_DEFINITION_WITH_ID_S_DECLARED, endpointId));
        } else {
            return addDynamicEndpointDescription(endpointId, name, dataType, metaData, UUID.randomUUID().toString(),
                null, checkIfDeclared);
        }
    }

    /**
     * Add a dynamic endpoint to the list of dynamic endpoints with a random identifier.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as the underlying
     *        {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @param parentGroup name of parent input group, <code>null</code> for using default group or
     *        none
     * @param checkIfDeclared perform check if dynamic endpoint is declared
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already
     *         exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData, String parentGroup, boolean checkIfDeclared) throws IllegalArgumentException {
        return addDynamicEndpointDescription(endpointId, name, dataType, metaData, UUID.randomUUID().toString(),
            parentGroup, checkIfDeclared);
    }
    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as the underlying
     *        {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @param identifier identifier of the endpoint
     * @param parentGroup name of parent input group, <code>null</code> for using default group or
     *        none
     * @param checkIfDeclared perform check if dynamic endpoint is declared
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already
     *         exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData, String identifier, String parentGroup, boolean checkIfDeclared) throws IllegalArgumentException {

        if (checkIfDeclared && (!isDynamicEndpointDefinitionDeclared(endpointId) || !isValidEndpointName(name))) {
            String message;
            if (!isDynamicEndpointDefinitionDeclared(endpointId)) {
                message = StringUtils.format(NO_DYNAMIC_ENDPOINT_DEFINITION_WITH_ID_S_DECLARED, endpointId);
            } else {
                message = MESSAGE_DESIRED_ENDPOINT_NAME_ALREADY_EXISTS + name;
            }
            throw new IllegalArgumentException(message);
        }
        
        EndpointDescription desc = new EndpointDescription(endpointDefinitionsProvider
            .getDynamicEndpointDefinition(endpointId), identifier);

        desc.setName(name);
        desc.setDynamicEndpointIdentifier(endpointId);
        desc.setDataType(dataType);
        for (String key : metaData.keySet()) {
            desc.setMetaDataValue(key, metaData.get(key));
        }
        if (parentGroup != null) { // default (if there is one) should not be used as explicit
                                   // parent group is given
            desc.setParentGroupName(parentGroup);
        }
        dynamicEndpointDescriptions.put(name, desc);
        
        firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Added, desc, null));

        return desc;
    }

    /**
     * Add a dynamic endpoint to the list of dynamic endpoints.
     * 
     * @param endpointId identifier of dynamic {@link EndpointDefinition} to chose as the underlying
     *        {@link EndpointDefinition}
     * @param name name to set
     * @param dataType data type to set
     * @param metaData meta data to set
     * @param identifier identifier of the endpoint
     * @return {@link EndpointDescription} object created and added or <code>null</code> if the name
     *         already exists
     * @throws IllegalArgumentException if dynamic endpoint description with given name already
     *         exists or new name is invalid
     */
    public EndpointDescription addDynamicEndpointDescription(String endpointId, String name, DataType dataType,
        Map<String, String> metaData, String identifier) throws IllegalArgumentException {
        if (!isDynamicEndpointDefinitionDeclared(endpointId)) {
            throw new IllegalArgumentException(StringUtils.format(NO_DYNAMIC_ENDPOINT_DEFINITION_WITH_ID_S_DECLARED, endpointId));
        }
        return addDynamicEndpointDescription(endpointId, name, dataType, metaData, identifier,
            getDynamicEndpointDefinition(endpointId).getParentGroupName(), true);
    }

    private boolean isDynamicEndpointGroupDefinitionDeclared(String endpointGroupId) {
        return endpointDefinitionsProvider.getDynamicEndpointGroupDefinition(endpointGroupId) != null;
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
     * Removes dynamic {@link EndpointDescription} with given name without fireing a property change
     * event.
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
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists or
     *         new name is invalid
     */
    public synchronized EndpointDescription editDynamicEndpointDescription(String oldName, String newName, DataType newDataType,
        Map<String, String> newMetaData, String newDynEndpointId) throws IllegalArgumentException {
        return editDynamicEndpointDescription(oldName, newName, newDataType, newMetaData, newDynEndpointId,
            getDynamicEndpointDefinition(newDynEndpointId).getParentGroupName());
    }

    /**
     * Edits a dynamic {@link EndpointDescription}.
     * 
     * @param oldName old name of {@link EndpointDescription}
     * @param newName new name of {@link EndpointDescription}
     * @param newDataType new {@link DataType}
     * @param newMetaData new meta data {@link Map}
     * @param newDynEndpointId new name of endpoint the input belongs to
     * @param newParentGroup new name of endpoint parent group the input belongs to
     * @return {@link EndpointDescription} edited
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists or
     *         new name is invalid
     */
    public synchronized EndpointDescription editDynamicEndpointDescription(String oldName, String newName, DataType newDataType,
        Map<String, String> newMetaData, String newDynEndpointId, String newParentGroup) throws IllegalArgumentException {

        EndpointDescription description = dynamicEndpointDescriptions.remove(oldName);
        if (description == null) {
            throw new IllegalArgumentException(StringUtils.format(MESSAGE_DYNAMIC_ENDPOINT_DESCRIPTION_DOESNT_EXIST, oldName));
        }
        EndpointDescription oldDescription = EndpointDescription.copy(description);
        try {
            if (!isValidEndpointName(newName)) {
                throw new IllegalArgumentException(MESSAGE_DESIRED_ENDPOINT_NAME_ALREADY_EXISTS + newName);
            }
            description.setName(newName);
            description.setDataType(newDataType);
            description.setMetaData(newMetaData);
            description.getMetaData().put(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE,
                oldDescription.getMetaDataValue(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE));
            if (newDynEndpointId != null) {
                description.setDynamicEndpointIdentifier(newDynEndpointId);
            }
            description.setParentGroupName(newParentGroup);
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
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists or
     *         new name is invalid
     */
    public synchronized EndpointDescription editDynamicEndpointDescription(String oldName, String newName, DataType newDataType,
        Map<String, String> newMetaData) throws IllegalArgumentException {
        if (getEndpointDescription(oldName) == null) {
            throw new IllegalArgumentException(StringUtils.format(MESSAGE_DYNAMIC_ENDPOINT_DESCRIPTION_DOESNT_EXIST, oldName));
        }
        return editDynamicEndpointDescription(oldName, newName, newDataType, newMetaData,
            getEndpointDescription(oldName).getDynamicEndpointIdentifier());
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

        return editStaticEndpointDescription(name, newDataType, newMetaData, true);
    }

    /**
     * Edits a static {@link EndpointDescription}.
     * 
     * @param name name of {@link EndpointDescription} to edit
     * @param newDataType new {@link DataType}
     * @param newMetaData new meta data {@link Map}
     * @param checkIfDeclared perform check if static endpoint is declared
     * @return {@link EndpointDescription} edited
     * @throws IllegalArgumentException if no dynamic endpoint description with given name exists
     */
    public synchronized EndpointDescription editStaticEndpointDescription(String name, DataType newDataType,
        Map<String, String> newMetaData, boolean checkIfDeclared) throws IllegalArgumentException {

        EndpointDescription description = staticEndpointDescriptions.get(name);
        if (description == null) {
            if (checkIfDeclared) {
                throw new IllegalArgumentException("Static endpoint description with name '" + name + "' doesn't exist");
            } else { // add description
                description = new EndpointDescription(null, endpointType);
                description.setName(name);
                description.setDataType(newDataType);
                description.setMetaData(newMetaData);
                staticEndpointDescriptions.put(name, description);
                return description;
            }
        }

        EndpointDescription oldDescription = EndpointDescription.copy(description);

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
     * @param name name to validate
     * @return <code>true</code> if no other endpoint group with given name exists, else
     *         <code>false</code>
     */
    public boolean isValidEndpointGroupName(String name) {
        return name != null && !name.isEmpty() && !staticEndpointGroupDescriptions.containsKey(name)
            && !dynamicEndpointGroupDescriptions.containsKey(name);
    }

    /**
     * @param endpointName name affected {@link EndpointDescription}
     * @param dataType {@link DataType} to add as a connected one
     */
    public void addConnectedDataType(String endpointName, DataType dataType) {

        EndpointDescription endpointDesc = getNotClonedEndpointDescription(endpointName);

        if (endpointDesc != null) {
            EndpointDescription oldEndpointDesc = EndpointDescription.copy(endpointDesc);
            endpointDesc.addConnectedDataType(dataType);
            firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Modified, endpointDesc, oldEndpointDesc));
        }
    }

    /**
     * @param endpointName name affected {@link EndpointDescription}
     * @param dataType {@link DataType} to remove as a connected one
     */
    public void removeConnectedDataType(String endpointName, DataType dataType) {
        EndpointDescription endpointDesc = getNotClonedEndpointDescription(endpointName);

        if (endpointDesc != null) {
            EndpointDescription oldEndpointDesc = EndpointDescription.copy(endpointDesc);
            endpointDesc.removeConnectedDataType(dataType);
            firePropertyChange(PROPERTY_ENDPOINT, new EndpointChange(EndpointChange.Type.Modified, endpointDesc, oldEndpointDesc));
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
