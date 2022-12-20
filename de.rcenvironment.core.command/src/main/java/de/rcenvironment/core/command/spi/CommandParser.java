/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.FileLoggingTextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.MultiTextOutputReceiver;

/**
 * Parses tokens of commands.
 * The command itself as well as its parameters are parsed.
 * 
 * @author Sebastian Nocke
 *
 */
public class CommandParser {

    private static final String QUOTAION_MARK = "\"";
    
    private static final String HELP = "HELP";
    
    private static final String DEV = "DEV";
    
    private static final String EXPLAIN = "explain";
    
    private static final String DASH = "-";
    
    private static final String DETAILS_SHORT = "-d";

    private static final String DETAILS_LONG = "--details";
    
    private static final String DEV_LONG = "--dev";
    
    private static final String SAVETO = "saveto";
    
    private static final String MIRROR_SHORT = "-m";
    
    private static final String AUTO = "--auto";
    
    private static final String ASCIIDOC = "--asciidoc";
    
    private static final CommandFlag DEV_FLAG = new CommandFlag(DEV_LONG, DEV_LONG);
    
    private static final CommandFlag DETAILS_FLAG = new CommandFlag(DETAILS_SHORT, DETAILS_LONG);
    
    private static final CommandFlag ASCIIDOC_FLAG = new CommandFlag(ASCIIDOC);
    
    private Map<String, MainCommandDescription> registeredCommands = new HashMap<>();
    
    public void registerCommands(MainCommandDescription[] commands) {
        
        synchronized (registeredCommands) {
            Arrays.stream(commands).forEach(command -> registeredCommands.put(command.getCommand(), command));
        }
    }
    
    public void unregisterCommands(MainCommandDescription[] commands) {
        synchronized (registeredCommands) {
            Arrays.stream(commands).forEach(command -> {
                if (registeredCommands.containsKey(command.getCommand())) {
                    registeredCommands.remove(command.getCommand());
                }
            });
        }
    }
    
    public ExecutableCommand parseCommand(CommandContext context) throws CommandException {
        
        String firstToken = context.consumeNextToken();
        
        if (registeredCommands.containsKey(firstToken)) {
            
            MainCommandDescription mainCommand = registeredCommands.get(firstToken);
            
            if (HELP.equalsIgnoreCase(firstToken)) {
                context.setParsedModifiers(parseHelpCommand(context, false));
                return new ExecutableCommand(mainCommand.getHandler(), context);
                
            } else if (DEV.equalsIgnoreCase(firstToken)) {
                context.setParsedModifiers(parseHelpCommand(context, true));
                return new ExecutableCommand(mainCommand.getHandler(), context);
                
            } else if (SAVETO.equalsIgnoreCase(firstToken)) {
                return parseSavetoCommand(context);
                
            } else if (EXPLAIN.equalsIgnoreCase(firstToken)) {
                return new ExecutableCommand(mainCommand.getHandler(), context);
            }
            
            String secondToken = context.peekNextToken();

            if (secondToken == null) {
                
                if (mainCommand.isExecutable()) {
                    ParsedCommandModifiers values = mainCommand.getModifiers().parseModifiers(context);
                    context.setParsedModifiers(values);
                    return new ExecutableCommand(mainCommand.getHandler(), context);
                    
                } else {
                    throw CommandException.syntaxError(QUOTAION_MARK + firstToken
                            + "\" is not an executable command, a subcommand is needed", context);
                    
                }
                
            }
            
            if (mainCommand.hasSubCommand(secondToken)) {
                SubCommandDescription command = mainCommand.getSubCommand(context.consumeNextToken());
                ParsedCommandModifiers values = command.getModifiers().parseModifiers(context);
                
                context.setParsedModifiers(values);
                return new ExecutableCommand(command.getHandler(), context);
                
            } else if (mainCommand.isExecutable()) {
            
                ParsedCommandModifiers values = mainCommand.getModifiers().parseModifiers(context);
                context.setParsedModifiers(values);
                return new ExecutableCommand(mainCommand.getHandler(), context);
                
            } else {
                throw CommandException.syntaxError(QUOTAION_MARK + secondToken
                        + "\" is not a subcommand of \"" + firstToken + QUOTAION_MARK, context);
           
            }
            
        }
        
        throw CommandException.unknownCommand(context);
    }
    
