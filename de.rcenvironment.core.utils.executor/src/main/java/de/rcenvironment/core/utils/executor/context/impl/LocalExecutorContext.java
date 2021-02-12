/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.executor.context.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.executor.LocalCommandLineExecutor;
import de.rcenvironment.core.utils.executor.context.spi.ExecutorContext;

/**
 * An {@link ExecutorContext} implementation for local execution using a {@link LocalCommandLineExecutor}. Acquires temporary directories
 * from the default {@link TempFileService}.
 * 
 * @author Robert Mischke
 */
public class LocalExecutorContext implements ExecutorContext {

    private File currentSandboxDir;

    private Log log = LogFactory.getLog(getClass());

    @Override
    public void setUpSession() throws IOException {
        // NOP
    }

    @Override
    public void tearDownSession() {
        // TODO actively dispose directories created via #createUniqueTempDir()?
    }

    @Override
    public CommandLineExecutor setUpSandboxedExecutor() throws IOException {
        // prevent coding errors from reusing undisposed contexts
        if (currentSandboxDir != null) {
            throw new IllegalStateException("The previous sandbox has not been disposed yet");
        }
        // create new sandbox
        currentSandboxDir = TempFileServiceAccess.getInstance().createManagedTempDir("sandbox");
        log.debug("Prepared local sandbox at " + currentSandboxDir);
        return new LocalCommandLineExecutor(currentSandboxDir);
    }

    @Override
    public void tearDownSandbox(CommandLineExecutor executor) throws IOException {
        if (currentSandboxDir == null) {
            // accept tear down of uninitialized sandbox to allow simple cleanup with try...finally
            log.debug("No initialized local sandbox, ignoring tear down request");
            return;
        }
        log.debug("Cleaning local sandbox at " + currentSandboxDir.getAbsolutePath());
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(currentSandboxDir);
        currentSandboxDir = null;
    }

    @Override
    public String createUniqueTempDir(String contextHint) throws IOException {
        String tempDirPath = TempFileServiceAccess.getInstance().createManagedTempDir(contextHint).getAbsolutePath();
        log.debug("Created new local temp directory at " + tempDirPath);
        return tempDirPath;
    }

}
