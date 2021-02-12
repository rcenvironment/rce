/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.SingleCommandHandler;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

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
 * @author Tobias Rodehutskors (Injection of the FileLogger)
 */
public class MultiCommandHandler implements Callable<CommandExecutionResult> {

    // /**
    // * The command string of the 'saveto' command.
    // */
    // public static final String SAVETO = "saveto";
    //
    // /**
    // * TODO This function should be placed in a helper class FileUtils.
    // *
    // * These Strings are not allowed as filenames on the Windows platform.
    // */
    // private static final String[] FORBIDDEN_FILENAMES = { "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6",
    // "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9" };

    private static final String DOUBLE_QUOTE = "\"";

    private static final String ESCAPED_DOUBLE_QUOTE = "\\\"";

    // private static final String MIRROR = "-m";
    //
    // private static final String AUTO = "--auto";

    /**
     * Example of sequential execution syntax: <code>"command1 param1 ; command2 param2a param2b"</code>. Note the token separators (in this
     * case, spaces) around the actual separator.
     */
    private static final String SEQUENTIAL_EXECUTION_SEPARATOR = ";";

    private final List<String> rawTokens;

    private final Deque<String> remainingTokens;

    private TextOutputReceiver outputReceiver;

    private final SingleCommandHandler singleCommandHandler;

    private volatile Object initiatorInformation;

    // private final File profileOutputDirectory;

    public MultiCommandHandler(List<String> tokens, TextOutputReceiver outputReceiver, SingleCommandHandler singleCommandHandler,
        File profileOutput) {

        this.rawTokens = tokens;
        this.remainingTokens = new LinkedList<String>(); // empty list; filled after normalization
        this.outputReceiver = outputReceiver;
        this.singleCommandHandler = singleCommandHandler;
        // this.profileOutputDirectory = profileOutput;
    }

    /**
     * This constructor is manly intended for easing testing. As it does not specify the location of the profile output directory, this
     * class will return an error if the 'saveto' command is encountered during command execution.
     */
    public MultiCommandHandler(List<String> tokens, TextOutputReceiver outputReceiver, SingleCommandHandler singleCommandHandler) {

        this(tokens, outputReceiver, singleCommandHandler, null);
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
        // outputReceiver.addOutput("Pre: " + rawTokens);
        try {
            List<String> normalizedTokens = normalizeTokens(rawTokens);
            remainingTokens.addAll(normalizedTokens);
        } catch (IllegalArgumentException e) {
            // TODO use onFatalError() instead?
            outputReceiver.addOutput("Syntax Error: " + e.getMessage());
            outputReceiver.onFinished();
            return CommandExecutionResult.ERROR;
        }
        // outputReceiver.addOutput("Post: " + remainingTokens);

        if (remainingTokens.isEmpty()) {
            // empty command string -> trigger help output
            CommandContext context = new CommandContext(new ArrayList<>(remainingTokens), outputReceiver, initiatorInformation);
            outputReceiver.onFatalError(CommandException.requestHelp(context));
            return CommandExecutionResult.DEFAULT; // TODO add result key for this?
        }
        try {
            // // we cannot invoke the file logger earlier, since we need to check the provided commands, which needs to be normalized first
            // injectFileLoggerIfNecessary();

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

    // /**
    // * TODO This function should be placed in a helper class FileUtils.
    // *
    // * This methods checks if a filename was supplied, which does not use directory traversal.
    // */
    // private static boolean isValidFilename(File profileOutputDirectory2, String filename) {
    // if (filename == null) {
    // return false;
    // }
    //
    // // check for directory traversal
    // if (!FilenameUtils.getName(filename).equals(filename)) {
    // return false;
    // }
    //
    // if (Arrays.asList(FORBIDDEN_FILENAMES).contains(filename)) {
    // return false;
    // }
    //
    // return true;
    // }

    // /**
    // * Parses the first tokens to check whether 'saveto [-m] (<filename>|--auto) <command(s)>' was specified. If this is the case, it
    // either
    // * replaces the original output receiver with a {@link FileLoggingTextOutputReceiver} or adds a {@link FileLoggingTextOutputReceiver}
    // to
    // * the original receiver depending on the presence of the mirror option.
    // *
    // * @throws CommandException
    // */
    // private void injectFileLoggerIfNecessary() throws CommandException {
    // // check if 'safeto' is specified and all necessary options are given
    // if (SAVETO.equals(remainingTokens.peekFirst())) {
    // List<String> collectedTokens = new LinkedList<String>();
    // collectedTokens.add(remainingTokens.pollFirst());
    //
    // boolean mirror = false;
    //
    // // check if mirror option is set
    // if (MIRROR.equals(remainingTokens.peekFirst())) {
    // collectedTokens.add(remainingTokens.pollFirst());
    // mirror = true;
    // }
    //
    // String filename = remainingTokens.peekFirst();
    // collectedTokens.add(remainingTokens.pollFirst());
    // CommandContext commandContext = new CommandContext(collectedTokens, outputReceiver, initiatorInformation);
    //
    // if (profileOutputDirectory == null) {
    // throw CommandException.executionError("Internal Error: The profile output directory is unkown.", commandContext);
    // }
    //
    // if (AUTO.equals(filename)) {
    // // It is unlikely that this file already exists
    // filename = "cmd_" + System.currentTimeMillis() + ".txt";
    // }
    //
    // if (!isValidFilename(profileOutputDirectory, filename)) {
    // throw CommandException.syntaxError("You either need to specify the '--auto' option or supply a valid filename.",
    // commandContext);
    // }
    //
    // // construct the path to the file and create it
    // Path filePath = profileOutputDirectory.toPath().resolve(filename);
    // try {
    // filePath = Files.createFile(filePath);
    // // success
    // } catch (FileAlreadyExistsException e) {
    // throw CommandException.executionError("This file already exists. Please choose another file.", commandContext);
    // } catch (IOException e) {
    // throw CommandException.executionError(
    // "Encountered an IO error. Please try again with another file." + System.lineSeparator() + "IO error: " + e.toString(),
    // commandContext);
    // }
    //
    // injectFileLogger(filePath, mirror);
    // }
    // }
    //
    // private void injectFileLogger(Path file, boolean mirror) {
    // FileLoggingTextOutputReceiver fileLogger = new FileLoggingTextOutputReceiver(file);
    // fileLogger.onStart();
    //
    // if (mirror) {
    // // forward the received text to the original receiver and to the file logger
    // MultiTextOutputReceiver multiOutputReceiver = new MultiTextOutputReceiver();
    // multiOutputReceiver.addTextOutputReceiver(fileLogger);
    // multiOutputReceiver.addTextOutputReceiver(outputReceiver);
    // outputReceiver = multiOutputReceiver;
    // } else {
    // // replace the original receiver with the file logger
    // outputReceiver.onFinished();
    // outputReceiver = fileLogger;
    // }
    // }

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

        executeSingleCommand(tokens);
    }

    private void executeSingleCommand(List<String> tokens) throws CommandException {
        CommandContext commandContext = new CommandContext(tokens, outputReceiver, initiatorInformation);
        singleCommandHandler.execute(commandContext);
    }

}
