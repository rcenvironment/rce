/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

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
