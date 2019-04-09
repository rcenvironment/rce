/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.io.PrintStream;

/**
 * An interface providing a way to get extended node information for a node id.
 * 
 * @author Robert Mischke
 */
public interface NodeInformationRegistry {

    /**
     * Returns a {@link SharedNodeInformationHolder} instance that can be queried for the information gathered about the specified node.
     * 
     * @param id the id of the node
     * @return the {@link SharedNodeInformationHolder} interface for this node
     */
    SharedNodeInformationHolder getNodeInformationHolder(String id);

    /**
     * Debug method; prints all registered id-to-displayName associations.
     * 
     * @param output the output to write to
     * @param introText an optional line to be printered before the output; set to null to disable
     */
    void printAllNameAssociations(PrintStream output, String introText);
}
