/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.impl;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Implementation of {@link EndpointDatumRecipient}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class EndpointDatumRecipientImpl implements EndpointDatumRecipient {

    private String inputIdentifier;

    private String inputsComponentExecutionIdentifier;

    private String inputsComponentInstanceName;

    private LogicalNodeId destinationNodeId;

    private NetworkDestination networkDestination;

    @Override
    public String getInputName() {
        return inputIdentifier;
    }

    @Override
    public String getInputsComponentExecutionIdentifier() {
        return inputsComponentExecutionIdentifier;
    }

    @Override
    public String getInputsComponentInstanceName() {
        return inputsComponentInstanceName;
    }

    @Override
    public LogicalNodeId getDestinationNodeId() {
        return destinationNodeId;
    }

    @Override
    public NetworkDestination getNetworkDestination() {
        return networkDestination;
    }

    public void setIdentifier(String identifier) {
        this.inputIdentifier = identifier;
    }

    public void setInputsComponentExecutionIdentifier(String inputsComponentExecutionIdentifier) {
        this.inputsComponentExecutionIdentifier = inputsComponentExecutionIdentifier;
    }

    public void setInputsComponentInstanceName(String inputsComponentInstanceName) {
        this.inputsComponentInstanceName = inputsComponentInstanceName;
    }

    public void setDestinationNodeId(LogicalNodeId inputsNodeId) {
        this.destinationNodeId = inputsNodeId;
    }

    public void setNetworkDestination(NetworkDestination networkDestination) {
        this.networkDestination = networkDestination;
    }
}
