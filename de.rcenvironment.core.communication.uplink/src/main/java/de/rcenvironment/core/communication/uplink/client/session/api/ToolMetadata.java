/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the data that identifies a published tool, and provides relevant information for change detection and authorization. Note that
 * this does contain the actual tool's description; see {@link ToolDescriptor} for this.
 *
 * @author Robert Mischke
 */
public abstract class ToolMetadata implements Serializable {

    // TODO review the {@link Serializable} nature before final release
    private static final long serialVersionUID = 1490295701575450991L;

    private final String toolId;

    private final String toolVersion;

    private final Set<String> authGroupIds;

    private final String toolDataHash;

    public ToolMetadata() {
        // default constructor for deserialization
        this(null, null, null, null);
    }

    public ToolMetadata(String toolId, String toolVersion, Set<String> authGroupIds, String toolDataHash) {
        this.toolId = toolId;
        this.toolVersion = toolVersion;
        this.authGroupIds = authGroupIds;
        this.toolDataHash = toolDataHash;
    }

    public String getToolId() {
        return toolId;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    /**
     * @return the list of full group id ("<name>:<suffix>")
     */
    public Set<String> getAuthGroupIds() {
        return authGroupIds;
    }

    /**
     * @return A hash based on the serialized tool data; for efficient change detection without decrypting the full data over and over.
     *         (Note: this should either include a stable salt, e.g. the group id, or be encrypted to prevent information leakage).
     */
    public String getToolDataHash() {
        return toolDataHash;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        // assuming all fields to be non-null here
        result = prime * result + authGroupIds.hashCode();
        result = prime * result + toolDataHash.hashCode();
        result = prime * result + toolId.hashCode();
        result = prime * result + toolVersion.hashCode();
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
        ToolMetadata other = (ToolMetadata) obj;
        return Objects.equals(authGroupIds, other.authGroupIds)
            && Objects.equals(toolDataHash, other.toolDataHash)
            && Objects.equals(toolId, other.toolId)
            && Objects.equals(toolVersion, other.toolVersion);
    }

}
