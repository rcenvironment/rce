/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NodeInformation;

/**
 * A shared holder for information associated with a (globally unique) node id. Each
 * {@link NodeIdentifier} or {@link NodeIdentifier} with the same node id is supposed to
 * reference the same holder object. This way, changes made to it are immediately accessible to all
 * users of these references. All public methods of this class are thread-safe.
 * 
 * @author Robert Mischke
 */
public class NodeInformationHolder implements NodeInformation {

    private String displayName;

    private boolean isWorkflowHost;

    @Override
    public synchronized String getDisplayName() {
        // NOTE: synchronization is added for thread visibility, not atomicity
        return displayName;
    }

    public synchronized void setDisplayName(String displayName) {
        // NOTE: synchronization is added for thread visibility, not atomicity
        this.displayName = displayName;
    }

    public synchronized boolean isWorkflowHost() {
        return isWorkflowHost;
    }

    public synchronized void setIsWorkflowHost(boolean isWorkflowHost) {
        this.isWorkflowHost = isWorkflowHost;
    }

}
