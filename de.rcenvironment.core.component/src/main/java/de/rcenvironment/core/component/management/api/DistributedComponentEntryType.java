/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.api;

/**
 * Determines the kind of component data a {@link DistributedComponentEntry} represents.
 *
 * @author Robert Mischke
 * @author Alexander Weinert (isRemotelyAccessible)
 */
public enum DistributedComponentEntryType {
    /**
     * The component is present on the local node and not accessible to any remote node.
     */
    LOCAL,
    /**
     * The component is "local-only", ie it can and will never be published, regardless of local authorization settings.
     */
    FORCED_LOCAL,
    /**
     * The component is present on the local node and is generally accessible to remote nodes, although authorization restrictions may still
     * apply.
     */
    SHARED,
    /**
     * This component is located on a remote node, and is accessible because its authorization criteria were met by the local node.
     */
    REMOTE;

    /**
     * Semantic method for testing whether a component is located on the local node. The result is currently equivalent to
     * {@link DistributedComponentEntryType#LOCAL} || {@link DistributedComponentEntryType#FORCED_LOCAL} ||
     * {@link DistributedComponentEntryType#SHARED}.
     * 
     * @return true if the component is local
     */
    public boolean isLocal() {
        return this != REMOTE;
    }
    
    /**
     * Semantic method for testing whether a component is accessible on any non-local node. The result is currently equivalent to
     * {@link #LOCAL} || {@link #FORCED_LOCAL}. In particular, if a component is only accessible due to being published on a remote node,
     * this method returns true.
     * 
     * @return True if the component is accessible on any other machine than the local one.
     */
    public boolean isRemotelyAccessible() {
        return this.equals(SHARED) || this.equals(REMOTE);
    }
}
