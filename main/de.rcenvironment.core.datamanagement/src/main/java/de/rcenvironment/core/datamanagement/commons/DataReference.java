/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.Set;

import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * Identifier for data references holding {@link BinaryReference}s.
 * 
 * @author Jan Flink
 * @author Brigitte Boden (adapted used node id type)
 * @author Robert Mischke (minor id fix)
 */
public final class DataReference implements Serializable {

    private static final long serialVersionUID = -5443653424654542352L;

    private final String dataReferenceKey;

    private final InstanceNodeId storageInstanceId; // TODO (p1) 9.0.0 rename to storageNodeId again; breaks serialization if changed

    private Set<BinaryReference> binaryReferences;

    public DataReference(String dataReferenceKey, LogicalNodeId storageInstanceId, Set<BinaryReference> binaryReferences) {
        this.dataReferenceKey = dataReferenceKey;
        // TODO (p1) 9.0.0 remove conversion again
        this.storageInstanceId = storageInstanceId.convertToInstanceNodeId();
        this.binaryReferences = binaryReferences;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DataReference) {
            final DataReference other = (DataReference) obj;
            return dataReferenceKey.equals(other.dataReferenceKey);
        }
        return false;
    }

    /**
     * @return the key of this {@link DataReference}.
     */

    public String getDataReferenceKey() {
        return dataReferenceKey;
    }

    /**
     * @return the {@link LogicalNodeId} of the platform this {@link DataReference} is hosted.
     */
    public LogicalNodeId getStorageNodeId() {
        // TODO (p1) 9.0.0 remove conversion again
        return storageInstanceId.convertToDefaultLogicalNodeId();
    }

    @Override
    public String toString() {
        return dataReferenceKey.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + dataReferenceKey.hashCode();
        return result;
    }

    public Set<BinaryReference> getBinaryReferences() {
        return binaryReferences;
    }
}
