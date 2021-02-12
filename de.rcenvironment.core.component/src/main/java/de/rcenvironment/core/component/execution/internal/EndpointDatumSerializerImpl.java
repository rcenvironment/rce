/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
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
 * @author Robert Mischke (8.0.0 id adaptations)
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
        parts[4] = endpoint.getDestinationNodeId().getLogicalNodeIdString();
        parts[5] = endpoint.getOutputsComponentExecutionIdentifier();
        parts[6] = endpoint.getOutputsNodeId().getLogicalNodeIdString();
        parts[7] = endpoint.getWorkflowExecutionIdentifier();
        parts[8] = endpoint.getWorkflowControllerLocation().getLogicalNodeIdString();
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
        // the distinction between internal TDs and the other "normal" TDs should be improved; trial and error approach should be avoided;
        // e.g. provide type information in the serialized endpoint string explicitly or even better use the TypedDatumSerializer also for
        // internal TDs (note: TypedDatumSerializer is also used by components which should not (de-)serialize or even see internal TDs)
        // --seid_do
        TypedDatum typedDatum = InternalTDImpl.fromString(parts[1]);
        if (typedDatum == null) {
            typedDatum = typedDatumSerializer.deserialize(parts[1]);
        }
        endpoint.setValue(typedDatum);

        EndpointDatumRecipient endpointDatumRecipient =
            EndpointDatumRecipientFactory.createEndpointDatumRecipient(parts[0], parts[2], parts[3],
                NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(parts[4]));
        endpoint.setEndpointDatumRecipient(endpointDatumRecipient);
        endpoint.setOutputsComponentExecutionIdentifier(parts[5]);
        endpoint.setOutputsNodeId(NodeIdentifierUtils
            .parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(parts[6]));
        endpoint.setWorkflowExecutionIdentifier(parts[7]);
        endpoint.setWorkflowNodeId(NodeIdentifierUtils
            .parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(parts[8]));
        if (!parts[9].isEmpty()) {
            endpoint.setDataManagementId(Long.valueOf(parts[9]));
        }
        return endpoint;
    }

    protected void bindTypedDatumService(TypedDatumService typedDatumService) {
        typedDatumSerializer = typedDatumService.getSerializer();
    }
}
