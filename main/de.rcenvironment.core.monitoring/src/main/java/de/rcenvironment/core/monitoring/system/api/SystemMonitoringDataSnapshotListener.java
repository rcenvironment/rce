/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

/**
 * This interface describes a method which is called when the {@link SystemMonitoringDataSnapshot} is fetched. It serves as a callback
 * method.
 * 
 * @author David Scholz
 */
public interface SystemMonitoringDataSnapshotListener {

    /**
     * Hands off {@link SystemMonitoringDataSnapshot} to the corresponding view if the data is fetched by the
     * {@link SystemMonitoringDataPollingManager}.
     * 
     * @param model The model which holds the monitoring data.
     */
    void onMonitoringDataChanged(final SystemMonitoringDataSnapshot model);

}
