/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.api;

import java.util.Collection;


/**
 * Listener for changes in SSH connections.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public interface SshConnectionListener {

    /**
     * Reports that the collection of registered {@link SshConnectionSetup}s has changed. This event is fired once for each new
     * listener to bring it up to sync, and then fired again together with each {@link #onCreated(SshConnectionSetup)} and
     * {@link #onDisposed(SshConnectionSetup)} callback. This approach allows listeners to choose whether to process the whole list
     * again, or only react to the new delta. (Note that listeners should make no assumptions about the order of the complete update vs.
     * delta callbacks).
     * 
     * @param connections the new collection of {@link SshConnectionSetup}s
     */
    void onCollectionChanged(Collection<SshConnectionSetup> connections);

    /**
     * Reports that a new {@link SshConnectionSetup} was created and registered.
     * 
     * @param setup the new {@link SshConnectionSetup}
     */
    void onCreated(SshConnectionSetup setup);

    /**
     * Reports that a {@link SshConnectionSetup} was successfully connected.
     * 
     * @param setup the new {@link SshConnectionSetup}
     */
    void onConnected(SshConnectionSetup setup);

    /**
     * Reports that a connection attempt has failed, and whether the connection will be retried automatically. Note that this does not cover
     * the breakdown of established connections; use {@link #onConnectionClosed(SshConnectionSetup)} for this.
     * 
     * @param setup the affected {@link SshConnectionSetup}
     * @param reason Reason for failure.
     * @param firstConsecutiveFailure true if this connection has neither failed nor connected successfully since startup or receiving a
     *        manual "connect" command
     * @param willAutoRetry true if the connection will be auto-retried
     */
    void onConnectionAttemptFailed(SshConnectionSetup setup, String reason, boolean firstConsecutiveFailure, boolean willAutoRetry);

    /**
     * Reports that an established connection was closed. Possible reason parameters for this callback are
     * {@link DisconnectReason#ACTIVE_SHUTDOWN}, {@link DisconnectReason#REMOTE_SHUTDOWN}, and {@link DisconnectReason#ERROR}.
     * 
     * @param setup the affected {@link SshConnectionSetup}
     * @param willAutoRetry true if the connection will be auto-retried
     */
    void onConnectionClosed(SshConnectionSetup setup, boolean willAutoRetry);

    /**
     * Reports that a new {@link SshConnectionSetup} was unregistered and disposed.
     * 
     * @param setup the new {@link SshConnectionSetup}
     */
    void onDisposed(SshConnectionSetup setup);

}
