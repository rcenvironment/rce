/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

import java.util.Set;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;

/**
 * An abstract class to simplify future extensions; not actually needed at this time.
 * 
 * @author Robert Mischke
 * 
 * @param <TModuleConf> the type providing configuration data for this module; use {@link Void} if no configuration is required
 */
public abstract class AbstractToolkitModule<TModuleConf> implements ToolkitModule<TModuleConf> {

    private final TModuleConf configuration = createConfigurationObject();

    @Override
    public abstract void registerMembers(ObjectGraph objectGraph);

    @Override
    public void suggestMissingModuleDependencies(ObjectGraph objectGraph,
        Set<Class<? extends de.rcenvironment.toolkit.core.spi.module.ToolkitModule<?>>> modulesToLoad) {};

    @Override
    public void registerShutdownHooks(ImmutableServiceRegistry serviceRegistry, ShutdownHookReceiver shutdownHookReceiver) {};

    @Override
    public TModuleConf getConfiguration() {
        return configuration;
    }
}
