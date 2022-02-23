/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.setup;

import java.util.Set;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.spi.module.AbstractToolkitModule;
import de.rcenvironment.toolkit.core.spi.module.ObjectGraph;
import de.rcenvironment.toolkit.core.spi.module.ShutdownHookReceiver;
import de.rcenvironment.toolkit.core.spi.module.ToolkitModule;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadPoolManagementAccess;
import de.rcenvironment.toolkit.modules.concurrency.internal.AsyncTaskServiceImpl;
import de.rcenvironment.toolkit.modules.concurrency.internal.ConcurrencyUtilsFactoryImpl;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.introspection.setup.IntrospectionModule;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;
import de.rcenvironment.toolkit.modules.statistics.setup.StatisticsModule;

/**
 * A module providing concurrency-related services, including {@link AsyncTaskService} and {@link ConcurrencyUtilsFactory}.
 * 
 * @author Robert Mischke
 */
public class ConcurrencyModule extends AbstractToolkitModule<ConcurrencyModuleConfiguration> {

    @Override
    public void registerMembers(ObjectGraph objectGraph) {
        objectGraph.registerObject(getConfiguration()); // provide the configuration object via dependency injection
        objectGraph.registerServiceClass(AsyncTaskServiceImpl.class);
        objectGraph.registerServiceClass(ConcurrencyUtilsFactoryImpl.class);
    }

    @Override
    public ConcurrencyModuleConfiguration createConfigurationObject() {
        return new ConcurrencyModuleConfiguration();
    }

    @Override
    public void suggestMissingModuleDependencies(ObjectGraph objectGraph, Set<Class<? extends ToolkitModule<?>>> modulesToLoad) {
        if (objectGraph.isMissingService(StatusCollectionRegistry.class)) {
            modulesToLoad.add(IntrospectionModule.class);
        }
        if (objectGraph.isMissingService(StatisticsTrackerService.class)) {
            modulesToLoad.add(StatisticsModule.class);
        }
    }

    @Override
    public void registerShutdownHooks(ImmutableServiceRegistry serviceRegistry, ShutdownHookReceiver shutdownHookReceiver) {
        final ThreadPoolManagementAccess threadPoolManagementAccess = serviceRegistry.getService(ThreadPoolManagementAccess.class);
        shutdownHookReceiver.addShutdownHook(new Runnable() {

            @Override
            public void run() {
                threadPoolManagementAccess.shutdown();
            }
        });
    }

}
