/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.internal;

import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.setup.ToolkitConfiguration;
import de.rcenvironment.toolkit.core.setup.ToolkitSetup;
import de.rcenvironment.toolkit.modules.concurrency.setup.ConcurrencyModule;
import de.rcenvironment.toolkit.modules.introspection.setup.IntrospectionModule;
import de.rcenvironment.toolkit.modules.objectbindings.setup.ObjectBindingsModule;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.setup.StatisticsModule;

/**
 * Provides a ToolkitConfiguration containing all modules in this project with reasonable default values.
 * 
 * @author Robert Mischke
 */
public class DefaultToolkitConfiguration implements ToolkitConfiguration {

    @Override
    public void configure(ToolkitSetup setup) throws ToolkitException {
        setup.configureModule(ConcurrencyModule.class).setThreadPoolName("DefaultThreadPool");
        setup.configureModule(StatisticsModule.class)
            // set the default statistics level to DEVELOPMENT for unit/integration testing
            .setStatisticsFilterLevel(StatisticsFilterLevel.DEVELOPMENT)
            // configure default stacktraces to include "de.rcenvironment.*" and show method names
            .configureDefaultCompactStacktraces("de\\.rcenvironment\\..*", "<", true);
        setup.configureModule(ObjectBindingsModule.class);
        setup.configureModule(IntrospectionModule.class);
    }

}
