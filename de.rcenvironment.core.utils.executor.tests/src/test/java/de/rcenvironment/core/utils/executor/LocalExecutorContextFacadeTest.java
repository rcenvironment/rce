/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.executor.context.SandboxedExecutorLifeCycleFacade;
import de.rcenvironment.core.utils.executor.context.impl.LocalExecutorContextFactory;

/**
 * Tests a {@link SandboxedExecutorLifeCycleFacade} configured with a
 * {@link LocalExecutorContextFactory}.
 * 
 * @author Robert Mischke
 */
public class LocalExecutorContextFacadeTest extends CommonExecutorTests {

    /**
     * Static test setup.
     */
    @BeforeClass
    public static void classSetUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Basic test of {@link SandboxedExecutorLifeCycleFacade} operation.
     * 
     * @throws Exception on unexpected test errors
     */
    @Test(timeout = DEFAULT_TEST_TIMEOUT)
    public void testBasicLifeCycle() throws Exception {
        LocalExecutorContextFactory contextFactory = new LocalExecutorContextFactory(false);
        SandboxedExecutorLifeCycleFacade facade = new SandboxedExecutorLifeCycleFacade(contextFactory);
        facade.setUpSession();
        try {
            CommandLineExecutor executor;
            // run once
            executor = facade.setUpExecutionPhase();
            try {
                testCrossPlatformEcho(executor);
            } finally {
                facade.tearDownExecutionPhase(executor);
            }
            // run again
            executor = facade.setUpExecutionPhase();
            try {
                testCrossPlatformEcho(executor);
            } finally {
                facade.tearDownExecutionPhase(executor);
            }
            // TODO improve test: test both sandbox strategies; verify sandbox behavior; ...
        } finally {
            facade.tearDownSession();
        }
    }

}
