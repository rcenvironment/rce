/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.util.Collection;

/**
 * Listener for changes in SSH connections.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public interface SshUplinkConnectionListener {

    /**
     * Reports that the collection of registered {@link SshUplinkConnectionSetup}s has changed. This event is fired once for each new
     * listener to bring it up to sync, and then fired again together with each {@link #onCreated(SshUplinkConnectionSetup)} and
     * {@link #onDisposed(SshUplinkConnectionSetup)} callback. This approach allows listeners to choose whether to process the whole list
     * again, or only react to the new delta. (Note that listeners should make no assumptions about the order of the complete update vs.
     * delta callbacks).
     * 
     * @param connections the new collection of {@link SshUplinkConnectionSetup}s
     */
    void onCollectionChanged(Collection<SshUplinkConnectionSetup> connections);

    /**
     * Reports that a new {@link SshUplinkConnectionSetup} was created and registered.
     * 
     * @param setup the new {@link SshUplinkConnectionSetup}
     */
    void onCreated(SshUplinkConnectionSetup setup);

    /**
     * Reports that a {@link SshUplinkConnectionSetup} was successfully connected.
     * 
     * @param setup the new {@link SshUplinkConnectionSetup}
     */
    void onConnected(SshUplinkConnectionSetup setup);

    /**
     * Reports that a connection attempt has failed, and whether the connection will be retried automatically. Note that this does not cover
     * the breakdown of established connections; use {@link #onConnectionClosed(SshUplinkConnectionSetup)} for this.
     * 
     * @param setup the affected {@link SshUplinkConnectionSetup}
     * @param reason Reason for failure.
     * @param firstConsecutiveFailure true if this connection has neither failed nor connected successfully since startup or receiving a
     *        manual "connect" command
     * @param willAutoRetry true if the connection will be auto-retried
     */
    void onConnectionAttemptFailed(SshUplinkConnectionSetup setup, String reason, boolean firstConsecutiveFailure, boolean willAutoRetry);

    /**
     * Reports that an established connection was closed. Possible reason parameters for this callback are
     * {@link DisconnectReason#ACTIVE_SHUTDOWN}, {@link DisconnectReason#REMOTE_SHUTDOWN}, and {@link DisconnectReason#ERROR}.
     * 
     * @param setup the affected {@link SshUplinkConnectionSetup}
     * @param willAutoRetry true if the connection will be auto-retried
     */
    void onConnectionClosed(SshUplinkConnectionSetup setup, boolean willAutoRetry);

    /**
     * Reports that a new {@link SshUplinkConnectionSetup} was unregistered and disposed.
     * 
     * @param setup the new {@link SshUplinkConnectionSetup}
     */
    void onDisposed(SshUplinkConnectionSetup setup);
    
    /**
     * Reports that a new list of publication entries has arrived via an uplink connection.
     *  
     * @param publicationEntries the new list of publicationEntries
     * @param connectionId the id of the connection from which these entries where published
     */
    void onPublicationEntriesChanged(ToolDescriptorListUpdate publicationEntries, String connectionId);


}