    private ParsedCommandModifiers parseHelpCommand(CommandContext context, boolean isDev) throws CommandException {
        
        List<String> parameters = context.consumeRemainingTokens();
        ParsedStringParameter[] params;
        CommandFlag[] flags;
        ParsedCommandModifiers modifiers;
        
        List<CommandFlag> cFlag = new ArrayList<>();
        
        if (isDev) {
            cFlag.add(DEV_FLAG);
        }
        
        if (!parameters.isEmpty()) {
            
            if (parameters.get(0).startsWith(DASH)) {
                
                params = new ParsedStringParameter[0];
                
                int devIndex = parameters.indexOf(DEV_LONG);
                
                if (devIndex != 0 - 1 && !cFlag.contains(DEV_FLAG)) {
                    cFlag.add(DEV_FLAG);
                    parameters.remove(devIndex);
                    
                }
                
                int showIndex = parameters.indexOf(DETAILS_SHORT);
                
                if (showIndex == 0 - 1) {
                    showIndex = parameters.indexOf(DETAILS_LONG);
                    
                }
                
                if (showIndex != 0 - 1) {
                    cFlag.add(DETAILS_FLAG);
                    parameters.remove(showIndex);
                    
                }
                
                int asciidocIndex = parameters.indexOf(ASCIIDOC);
                
                if (asciidocIndex != 0 - 1) {
                    cFlag.add(ASCIIDOC_FLAG);
                    parameters.remove(asciidocIndex);
                }
                
                flags = cFlag.toArray(new CommandFlag[cFlag.size()]);
                
                if (!parameters.isEmpty()) {
                    throw CommandException.syntaxError("Flags cannot be followed by a specific command", context);
                
                }
                
            } else {
                
                params = new ParsedStringParameter[] { new ParsedStringParameter(parameters.get(0)) };
                parameters.remove(0);
                
                int devIndex = parameters.indexOf(DEV_LONG);
                
                if (devIndex != 0 - 1) {
                    cFlag.add(DEV_FLAG);
                    parameters.remove(devIndex);
                    
                }
                
                int showIndex = parameters.indexOf(DETAILS_SHORT);
                
                if (showIndex == 0 - 1) {
                    showIndex = parameters.indexOf(DETAILS_LONG);
                    
                }
                
                if (showIndex != 0 - 1) {
                    cFlag.add(DETAILS_FLAG);
                    parameters.remove(showIndex);
                    
                }
                
                int asciidocIndex = parameters.indexOf(ASCIIDOC);
                
                if (asciidocIndex != 0 - 1) {
                    cFlag.add(ASCIIDOC_FLAG);
                    parameters.remove(asciidocIndex);
                }
                
                if (!parameters.isEmpty()) {
                    throw CommandException.syntaxError("Unknown flags entered", context);
                }
                
                flags = cFlag.toArray(new CommandFlag[cFlag.size()]);
                
            }
            
            modifiers = new ParsedCommandModifiers(params, flags, new HashMap<>());
            
        } else {
            
            params = new ParsedStringParameter[0];
            flags = cFlag.toArray(new CommandFlag[cFlag.size()]);
            modifiers = new ParsedCommandModifiers(params, flags, new HashMap<>());
            
        }
        
        return modifiers;
    }
    
    private ExecutableCommand parseSavetoCommand(CommandContext context) throws CommandException {
        
        String token = context.consumeNextToken();
        
        boolean mirror = MIRROR_SHORT.equalsIgnoreCase(token);
        
        if (mirror) {
            token = context.consumeNextToken();
            
        }
        
        if (AUTO.equalsIgnoreCase(token)) {
            token = "cmd_" + System.currentTimeMillis() + ".txt";
            
        }
        
        File file = new File(token);
        Path path = file.toPath();
        
        FileLoggingTextOutputReceiver fileReciever = new FileLoggingTextOutputReceiver(path);
        fileReciever.onStart();
        
        TextOutputReceiver originalReceiver = context.getOutputReceiver();
        originalReceiver.addOutput(StringUtils.format("Writing command output to file: %s", path.toAbsolutePath().toString()));
        
        TextOutputReceiver newReceiver;
        
        if (mirror) {
            MultiTextOutputReceiver multiOutput = new MultiTextOutputReceiver();
            multiOutput.addTextOutputReceiver(fileReciever);
            multiOutput.addTextOutputReceiver(originalReceiver);
            newReceiver = multiOutput;
            
        } else {
            newReceiver = fileReciever;
            
        }
        
        CommandContext newContext = new CommandContext(context.consumeRemainingTokens(), newReceiver, context.getInvokerInformation());

        try {
            return parseCommand(newContext);
            
        } catch (CommandException e) {
            throw CommandException.syntaxError("There was an error parsing the command to save", context);
            
        }
        
    }
    
}
