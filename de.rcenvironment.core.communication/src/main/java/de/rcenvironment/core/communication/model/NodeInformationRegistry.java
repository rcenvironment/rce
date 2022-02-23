/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.io.PrintStream;

import de.rcenvironment.core.communication.api.NodeNameResolver;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;

/**
 * An interface providing a way to get extended node information for a node id.
 * 
 * @author Robert Mischke
 */
public interface NodeInformationRegistry extends NodeNameResolver {

    /**
     * Sets the name to associate with the given instance session; this is typically called for the local node on startup, and when learning
     * of new remote instance sessions.
     * 
     * @param id the instance session id
     * @param newName the name to associate
     */
    void associateDisplayName(InstanceNodeSessionId id, String newName);

    /**
     * Sets the name to associate with a specific logical node on the given instance session. This is typically the result of a remote node
     * explicitly announcing such a name mapping. Passing null as the new name removes any existing name association.
     * 
     * @param id the logical node session id
     * @param newName the name to associate, or null to remove an existing association
     */
    void associateDisplayNameWithLogicalNode(LogicalNodeSessionId id, String newName);

    /**
     * Debug method; prints all registered id-to-displayName associations.
     * 
     * @param output the output to write to
     * @param introText an optional line to be printered before the output; set to null to disable
     */
    void printAllNameAssociations(PrintStream output, String introText);

}
