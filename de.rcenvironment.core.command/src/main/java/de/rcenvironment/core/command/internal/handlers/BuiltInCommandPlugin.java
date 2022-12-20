/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Version;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.FileParameter;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedFileParameter;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Provides built-in console commands, like printing "help" or version
 * information.
 * 
 * @author Robert Mischke
 */
public class BuiltInCommandPlugin implements CommandPlugin {

    private static final String CMD_DEV = "dev";

    private static final String CMD_DUMMY = "dummy";

    private static final String CMD_HELP = "help";

    private static final String CMD_OSGI = "osgi";

    private static final String CMD_STATS = "stats";

    private static final String CMD_TASKS = "tasks";

    private static final String CMD_VERSION = "version";

    private static final String CMD_CRASH = "force-crash";

    private static final String CMD_SAVETO = "saveto";
    
    private static final String CMD_EXPLAIN = "explain";
    
    private static final String DEV = "--dev";

    private static final CommandFlag TASKS_ALL_FLAG = new CommandFlag("-a", "--all",
            "Show all tasks, including inactive ones");

    private static final CommandFlag TASKS_EXTENDED_FLAG = new CommandFlag("-i", "--unique",
            "Extended information: list tasks with a unique id");
    
    private static final CommandFlag VERSION_DETAILED_FLAG = new CommandFlag("-d", "--detailed",
            "Show detailed information about the version");

    private static final CommandFlag DEV_FLAG = new CommandFlag(DEV, DEV, "show dev commands");
    
    private static final CommandFlag DETAILS_FLAG = new CommandFlag("-d", "--details", "show details of the commands");
    
    private static final CommandFlag ASCIIDOC_FLAG = new CommandFlag("--asciidoc", "--asciidoc", "output in asciidoc format");
    
    private static final IntegerParameter CRASH_TIMEOUT_PARAMETER = new IntegerParameter(null, "delay", "delay in milliseconds");

    private static final StringParameter OSGI_COMMAND_PARAMETER = new StringParameter(null, "command", "osgi command");

    private static final FileParameter OUTPUT_FILE = new FileParameter("output file", "file to which the output will be written");
    
    private static final StringParameter COMMAND_GROUP_PARAMETER = new StringParameter("", "command group",
            "(optional) the command group of which the commands should be shown");
    
    private static final StringParameter COMMAND_PARAMETER = new StringParameter("", "command",
            "command whos output will be saved, does not have to be surounded by quotation marks");
    
    private static final FileParameter OSGI_FILE_PARAMETER = new FileParameter("filename", "path to file");

    private static final NamedSingleParameter NAMED_OSGI_FILE_PARAMETER = new NamedSingleParameter("-o",
            "text output to a file", OSGI_FILE_PARAMETER);

    @Override
    public MainCommandDescription[] getCommands() {
        return new MainCommandDescription[] {
            new MainCommandDescription(CMD_HELP, "list available commands", "list available commands", this::performUserHelp,
                    new CommandModifierInfo(
                            new AbstractCommandParameter[] { COMMAND_GROUP_PARAMETER },
                            new CommandFlag[] { DETAILS_FLAG, DEV_FLAG, ASCIIDOC_FLAG }
                        )
                    ),
            new MainCommandDescription(CMD_VERSION, "print version information", "print version information", this::performVersion,
                    new CommandModifierInfo(new CommandFlag[] { VERSION_DETAILED_FLAG })
                    ),
            new MainCommandDescription(CMD_DEV, "alias of \"help --dev\"", "alias of \"help --dev\"", this::performDevHelp,
                    new CommandModifierInfo(
                            new AbstractCommandParameter [] { COMMAND_GROUP_PARAMETER },
                            new CommandFlag[] { DETAILS_FLAG, ASCIIDOC_FLAG }
                        ),
                    true),
            new MainCommandDescription(CMD_CRASH,
                    "\"kills\" the instance without proper shutdown at <delay> milliseconds after the command is executed",
                    "\"kills\" the instance without proper shutdown at <delay> milliseconds after the command is executed",
                    this::performCrash,
                    new CommandModifierInfo(new AbstractCommandParameter[] { CRASH_TIMEOUT_PARAMETER }), true),
            new MainCommandDescription(CMD_DUMMY, "prints a test message", "prints a test message", this::performDummy, true),
            new MainCommandDescription(CMD_STATS, "show internal statistics", "show internal statistics", this::performStats, true),
            new MainCommandDescription(CMD_OSGI,
                    "executes an OSGi/Equinox console command; use -o to write text output to a file",
                    "executes an OSGi/Equinox console command; use -o to write text output to a file",
                    this::performOsgi,
                    new CommandModifierInfo(new AbstractCommandParameter[] { OSGI_COMMAND_PARAMETER },
                            new NamedParameter[] { NAMED_OSGI_FILE_PARAMETER }),
                    true),
            new MainCommandDescription(CMD_TASKS, "show information about internal tasks",
                    "show information about internal tasks", this::performTasks,
                    new CommandModifierInfo(new CommandFlag[] { TASKS_ALL_FLAG, TASKS_EXTENDED_FLAG }), true),
            new MainCommandDescription(CMD_SAVETO, "save the command output to a file",
                    "save the command output to a file", this::placeholder,
                    new CommandModifierInfo(new AbstractCommandParameter[] { OUTPUT_FILE, COMMAND_PARAMETER })
                ),
            new MainCommandDescription(CMD_EXPLAIN, "show tokens", "show tokens", this::performExplain)
            };
    }

