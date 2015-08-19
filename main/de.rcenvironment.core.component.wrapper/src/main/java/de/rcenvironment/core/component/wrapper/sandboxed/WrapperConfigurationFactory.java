/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper.sandboxed;

/**
 * This interface separates the configuration of wrapper instances from the generic wrapper
 * implementation (abstract factory pattern). Defines the {@link ExecutionEnvironment} and the
 * {@link SandboxBehaviour} to use; logically, these are independent, although each
 * {@link SandboxBehaviour} requires an {@link ExecutionEnvironment} instance to delegate its
 * operations to.
 * 
 * @author Robert Mischke
 * 
 */
public interface WrapperConfigurationFactory {

    /**
     * @return a configured {@link ExecutionEnvironment}
     */
    ExecutionEnvironment createExecutionEnvironment();

    /**
     * @param executionEnvironment the {@link ExecutionEnvironment} to perform the
     *        {@link SandboxBehaviour} in; usually previously acquired from
     *        {@link #createExecutionEnvironment()}
     * @return a configured {@link SandboxBehaviour}
     */
    SandboxBehaviour createSandboxBehaviour(ExecutionEnvironment executionEnvironment);
}
