/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.List;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Interface for actual processors of received {@link ConsoleRow}s.
 * 
 * @author Robert Mischke
 */
public interface ConsoleRowProcessor {

    /**
     * Processes a batch of received {@link ConsoleRow}. The processor should not try to modify the provided list, as it may be shared
     * and/or immutable.
     * 
     * @param rows the {@link ConsoleRow}s to process.
     */
    void processConsoleRows(List<ConsoleRow> rows);
}
