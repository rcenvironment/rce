/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess;

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

}
