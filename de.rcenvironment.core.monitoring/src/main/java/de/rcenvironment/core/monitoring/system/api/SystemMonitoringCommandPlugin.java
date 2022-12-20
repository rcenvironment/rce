/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.command.common.CommandException;
import de.rcenvironment.core.command.spi.AbstractCommandParameter;
import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandFlag;
import de.rcenvironment.core.command.spi.CommandModifierInfo;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.IntegerParameter;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.MultiStateParameter;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.monitoring.system.api.model.AverageOfDoubles;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link CommandPlugin} providing console commands to fetch system monitoring and load data from the local and remote nodes.
 *
 * @author Robert Mischke
 */
public class SystemMonitoringCommandPlugin implements CommandPlugin {

    private static final String ROOT_COMMAND = "sysmon";

    private static final String SUBCOMMAND_FETCH_LOCAL = "--local";

    private static final String SUBCOMMAND_FETCH_LOCAL_SHORT = "-l";

    private static final String SUBCOMMAND_FETCH_REMOTE = "--remote";

    private static final String SUBCOMMAND_FETCH_REMOTE_SHORT = "-r";

    private static final String SUBCOMMAND_API = "api";

    private static final String SUBCOMMAND_REMOTE = "remote";
    
    private static final String DEFAULT = "default";
    
    private static final String AVG_CPU_RAM = "avgcpu+ram";

    private static final int DEFAULT_FETCH_TIME_SPAN_VALUE_SEC = 10;

    private static final int DEFAULT_FETCH_TIME_LIMIT_VALUE_MSEC = 1000;

    private static final int SEC_TO_MSEC = 1000;

    private static final CommandFlag LOCAL_FLAG = new CommandFlag(
            SUBCOMMAND_FETCH_LOCAL_SHORT, SUBCOMMAND_FETCH_LOCAL, "prints system monitoring data for the local instance");
    
    private static final CommandFlag REMOTE_FLAG = new CommandFlag(
            SUBCOMMAND_FETCH_REMOTE_SHORT, SUBCOMMAND_FETCH_REMOTE,
            "fetches system monitoring data from all reachable nodes in the network, and prints it in a human-readable format");
    
    private static final MultiStateParameter API_PARAMETER = new MultiStateParameter("operation" ,
            "operation to perform; avgcpu+ram: fetches the average CPU load over the given time span and the current free RAM",
            DEFAULT, AVG_CPU_RAM);
    
    private static final IntegerParameter TIME_SPAN_PARAMETER = new IntegerParameter(null, "time span",
            "the maximum time span (in seconds) to aggregate load data over");
    
    private static final IntegerParameter TIME_LIMIT_PARAMETER = new IntegerParameter(null, "time limit",
            "the maximum time (in milliseconds) to wait for each node's load data response");
    
    private LocalSystemMonitoringAggregationService localSystemMonitoringAggregationService;

    private CommunicationService communicationService;

    private InstanceNodeSessionId localInstanceNodeSessionId;

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription commands = new MainCommandDescription(ROOT_COMMAND, "fetch system monitoring data",
            "basic system-monitoring information", this::performSysMain,
            new CommandModifierInfo(
                new CommandFlag[] {
                    LOCAL_FLAG,
                    REMOTE_FLAG
                }
            ),
            new SubCommandDescription(SUBCOMMAND_REMOTE,
                "fetches system monitoring data from all reachable nodes in the network, and prints it in a human-readable format",
                this::performSysmonRemote
            ),
            new SubCommandDescription(SUBCOMMAND_API,
                "fetches system monitoring data from all reachable nodes in the network,"
                + "and prints it in a parser-friendly format.", this::performSysApi,
                new CommandModifierInfo(
                    new AbstractCommandParameter[] {
                        API_PARAMETER,
                        TIME_SPAN_PARAMETER,
                        TIME_LIMIT_PARAMETER
                    }
                )
            )
        );
        
