/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Receiver/processor for single {@link ConsoleRow} events.
 * 
 * @author Robert Mischke
 */
public interface SingleConsoleRowsProcessor {

    /**
     * Called on a {@link ConsoleRow} event.
     * 
     * @param consoleRow the new {@link ConsoleRow}
     */
    void onConsoleRow(ConsoleRow consoleRow);

}
