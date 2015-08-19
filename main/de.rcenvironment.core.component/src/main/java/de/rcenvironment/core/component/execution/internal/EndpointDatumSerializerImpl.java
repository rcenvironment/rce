/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumImpl;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link EndpointDatumSerializer}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumSerializerImpl implements EndpointDatumSerializer {
    
    private TypedDatumSerializer typedDatumSerializer;

    @Override
    public String serializeEndpointDatum(EndpointDatum endpoint) {
        String[] parts = new String[7];
        parts[0] = endpoint.getInputName();
        TypedDatum value = endpoint.getValue();
        if (value.getDataType().equals(DataType.Internal)) {
            parts[1] = ((InternalTDImpl) value).serialize();
        } else {
            parts[1] = typedDatumSerializer.serialize(value);
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
    
    @Override
    public EndpointDatum deserializeEndpointDatum(String serializedEndpoint) {
        String[] parts = StringUtils.splitAndUnescape(serializedEndpoint);
        EndpointDatumImpl endpoint = new EndpointDatumImpl();
        endpoint.setInputName(parts[0]);
        try {
            endpoint.setValue(InternalTDImpl.fromString(parts[1]));
        } catch (IllegalArgumentException e) {
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
