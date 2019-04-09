/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A combined key comprised of a node id and a metadata key.
 * 
 * @author Robert Mischke
 */
public class CompositeNodePropertyKey {

    private final String instanceSessionId;

    private final String dataKey;

    private final String compositeKey;

    public CompositeNodePropertyKey(String instanceSessionId, String dataKey) {
        this.instanceSessionId = instanceSessionId;
        this.dataKey = dataKey;
        this.compositeKey = StringUtils.format("%s:%s", instanceSessionId, dataKey);
    }

    public String getInstanceNodeSessionIdString() {
        return instanceSessionId;
    }

    public String getDataKey() {
        return dataKey;
    }

    @Override
    public int hashCode() {
        // delegate to compositeKey
        return compositeKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompositeNodePropertyKey)) {
            return false;
        }
        // delegate to compositeKey equality
        return compositeKey.equals(((CompositeNodePropertyKey) obj).compositeKey);
    }

    public String getAsUniqueString() {
        return compositeKey;
    }

    @Override
    public String toString() {
        return compositeKey;
    }

}
