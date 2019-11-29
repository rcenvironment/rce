/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Collection;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Immutable holder for a consistent set of {@link ComponentInstallation} knowledge, representing which installations were published by
 * known nodes.
 * 
 * @author Robert Mischke
 */
public interface DistributedComponentKnowledge {

    /**
     * If includeInaccessible is set to true, the returned collection may contain DistributedComponentEntries whose component installation
     * is set to null. This is due to the remote instance sending encrypted information about its published components, which cannot be
     * decrypted without being a member of the corresponding authorization group. If includeInaccessible is set to false, then all
     * DistributedComponentEntries in the returned collection have a non-null component installation.
     * 
     * @param nodeId              the node of interest
     * @param includeInaccessible If false, the returned collection is filtered to only include those components that are published in an
     *                            authorization group that the current instance has access to. Otherwise, the list includes all components
     *                            published on the given node.
     * @return all {@link ComponentInstallation}s published by the given node; TODO clarify: add special treatment of local node or not?
     */
    Collection<DistributedComponentEntry> getKnownSharedInstallationsOnNode(ResolvableNodeId nodeId, boolean includeInaccessible);


    /**
     * @return all known {@link ComponentInstallation}s published by the local and reachable remote nodes
     */
    Collection<DistributedComponentEntry> getKnownSharedInstallations();

    /**
     * @return an unfiltered list of {@link ComponentInstallation}s on the local node
     */
    Collection<DistributedComponentEntry> getAllLocalInstallations();

    /**
     * Convenience method.
     * 
     * @return all entries of type {@link DistributedComponentEntryType#SHARED}
     */
    Collection<DistributedComponentEntry> getSharedAccessInstallations();

    /**
     * Convenience method.
     * 
     * @return all entries of type {@link DistributedComponentEntryType#LOCAL} or {@link DistributedComponentEntryType#FORCED_LOCAL}
     */
    Collection<DistributedComponentEntry> getLocalAccessInstallations();

    /**
     * Convenient method merging results from {@link DistributedComponentKnowledge#getKnownSharedInstallations()} and
     * {@link DistributedComponentKnowledge#getAllLocalInstallations()}.
     * 
     * @return all {@link ComponentInstallation}s published by known and reachable nodes and unfiltered list of
     *         {@link ComponentInstallation}s on the local node
     */
    Collection<DistributedComponentEntry> getAllInstallations(); // TODO rename to "all available"

}
