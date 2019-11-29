/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context.spi;

import java.io.IOException;

import de.rcenvironment.core.utils.executor.CommandLineExecutor;

/**
 * Interface that allows pluggable sandbox behaviors (strategy pattern).
 * 
 * TODO expand documentation?
 * 
 * @author Robert Mischke
 */
public interface SandboxStrategy {

    /**
     * Fetches the proper {@link CommandLineExecutor} to use for the next execution phase. The
     * executor may or may not use the same sandbox as a previous execution phase; this is
     * determined by the chosen sandbox strategy.
     * 
     * IMPORTANT: Callers must return the acquired executor with
     * {@link #afterExecutionPhase(CommandLineExecutor)} when finished.
     * 
     * @return the executor to use for the next execution phase
     * @throws IOException on general I/O errors
     */
    CommandLineExecutor prepareExecutionPhase() throws IOException;

    /**
     * Returns an exeutor that was acquired via {@link #prepareExecutionPhase()}. Callers of
     * {@link #prepareExecutionPhase()} *MUST* call this method when finished, as it may perform
     * important cleanup operations.
     * 
     * @param executor the previously-acquired executor
     * @throws IOException on general I/O errors
     */
    void afterExecutionPhase(CommandLineExecutor executor) throws IOException;

    /**
     * Life cycle hook that allows for final cleanup of sandbox-related resources.
     * 
     * @throws IOException on general I/O errors
     */
    void beforeSessionTeardown() throws IOException;
}
