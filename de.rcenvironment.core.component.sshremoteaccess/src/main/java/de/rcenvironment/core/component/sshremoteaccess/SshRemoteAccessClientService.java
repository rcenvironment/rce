/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess;

import java.io.File;
import java.util.Map;

/**
 * Service for remote access on tools that are published via SSH. Registers each remote tool as a component on the local node.
 *
 * @author Brigitte Boden
 */
public interface SshRemoteAccessClientService {

    /**
     * Registers all remote tools and workflows from a connection as components on the local host.
     * 
     * @param connectionId id of the ssh connection over which the tools are accessed.
     */
    void updateSshRemoteAccessComponents(String connectionId);

    /**
     * Registers all remote tools and workflows from all available connections as components on the local host.
     * 
     */
    void updateSshRemoteAccessComponents();

    /**
     * Gets list of node Ids on which documentation for a given tool is available.
     * 
     * @param toolId the tool identifier
     * @return list of node Ids
     */
    Map<String, String> getListOfToolsWithDocumentation(String toolId);

    /**
     * Downloads the tool documentation for a given tool identifier and node id.
     * 
     * @param toolId the tool identifier
     * @param nodeId the node id
     * @param hashValue the has value of the documentation
     * @return The documentation file
     */
    File downloadToolDocumentation(String toolId, String nodeId, String hashValue);

}
