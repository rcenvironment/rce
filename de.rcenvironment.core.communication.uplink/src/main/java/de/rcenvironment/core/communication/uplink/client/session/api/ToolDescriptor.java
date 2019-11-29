/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import de.rcenvironment.core.component.management.utils.JsonDataWithOptionalEncryption;
import java.io.Serializable;
import java.util.Set;

/**
 * Extends {@link ToolMetadata} with serialized details and execution information of the tool.
 *
 * @author Robert Mischke
 */
public class ToolDescriptor extends ToolMetadata implements Serializable {

    // TODO review the {@link Serializable} nature before final release
    private static final long serialVersionUID = -6724382664415473649L;

    private final JsonDataWithOptionalEncryption serializedToolData;

    public ToolDescriptor() {
        // default constructor for deserialization
        super();
        serializedToolData = null;
    }

    public ToolDescriptor(String toolId, String toolVersion, Set<String> authGroupIds, String toolDataHash,
        JsonDataWithOptionalEncryption serializedToolData) {
        super(toolId, toolVersion, authGroupIds, toolDataHash);
        this.serializedToolData = serializedToolData;
    }

    public JsonDataWithOptionalEncryption getSerializedToolData() {
        return serializedToolData;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // assuming all fields to be non-null here
        result = prime * result + serializedToolData.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ToolDescriptor other = (ToolDescriptor) obj;
        return super.equals(other)
            && serializedToolData.equals(other.serializedToolData);
    }

}
