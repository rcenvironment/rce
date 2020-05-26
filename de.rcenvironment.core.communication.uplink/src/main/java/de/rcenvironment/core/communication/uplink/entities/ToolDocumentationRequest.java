/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.entities;

import java.io.Serializable;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;

/**
 * Represents a request for a tool's documentation data, which may or may not be available. Represented by a
 * {@link MessageType#TOOL_DOCUMENTATION_REQUEST} message.
 *
 * @author Robert Mischke
 */
public class ToolDocumentationRequest implements Serializable {

    private static final long serialVersionUID = -5614177962145211267L;

    private String referenceId;

    public ToolDocumentationRequest() {}

    public ToolDocumentationRequest(String docReferenceId) {
        this.referenceId = docReferenceId;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
