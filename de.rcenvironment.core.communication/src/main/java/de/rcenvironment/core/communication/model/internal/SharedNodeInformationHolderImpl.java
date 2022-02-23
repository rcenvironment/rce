/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.SharedNodeInformationHolder;

/**
 * A shared holder for information associated with a (globally unique) node id. Each {@link InstanceNodeSessionId} or
 * {@link InstanceNodeSessionId} with the same node id is supposed to reference the same holder object. This way, changes made to it are
 * immediately accessible to all users of these references. All public methods of this class are thread-safe.
 * 
 * @author Robert Mischke
 */
public class SharedNodeInformationHolderImpl implements SharedNodeInformationHolder {

    private String displayName;

    @Override
    public synchronized String getDisplayName() {
        // NOTE: synchronization is added for thread visibility, not atomicity
        return displayName;
    }

    public synchronized void setDisplayName(String displayName) {
        // NOTE: synchronization is added for thread visibility, not atomicity
        this.displayName = displayName;
    }
}
