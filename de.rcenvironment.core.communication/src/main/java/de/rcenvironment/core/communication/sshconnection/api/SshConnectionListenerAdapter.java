/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.api;

import java.util.Collection;

/**
 * Empty methods implementation of {@link SshConnectionListener}.
 *
 * @author Brigitte Boden
 */
public class SshConnectionListenerAdapter implements SshConnectionListener {

    @Override
    public void onCollectionChanged(Collection<SshConnectionSetup> connections) {}

    @Override
    public void onCreated(SshConnectionSetup setup) {}

    @Override
    public void onConnected(SshConnectionSetup setup) {}

    @Override
    public void onConnectionAttemptFailed(SshConnectionSetup setup, String reason, boolean firstConsecutiveFailure, 
        boolean willAutoRetry) {}

    @Override
    public void onConnectionClosed(SshConnectionSetup setup, boolean willAutoRetry) {}

    @Override
    public void onDisposed(SshConnectionSetup setup) {}

}
