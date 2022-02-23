/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.connection.api;

import java.util.Collection;

/**
 * Empty-method implementation of {@link ConnectionSetupListener} to simplify subclass implementations.
 * 
 * @author Robert Mischke
 */
public class ConnectionSetupListenerAdapter implements ConnectionSetupListener {

    @Override
    public void onCollectionChanged(Collection<ConnectionSetup> setups) {}

    @Override
    public void onCreated(ConnectionSetup setup) {}

    @Override
    public void onStateChanged(ConnectionSetup setup, ConnectionSetupState oldState, ConnectionSetupState newState) {}

    @Override
    public void onConnectionAttemptFailed(ConnectionSetup setup, boolean firstConsecutiveFailure, boolean willAutoRetry) {}

    @Override
    public void onConnectionClosed(ConnectionSetup setup, DisconnectReason disconnectReason, boolean willAutoRetry) {}

    @Override
    public void onDisposed(ConnectionSetup setup) {}

}
