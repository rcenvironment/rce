/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.Serializable;

/**
 * Represents the response to a {@link ToolExecutionRequest}. Currently, it only contains the minimal information whether the request was
 * accepted or not.
 *
 * @author Robert Mischke
 */
public class ToolExecutionRequestResponse implements Serializable {

    private static final long serialVersionUID = -8729588131466949408L;

    private boolean accepted;

    public ToolExecutionRequestResponse() {
        // for deserialization
    }

    public ToolExecutionRequestResponse(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

}
