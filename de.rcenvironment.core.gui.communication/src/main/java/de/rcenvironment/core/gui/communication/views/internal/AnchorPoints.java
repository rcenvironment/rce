/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.internal;

import de.rcenvironment.core.gui.communication.views.NetworkViewContentProvider;

/**
 * Common nodes in the network view tree that are needed to coordinate contributors and the central {@link NetworkViewContentProvider}.
 * 
 * @author Robert Mischke
 */
public enum AnchorPoints {
    /**
     * A symbolic node representing the invisible tree root.
     */
    SYMBOLIC_ROOT_NODE,

    /**
     * The parent node of the nodes representing instances/nodes in the network.
     */
    INSTANCES_PARENT_NODE,

    /**
     * The root node of the "RCE Network" section.
     */
    MAIN_NETWORK_SECTION_PARENT_NODE,

    /**
     * The root node of the "SSH Remote Access" section.
     */
    SSH_REMOTE_ACCESS_SECTION_PARENT_NODE,
    
    /**
     * The root node of the "SSH Uplink" section.
     */
    SSH_UPLINK_SECTION_PARENT_NODE;
}
