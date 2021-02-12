/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.objectbindings.setup;

import java.util.Set;

import de.rcenvironment.toolkit.core.spi.module.AbstractZeroConfigurationToolkitModule;
import de.rcenvironment.toolkit.core.spi.module.ObjectGraph;
import de.rcenvironment.toolkit.core.spi.module.ToolkitModule;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.introspection.setup.IntrospectionModule;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsConsumer;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;
import de.rcenvironment.toolkit.modules.objectbindings.internal.ObjectBindingsServiceImpl;

/**
 * A module providing the {@link ObjectBindingsService} and the related {@link ObjectBindingsConsumer} interface.
 * 
 * @author Robert Mischke
 */
public class ObjectBindingsModule extends AbstractZeroConfigurationToolkitModule {

    @Override
    public void registerMembers(ObjectGraph objectGraph) {
        objectGraph.registerServiceClass(ObjectBindingsServiceImpl.class);
    }

    @Override
    public void suggestMissingModuleDependencies(ObjectGraph objectGraph, Set<Class<? extends ToolkitModule<?>>> modulesToLoad) {
        if (objectGraph.isMissingService(StatusCollectionRegistry.class)) {
            modulesToLoad.add(IntrospectionModule.class);
        }
    }
}
