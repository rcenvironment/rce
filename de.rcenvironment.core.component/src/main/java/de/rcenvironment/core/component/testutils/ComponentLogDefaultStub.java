/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.component.execution.api.ComponentLog;

/**
 * Default mock for {@link ComponentLog}.
 * 
 * @author Doreen Seider
 */
public class ComponentLogDefaultStub implements ComponentLog {

    @Override
    public void toolStdout(String message) {}

    @Override
    public void toolStderr(String message) {}

    @Override
    public void componentError(String message) {}

    @Override
    public void componentWarn(String message) {}

    @Override
    public void componentInfo(String message) {}

    @Override
    public void componentError(String message, Throwable t, String errorId) {}

}
