/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.api;

import java.util.Collection;

/**
 * Callback interface for connection-related events. TODO
 * 
 * @author Robert Mischke
 */
public interface ConnectionSetupListener {

    /**
     * Reports that the collection of registered {@link ConnectionSetup}s has changed. This event is fired once for each new listener to
     * bring it up to sync, and then fired again together with each {@link #onCreated(ConnectionSetup)} and
     * {@link #onDisposed(ConnectionSetup)} callback. This approach allows listeners to choose whether to process the whole list again, or
     * only react to the new delta. (Note that listeners should make no assumptions about the order of the complete update vs. delta
     * callbacks).
     * 
     * @param setups the new collection of {@link ConnectionSetup}s
     */
    void onCollectionChanged(Collection<ConnectionSetup> setups);

    /**
     * Reports that a new {@link ConnectionSetup} was created and registered.
     * 
     * @param setup the new {@link ConnectionSetup}
     */
    void onCreated(ConnectionSetup setup);

    /**
     * Reports that a {@link ConnectionSetup} changed its internal state.
     * 
     * @param setup the new {@link ConnectionSetup}
     * @param oldState the previous {@link ConnectionSetupState}
     * @param newState the new {@link ConnectionSetupState}
     */
    void onStateChanged(ConnectionSetup setup, ConnectionSetupState oldState, ConnectionSetupState newState);

    /**
     * Reports that a connection attempt has failed, and whether the connection will be retried automatically. Not that this does not cover
     * the breakdown of established connections; use {@link #onStateChanged(ConnectionSetup, ConnectionSetupState)} for this.
     * 
     * @param setup the affected {@link ConnectionSetup}
     * @param firstConsecutiveFailure true if this connection has neither failed nor connected successfully since startup or receiving a
     *        manual "connect" command
     * @param willAutoRetry true if the connection will be auto-retried
     */
    void onConnectionAttemptFailed(ConnectionSetup setup, boolean firstConsecutiveFailure, boolean willAutoRetry);

    /**
     * Reports that an established connection was closed. Possible reason parameters for this callback are
     * {@link DisconnectReason#ACTIVE_SHUTDOWN}, {@link DisconnectReason#REMOTE_SHUTDOWN}, and {@link DisconnectReason#ERROR}.
     * 
     * @param setup the affected {@link ConnectionSetup}
     * @param disconnectReason the reason for the connection shutdown
     * @param willAutoRetry true if the connection will be auto-retried
     */
    void onConnectionClosed(ConnectionSetup setup, DisconnectReason disconnectReason, boolean willAutoRetry);

    /**
     * Reports that a new {@link ConnectionSetup} was unregistered and disposed.
     * 
     * @param setup the new {@link ConnectionSetup}
     */
    void onDisposed(ConnectionSetup setup);

}
