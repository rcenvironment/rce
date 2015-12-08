/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.internal;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.SingleCommandHandler;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Handler/parser for a RCE multi-command. A multi-command may contain multiple commands which should be executed sequentially and/or in
 * parallel. The kind of execution is controlled by the structure of the command string.
 * 
 * NOTE: Currently, only sequential execution is supported. Parallel execution will be added as needed.
 * 
 * The current use cases are invocation via
 * <ul>
 * <li>the RCE command line, e.g.: <b>./rce --headless --exec "&lt;command string&gt;"</b></li>
 * <li>the OSGi console, e.g.: OSGi> <b>rce &lt;command string&gt;</b></li>
 * </ul>
 * 
 * The command is passed to this class as a list of tokens. The details of string tokenization are left to the caller. Note that usually,
 * the individual command parameters avoid the use of spaces; therefore, tokenization by spaces is usually sufficient.
 * 
 * @author Robert Mischke
 */
public class MultiCommandHandler implements Callable<CommandExecutionResult> {

    private static final String DOUBLE_QUOTE = "\"";

    private static final String ESCAPED_DOUBLE_QUOTE = "\\\"";

    /**
     * Example of sequential execution syntax: <code>"command1 param1 ; command2 param2a param2b"</code>. Note the token separators (in this
     * case, spaces) around the actual separator.
     */
    private static final String SEQUENTIAL_EXECUTION_SEPARATOR = ";";

    private final List<String> rawTokens;

    private final Deque<String> remainingTokens;

    private final TextOutputReceiver outputReceiver;

    private final SingleCommandHandler singleCommandHandler;

    private volatile Object initiatorInformation;

    public MultiCommandHandler(List<String> tokens, TextOutputReceiver outputReceiver, SingleCommandHandler singleCommandHandler) {
        this.rawTokens = tokens;
        this.remainingTokens = new LinkedList<String>(); // empty list; filled after normalization
        this.outputReceiver = outputReceiver;
        this.singleCommandHandler = singleCommandHandler;
    }

    /**
     * Executes the command provided via the constructor.
     * 
     * @return the result of this multi-command invocation
     */
    @TaskDescription("Text command execution")
    @Override
    public CommandExecutionResult call() {
        outputReceiver.onStart();

        // outputReceiver.addOutput("Pre: " + tokens);
        try {
            List<String> normalizedTokens = normalizeTokens(rawTokens);
            remainingTokens.addAll(normalizedTokens);
        } catch (IllegalArgumentException e) {
            // TODO use onFatalError() instead?
            outputReceiver.addOutput("Syntax Error: " + e.getMessage());
            outputReceiver.onFinished();
            return CommandExecutionResult.ERROR;
        }
        // outputReceiver.addOutput("Post: " + tokens);

        if (remainingTokens.isEmpty()) {
            // empty command string -> trigger help output
            CommandContext context = new CommandContext(new ArrayList<>(remainingTokens), outputReceiver, initiatorInformation);
            outputReceiver.onFatalError(CommandException.requestHelp(context));
            return CommandExecutionResult.DEFAULT; // TODO add result key for this?
        }
        try {
            List<String> collectedTokens = new LinkedList<String>();
            String token;
            do {
                token = getNextToken();
                if (token == null || token.equals(SEQUENTIAL_EXECUTION_SEPARATOR)) {
                    processSequentialPart(collectedTokens);
                    // reset
                    collectedTokens = new LinkedList<String>();
                } else {
                    collectedTokens.add(token);
                }
            } while (token != null);
            outputReceiver.onFinished();
            return CommandExecutionResult.DEFAULT;
        } catch (CommandException e) {
            outputReceiver.onFatalError(e);
            return CommandExecutionResult.ERROR;
        }
    }

    /**
     * Retrieves information about the source that invoked this command; see {@link #setInitiatorInformation(Object)}.
     * 
     * @return the information object
     */
    public Object getInitiatorInformation() {
        return initiatorInformation;
    }

    /**
     * Attaches an arbitrary object to transport information about the source that invoked this command.
     * 
     * @param shellAccountInformation the information object
     */
    public void setInitiatorInformation(Object shellAccountInformation) {
        this.initiatorInformation = shellAccountInformation;
    }

    private List<String> normalizeTokens(List<String> input) {
        List<String> output = new ArrayList<>();
        String quotedPartBuffer = null;
        for (String token : input) {
            if (token.isEmpty()) {
                continue;
            }
            if (quotedPartBuffer == null) {
                if (token.startsWith(DOUBLE_QUOTE) && !token.equals("\"")) {
                    if (token.endsWith(DOUBLE_QUOTE) && !token.endsWith(ESCAPED_DOUBLE_QUOTE)) {
                        // self-contained quoted part: unwrap and add to output
                        output.add(unescapeQuotes(token.substring(1, token.length() - 1)));
                    } else {
                        // start new quoted part
                        quotedPartBuffer = token.substring(1);
                    }
                } else {
                    // nothing special, just add to new list
                    output.add(unescapeQuotes(token));
                }
            } else {
                if (token.endsWith(DOUBLE_QUOTE) && !token.endsWith(ESCAPED_DOUBLE_QUOTE)) {
                    // end of quoted part
                    quotedPartBuffer += " " + token.substring(0, token.length() - 1);
                    output.add(unescapeQuotes(quotedPartBuffer));
                    quotedPartBuffer = null;
                } else {
                    // quoted part continued
                    // TODO check for nested quoted parts?
                    quotedPartBuffer += " " + token;
                }
            }
        }

        if (quotedPartBuffer != null) {
            throw new IllegalArgumentException("Unfinished quoted command part: " + quotedPartBuffer);
        }
        return output;
    }

    private String unescapeQuotes(String substring) {
        return substring.replace(ESCAPED_DOUBLE_QUOTE, DOUBLE_QUOTE);
    }

    private String getNextToken() {
        return remainingTokens.pollFirst();
    }

    private void processSequentialPart(List<String> tokens) throws CommandException {
        if (tokens.isEmpty()) {
            // ignore if empty
            return;
        }
        // TODO parse for parallel sections

        try {
            executeSingleCommand(tokens);
        } catch (CommandException e) {
            if (e.getType().equals(CommandException.Type.HELP_REQUESTED)) {
                outputReceiver.onFatalError(e);
            } else {
                throw e;
            }
        }
    }

    private void executeSingleCommand(List<String> tokens) throws CommandException {
        CommandContext commandContext = new CommandContext(tokens, outputReceiver, initiatorInformation);
        singleCommandHandler.execute(commandContext);
    }

}
