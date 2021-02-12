/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

/**
 * This checked exception can be used to indicate that the System should exit. However instead of calling System.exit() this exception makes
 * this decision more visible to the programmer and can also be used in unit tests.
 *
 * @author Tobias Brieden
 */
public class SystemExitException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 7611694423737390496L;

    private final int exitCode;

    public SystemExitException(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}
