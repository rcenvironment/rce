/*
 * Copyright (C) 2006-2016 DLR, Germany
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
 * A sandbox strategy that creates a new sandbox for each execution phase.
 * 
 * @author Robert Mischke
 */
public class IndividualSandboxStrategy implements SandboxStrategy {

    private ExecutorContext executionEnvironment;

    private CommandLineExecutor currentExecutor;

    public IndividualSandboxStrategy(ExecutorContext executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }

    @Override
    public CommandLineExecutor prepareExecutionPhase() throws IOException {
        // create a new executor and sandbox
        currentExecutor = executionEnvironment.setUpSandboxedExecutor();
        return currentExecutor;
    }

    @Override
    public void afterExecutionPhase(CommandLineExecutor executor) throws IOException {
        // tear down after each execution phase
        executionEnvironment.tearDownSandbox(executor);
    }

    @Override
    public void beforeSessionTeardown() {
        // NOP
    }
}
