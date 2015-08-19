/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.command.internal.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;

/**
 * Provides built-in console commands, like printing "help" or version information.
 * 
 * @author Robert Mischke
 */
public class BuiltInCommandPlugin implements CommandPlugin {

    private static final String CMD_DEV = "dev";

    private static final String CMD_DUMMY = "dummy";

    private static final String CMD_HELP = "help";

    private static final String CMD_HELP_DEV = "help --dev";

    private static final String CMD_OSGI = "osgi";

    private static final String CMD_STATS = "stats";

    private static final String CMD_TASKS = "tasks";

    private static final String CMD_VERSION = "version";

    private static final String CMD_CRASH = "force-crash";

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_HELP, "", false, "list available commands"));
        contributions.add(new CommandDescription(CMD_VERSION, "", false, "print version information"));
        // developer commands
        contributions.add(new CommandDescription(CMD_DEV, "", true, "alias of \"help --dev\" [deprecated]"));
        contributions.add(new CommandDescription(CMD_DUMMY, "", true, "prints a test message"));
        contributions.add(new CommandDescription(CMD_CRASH, "<delay>", true,
            "\"kills\" the instance without proper shutdown at <delay> milliseconds after the command is executed"));
        contributions.add(new CommandDescription(CMD_HELP_DEV, "", true, "list available commands (including developer commands)"));
        contributions.add(new CommandDescription(CMD_STATS, "", true, "show internal statistics"));
        contributions.add(new CommandDescription(CMD_OSGI, "[-o <filename>] <command>", true,
            "executes an OSGi/Equinox console command; use -o to write text output to a file"));
        contributions.add(new CommandDescription(CMD_TASKS, "[-a]", true, "show information about internal tasks",
            "-a - Extended information: list tasks with a unique id"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        String cmd = context.consumeNextToken();
        if (CMD_HELP.equals(cmd)) {
            boolean devOption = context.consumeNextTokenIfEquals("--dev");
            performHelp(context, devOption);
        } else if (CMD_VERSION.equals(cmd)) {
            performVersion(context);
        } else if (CMD_OSGI.equals(cmd)) {
            performOsgi(context);
        } else if (CMD_CRASH.equals(cmd)) {
            performCrash(context);
        } else if (CMD_DEV.equals(cmd)) {
            // deprecated alias of "help --dev"
            performHelp(context, true);
        } else if (CMD_STATS.equals(cmd)) {
            performStats(context);
        } else if (CMD_TASKS.equals(cmd)) {
            performTasks(context);
        } else if (CMD_DUMMY.equals(cmd)) {
            performDummy(context);
        } else {
            throw new IllegalStateException();
        }
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
    private void performDummy(final CommandContext context) throws CommandException {
        context.println("Dummy command executing");
    }

    private void performOsgi(CommandContext context) throws CommandException {
        final EquinoxConsoleCommandInvoker commandInvoker = new EquinoxConsoleCommandInvoker();
        final boolean logToFile = context.consumeNextTokenIfEquals("-o");
        if (!logToFile) {
            commandInvoker.execute(context);
        } else {
            String outputFilename = context.consumeNextToken();
            if (outputFilename == null) {
                throw CommandException.syntaxError("Missing filename after -o parameter", context);
            }
            final CapturingTextOutReceiver outputReceiver = new CapturingTextOutReceiver("");
            CommandContext wrappedContext =
                new CommandContext(context.consumeRemainingTokens(), outputReceiver, context.getInvokerInformation());
            commandInvoker.execute(wrappedContext);
            File outputFile = new File(outputFilename);
            try {
                FileUtils.write(outputFile, outputReceiver.getBufferedOutput());
                context.println("Logged output to " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                context.println("Internal error: Failed to write to output file " + outputFile.getAbsolutePath());
            }
        }
    }

    private void performVersion(CommandContext context) {
        context.println("RCE platform version: " + VersionUtils.getVersionOfPlatformBundles());
        context.println("RCE core version: " + VersionUtils.getVersionOfCoreBundles());
        context.println("RCE product version: " + VersionUtils.getVersionOfProduct());
    }

    private void performCrash(CommandContext context) {
        int delayMsec = Integer.parseInt(context.consumeNextToken());
        LogFactory.getLog(getClass()).warn(StringUtils.format("Killing the instance (using System.exit(1)) in %,d msec...", delayMsec));
        SharedThreadPool.getInstance().scheduleAfterDelay(new Runnable() {

            @Override
            @TaskDescription("Simulate an instance crash (triggered by console command)")
            public void run() {
                System.exit(1);
            }
        }, delayMsec);
    }

    /**
     * Prints statistics about asynchronous tasks.
     * 
     * @param context
     * 
     * @return String the console output
     */
    private void performTasks(CommandContext context) {
        // check for "-a" (all) flag
        boolean addTaskIds = "-a".equals(context.consumeNextToken());
        context.println(SharedThreadPool.getInstance().getFormattedStatistics(addTaskIds));
    }

    private void performStats(CommandContext context) {
        Map<String, Map<String, Long>> report = StatsCounter.getFullReport();
        for (Map.Entry<String, Map<String, Long>> category : report.entrySet()) {
            context.println(category.getKey());
            for (Map.Entry<String, Long> entry : category.getValue().entrySet()) {
                context.println(StringUtils.format("  %,d - %s", entry.getValue(), entry.getKey()));
            }
        }
    }

}
