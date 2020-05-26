/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.console.osgi;

import org.eclipse.osgi.framework.console.CommandInterpreter;

import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.spi.AbstractInteractiveCommandConsole;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Adapter to use an existing OSGi {@link CommandInterpreter} as a {@link TextOutputReceiver}. Only {@link #addOutput(String)} and
 * {@link #onFatalError(Exception)} have an effect; {@link #onStart()} and {@link #onFinished()} are ignored.
 * 
 * @author Robert Mischke
 */
public class OsgiConsoleOutputAdapter extends AbstractInteractiveCommandConsole {

    private final CommandInterpreter interpreter;

    // TODO merge back as nested class as it contains almost no functionality anymore? - misc_ro, 2014-06
    public OsgiConsoleOutputAdapter(CommandInterpreter interpreter, CommandExecutionService commandExecutionService) {
        super(commandExecutionService, "rce ");
        this.interpreter = interpreter;
    }

    @Override
    public void addOutput(String line) {
        interpreter.println(line);
    }

}
