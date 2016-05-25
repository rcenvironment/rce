/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandDescription;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.BenchmarkService;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.GraphvizUtils;

/**
 * A {@link CommandPlugin} providing "net [...]" commands for querying the network and topology state, as well as performing benchmark
 * operations.
 * 
 * @author Robert Mischke
 * @author Jan Flink ("net components")
 */
public class NetCommandPlugin implements CommandPlugin {

    private static final String CMD_NET = "net";

    private CommunicationService communicationService;

    private BenchmarkService benchmarkService;

    private NodePropertiesService nodePropertiesService;

    private MessageChannelService messageChannelService;

    private final Log log = LogFactory.getLog(getClass());

    private File outputDir;

    @Override
    public Collection<CommandDescription> getCommandDescriptions() {
        final Collection<CommandDescription> contributions = new ArrayList<CommandDescription>();
        contributions.add(new CommandDescription(CMD_NET, "", false, "short version of \"net info\""));
        contributions.add(new CommandDescription("net info", "", false, "show a list of reachable RCE nodes"));
        contributions.add(new CommandDescription("net graph", "[<base name>]", true,
            "generates a Graphviz file of the current network topology"));
        contributions.add(new CommandDescription("net filter", "", false, "show IP filter status"));
        contributions.add(new CommandDescription("net filter reload", "", false, "reloads the IP filter configuration"));
        // developer commands
        contributions.add(new CommandDescription("net graph -a", "[<base name>]", true,
            "like \"net graph\", but include unreachable nodes"));
        contributions.add(new CommandDescription("net bench", "<taskdef>[;<taskDef>]*", true, "run communication benchmark",
            "<taskDef> = <targetNode|*>([<numMessages>],[<requestSize>],[<responseSize>],",
            "                           [<responseDelay(msec)>],[<threadsPerTarget>])"));
        contributions.add(new CommandDescription("net np", "", true, "show known RCE node properties"));
        return contributions;
    }

    @Override
    public void execute(CommandContext context) throws CommandException {
        context.consumeExpectedToken(CMD_NET);
        String subCmd = context.consumeNextToken();
        if (subCmd == null) {
            // "net" -> "net info"
            performNetInfo(context);
        } else {
            if ("add".equals(subCmd)) {
                // "net add <...>"
                context.println("Obsolete command; use \"cn add\" instead");
                return;
            } else if ("bench".equals(subCmd)) {
                // "net bench <...>"
                performNetBench(context);
            } else if ("filter".equals(subCmd)) {
                performNetFilter(context);
            } else if ("graph".equals(subCmd)) {
                performNetGraph(context);
            } else if ("info".equals(subCmd)) {
                // TODO review: add "extended" info output? (e.g. "-a" flag)
                performNetInfo(context);
            } else if ("np".equals(subCmd)) {
                performNetNp(context);
            } else {
                throw CommandException.unknownCommand(context);
            }
        }
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindCommunicationService(CommunicationService newInstance) {
        this.communicationService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindBenchmarkService(BenchmarkService newInstance) {
        this.benchmarkService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindNodePropertiesService(NodePropertiesService newInstance) {
        this.nodePropertiesService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindConfigurationService(ConfigurationService newInstance) {
        this.outputDir = newInstance.getConfigurablePath(ConfigurablePathId.PROFILE_OUTPUT);
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindMessageChannelService(MessageChannelService newInstance) {
        messageChannelService = newInstance;
    }

    private void performNetGraph(CommandContext context) throws CommandException {
        // sanity check
        if (outputDir == null || !outputDir.isDirectory()) {
            throw new IllegalStateException("Invalid output dir: " + outputDir);
        }
        String formatName = "graphviz";
        if (context.consumeNextTokenIfEquals("-a")) {
            formatName = "graphviz-all";
            context.setDeveloperCommandSetEnabled(true);
        }
        List<String> parameters = context.consumeRemainingTokens();
        if (parameters.size() > 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        String baseName = "rce_network"; // default
        if (parameters.size() == 1) {
            baseName = parameters.get(0);
        }

        String graphvizData = communicationService.getFormattedNetworkInformation(formatName);

        File gvFile = new File(outputDir, baseName + ".gv");
        File pngFile = new File(outputDir, baseName + ".png");
        try {
            FileUtils.writeStringToFile(gvFile, graphvizData);
            context.println("Graphviz file written to " + gvFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing script file " + gvFile.getAbsolutePath(), e);
            return;
        }
        if (GraphvizUtils.renderDotFileToPng(gvFile, pngFile, context.getOutputReceiver())) {
            context.println("PNG file written to " + pngFile.getAbsolutePath());
        } else {
            context.println("Error running graphviz - PNG file " + pngFile.getAbsolutePath() + " was probably not generated");
        }
    }

    private void performNetInfo(CommandContext context) {
        context.println(communicationService.getFormattedNetworkInformation("info"));
    }

    private void performNetBench(CommandContext context) throws CommandException {
        context.setDeveloperCommandSetEnabled(true);
        List<String> parameters = context.consumeRemainingTokens();
        if (parameters.size() != 1) {
            throw CommandException.wrongNumberOfParameters(context);
        }

        BenchmarkSetup setup;
        try {
            String benchmarkDescription = parameters.get(0);
            setup = benchmarkService.parseBenchmarkDescription(benchmarkDescription);
        } catch (IllegalArgumentException e) {
            throw CommandException.syntaxError("Error parsing benchmark setup: " + e.toString(), context);
        }

        context.println("Benchmark starting");
        benchmarkService.executeBenchmark(setup, context.getOutputReceiver());
        context.println("Benchmark complete");
    }

    private void performNetFilter(CommandContext context) throws CommandException {
        String nextToken = context.consumeNextToken();
        if (nextToken == null) {
            // show status
            messageChannelService.printIPFilterInformation(context.getOutputReceiver());
        } else if ("reload".equals(nextToken)) {
            messageChannelService.loadAndApplyIPFilterConfiguration();
            messageChannelService.printIPFilterInformation(context.getOutputReceiver());
        } else {
            throw CommandException.unknownCommand(context);
        }
    }

    private void performNetNp(CommandContext context) {
        Set<NodeIdentifier> nodes = communicationService.getReachableNodes();
        Map<NodeIdentifier, Map<String, String>> allMetadata = nodePropertiesService.getAllNodeProperties(nodes);
        context.println("Known node properties:");
        for (Map.Entry<NodeIdentifier, Map<String, String>> entry1 : allMetadata.entrySet()) {
            NodeIdentifier nodeId = entry1.getKey();
            context.println(nodeId.toString());
            Map<String, String> map = entry1.getValue();
            for (Map.Entry<String, String> entry2 : map.entrySet()) {
                context.println(StringUtils.format("  %s = %s", entry2.getKey(), entry2.getValue()));
            }
        }

    }
    
}