        return new MainCommandDescription[] { commands };
    }
    
    private enum FetchLocation {
        Local,
        Remote
    }

    private void performSysMain(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        boolean local = modifiers.hasCommandFlag(SUBCOMMAND_FETCH_LOCAL);
        boolean remote = modifiers.hasCommandFlag(SUBCOMMAND_FETCH_REMOTE);
        
        if (local && remote) {
            throw CommandException.syntaxError("Only one of the two flags \"--local\"/\"--remote\" can be entered", context);
        }
        
        if (!local && !remote) {
            throw CommandException.syntaxError("One of the two flags \"--local\"/\"--remote\" can be entered", context);
        }
        
        if (local) {
            performSysmon(context, FetchLocation.Local);
        } else {
            performSysmon(context, FetchLocation.Remote);
        }
    }
    
    private void performSysmonRemote(CommandContext context) throws CommandException {
        performSysmon(context, FetchLocation.Remote);
    }
    
    private void performSysmon(CommandContext context, FetchLocation fetchLocation) throws CommandException {
        
        switch (fetchLocation) {
        case Local:
            performPrintLocalSysMonData(context, DEFAULT_FETCH_TIME_SPAN_VALUE_SEC * SEC_TO_MSEC,
                    DEFAULT_FETCH_TIME_LIMIT_VALUE_MSEC, true);
            break;
        case Remote:
            performCollectAndPrintSysMonData(context, DEFAULT_FETCH_TIME_SPAN_VALUE_SEC * SEC_TO_MSEC,
                    DEFAULT_FETCH_TIME_LIMIT_VALUE_MSEC, true);
            break;
        default:
            CommandException.executionError("fetch location error", context);
        }
    }
    
    private void performSysApi(CommandContext context) throws CommandException {
        ParsedCommandModifiers modifiers = context.getParsedModifiers();
        
        final int timeSpanSec = ((ParsedIntegerParameter) modifiers.getPositionalCommandParameter(1)).getResult();
        final int timeLimitMsec = ((ParsedIntegerParameter) modifiers.getPositionalCommandParameter(2)).getResult();
        performCollectAndPrintSysMonData(context, timeSpanSec * SEC_TO_MSEC, timeLimitMsec, false);
    }
    
    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindLocalSystemMonitoringAggregationService(LocalSystemMonitoringAggregationService newInstance) {
        this.localSystemMonitoringAggregationService = newInstance;
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
    public void bindPlatformService(PlatformService newInstance) {
        // only needed for fetching the local node id, so the service instance itself is not stored
        this.localInstanceNodeSessionId = newInstance.getLocalInstanceNodeSessionId();
    }

    private void performPrintLocalSysMonData(final CommandContext context, int timeSpanMsec, int timeLimitMsec, boolean humanReadable)
        throws CommandException {
        // construct a set with only the local node id to only have a single code path for local and remote use cases
        final Set<InstanceNodeSessionId> singleNodeIdSet = new HashSet<>();
        singleNodeIdSet.add(localInstanceNodeSessionId);
        // delegate
        performCollectAndPrintSysMonData(context, singleNodeIdSet, timeSpanMsec, timeLimitMsec, humanReadable);
    }

    private void performCollectAndPrintSysMonData(final CommandContext context, int timeSpanMsec, int timeLimitMsec, boolean humanReadable)
        throws CommandException {
        // TODO possible expansion: allow nodes to set a node property indicating whether they publish their load data or not?
        final Set<InstanceNodeSessionId> reachableInstanceNodes = communicationService.getReachableInstanceNodes();
        // delegate
        performCollectAndPrintSysMonData(context, reachableInstanceNodes, timeSpanMsec, timeLimitMsec, humanReadable);
    }

    private void performCollectAndPrintSysMonData(final CommandContext context, Set<InstanceNodeSessionId> nodeIds, int timeSpanMsec,
        int timeLimitMsec, boolean humanReadable) throws CommandException {

        // fetch data
        final Map<InstanceNodeSessionId, SystemLoadInformation> resultMap;
        try {
            resultMap = localSystemMonitoringAggregationService
                .collectSystemMonitoringDataWithTimeLimit(nodeIds, timeSpanMsec, timeLimitMsec);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw CommandException.executionError(e.toString(), context);
        }

        // select output format string
        final String formatString;
        if (humanReadable) {
            formatString = "%s - Average CPU load:%6.2f (%2d samples over %5d msec), Available RAM:%6d kiB";
        } else {
            formatString = "id=%s CpuAvg=%.2f n=%d t=%d FreeRam=%d";
        }

        // generate output
        final StringBuilder outputBuffer = new StringBuilder();
        for (Entry<InstanceNodeSessionId, SystemLoadInformation> e : resultMap.entrySet()) {
            final SystemLoadInformation data = e.getValue();
            final AverageOfDoubles cpuLoadAvg = data.getCpuLoadAvg();

            // conversions (extracted for legibility)
            final String nodeIdString = e.getKey().getInstanceNodeSessionIdString();
            final int cpuLoadAvgTimeSpan =
                cpuLoadAvg.getNumSamples() * LocalSystemMonitoringAggregationService.SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC;
            final double cpuAvgAsPercentage = cpuLoadAvg.getAverage() * 100;

            // append line to buffer, with no newline after the last one
            if (outputBuffer.length() != 0) {
                outputBuffer.append("\n");
            }
            outputBuffer
                .append(StringUtils.format(formatString, nodeIdString, cpuAvgAsPercentage, cpuLoadAvg.getNumSamples(), cpuLoadAvgTimeSpan,
                    data.getAvailableRam()));

            if (humanReadable) {
                outputBuffer.append(" (");
                outputBuffer.append(e.getKey().getAssociatedDisplayName());
                outputBuffer.append(")");
            }
        }

        // print
        context.println(outputBuffer.toString());
    }

    // TODO (p2) refactor into common utility method; duplicated in Remote Access plugin
    private int parseRequiredPositiveIntParameter(final CommandContext context, String name) throws CommandException {
        final String parameter = context.consumeNextToken();
        if (parameter == null) {
            throw CommandException.wrongNumberOfParameters(context);
        }
        final int timespan;
        try {
            timespan = Integer.parseInt(parameter);
            if (timespan <= 0) {
                throw CommandException.syntaxError("The " + name
                    + " parameter must be positive: " + parameter, context);
            }
        } catch (NumberFormatException e) {
            throw CommandException.syntaxError("The " + name
                + " parameter must be an integer number: " + parameter, context);
        }
        return timespan;
    }
}
