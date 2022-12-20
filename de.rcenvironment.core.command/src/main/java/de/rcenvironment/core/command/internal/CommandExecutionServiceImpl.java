/*
  * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.internal;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.internal.handlers.BuiltInCommandPlugin;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandParser;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.ListCommandParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.MultiStateParameter;
import de.rcenvironment.core.command.spi.NamedMultiParameter;
import de.rcenvironment.core.command.spi.NamedParameter;
import de.rcenvironment.core.command.spi.NamedSingleParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Default implementation of {@link CommandExecutionService}.
 * 
 * @author Robert Mischke
 */
public class CommandExecutionServiceImpl implements CommandExecutionService {

    private static final String INDENT = "\t";

    private static final String NEWLINE_INDENT = "\n\t";

    private static final String PARAMETER_OPEN = "<";

    private static final String PARAMETER_CLOSE = ">";

    private static final String OPTIONAL_OPEN = "[";

    private static final String OPTIONAL_CLOSE = "]";

    private static final String SEPERATOR = " - ";

    private static final String SPACE = " ";

    private static final String VERTICAL_LINE = "|";

    private final AsyncTaskService threadPool;

    private final Set<MainCommandDescription> cmdDesc = new HashSet<>();

    private ConfigurationService configurationService;

    private CommandParser parser = new CommandParser();

    /**
     * An output adapter for rendering command output as AsciiDoc.
     *
     * @author Sebastian Nocke
     * @author Robert Mischke
     * @author Jan Flink
     */
    public static class AsciiDocHelpFormatter {

        private static final String TABLE_CELL_START = "|";

        private static final String HARD_LINE_BREAK = " +\n";

        private static final String DESCRIPTION_TO_PARAMETERS_SEPARATOR = " +\n\n";

        private static final String PARAMETER_DECLARATION_PRE = "_";

        private static final String PARAMETER_DECLARATION_POST = "_: ";

        private CommandExecutionServiceImpl service;

        private TextOutputReceiver outputReceiver;

        public AsciiDocHelpFormatter(CommandExecutionServiceImpl commandExecutionService, TextOutputReceiver outputReceiver) {
            this.service = commandExecutionService;
            this.outputReceiver = outputReceiver;
        }

        public void generateCommandDocumentation(Set<MainCommandDescription> mainCommands, boolean showDevCommands) {
            mainCommands.stream().sorted((desc0, desc1) -> desc0.getCommand().compareTo(desc1.getCommand()))
                .filter(desc -> service.commandGroupShouldBeShown(desc, showDevCommands))
                .forEach(desc -> renderMainCommandSection(desc, showDevCommands));
        }

        private void renderMainCommandSection(MainCommandDescription mainCommand, boolean showDevCommands) {
            outputReceiver.addOutput("*The '" + mainCommand.getCommand() + "' command [[" + mainCommand.getCommand() + "]]*");
            printTableHeader();

            if (mainCommand.isExecutable() && mainCommand.getIsDevelopercommand() == showDevCommands) {
                outputReceiver.addOutput(TABLE_CELL_START + escapeAsciiDocMarkup(mainCommandSyntax(mainCommand))
                    + TABLE_CELL_START + escapeAsciiDocMarkup(mainCommandDescriptionText(mainCommand)));
            }
            Arrays.stream(mainCommand.getSubCommands()).filter(sub -> sub.getIsDevelopercommand() == showDevCommands)
                .sorted((sub0, sub1) -> sub0.getCommand().compareTo(sub1.getCommand()))
                .forEach(sub -> outputReceiver.addOutput(TABLE_CELL_START + escapeAsciiDocMarkup(subCommandSyntax(sub, mainCommand))
                    + TABLE_CELL_START + escapeAsciiDocMarkup(subCommandDescriptionText(sub))));

            printTableFooter();
        }

        private String mainCommandSyntax(MainCommandDescription mainCommand) {
            StringBuilder builder = new StringBuilder();
            builder.append(mainCommand.getCommand())
                .append(service.getSyntaxParameters(mainCommand.getModifiers()));

            return builder.toString();
        }

        private String subCommandSyntax(SubCommandDescription command, MainCommandDescription mainCommand) {
            StringBuilder builder = new StringBuilder();
            builder.append(mainCommand.getCommand())
                .append(SPACE)
                .append(command.getCommand())
                .append(service.getSyntaxParameters(command.getModifiers()));

            return builder.toString();
        }

