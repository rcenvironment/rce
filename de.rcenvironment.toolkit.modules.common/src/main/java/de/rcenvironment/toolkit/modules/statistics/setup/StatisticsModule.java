/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.statistics.setup;

import de.rcenvironment.toolkit.core.spi.module.AbstractToolkitModule;
import de.rcenvironment.toolkit.core.spi.module.ObjectGraph;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;
import de.rcenvironment.toolkit.modules.statistics.internal.StatisticsTrackerServiceImpl;

/**
 * Provides {@link StatisticsTrackerService} for tracking event and value statistics.
 * 
 * @author Robert Mischke
 */
public class StatisticsModule extends AbstractToolkitModule<StatisticsModuleConfiguration> {

    @Override
    public void registerMembers(ObjectGraph objectGraph) {
        objectGraph.registerObject(getConfiguration()); // provide the configuration object via dependency injection
        objectGraph.registerServiceClass(StatisticsTrackerServiceImpl.class);
    }

    @Override
    public StatisticsModuleConfiguration createConfigurationObject() {
        return new StatisticsModuleConfiguration();
    }
}
