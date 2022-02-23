/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.wrapper.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.wrapper.sandboxed.ExecutionEnvironment;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.LocalCommandLineExecutor;

/**
 * An {@link ExecutionEnvironment} that sets up {@link CommandLineExecutor}s for the local system.
 * 
 * @author Robert Mischke
 */
public class LocalExecutionEnvironment implements ExecutionEnvironment {

    private File localSandboxDir;

    private Log log = LogFactory.getLog(getClass());

    @Override
    public void setupStaticEnvironment() throws IOException {
        // NOP
    }

    @Override
    public CommandLineExecutor setupExecutorWithSandbox()
        throws IOException {
        localSandboxDir = TempFileServiceAccess.getInstance()
            .createManagedTempDir("sandbox");
        log.debug("Prepared local sandbox at " + localSandboxDir);
        return new LocalCommandLineExecutor(localSandboxDir);
    }

    @Override
    public String createUniqueTemporaryStoragePath() throws IOException {
        return TempFileServiceAccess.getInstance()
            .createManagedTempDir("static-session-storage")
            .getAbsolutePath();
    }

    @Override
    public void tearDownSandbox(CommandLineExecutor executor)
        throws IOException {
        if (localSandboxDir != null){
            log.debug("Cleaning local sandbox at "
                + localSandboxDir.getAbsolutePath());
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(
                localSandboxDir);
        } else {
            log.warn("Failed to clean local sandbox as local sandbox directory was null.");
        }
    }

    @Override
    public void tearDownStaticEnvironment() {
        // NOP
    }
}