        private String mainCommandDescriptionText(MainCommandDescription mainCommand) {
            StringJoiner joiner = new StringJoiner(SPACE);
            joiner.add(normalizeCommandDescription(mainCommand.getDescription()));

            if (!mainCommand.getModifiers().isEmpty()) {
                joiner.add(DESCRIPTION_TO_PARAMETERS_SEPARATOR);
            }

            mainCommand.getModifiers().getPositionals().stream().forEachOrdered(renderDescriptionOfPositional(joiner));

            mainCommand.getModifiers().getNamedParameters().stream().forEach(renderDescriptionOfNamed(joiner));

            mainCommand.getModifiers().getFlags().stream().forEach(renderDescriptionOfFlag(joiner));

            return joiner.toString();
        }

        private String subCommandDescriptionText(SubCommandDescription command) {
            StringJoiner joiner = new StringJoiner(SPACE);
            joiner.add(normalizeCommandDescription(command.getDescription()));

            if (!command.getModifiers().isEmpty()) {
                joiner.add(DESCRIPTION_TO_PARAMETERS_SEPARATOR);
            }

            command.getModifiers().getPositionals().stream().forEachOrdered(renderDescriptionOfPositional(joiner));

            command.getModifiers().getNamedParameters().stream().forEach(renderDescriptionOfNamed(joiner));

            command.getModifiers().getFlags().stream().forEach(renderDescriptionOfFlag(joiner));

            return joiner.toString();
        }

        // ensure all descriptions start with a capital letter and end with a full stop
        private String normalizeCommandDescription(String description) {
            if (!description.endsWith(".")) {
                description = description + ".";
            }
            return StringUtils.capitalize(description);
        }

        private Consumer<? super CommandFlag> renderDescriptionOfFlag(StringJoiner joiner) {
            return flag -> joiner
                .add(PARAMETER_DECLARATION_PRE + service.getSyntaxOfFlag(flag) + PARAMETER_DECLARATION_POST)
                .add(flag.getInfotext())
                .add(HARD_LINE_BREAK);
        }

        private Consumer<? super NamedParameter> renderDescriptionOfNamed(StringJoiner joiner) {
            return parameter -> joiner
                .add(PARAMETER_DECLARATION_PRE + service.getSyntaxOfNamedParameter(parameter) + PARAMETER_DECLARATION_POST)
                .add(parameter.getInfotext())
                .add(HARD_LINE_BREAK);
        }

        private Consumer<? super AbstractCommandParameter> renderDescriptionOfPositional(StringJoiner joiner) {
            return parameter -> joiner
                .add(PARAMETER_DECLARATION_PRE + service.getSyntaxOfPositionalParameter(parameter) + PARAMETER_DECLARATION_POST)
                .add(parameter.getDescription())
                .add(HARD_LINE_BREAK);
        }

        private void printTableHeader() {
            outputReceiver.addOutput("|===");
            outputReceiver.addOutput("|Command |Description");
            outputReceiver.addOutput("");
        }

        private void printTableFooter() {
            outputReceiver.addOutput("|===");
        }

        private String escapeAsciiDocMarkup(String input) {
            // prevent certain parts of command descriptions being treated as markup
            return input.replace(VERTICAL_LINE, "\\|").replace("<--", "\\<--");
        }
    }

    public CommandExecutionServiceImpl() {
        threadPool = ConcurrencyUtils.getAsyncTaskService();
    }

    /**
     * OSGi-DS activation method.
     */
    public void activate() {
        registerCommandPlugin(new BuiltInCommandPlugin());
    }

    /**
     * Registers a {@link CommandPlugin}; may be called by OSGi-DS or manually.
     * 
     * @param plugin the plugin to add
     */
    public void registerCommandPlugin(CommandPlugin plugin) {
        parser.registerCommands(plugin.getCommands());

        cmdDesc.addAll(Arrays.asList(plugin.getCommands()));
    }

    /**
     * Unregisters a {@link CommandPlugin}; may be called by OSGi-DS or manually.
     * 
     * @param plugin the plugin to remove
     */
    public void unregisterCommandPlugin(CommandPlugin plugin) {
        // TODO unregister help contributions once plugins are actually removed
        // dynamically;
        // right now, this only exists for symmetry

        cmdDesc.removeAll(Arrays.asList(plugin.getCommands()));
    }

    /**
     * Bind configuration service method.
     *
     * @param service The service to bind.
     */
    public void bindConfigurationService(ConfigurationService service) {
        configurationService = service;
    }

