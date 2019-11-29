/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.BenchmarkService;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.management.RemoteBenchmarkService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Default {@link BenchmarkService} implementation.
 * 
 * @author Robert Mischke
 */
public class BenchmarkServiceImpl implements BenchmarkService, RemoteBenchmarkService {

    private CommunicationService commService;

    private PlatformService platformService;

    private LiveNetworkIdResolutionService idResolutionService;

    /**
     * Internal implementation of {@link BenchmarkSetup}.
     * 
     * @author Robert Mischke
     */
    private class BenchmarkSetupImpl implements BenchmarkSetup {

        private List<BenchmarkSubtaskImpl> subtasks;

        BenchmarkSetupImpl(List<BenchmarkSubtaskImpl> subtasks) {
            this.subtasks = subtasks;
        }

        @Override
        public List<BenchmarkSubtaskImpl> getSubtasks() {
            return subtasks;
        }

    }

    @Override
    public BenchmarkSetup parseBenchmarkDescription(String definition) {
        List<BenchmarkSubtaskImpl> subtasks = new ArrayList<BenchmarkSubtaskImpl>();
        // TODO should use length constants instead; would be nice to also support instance ids and "upcast"
        Pattern cmdPattern = Pattern.compile("([0-9a-f]{32}(?::[0-9a-f]{10})?|\\*|\\*\\*)\\((\\d*),(\\d*),(\\d*),(\\d*),(\\d*)\\)");
        Matcher matcher = cmdPattern.matcher(definition);

        while (matcher.find()) {
            String targetNodeString = matcher.group(1);

            int numMessages = parseInt(matcher.group(2), 1);
            int requestSize = parseInt(matcher.group(3), 1);
            int responseSize = parseInt(matcher.group(4), 1);
            int responseDelay = parseInt(matcher.group(5), 0);
            int numSenders = parseInt(matcher.group(6), 1);

            List<InstanceNodeSessionId> targetNodes = new ArrayList<InstanceNodeSessionId>();
            if (targetNodeString.equals("*")) {
                // * = add all, except "self"
                Set<InstanceNodeSessionId> knownNodes = new HashSet<InstanceNodeSessionId>(commService.getReachableInstanceNodes());
                knownNodes.remove(platformService.getLocalInstanceNodeSessionId());
                targetNodes.addAll(knownNodes);
            } else if (targetNodeString.equals("**")) {
                // ** = add all, including "self"
                targetNodes.addAll(commService.getReachableInstanceNodes());
            } else {
                try {
                    targetNodes.add(idResolutionService.resolveInstanceNodeIdStringToInstanceNodeSessionId(targetNodeString));
                } catch (IdentifierException e) {
                    throw new IllegalArgumentException("Could not resolve '" + targetNodeString
                        + "' to a valid node within the current network");
                }
            }
            BenchmarkSubtaskImpl subtask =
                new BenchmarkSubtaskImpl(targetNodes, numMessages, requestSize, responseSize, responseDelay, numSenders);
            subtasks.add(subtask);
        }
        if (subtasks.isEmpty()) {
            throw new IllegalArgumentException("Malformed task definition: '" + definition + "'");
        }
        return new BenchmarkSetupImpl(subtasks);
    }

    @Override
    public void executeBenchmark(BenchmarkSetup setup, TextOutputReceiver outputReceiver) {
        BenchmarkProcess benchmark = createBenchmarkProcess(setup, outputReceiver);
        benchmark.run();
    }

    @Override
    public void asyncExecBenchmark(BenchmarkSetup setup, TextOutputReceiver outputReceiver) {
        BenchmarkProcess benchmark = createBenchmarkProcess(setup, outputReceiver);
        ConcurrencyUtils.getAsyncTaskService().execute(benchmark);
    }

    @Override
    @AllowRemoteAccess
    public Serializable respond(Serializable input, Integer respSize, Integer respDelay) {
        try {
            Thread.sleep(respDelay);
        } catch (InterruptedException e) {
            LogFactory.getLog(getClass()).warn("Interrupted while waiting to send benchmark response", e);
        }
        return new byte[respSize];
    }

    protected void activate(BundleContext context) {}

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        this.commService = newCommunicationService;
    }

    protected void bindLiveNetworkIdResolutionService(LiveNetworkIdResolutionService newInstance) {
        this.idResolutionService = newInstance;
    }

    protected void bindPlatformService(PlatformService newService) {
        this.platformService = newService;
    }

    private BenchmarkProcess createBenchmarkProcess(BenchmarkSetup setup, TextOutputReceiver outputReceiver) {
        BenchmarkProcess benchmark = new BenchmarkProcess(setup, outputReceiver, commService);
        return benchmark;
    }

    private int parseInt(String input, int defValue) {
        if (input.length() == 0) {
            return defValue;
        }
        return Integer.parseInt(input);
    }

}
