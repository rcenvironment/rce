/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context.impl;

import de.rcenvironment.core.utils.executor.context.ExecutorContextFactory;
import de.rcenvironment.core.utils.executor.context.spi.ExecutorContext;
import de.rcenvironment.core.utils.executor.context.spi.SandboxStrategy;

/**
 * Implementation of a {@link ExecutorContextFactory} that creates {@link LocalCommandLineExecutor}
 * instances, and can provide either matching {@link ContinuousSandboxStrategy} or
 * {@link IndividualSandboxStrategy} instances, depending on a configuration setting.
 * 
 * @author Robert Mischke
 */
public class LocalExecutorContextFactory implements ExecutorContextFactory {

    private boolean useContiuousSandbox;

    /**
     * @param useContiuousSandbox set to true to receive {@link ContinuousSandboxStrategy} instances
     *        from {@link #createSandboxStrategy(ExecutorContext)}; false to receive
     *        {@link IndividualSandboxStrategy} instances
     */
    public LocalExecutorContextFactory(boolean useContiuousSandbox) {
        this.useContiuousSandbox = useContiuousSandbox;
    }

    @Override
    public ExecutorContext createExecutorContext() {
        return new LocalExecutorContext();
    }

    @Override
    public SandboxStrategy createSandboxStrategy(ExecutorContext executorContext) {
        if (useContiuousSandbox) {
            return new ContinuousSandboxStrategy(executorContext);
        } else {
            return new IndividualSandboxStrategy(executorContext);
        }
    }

}
