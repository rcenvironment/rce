/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.api;

import java.util.List;
import java.util.concurrent.Future;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Interface for the execution of string commands. Examples of commands are workflow execution, network status and manipulation, and
 * developer/debug output generation.
 * 
 * @author Robert Mischke
 */
public interface CommandExecutionService {

    /**
     * Asynchronously executes a multi-command. A multi-command is a list of string tokens describing one or more commands, which are to be
     * executed sequentially and/or in parallel. Special tokens are used to separate individual commands, and to mark commands for parallel
     * execution. The default is sequential execution.
     * 
     * TODO add documentation of special tokens; add examples
     * 
     * All output is sent to the provided {@link TextOutputReceiver}. Before execution starts, {@link TextOutputReceiver#onStart()} is
     * called. If an error occurs during execution, it is passed to {@link TextOutputReceiver#onFatalError(Exception)}. If no exception
     * occurs, {@link TextOutputReceiver#onFinished()} is called after the last command has terminated.
     * 
     * @param tokens the tokens of the multi-command to execute
     * @param outputReceiver the receiver of output and execution lifecycle events
     * @param initiator an arbitrary object providing information about the source that initiated this command; may be used to check account
     *        permissions
     * @return a {@link Future} for a {@link CommandExecutionResult} that can be waited for to detect the end of command execution
     */
    Future<CommandExecutionResult> asyncExecMultiCommand(List<String> tokens, TextOutputReceiver outputReceiver, Object initiator);

    /**
     * Writes command help information to the given {@link TextOutputReceiver}.
     * 
     * @param addCommonPrefix true if the common prefix ("rce ...") should be added
     * @param showDevCommands whether developer commands should be included
     * @param outputReceiver the {@link TextOutputReceiver} to write to
     */
    void printHelpText(boolean addCommonPrefix, boolean showDevCommands, TextOutputReceiver outputReceiver);

    /**
     * Prints the same output as {@link #printHelpText(boolean, TextOutputReceiver)}, but collects the output and returns it as a
     * newline-separated string.
     * 
     * @param addCommonPrefix true if the common prefix ("rce ...") should be added
     * @param showDevCommands whether developer commands should be included
     * @return the collected output
     */
    String getHelpText(boolean addCommonPrefix, boolean showDevCommands);

}
