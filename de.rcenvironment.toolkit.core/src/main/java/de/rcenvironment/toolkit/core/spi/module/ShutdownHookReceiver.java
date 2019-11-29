/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

/**
 * A simple interface for collection {@link Runnable}s to be called as shutdown hooks.
 * 
 * @author Robert Mischke
 */
public interface ShutdownHookReceiver {

    /**
     * Adds a {@link Runnable} to be called as shutdown hook later.
     * 
     * @param shutdownHook the new shutdown hook
     */
    void addShutdownHook(Runnable shutdownHook);
}
