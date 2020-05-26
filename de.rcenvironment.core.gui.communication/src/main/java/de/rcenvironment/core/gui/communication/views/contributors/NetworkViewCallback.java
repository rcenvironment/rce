/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views.contributors;


/**
 * Simple interface used by connection list contributors to trigger update 
 * of possible actions in network view.
 * 
 * @author Brigitte Boden
 *
 */
public interface NetworkViewCallback {
    /**
     * Trigger update of network view for state change of contributed node.
     * 
     * @param node The node for which the status has changed.
     * 
     */
    void onStateChanged(Object node);
}
