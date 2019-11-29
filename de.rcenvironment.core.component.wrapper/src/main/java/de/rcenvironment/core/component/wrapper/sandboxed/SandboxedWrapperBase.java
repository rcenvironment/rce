/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper.sandboxed;

import java.io.IOException;

import de.rcenvironment.core.component.datamanagement.stateful.StatefulComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.wrapper.MonitoringEventListener;
import de.rcenvironment.core.component.wrapper.WrapperBase;
import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;

/**
 * A base class for wrappers that execute wrapped tools in "sandbox" environments, ie in temporary
 * work directories. The definition of the execution environment and the sandbox behaviour is
 * performed by an instance of the {@link WrapperConfigurationFactory} interface. Examples of
 * execution environments are local execution or delegation via SSH. Examples of sandbox behaviours
 * are "create a new work directory on every run" or "keep one directory for all repeated runs".
 * 
 * @author Robert Mischke
 * 
 * @param <C> the type of the configuration object passed to each tool invocation
 * @param <R> the type of the result returned from each tool invocation
 */
public abstract class SandboxedWrapperBase<C, R> extends
    WrapperBase<C, R> {

    private ExecutionEnvironment executionEnvironment;

    private SandboxBehaviour sandboxStrategy;

    private String workDirPath;

    private WrapperConfigurationFactory configurationFactory;

    public SandboxedWrapperBase(WrapperConfigurationFactory configurationFactory,
        StatefulComponentDataManagementService fileReferenceHandler,
        MonitoringEventListener listener, ComponentContext compContext) {
        super(fileReferenceHandler, listener, compContext);
        this.configurationFactory = configurationFactory;
    }

    @Override
    public void setupStaticEnvironment() throws IOException,
        ValidationFailureException {

        // select appropriate implementation (strategy pattern)
        executionEnvironment = configurationFactory.createExecutionEnvironment();

        sandboxStrategy = configurationFactory.createSandboxBehaviour(executionEnvironment);

        // delegate
        executionEnvironment.setupStaticEnvironment();
    }

    @Override
    public final R execute(C runConfiguration) throws IOException,
        ValidationFailureException {
        CommandLineExecutor executor = setupRunEnvironment(runConfiguration);
        try {
            return executeInRunEnvironment(runConfiguration, executor);
        } finally {
            tearDownRunEnvironment(runConfiguration, executor);
        }
    };

    @Override
    public void tearDownStaticEnvironment() throws IOException {
        sandboxStrategy.beforeTearDownStaticEnvironment();
        executionEnvironment.tearDownStaticEnvironment();
    }

    protected CommandLineExecutor setupRunEnvironment(C runConfiguration)
        throws IOException, ValidationFailureException {
        CommandLineExecutor executor = sandboxStrategy.setupSingleRun();
        workDirPath = executor.getWorkDirPath();
        return executor;
    }

    protected abstract R executeInRunEnvironment(C runConfiguration,
        CommandLineExecutor executor);

    protected void tearDownRunEnvironment(C runConfiguration,
        CommandLineExecutor executor) throws IOException,
        ValidationFailureException {
        sandboxStrategy.afterSingleRun(executor);
    }

    protected String getWorkDirPath() {
        return workDirPath;
    }

}
