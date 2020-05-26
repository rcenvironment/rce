/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.executor;

import org.apache.commons.exec.ExecuteWatchdog;

/**
 * This extension of the {@link ExecuteWatchdog} is used to get access to the {@link java.lang.Process} object which represents the system
 * process. You can access the the {@link java.lang.Process} object after you have started the execution asynchronously.
 * 
 * @author Tobias Rodehutskors
 */
class ProcessExtractor extends ExecuteWatchdog {

    private Process process;

    ProcessExtractor() {
        super(ExecuteWatchdog.INFINITE_TIMEOUT);
    }

    @Override
    public synchronized void start(Process pProcess) {
        this.process = pProcess;
        super.start(process);
    }

    public boolean hasStarted() {
        return process != null;
    }

    /**
     * @return the process or null. There can either happen if the process has been executed synchronously the process has not been started
     *         yet.
     */
    public Process getProcess() {
        return process;
    }

}
