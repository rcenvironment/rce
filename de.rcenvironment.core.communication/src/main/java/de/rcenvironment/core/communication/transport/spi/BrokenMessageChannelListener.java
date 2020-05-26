/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.spi;


/**
 * Internal callback interface for the communication bundle and transport implementations. It
 * provides a callback for low-level connection breakdowns. These are handled internally by the
 * communication layer, and are not meant to propagate to other communication layers or external
 * bundles.
 * 
 * @author Robert Mischke
 */
public interface BrokenMessageChannelListener {

    /**
     * Reports a connection that was detected as "broken", ie unusable from an unexpected event.
     * 
     * @param connection the affected connection
     */
    void onChannelBroken(MessageChannel connection);
}
