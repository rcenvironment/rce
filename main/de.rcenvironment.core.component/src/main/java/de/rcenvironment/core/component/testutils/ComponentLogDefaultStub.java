/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
