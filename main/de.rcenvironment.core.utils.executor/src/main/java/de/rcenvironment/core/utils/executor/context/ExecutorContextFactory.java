/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
