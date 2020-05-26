/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.model.endpoint.api.EndpointChange;
import de.rcenvironment.core.component.model.endpoint.api.EndpointChange.Type;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * {@link PropertyChangeListener} for {@link EndpointChange}s.
 *
 * @author Christian Weiss
 * @author Doreen Seider
 */
public class EndpointChangeListener implements PropertyChangeListener {

    private final WorkflowDescription workflowDesc;

    public EndpointChangeListener(WorkflowDescription newWorkflowDesc) {
        workflowDesc = newWorkflowDesc;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (!(event.getNewValue() instanceof EndpointChange)) {
            LogFactory.getLog(getClass()).debug(
                    "ignoring property change event because it is not of type " + EndpointChange.class.getSimpleName());
            return;
        }
        EndpointChange epChange = (EndpointChange) event.getNewValue();

        if (epChange.getType() == Type.Removed) {
            onRemoved(epChange);
        } else if (epChange.getType() == Type.Modified) {
            onModified(epChange);
        }
    }

    private void onRemoved(EndpointChange epChange) {
        // if an endpoint was removed, automatically remove the associated connections
        final List<Connection> superfluousConnections = new LinkedList<Connection>();

        final String removedEndpointId = epChange.getOldEndpointDescription().getIdentifier();
        // check for each connection if the removed endpoint is one of the connection
        // endpoints
        for (Connection connection : workflowDesc.getConnections()) {
            final boolean sourceRemoved = connection.getOutput().getIdentifier().equals(removedEndpointId);
            final boolean targetRemoved = connection.getInput().getIdentifier().equals(removedEndpointId);
            // if one of the endpoints of the connection was removed, the connection is
            // superfluous
            if (sourceRemoved || targetRemoved) {
                superfluousConnections.add(connection);
            }
        }
        // remove all connections that have been identified as superfluous
        workflowDesc.removeConnections(superfluousConnections);
    }

    private void onModified(EndpointChange epChange) {
        if (epChange.getEndpointDescription().getDataType() != epChange.getOldEndpointDescription().getDataType()) {
            onEndpointDataTypeChanged(epChange);
        } else if (!epChange.getEndpointDescription().getName()
                .equals(epChange.getOldEndpointDescription().getName())) {
            for (Connection connection : workflowDesc.getConnections()) {
                if (connection.getInput().getIdentifier().equals(epChange.getEndpointDescription().getIdentifier())) {
                    connection.getInput().setName(epChange.getEndpointDescription().getName());
                } else if (connection.getOutput().getIdentifier()
                        .equals(epChange.getEndpointDescription().getIdentifier())) {
                    connection.getOutput().setName(epChange.getEndpointDescription().getName());
                }
            }
        }
    }

    private void onEndpointDataTypeChanged(EndpointChange epChange) {
        DataType newDataType = epChange.getEndpointDescription().getDataType();
        if (epChange.getOldEndpointDescription().isDataTypeValid(newDataType)) {
            for (Connection connection : workflowDesc.getConnections()) {
                if (connection.getInput().getIdentifier().equals(epChange.getEndpointDescription().getIdentifier())) {
                    EndpointDescriptionsManager outputDescManager = workflowDesc
                            .getWorkflowNode(connection.getSourceNode().getIdentifierAsObject())
                            .getComponentDescription().getOutputDescriptionsManager();
                    outputDescManager.removeConnectedDataType(connection.getOutput().getName(),
                            epChange.getOldEndpointDescription().getDataType());
                    outputDescManager.addConnectedDataType(connection.getOutput().getName(), newDataType);
                    connection.getInput().setDataType(newDataType);
                } else if (connection.getOutput().getIdentifier()
                        .equals(epChange.getEndpointDescription().getIdentifier())) {
                    EndpointDescriptionsManager inputDescManager = workflowDesc
                            .getWorkflowNode(connection.getTargetNode().getIdentifierAsObject())
                            .getComponentDescription().getInputDescriptionsManager();
                    inputDescManager.removeConnectedDataType(connection.getInput().getName(),
                            epChange.getOldEndpointDescription().getDataType());
                    inputDescManager.addConnectedDataType(connection.getInput().getName(), newDataType);
                    connection.getOutput().setDataType(newDataType);
                }
            }
        } else {
            onRemoved(epChange);
        }
    }
}