    @Override
    public Future<CommandExecutionResult> asyncExecMultiCommand(List<String> tokens, TextOutputReceiver outputReceiver,
        Object initiator) {

        File profileOutput = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT);
        MultiCommandHandler multiCommandHandler = new MultiCommandHandler(tokens, outputReceiver, parser,
            profileOutput);
        multiCommandHandler.setInitiatorInformation(initiator);
        return threadPool.submit(multiCommandHandler);
    }

    @Override
    public void printHelpText(ParsedCommandModifiers modifiers, TextOutputReceiver outputReceiver) {
        synchronized (cmdDesc) {
            ParsedStringParameter command = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);

            boolean showDevCommands = modifiers.hasCommandFlag("--dev");
            boolean showCommandModifiers = modifiers.hasCommandFlag("--details");
            boolean asciidocFormat = modifiers.hasCommandFlag("--asciidoc");

            if (asciidocFormat) {
                new AsciiDocHelpFormatter(this, outputReceiver).generateCommandDocumentation(cmdDesc, showDevCommands);
                return;
            }

            if (command == null) {
                printGeneralHelp(showDevCommands, outputReceiver);
            } else {
                printSpecificHelp(command, showDevCommands, showCommandModifiers, outputReceiver);
            }
        }
    }

    private void printGeneralHelp(boolean showDevCommands, TextOutputReceiver outputReceiver) {
        if (showDevCommands) {
            outputReceiver.addOutput("RCE Console Dev Commands:");
        } else {
            outputReceiver.addOutput("RCE Console Commands:");
        }

        cmdDesc.stream().filter(desc -> commandGroupShouldBeShown(desc, showDevCommands))
            .sorted((desc0, desc1) -> desc0.getCommand().compareTo(desc1.getCommand()))
            .forEach(desc -> outputReceiver
                .addOutput(INDENT + desc.getCommand() + ": " + desc.getCommandGroupDescription()));

        if (showDevCommands) {
            outputReceiver.addOutput("For detailed information and subcommands type \"dev [command] [--details]\"");
        } else {
            outputReceiver.addOutput("For detailed information and subcommands type \"help [command] [--details]\"");
        }
    }

    private void printSpecificHelp(ParsedStringParameter command, boolean showDevCommands, boolean showCommandModifiers,
        TextOutputReceiver outputReceiver) {
        MainCommandDescription foundCommand = null;

        for (MainCommandDescription desc : cmdDesc) {
            if (desc.getCommand().equalsIgnoreCase(command.getResult())) {
                foundCommand = desc;
                break;
            }
        }

        if (foundCommand == null) {
            outputReceiver.addOutput("Specified command was not found");

        } else {
            printMaincommandHelp(foundCommand, showCommandModifiers, outputReceiver);

            SubCommandDescription[] fittigSubCommands = null;

            if (showDevCommands) {
                fittigSubCommands = Arrays.stream(foundCommand.getSubCommands())
                    .toArray(size -> new SubCommandDescription[size]);

            } else {
                fittigSubCommands = Arrays.stream(foundCommand.getSubCommands())
                    .filter(subcommand -> !subcommand.getIsDevelopercommand())
                    .toArray(size -> new SubCommandDescription[size]);

            }

            if (fittigSubCommands.length != 0) {
                outputReceiver.addOutput(foundCommand.getCommand() + " subcommands:");

                final String commandName = foundCommand.getCommand();

                Arrays.stream(fittigSubCommands)
                    .sorted((desc0, desc1) -> desc0.getCommand().compareTo(desc1.getCommand()))
                    .forEachOrdered(subCommand -> printSubcommandHelp(commandName, subCommand, showCommandModifiers,
                        outputReceiver));
            }
        }
    }

    private void printMaincommandHelp(MainCommandDescription command, boolean showModifiers,
        TextOutputReceiver outputReceiver) {

        if (!command.isExecutable()) {
            return;
        }

        StringBuilder lineBuilder = new StringBuilder(command.getCommand());

        CommandModifierInfo modifiers = command.getModifiers();

        lineBuilder.append(getSyntaxParameters(modifiers));

        lineBuilder.append(NEWLINE_INDENT).append(command.getDescription());

        if (showModifiers) {
            lineBuilder.append(getDetailedDescription(modifiers));
        }

        outputReceiver.addOutput(lineBuilder.toString());
    }

    private void printSubcommandHelp(String mainCommand, SubCommandDescription command, boolean showModifiers,
        TextOutputReceiver outputReceiver) {

        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append(mainCommand).append(" ").append(command.getCommand());

        CommandModifierInfo modifiers = command.getModifiers();

        lineBuilder.append(getSyntaxParameters(modifiers));

        lineBuilder.append(NEWLINE_INDENT).append(command.getDescription());

        if (showModifiers) {
            lineBuilder.append(getDetailedDescription(modifiers));
        }

        outputReceiver.addOutput(lineBuilder.toString());
    }

    private String getSyntaxParameters(CommandModifierInfo modifiers) {
        StringBuilder builder = new StringBuilder();

        modifiers.getPositionals().stream()
            .forEachOrdered(param -> builder.append(SPACE).append(getSyntaxOfPositionalParameter(param)));

        modifiers.getNamedParameters().stream()
            .forEachOrdered(param -> builder.append(SPACE).append(getSyntaxOfNamedParameter(param)));

        modifiers.getFlags().stream().forEachOrdered(flag -> builder.append(SPACE).append(getSyntaxOfFlag(flag)));

        return builder.toString();
    }

    private String getSyntaxOfPositionalParameter(AbstractCommandParameter parameter) {
        StringBuilder builder = new StringBuilder();
        builder.append(PARAMETER_OPEN).append(parameter.getName());

        if (parameter instanceof MultiStateParameter) {
            MultiStateParameter multi = (MultiStateParameter) parameter;
            StringJoiner joiner = new StringJoiner(VERTICAL_LINE);
            Arrays.stream(multi.getStates()).forEachOrdered(joiner::add);

            builder.append(": ").append(joiner.toString());
        } else if (parameter instanceof ListCommandParameter) {
            builder.append("...");
        }

        builder.append(PARAMETER_CLOSE);

        return builder.toString();
    }

    private String getSyntaxOfNamedParameter(NamedParameter parameter) {
        StringBuilder builder = new StringBuilder();
        builder.append(OPTIONAL_OPEN).append(parameter.getName()).append(" ");

        if (parameter instanceof NamedSingleParameter) {
            NamedSingleParameter single = (NamedSingleParameter) parameter;

            builder.append(PARAMETER_OPEN);
            if (single.getParameterType() instanceof MultiStateParameter) {
                MultiStateParameter multi = (MultiStateParameter) single.getParameterType();

                StringJoiner joiner = new StringJoiner(VERTICAL_LINE);
                Arrays.stream(multi.getStates()).forEachOrdered(joiner::add);

                builder.append(joiner.toString());

            } else {
                builder.append(single.getParameterType().getName());
                if (single.getParameterType() instanceof ListCommandParameter) {
                    builder.append("...");
                }
            }
            builder.append(PARAMETER_CLOSE);

        } else if (parameter instanceof NamedMultiParameter) {
            NamedMultiParameter multi = (NamedMultiParameter) parameter;

            builder.append(PARAMETER_OPEN);

            StringJoiner joiner = new StringJoiner(PARAMETER_CLOSE + " " + PARAMETER_OPEN);

            for (int i = 0; i < multi.getParameterTypes().length; i++) {
                AbstractCommandParameter param = multi.getParameterTypes()[i];

                if (i < multi.getMinParameters()) {
                    joiner.add(param.getName());
                } else {
                    joiner.add("(" + param.getName() + ")");
                }

            }
            builder.append(joiner.toString());

            builder.append(PARAMETER_CLOSE);

        } else {
            return "Should never appear";
        }

        builder.append(OPTIONAL_CLOSE);

        return builder.toString();
    }

    private String getSyntaxOfFlag(CommandFlag flag) {
        StringBuilder builder = new StringBuilder();
        builder.append(OPTIONAL_OPEN).append(flag.getLongFlag());
        if (!flag.getLongFlag().equals(flag.getShortFlag())) {
            builder.append(VERTICAL_LINE).append(flag.getShortFlag());
        }
        builder.append(OPTIONAL_CLOSE);

        return builder.toString();
    }

    private String getDetailedDescription(CommandModifierInfo modifiers) {
        StringBuilder builder = new StringBuilder();

        modifiers.getPositionals().stream()
            .forEachOrdered(param -> builder.append(getDetailedPositionalDescription(param)));

        modifiers.getNamedParameters().stream()
            .forEachOrdered(param -> builder.append(getDetailedNamedDescription(param)));

        modifiers.getFlags().stream()
            .forEachOrdered(flag -> builder.append(getDetailedFlagDescription(flag)));

        return builder.toString();
    }

    private String getDetailedPositionalDescription(AbstractCommandParameter parameter) {
        StringBuilder builder = new StringBuilder();
        builder.append(NEWLINE_INDENT).append(PARAMETER_OPEN).append(parameter.getName());

        if (parameter instanceof MultiStateParameter) {
            MultiStateParameter multi = (MultiStateParameter) parameter;
            StringJoiner joiner = new StringJoiner(VERTICAL_LINE);
            Arrays.stream(multi.getStates()).forEachOrdered(joiner::add);

            builder.append(": ").append(joiner.toString());
        } else if (parameter instanceof ListCommandParameter) {
            builder.append("...");
        }

        builder.append(PARAMETER_CLOSE).append(SEPERATOR).append(parameter.getDescription());

        return builder.toString();
    }

    private String getDetailedNamedDescription(NamedParameter parameter) {
        StringBuilder builder = new StringBuilder();

        builder.append(NEWLINE_INDENT).append(OPTIONAL_OPEN).append(parameter.getName()).append(" ");

        if (parameter instanceof NamedSingleParameter) {
            NamedSingleParameter single = (NamedSingleParameter) parameter;

            builder.append(PARAMETER_OPEN);
            if (single.getParameterType() instanceof MultiStateParameter) {
                MultiStateParameter multi = (MultiStateParameter) single.getParameterType();

                StringJoiner joiner = new StringJoiner(VERTICAL_LINE);
                Arrays.stream(multi.getStates()).forEachOrdered(joiner::add);

                builder.append(joiner.toString());

            } else {
                builder.append(single.getParameterType().getName());
                if (single.getParameterType() instanceof ListCommandParameter) {
                    builder.append("...");
                }
            }
            builder.append(PARAMETER_CLOSE);

        } else {
            NamedMultiParameter multi = (NamedMultiParameter) parameter;

            builder.append(PARAMETER_OPEN);

            StringJoiner joiner = new StringJoiner(", ");

            for (int i = 0; i < multi.getParameterTypes().length; i++) {
                AbstractCommandParameter param = multi.getParameterTypes()[i];

                if (i < multi.getMinParameters()) {
                    joiner.add(param.getName());
                } else {
                    joiner.add("(" + param.getName() + ")");
                }

            }
            builder.append(joiner.toString());

            builder.append(PARAMETER_CLOSE);

        }

        builder.append(OPTIONAL_CLOSE).append(SEPERATOR).append(parameter.getInfotext());

        if (parameter instanceof NamedSingleParameter) {
            NamedSingleParameter single = (NamedSingleParameter) parameter;

            if (single.getParameterType() instanceof MultiStateParameter) {
                StringJoiner joiner = new StringJoiner(", ");
                Arrays.stream(((MultiStateParameter) single.getParameterType()).getStates())
                    .forEachOrdered(joiner::add);
                builder.append(" values: ").append(joiner.toString());
            }
        }
        return builder.toString();
    }

    private String getDetailedFlagDescription(CommandFlag flag) {
        StringBuilder builder = new StringBuilder();
        builder.append(NEWLINE_INDENT).append(OPTIONAL_OPEN).append(flag.getLongFlag());

        if (!flag.getShortFlag().equals(flag.getLongFlag())) {
            builder.append(VERTICAL_LINE).append(flag.getShortFlag());
        }

        builder.append(OPTIONAL_CLOSE).append(SEPERATOR).append(flag.getInfotext());

        return builder.toString();
    }

    private boolean commandGroupShouldBeShown(MainCommandDescription mainCommand, boolean showDevCommands) {
        return mainCommand.getIsDevelopercommand() == showDevCommands || Arrays.stream(mainCommand.getSubCommands())
            .anyMatch(sub -> sub.getIsDevelopercommand() == showDevCommands);
    }

    @Override
    public String getHelpText(boolean addCommonPrefix, boolean showDevCommands) {
        CapturingTextOutReceiver capturingReceiver = new CapturingTextOutReceiver();
        ParsedCommandModifiers modifiers = new ParsedCommandModifiers();
        printHelpText(modifiers, capturingReceiver);

        return capturingReceiver.getBufferedOutput();
    }

    @Override
    public CommandParser getParser() {
        return parser;
    }

}
