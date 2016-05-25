/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
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
        String[] parts = new String[10];
        parts[0] = endpoint.getInputName();
        TypedDatum value = endpoint.getValue();
        if (value.getDataType().equals(DataType.Internal)) {
            parts[1] = ((InternalTDImpl) value).serialize();
        } else {
            parts[1] = typedDatumSerializer.serialize(value);
        }
        parts[2] = endpoint.getInputsComponentExecutionIdentifier();
        parts[3] = endpoint.getInputsComponentInstanceName();
        parts[4] = endpoint.getInputsNodeId().getIdString();
        parts[5] = endpoint.getOutputsComponentExecutionIdentifier();
        parts[6] = endpoint.getOutputsNodeId().getIdString();
        parts[7] = endpoint.getWorkflowExecutionIdentifier();
        parts[8] = endpoint.getWorkflowNodeId().getIdString();
        if (endpoint.getDataManagementId() == null) {
            parts[9] = "";
        } else {
            parts[9] = String.valueOf(endpoint.getDataManagementId());
        }
        return StringUtils.escapeAndConcat(parts);
    }
    
    @Override
    public EndpointDatum deserializeEndpointDatum(String serializedEndpoint) {
        String[] parts = StringUtils.splitAndUnescape(serializedEndpoint);
        EndpointDatumImpl endpoint = new EndpointDatumImpl();
        try {
            endpoint.setValue(InternalTDImpl.fromString(parts[1]));
        } catch (IllegalArgumentException e) {
            endpoint.setValue(typedDatumSerializer.deserialize(parts[1]));
        }
        
        EndpointDatumRecipient endpointDatumRecipient = EndpointDatumRecipientFactory.createEndpointDatumRecipient(
            parts[0], parts[2], parts[3], NodeIdentifierFactory.fromNodeId(parts[4]));
        endpoint.setEndpointDatumRecipient(endpointDatumRecipient);
        endpoint.setOutputsComponentExecutionIdentifier(parts[5]);
        endpoint.setOutputsNodeId(NodeIdentifierFactory.fromNodeId(parts[6]));
        endpoint.setWorkflowExecutionIdentifier(parts[7]);
        endpoint.setWorkfowNodeId(NodeIdentifierFactory.fromNodeId(parts[8]));
        if (!parts[9].isEmpty()) {
            endpoint.setDataManagementId(Long.valueOf(parts[9]));
        }
        return endpoint;
    }
    
    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        typedDatumSerializer = typedDatumService.getSerializer();
    }
}
