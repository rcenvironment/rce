/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Provides information about a single endpoint.
 * 
 * @author Doreen Seider
 */
public class EndpointDescription extends EndpointGroupDescription {

    private static final long serialVersionUID = -3853446362359127472L;

    private static TypedDatumConverter typedDatumConverter;

    private EndpointDefinition endpointDefinition;

    private DataType dataType;

    private Map<String, String> metaData;

    private Map<DataType, Integer> connectedDataTypes;

    private String identifier;
    
    @Deprecated
    public EndpointDescription() {}

    public EndpointDescription(EndpointDefinition newEndpointDefinition, String identifier) {
        super(newEndpointDefinition);
        this.endpointDefinition = newEndpointDefinition;
        this.identifier = identifier;

        metaData = new HashMap<String, String>();

        if (endpointDefinition != null) {
            dataType = endpointDefinition.getDefaultDataType();
            EndpointMetaDataDefinition metaDataDesc = endpointDefinition.getMetaDataDefinition();
            for (String key : metaDataDesc.getMetaDataKeys()) {
                if (metaDataDesc.getDefaultValue(key) != null) {
                    metaData.put(key, metaDataDesc.getDefaultValue(key));
                }
            }
        }
        connectedDataTypes = new HashMap<DataType, Integer>();
    }

    public EndpointDescription(EndpointDefinition newEndpointDefinition, EndpointType endpointType) {
        this(newEndpointDefinition, UUID.randomUUID().toString());
    }

    /**
     * @return backing {@link EndpointDefinition}
     */
    public EndpointDefinition getEndpointDefinition() {
        return endpointDefinition;
    }

    public DataType getDataType() {
        return dataType;
    }

    /**
     * @param type data type to set.
     */
    public void setDataType(DataType type) {
        if (endpointDefinition != null && !endpointDefinition.getPossibleDataTypes().contains(type)) {
            throw new IllegalArgumentException("Given data type is invalid: " + type);
        }
        this.dataType = type;
    }

    /**
     * @param type {@link DataType} to check
     * @return <code>true</code> if is is valid (is possible data type and all connected outputs are compatible), otherwise
     *         <code>false</code>
     */
    public boolean isDataTypeValid(DataType type) {
        if (endpointDefinition == null || !endpointDefinition.getPossibleDataTypes().contains(type)) {
            return false;
        }
        for (DataType t : connectedDataTypes.keySet()) {
            if (endpointDefinition.getEndpointType() == EndpointType.INPUT) {
                if (t != type && !typedDatumConverter.isConvertibleTo(t, type)) {
                    return false;
                }
            } else {
                if (t != type && !typedDatumConverter.isConvertibleTo(type, t)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param key key of meta data to get
     * @return meta data value for this key or <code>null</code> if there is no one defined for this key
     */
    public String getMetaDataValue(String key) {
        return metaData.get(key);
    }

    /**
     * @param configuration current configuration used to get active meta data
     * @return currently active meta data
     */
    public Map<String, String> getActiveMetaData(Map<String, String> configuration) {
        Map<String, String> activeMetaData = new HashMap<>();
        for (String metaDataKey : metaData.keySet()) {
            Map<String, List<String>> activationFilter = endpointDefinition
                .getMetaDataDefinition().getActivationFilter(metaDataKey);
            for (String configKey : activationFilter.keySet()) {
                if (configuration.containsKey(configKey) && activationFilter.get(configKey).contains(configuration.get(configKey))) {
                    activeMetaData.put(metaDataKey, metaData.get(metaDataKey));
                }
            }
        }
        return activeMetaData;
    }

    /**
     * @return currently active meta data
     */
    public Map<String, String> getMetaDataToPersist() {
        Map<String, String> persistentMetaData = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (String metaDataKey : metaData.keySet()) {
            if (endpointDefinition.getMetaDataDefinition().isPersistent(metaDataKey)) {
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("guiName", endpointDefinition.getMetaDataDefinition().getGuiName(metaDataKey));
                rootNode.put("value", getMetaDataValue(metaDataKey));
                persistentMetaData.put(metaDataKey, rootNode.toString());
            }
        }
        return persistentMetaData;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    /**
     * @param key key of meta data to set
     * @param value meta data value to set
     */
    public void setMetaDataValue(String key, String value) {
        metaData.put(key, value);
    }

    /**
     * @param newMetaData meta data to set
     */
    public void setMetaData(Map<String, String> newMetaData) {
        metaData = newMetaData;
    }

    /**
     * @param type data type this endpoint is connected to
     */
    public void addConnectedDataType(DataType type) {
        if (connectedDataTypes.containsKey(type) && connectedDataTypes.get(type) != 0) {
            connectedDataTypes.put(type, connectedDataTypes.get(type) + 1);
        } else {
            connectedDataTypes.put(type, 1);
        }
    }

    /**
     * @param type data type this endpoint is not connected to anymore
     */
    public void removeConnectedDataType(DataType type) {
        if (connectedDataTypes.containsKey(type)) {
            connectedDataTypes.put(type, connectedDataTypes.get(type) - 1);

            if (connectedDataTypes.get(type) <= 0) {
                connectedDataTypes.remove(type);
            }
        }
    }

    public List<DataType> getConnectedDataTypes() {
        return new ArrayList<DataType>(connectedDataTypes.keySet());
    }

    public boolean isConnected() {
        return !connectedDataTypes.isEmpty();
    }

    /**
     * @return <code>true</code> if the input execution constraint is 'required'
     */
    public boolean isRequired() {
        String currentConstraint = getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);
        if (currentConstraint != null) {
            if (currentConstraint.equals(InputExecutionContraint.Required.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies the {@link EndpointDescription} instance.
     * 
     * @param endpointDescription {@link EndpointDescription} to copy
     * @return {@link EndpointDescription} copied
     */
    public static EndpointDescription copy(EndpointDescription endpointDescription) {
        EndpointDescription copiedDesc = new EndpointDescription(endpointDescription.getEndpointDefinition(),
            endpointDescription.getIdentifier());
        copiedDesc.setDataType(endpointDescription.getDataType());
        copiedDesc.setDynamicEndpointIdentifier(endpointDescription.getDynamicEndpointIdentifier());
        copiedDesc.setParentGroupName(endpointDescription.getParentGroupName());
        for (DataType type : endpointDescription.getConnectedDataTypes()) {
            copiedDesc.addConnectedDataType(type);
        }
        for (String key : endpointDescription.getMetaData().keySet()) {
            if (endpointDescription.getMetaData().get(key) != null) {
                copiedDesc.setMetaDataValue(key, endpointDescription.getMetaData().get(key));
            }
        }
        try {
            copiedDesc.setName(endpointDescription.getName());
        } catch (UnsupportedOperationException e) {
            // nothing to do here. it says that we have an static endpoint here. name is
            // unmodifiable and already defined in declarative description
            e = null;
        }
        return copiedDesc;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String newIdentifier) {
        identifier = newIdentifier;
    }

    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        typedDatumConverter = typedDatumService.getConverter();
    }

}
