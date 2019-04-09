/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context;

import java.io.IOException;

import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.context.spi.ExecutorContext;
import de.rcenvironment.core.utils.executor.context.spi.SandboxStrategy;

/**
 * Convenience class that encapsulates the handling of {@link ExecutorContext} and
 * {@link SandboxStrategy} instances for typical use cases (Facade pattern).
 * 
 * @author Robert Mischke
 * 
 */
public class SandboxedExecutorLifeCycleFacade {

    private ExecutorContext executionContext;

    private SandboxStrategy sandboxStrategy;

    public SandboxedExecutorLifeCycleFacade(ExecutorContextFactory factory) {
        this.executionContext = factory.createExecutorContext();
        this.sandboxStrategy = factory.createSandboxStrategy(executionContext);
    }

    /**
     * Initializes an executor context session. A session is a scope for one or more command
     * execution phases that may share properties and/or resources.
     * 
     * @throws IOException on general I/O errors
     * @throws ValidationFailureException on validation errors caused by properties passed to the
     *         concrete implementation
     */
    public void setUpSession() throws IOException, ValidationFailureException {
        executionContext.setUpSession();
    }

    /**
     * Prepares and/or fetches the proper {@link CommandLineExecutor} to use for the next execution
     * phase. The executor may or may not use the same sandbox as a previous execution phase; this
     * is determined by the chosen sandbox strategy.
     * 
     * IMPORTANT: Callers must return the acquired executor with
     * {@link #tearDownExecutionPhase(CommandLineExecutor)} when finished.
     * 
     * @return the executor to use for the next execution phase
     * @throws IOException on general I/O errors
     */
    public CommandLineExecutor setUpExecutionPhase() throws IOException {
        return sandboxStrategy.prepareExecutionPhase();
    }

    /**
     * Returns an executor that was acquired via {@link #setUpExecutionPhase()}.
     * 
     * IMPORTANT: Callers of {@link #setUpExecutionPhase()} *MUST* call this method when finished,
     * as it may perform important cleanup operations.
     * 
     * @param executor the previously-acquired executor
     * @throws IOException on general I/O errors
     */
    public void tearDownExecutionPhase(CommandLineExecutor executor) throws IOException {
        sandboxStrategy.afterExecutionPhase(executor);
    }

    /**
     * Ends the executor context session.
     * 
     * @throws IOException on general I/O errors
     */
    public void tearDownSession() throws IOException {
        sandboxStrategy.beforeSessionTeardown();
        executionContext.tearDownSession();
    }

    /**
     * Provides access to the wrapped {@link ExecutorContext} if needed.
     * 
     * @return the {@link ExecutorContext} used for the current session
     */
    public ExecutorContext getExecutorContext() {
        return executionContext;
    }
}
