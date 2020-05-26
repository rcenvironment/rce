/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.introspection.setup;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.spi.module.AbstractZeroConfigurationToolkitModule;
import de.rcenvironment.toolkit.core.spi.module.ObjectGraph;
import de.rcenvironment.toolkit.core.spi.module.ShutdownHookReceiver;
import de.rcenvironment.toolkit.core.spi.module.ToolkitModule;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionService;
import de.rcenvironment.toolkit.modules.introspection.internal.StatusCollectionServiceImpl;
import de.rcenvironment.toolkit.utils.text.MultiLineOutput;

/**
 * A module providing the {@link StatusCollectionService}.
 * 
 * @author Robert Mischke
 */
public class IntrospectionModule extends AbstractZeroConfigurationToolkitModule {

    @Override
    public void registerMembers(ObjectGraph objectGraph) {
        objectGraph.registerServiceClass(StatusCollectionServiceImpl.class);
    }

    @Override
    public void suggestMissingModuleDependencies(ObjectGraph objectGraph, Set<Class<? extends ToolkitModule<?>>> modulesToLoad) {
        if (objectGraph.isMissingService(StatusCollectionRegistry.class)) {
            modulesToLoad.add(IntrospectionModule.class);
        }
    }

    @Override
    public void registerShutdownHooks(ImmutableServiceRegistry serviceRegistry, ShutdownHookReceiver shutdownHookReceiver) {
        final StatusCollectionService statusCollectionService = serviceRegistry.getService(StatusCollectionService.class);
        shutdownHookReceiver.addShutdownHook(new Runnable() {

            @Override
            public void run() {
                final Log log = LogFactory.getLog(getClass());

                final String statusLineOutputIndent = "  ";
                final String statusLineOutputSeparator = "\n";

                log.debug(
                    statusCollectionService.getCollectedDefaultStateInformation()
                        .asMultilineString("Final state information:", statusLineOutputIndent, statusLineOutputSeparator, null));

                final MultiLineOutput unfinishedOperationsReport = statusCollectionService.getCollectedUnfinishedOperationsInformation();
                if (unfinishedOperationsReport.hasContent()) {
                    log.debug(unfinishedOperationsReport.asMultilineString(
                        "Known unfinished operations on shutdown:", statusLineOutputIndent, statusLineOutputSeparator, null));
                } else {
                    log.debug("Known unfinished operations on shutdown: <none>");
                }
            }
        });
    }
}
