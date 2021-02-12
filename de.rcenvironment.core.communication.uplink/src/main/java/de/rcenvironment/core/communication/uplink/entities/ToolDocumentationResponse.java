/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.entities;

import java.io.Serializable;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;

/**
 * The response to a {@link ToolDocumentationRequest}, communicating whether the requested documentation can be provided, and if so, its
 * size in bytes. Represented as a {@link MessageType#TOOL_DOCUMENTATION_RESPONSE} message. The actual data will be sent afterwards as a
 * sequence of {@link MessageType#TOOL_DOCUMENTATION_CONTENT} data blocks.
 * 
 * @author Robert Mischke
 */
public class ToolDocumentationResponse implements Serializable {

    private static final long serialVersionUID = 663857054279732849L;

    private boolean available;

    private long size;

    public ToolDocumentationResponse() {}

    public ToolDocumentationResponse(boolean available, long size) {
        this.available = available;
        this.size = size;
    }

    public boolean isAvailable() {
        return available;
    }

    public long getSize() {
        return size;
    }

}
