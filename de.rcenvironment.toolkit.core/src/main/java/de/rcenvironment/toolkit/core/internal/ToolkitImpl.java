/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.internal;

import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.Toolkit;

/**
 * Default {@link Toolkit} implementation.
 * 
 * @author Robert Mischke
 */
final class ToolkitImpl implements Toolkit {

    private final ImmutableServiceRegistry serviceRegistry;

    private final List<Runnable> shutdownHooks;

    private boolean shutdownTriggered = false;

    ToolkitImpl(ImmutableServiceRegistry serviceRegistry, List<Runnable> shutdownHooks) {
        this.serviceRegistry = serviceRegistry;
        this.shutdownHooks = shutdownHooks;
    }

    @Override
    public ImmutableServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return serviceRegistry.getService(serviceClass);
    }

    @Override
    public void shutdown() {

        synchronized (this) {
            if (shutdownTriggered) {
                throw new IllegalStateException("Received more than one shutdown() call");
            }
            shutdownTriggered = true;
        }

        for (Runnable shutdownHook : shutdownHooks) {
            try {
                shutdownHook.run();
            } catch (RuntimeException e) {
                LogFactory.getLog(getClass()).error("Error executing shutdown hook", e);
            }
        }
        ToolkitInstanceTracker.getInstance().markDisposed(this);
    }
}
