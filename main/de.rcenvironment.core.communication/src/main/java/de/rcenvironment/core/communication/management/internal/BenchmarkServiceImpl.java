/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.management.BenchmarkService;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.management.RemoteBenchmarkService;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
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

    private BundleContext bundleContext;

    /**
     * Internal implementation of {@link BenchmarkSetup}.
     * 
     * @author Robert Mischke
     */
    private class BenchmarkSetupImpl implements BenchmarkSetup {

        private List<BenchmarkSubtaskImpl> subtasks;

        public BenchmarkSetupImpl(List<BenchmarkSubtaskImpl> subtasks) {
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
        Pattern cmdPattern = Pattern.compile("([0-9a-f]{32}|\\*|\\*\\*)\\((\\d*),(\\d*),(\\d*),(\\d*),(\\d*)\\)");
        Matcher matcher = cmdPattern.matcher(definition);

        while (matcher.find()) {
            String targetNodeString = matcher.group(1);

            int numMessages = parseInt(matcher.group(2), 1);
            int requestSize = parseInt(matcher.group(3), 1);
            int responseSize = parseInt(matcher.group(4), 1);
            int responseDelay = parseInt(matcher.group(5), 0);
            int numSenders = parseInt(matcher.group(6), 1);

            List<NodeIdentifier> targetNodes = new ArrayList<NodeIdentifier>();
            if (targetNodeString.equals("*")) {
                // * = add all, except "self"
                Set<NodeIdentifier> knownNodes = new HashSet<NodeIdentifier>(commService.getReachableNodes());
                knownNodes.remove(platformService.getLocalNodeId());
                targetNodes.addAll(knownNodes);
            } else if (targetNodeString.equals("**")) {
                // ** = add all, including "self"
                targetNodes.addAll(commService.getReachableNodes());
            } else {
                targetNodes.add(NodeIdentifierFactory.fromNodeId(targetNodeString));
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
        SharedThreadPool.getInstance().execute(benchmark);
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

    protected void activate(BundleContext context) {
        this.bundleContext = context;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        this.commService = newCommunicationService;
    }

    protected void bindPlatformService(PlatformService newService) {
        this.platformService = newService;
    }

    private BenchmarkProcess createBenchmarkProcess(BenchmarkSetup setup, TextOutputReceiver outputReceiver) {
        BenchmarkProcess benchmark = new BenchmarkProcess(setup, outputReceiver, commService, bundleContext);
        return benchmark;
    }

    private int parseInt(String input, int defValue) {
        if (input.length() == 0) {
            return defValue;
        }
        return Integer.parseInt(input);
    }

}
