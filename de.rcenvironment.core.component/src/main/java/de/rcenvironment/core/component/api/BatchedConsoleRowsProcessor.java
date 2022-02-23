/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.api;

import java.io.Serializable;

import de.rcenvironment.core.component.execution.api.ConsoleRow;

/**
 * Callback interface for {@link ConsoleRow}s provisioning.
 * 
 * @author Doreen Seider
 */
public interface BatchedConsoleRowsProcessor extends Serializable {

    /**
     * Called when new {@link ConsoleRow}s are provided.
     * 
     * @param consoleRows {@link ConsoleRow}s to process
     */
    void processConsoleRows(ConsoleRow[] consoleRows);
}
