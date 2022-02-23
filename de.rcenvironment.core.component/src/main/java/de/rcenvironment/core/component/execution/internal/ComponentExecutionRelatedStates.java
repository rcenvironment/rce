/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.internal;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Store certain states that are shared between multiple instance.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionRelatedStates {

    protected AtomicInteger executionCount = new AtomicInteger(0);

    protected AtomicBoolean finalHistoryDataItemWritten = new AtomicBoolean(false);

    protected AtomicBoolean intermediateHistoryDataWritten = new AtomicBoolean(false);

    protected AtomicBoolean compHasSentConsoleRowLogMessages = new AtomicBoolean(false);

    protected AtomicInteger consoleRowSequenceNumber = new AtomicInteger(0);

    protected AtomicBoolean isComponentCancelled = new AtomicBoolean(false);

}
