/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.internal.InternalTDImpl;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * (De-)Serializes {@link EndpointDatum} objects.
 * 
 * @author Doreen Seider
 */
public final class EndpointDatumSerializer {
    
    protected static TypedDatumSerializer typedDatumSerializer;

    @Deprecated
    public EndpointDatumSerializer() {}
    
    /**
     * Serializes an {@link EndpointDatum}.
     * @param endpoint {@link EndpointDatum} to serialize
     * @return serialized {@link EndpointDatum}
     */
    public static String serializeEndpointDatum(EndpointDatum endpoint) {
        String[] parts = new String[7];
        parts[0] = endpoint.getInputName();
        if (endpoint.getValue().getDataType().equals(DataType.Internal)) {
            parts[1] = ((InternalTDImpl) endpoint.getValue()).serialize();
        } else {
            parts[1] = typedDatumSerializer.serialize(endpoint.getValue());
        }
        parts[2] = endpoint.getInputComponentExecutionIdentifier();
        parts[3] = endpoint.getInputsNodeId().getIdString();
        parts[4] = endpoint.getWorkflowExecutionIdentifier();
        parts[5] = endpoint.getWorkflowNodeId().getIdString();
        if (endpoint.getDataManagementId() == null) {
            parts[6] = "";
        } else {
            parts[6] = String.valueOf(endpoint.getDataManagementId());
        }
        return StringUtils.escapeAndConcat(parts);
    }
    
    /**
     * Deserializes an {@link EndpointDatum}.
     * 
     * @param serializedEndpoint {@link EndpointDatum} to deserialize
     * @return deserialized {@link EndpointDatum} object
     */
    public static EndpointDatum deserializeEndpointDatum(String serializedEndpoint) {
        String[] parts = StringUtils.splitAndUnescape(serializedEndpoint);
        EndpointDatumImpl endpoint = new EndpointDatumImpl();
        endpoint.setIdentifier(parts[0]);
        try {
            endpoint.setValue(InternalTDImpl.fromString(parts[1]));
        } catch (RuntimeException e) {
            endpoint.setValue(typedDatumSerializer.deserialize(parts[1]));
        }
        endpoint.setInputsComponentExecutionIdentifier(parts[2]);
        endpoint.setInputsNode(NodeIdentifierFactory.fromNodeId(parts[3]));
        endpoint.setWorkflowExecutionIdentifier(parts[4]);
        endpoint.setWorkfowNodeId(NodeIdentifierFactory.fromNodeId(parts[5]));
        if (!parts[6].isEmpty()) {
            endpoint.setDataManagementId(Long.valueOf(parts[6]));
        }
        return endpoint;
    }
    
    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        typedDatumSerializer = typedDatumService.getSerializer();
    }
}
