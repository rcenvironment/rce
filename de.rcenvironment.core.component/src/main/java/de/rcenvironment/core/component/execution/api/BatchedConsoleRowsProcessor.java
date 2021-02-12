/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;


/**
 * Callback interface for {@link ConsoleRow}s processing.
 * 
 * @author Doreen Seider
 */
public interface BatchedConsoleRowsProcessor {

    /**
     * Called when new {@link ConsoleRow}s are provided.
     * 
     * @param consoleRows {@link ConsoleRow}s to process
     */
    void processConsoleRows(ConsoleRow[] consoleRows);
}
