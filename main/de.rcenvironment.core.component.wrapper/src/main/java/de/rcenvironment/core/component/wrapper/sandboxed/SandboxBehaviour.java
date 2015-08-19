/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper.sandboxed;

import java.io.IOException;

import de.rcenvironment.core.utils.executor.CommandLineExecutor;

/**
 * Defines how the execution "sandbox", ie the working directory of the wrapped executable, behaves.
 * From a technical standpoint, this interface defines a set of lifecycle callbacks that are invoked
 * by the wrapper at the appropriate times (inversion of control).
 * 
 * @author Robert Mischke
 * 
 */
public interface SandboxBehaviour {

    /**
     * Prepares a single invocation of the wrapped executable.
     * 
     * @return the {@link CommandLineExecutor} to use for the execution; this may or may not return
     *         the same instance on every invocation
     * @throws IOException on I/O errors
     */
    CommandLineExecutor setupSingleRun() throws IOException;

    /**
     * Callback for operations after the executable was run. Note that this method must be passed
     * the same executor that was previously acquired from {@link #setupSingleRun()} and used to
     * invoke the executable; otherwise, the behaviour is undefined.
     * 
     * @param executor the {@link CommandLineExecutor} that was used for the execution
     * @throws IOException on I/O errors
     */
    void afterSingleRun(CommandLineExecutor executor) throws IOException;

    /**
     * Callback that signals that the execution environment (which can span over multiple
     * invocations of the target executable) is about to be destroyed. Final cleanup operations are
     * usually performed here.
     * 
     * @throws IOException on I/O errors
     */
    void beforeTearDownStaticEnvironment() throws IOException;

}
