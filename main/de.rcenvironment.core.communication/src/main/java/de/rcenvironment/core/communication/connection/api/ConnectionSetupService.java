/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.api;

import java.util.Collection;

import de.rcenvironment.core.communication.model.NetworkContactPoint;

/**
 * A service interface for inspecting and manipulating {@link ConnectionSetup} definitions.
 * 
 * @author Robert Mischke
 */
public interface ConnectionSetupService {

    /**
     * Creates a {@link ConnectionSetup} in its initial state. No implicit connection attempt is made; to connect, call
     * {@link ConnectionSetup#TODO} on the returned instance.
     * 
     * TODO extend documentation after API is settled
     * 
     * @param ncp the {@link NetworkContactPoint} describing the "physical" destination to connect to, and the transport to use
     * @param displayName the user-defined name for this connection
     * @param connnectOnStartup true if the new connection should auto-connect on startup
     * @return a {@link ConnectionSetup} in its initial state; no implicit connection attempt is made
     */
    ConnectionSetup createConnectionSetup(NetworkContactPoint ncp, String displayName, boolean connnectOnStartup);

    /**
     * Removes a {@link ConnectionSetup} from the set of registered channels. If this channel is {@link ConnectionSetupState#CONNECTED}, it
     * is implicitly closed.
     * 
     * @param channel the channel to discard
     */
    void disposeConnectionSetup(ConnectionSetup channel);

    /**
     * @return all registered {@link ConnectionSetup}s, regardless of their state
     */
    Collection<ConnectionSetup> getAllConnectionSetups();

    /**
     * @return the {@link ConnectionSetup} with the given id, or null if it does not exist
     * 
     * @param id the numeric id of the {@link ConnectionSetup}; see {@link ConnectionSetup#getId()}
     */
    ConnectionSetup getConnectionSetupById(long id);

}
