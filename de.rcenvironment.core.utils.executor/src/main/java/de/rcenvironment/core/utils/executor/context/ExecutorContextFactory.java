/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context;

import de.rcenvironment.core.utils.executor.context.spi.ExecutorContext;
import de.rcenvironment.core.utils.executor.context.spi.SandboxStrategy;

/**
 * This interface simplifies providing a matching {@link ExecutorContext} and
 * {@link SandboxStrategy} to consumers.
 * 
 * @author Robert Mischke
 */
public interface ExecutorContextFactory {

    /**
     * Creates a new executor context for a caller.
     * 
     * @return a new {@link ExecutorContext}
     */
    ExecutorContext createExecutorContext();

    /**
     * @param executorContext the {@link ExecutorContext} acquired via
     *        {@link #createExecutorContext()}
     * @return the {@link SandboxStrategy} to use
     */
    SandboxStrategy createSandboxStrategy(ExecutorContext executorContext);
}