    private void placeholder(CommandContext context) {}
    
    private void performDevHelp(CommandContext context) throws CommandException {
        performHelp(context, true);
    }

    private void performUserHelp(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        performHelp(context, modifiers.hasCommandFlag(DEV));
    }

    private void performHelp(CommandContext context, boolean devOption) throws CommandException {
        context.setDeveloperCommandSetEnabled(devOption);
        // "rce" or "rce help" -> print user help
        throw CommandException.requestHelp(context);
    }

    /**
     * Dummy command for testing.
     * 
     * @throws CommandException
     */
    private void performDummy(final CommandContext context) {
        context.println("Dummy command executing");
    }

    // Not working currently
    private void performOsgi(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        ParsedFileParameter logFileParameter = (ParsedFileParameter) modifiers.getCommandParameter("-o");

        final EquinoxConsoleCommandInvoker commandInvoker = new EquinoxConsoleCommandInvoker();
        final boolean logToFile = logFileParameter.getResult() != null;
        if (!logToFile) {
            commandInvoker.execute(context);
        } else {
            File outputFile = logFileParameter.getResult();
            if (outputFile == null) {
                throw CommandException.syntaxError("Missing filename after -o parameter", context);
            }
            final CapturingTextOutReceiver outputReceiver = new CapturingTextOutReceiver();
            commandInvoker.execute(context, outputReceiver);
            try {
                FileUtils.write(outputFile, outputReceiver.getBufferedOutput());
                context.println("Logged output to " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                context.println("Internal error: Failed to write to output file " + outputFile.getAbsolutePath());
            }
        }
    }

    private void performVersion(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        if (modifiers.getActiveFlags().contains(VERSION_DETAILED_FLAG)) {
            context.println("RCE platform version: " + VersionUtils.getPlatformVersion());
            context.println("RCE core version: " + VersionUtils.getCoreBundleVersion());
            context.println("RCE product version: " + VersionUtils.getProductVersion());
        } else {
            Version version = VersionUtils.getProductVersion();
            String buildId = VersionUtils.getBuildIdAsString(version);
            if (buildId == null) {
                buildId = "-";
            }
            context.println(StringUtils.format("%s (build ID: %s)", VersionUtils.getVersionAsString(version), buildId));
        }
    }

    private void performCrash(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        int delay = ((ParsedIntegerParameter) modifiers.getPositionalCommandParameter(0)).getResult();

        LogFactory.getLog(getClass())
                .warn(StringUtils.format("Killing the instance (using System.exit(1)) in %,d msec...", delay));
        ConcurrencyUtils.getAsyncTaskService().scheduleAfterDelay(new Runnable() {

            @Override
            @TaskDescription("Simulate an instance crash (triggered by console command)")
            public void run() {
                System.exit(1);
            }
        }, delay);
    }

    /**
     * Prints statistics about asynchronous tasks.
     * 
     * @param context
     * 
     * @return String the console output
     * @throws CommandException on syntax error
     */
    private void performTasks(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();

        boolean addTaskIds = modifiers.hasCommandFlag("-a");
        boolean includeInactive = modifiers.hasCommandFlag("-i");

        context.println(ConcurrencyUtils.getThreadPoolManagement().getFormattedStatistics(addTaskIds, includeInactive));
    }

    private void performStats(CommandContext context) {
        for (String line : StatsCounter.getFullReportAsStandardTextRepresentation()) {
            context.println(line);
        }
    }

    private void performExplain(CommandContext context) {
        List<String> tokenList = Collections.emptyList();
        
        if (context.getOriginalTokens().size() >= 2) {
            tokenList = context.getOriginalTokens().subList(1, context.getOriginalTokens().size());
        }
        
        context.println("Parsed command tokens: " + tokenList);
    }
}
