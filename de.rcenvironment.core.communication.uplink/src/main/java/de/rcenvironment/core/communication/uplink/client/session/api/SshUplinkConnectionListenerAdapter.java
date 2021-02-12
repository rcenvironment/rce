/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import java.util.Collection;

/**
 * Empty methods implementation of {@link SshUplinkConnectionListener}.
 *
 * @author Brigitte Boden
 */
public class SshUplinkConnectionListenerAdapter implements SshUplinkConnectionListener {

    @Override
    public void onCollectionChanged(Collection<SshUplinkConnectionSetup> connections) {}

    @Override
    public void onCreated(SshUplinkConnectionSetup setup) {}

    @Override
    public void onConnected(SshUplinkConnectionSetup setup) {}

    @Override
    public void onConnectionAttemptFailed(SshUplinkConnectionSetup setup, String reason, boolean firstConsecutiveFailure, 
        boolean willAutoRetry) {}

    @Override
    public void onConnectionClosed(SshUplinkConnectionSetup setup, boolean willAutoRetry) {}

    @Override
    public void onDisposed(SshUplinkConnectionSetup setup) {}

    @Override
    public void onPublicationEntriesChanged(ToolDescriptorListUpdate publicationEntries, String connectionId) {}

}
