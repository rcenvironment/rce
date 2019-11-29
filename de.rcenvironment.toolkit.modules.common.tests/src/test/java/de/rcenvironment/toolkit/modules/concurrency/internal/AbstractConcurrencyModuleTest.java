/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import org.junit.After;
import org.junit.Before;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.setup.ToolkitConfiguration;
import de.rcenvironment.toolkit.core.setup.ToolkitFactory;
import de.rcenvironment.toolkit.core.setup.ToolkitSetup;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadPoolManagementAccess;
import de.rcenvironment.toolkit.modules.concurrency.setup.ConcurrencyModule;

/**
 * Abstract base class for {@link ConcurrencyModule} unit tests. Its main purpose is to construct a new {@link Toolkit} instance for each
 * test run.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractConcurrencyModuleTest {

    private volatile Toolkit toolkit;

    private volatile ImmutableServiceRegistry serviceRegistry;

    /**
     * Creates a new test toolkit.
     * 
     * @throws ToolkitException on setup errors
     */
    @Before
    public final void setupToolkit() throws ToolkitException {
        toolkit = ToolkitFactory.create(new ToolkitConfiguration() {

            @Override
            public void configure(ToolkitSetup setup) throws ToolkitException {
                // only load the concurrency module; let automatic dependency checking resolve the rest
                setup.configureModule(ConcurrencyModule.class);
            }
        });
        serviceRegistry = toolkit.getServiceRegistry();
    }

    /**
     * Discards the current toolkit instance.
     */
    @After
    public final void tearDownToolkit() {
        toolkit.shutdown();
        toolkit = null;
        serviceRegistry = null;
    }

    protected final AsyncTaskService getAsyncTaskService() {
        return serviceRegistry.getService(AsyncTaskService.class);
    }

    protected final ConcurrencyUtilsFactory getConcurrencyUtilsFactory() {
        return serviceRegistry.getService(ConcurrencyUtilsFactory.class);
    }

    protected final ThreadPoolManagementAccess getThreadPoolManagement() {
        return serviceRegistry.getService(ThreadPoolManagementAccess.class);
    }

}
