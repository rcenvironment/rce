/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context.impl;

import java.io.IOException;

import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.context.spi.ExecutorContext;
import de.rcenvironment.core.utils.executor.context.spi.SandboxStrategy;

/**
 * A sandbox strategy that reuses a single sandbox for the whole duration of a
 * {@link ExecutorContext} session, ignoring execution phase switches.
 * 
 * @author Robert Mischke
 */
public class ContinuousSandboxStrategy implements SandboxStrategy {

    private CommandLineExecutor singleExecutor;

    private ExecutorContext executionEnvironment;

    public ContinuousSandboxStrategy(ExecutorContext executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }

    @Override
    public CommandLineExecutor prepareExecutionPhase() throws IOException {
        // only create one durable executor and sandbox
        if (singleExecutor == null) {
            singleExecutor = executionEnvironment.setUpSandboxedExecutor();
        }
        return singleExecutor;
    }

    @Override
    public void afterExecutionPhase(CommandLineExecutor executor) throws IOException {
        // consistency check
        if (executor != singleExecutor) {
            throw new IllegalStateException("Provided executor does not match internal executor");
        }
        // NOP
    }

    @Override
    public void beforeSessionTeardown() throws IOException {
        // tear down only on global/static tear-down
        executionEnvironment.tearDownSandbox(singleExecutor);
    }

}
