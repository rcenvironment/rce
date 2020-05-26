/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper;

import de.rcenvironment.core.utils.executor.CommandLineExecutor;

/**
 * A hook/listener interface for basic wrapper lifecycle events. Subinterfaces may add more detailed
 * lifecycle events.
 * 
 * @author Robert Mischke
 * 
 */
public interface BasicWrapperHook {

    /**
     * Invoked after the configuration for a single execution has happened, but before the actual
     * execution.
     * 
     * @param executor the {@link CommandLineExecutor} which will be used for the execution
     * @param remoteWorkDir the path of the work directory in the execution environment; may or may
     *        not be a local filesystem path
     */
    void beforeExecution(CommandLineExecutor executor, String remoteWorkDir);

    /**
     * Invoked immediately after each single execution.
     * 
     * @param executor the {@link CommandLineExecutor} that was used for the execution
     * @param remoteWorkDir the path of the work directory in the execution environment; may or may
     *        not be a local filesystem path
     */
    void afterExecution(CommandLineExecutor executor, String remoteWorkDir);

}
