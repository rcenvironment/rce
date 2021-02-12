/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;

/**
 * Callback interface for {@link FullSystemAndProcessDataSnapshot} updates, typically received from remote instances.
 * 
 * @author David Scholz
 * @author Robert Mischke (improved JavaDoc)
 */
public interface SystemMonitoringDataSnapshotListener {

    /**
     * Called when a new {@link FullSystemAndProcessDataSnapshot} has been received from the local or a remote node.
     * 
     * @param model a snapshot model of collected system information
     */
    void onMonitoringDataChanged(final FullSystemAndProcessDataSnapshot model);

}
