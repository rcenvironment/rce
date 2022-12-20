/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.command.spi.StringParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.BenchmarkService;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
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

    private static final String INFO = "info";
    
    private static final CommandFlag ALL_FLAG = new CommandFlag("-a", "--all", "include unreachable nodes");
    
    private static final StringParameter BASE_NAME_PARAMETER = new StringParameter(null, "base name", "base name parameter");
    
    private static final StringParameter BENCHMARK_DESCRIPTION = new StringParameter(null, "taskdef", "<targetNode|*>([<numMessages>],"
            + "[<requestSize>],[<responseSize>],[<responseDelay(msec)>],[<threadsPerTarget>])");
    
    private CommunicationService communicationService;

    private BenchmarkService benchmarkService;

    private NodePropertiesService nodePropertiesService;

    private MessageChannelService messageChannelService;

    private final Log log = LogFactory.getLog(getClass());

    private File outputDir;

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(CMD_NET,
            "query the network and topology state",
            "alias for 'net info'", this::performNetInfo,
            new SubCommandDescription(INFO, "show a list of reachable RCE nodes", this::performNetInfo),
            new SubCommandDescription("graph", "generates a Graphviz file of the current network topology", this::performNetGraph,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        BASE_NAME_PARAMETER
                    },
                    new CommandFlag[] {
                        ALL_FLAG
                    }
                ), true
            ),
            new SubCommandDescription("filter", "show IP filter status", this::performNetFilter),
            new SubCommandDescription("reload-filter", "reloads the IP filter configuration", this::performRelaodFilter),
            new SubCommandDescription("bench", "run communication benchmark", this::performNetBench,
                    new CommandModifierInfo(new AbstractCommandParameter[] { BENCHMARK_DESCRIPTION }), true),
            new SubCommandDescription("np", "show known RCE node properties", this::performNetNp, true)
        );
        
        return new MainCommandDescription[] { commands };
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
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // skip implicit bind actions if is was spawned as part of a default test environment;
            // if this causes errors in mocked service tests, invoke RuntimeDetection.allowSimulatedServiceActivation()
            return;
        }
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

    private void performNetGraph(CommandContext context) {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter baseNameParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        boolean hasAllFlag = modifiers.hasCommandFlag("-a");
        
        // sanity check
        if (outputDir == null || !outputDir.isDirectory()) {
            throw new IllegalStateException("Invalid output dir: " + outputDir);
        }
        
        String formatName = "graphviz";
        if (hasAllFlag) {
            formatName = "graphviz-all";
            context.setDeveloperCommandSetEnabled(true);
        }
        
        String baseName = "rce_network"; // default
        if (!baseNameParameter.getResult().equals("")) {
            baseName = baseNameParameter.getResult();
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
        context.println(communicationService.getFormattedNetworkInformation(INFO));
    }

    private void performNetBench(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        ParsedStringParameter benchmarkDescriptionParameter = (ParsedStringParameter) modifiers.getPositionalCommandParameter(0);
        
        context.setDeveloperCommandSetEnabled(true);

        BenchmarkSetup setup;
        try {
            String benchmarkDescription = benchmarkDescriptionParameter.getResult();
            setup = benchmarkService.parseBenchmarkDescription(benchmarkDescription);
        } catch (IllegalArgumentException e) {
            throw CommandException.syntaxError("Error parsing benchmark setup: " + e.toString(), context);
        }

        context.println("Benchmark starting");
        benchmarkService.executeBenchmark(setup, context.getOutputReceiver());
        context.println("Benchmark complete");
    }

    private void performNetFilter(CommandContext context) {
        // show status
        messageChannelService.printIPFilterInformation(context.getOutputReceiver());
    }
    
    private void performRelaodFilter(CommandContext context) {
        messageChannelService.loadAndApplyIPFilterConfiguration();
        messageChannelService.printIPFilterInformation(context.getOutputReceiver());
    }

    private void performNetNp(CommandContext context) {
        Set<InstanceNodeSessionId> nodes = communicationService.getReachableInstanceNodes();
        Map<InstanceNodeSessionId, Map<String, String>> allMetadata = nodePropertiesService.getAllNodeProperties(nodes);
        context.println("Known node properties:");
        for (Map.Entry<InstanceNodeSessionId, Map<String, String>> entry1 : allMetadata.entrySet()) {
            InstanceNodeSessionId nodeId = entry1.getKey();
            context.println(nodeId.toString());
            Map<String, String> map = entry1.getValue();
            for (Map.Entry<String, String> entry2 : map.entrySet()) {
                context.println(StringUtils.format("  %s = %s", entry2.getKey(), entry2.getValue()));
            }
        }

    }

}
