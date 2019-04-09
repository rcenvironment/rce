/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.BenchmarkSetup;
import de.rcenvironment.core.communication.management.RemoteBenchmarkService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A {@link Runnable} that performs a communication layer benchmark.
 * 
 * @author Robert Mischke
 */
public class BenchmarkProcess implements Runnable {

    // private static final int STATUS_OUTPUT_INTERVAL_MSEC = 5000;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * A {@link Runnable} that acts as a single-threaded sender within a benchmark.
     * 
     * @author Robert Mischke
     */
    private final class SenderTask implements Runnable {

        private InstanceNodeSessionId targetNode;

        private AtomicInteger messageCounter;

        private BenchmarkSubtaskImpl subtask;

        private RemoteBenchmarkService remoteService;

        SenderTask(BenchmarkSubtaskImpl subtask, InstanceNodeSessionId nodeId, AtomicInteger messageCounter) {
            this.targetNode = nodeId;
            this.messageCounter = messageCounter;
            this.subtask = subtask;
            this.remoteService = communicationService.getRemotableService(RemoteBenchmarkService.class, targetNode);
        }

        @Override
        @TaskDescription("Communication Layer: benchmark sender task")
        public void run() {
            // this ensures that all threads perform the predefined number of requests
            while (messageCounter.decrementAndGet() >= 0) {
                long startTime = System.nanoTime();
                RemoteOperationException error = null;
                try {
                    performRequest();
                } catch (RemoteOperationException e) {
                    // optional logging of connection errors is left to the calling code
                    error = e;
                }
                long duration = System.nanoTime() - startTime;
                subtask.recordSingleResult(targetNode, duration, error);
            }
        }

        private void performRequest() throws RemoteOperationException {
            Serializable response = remoteService.respond(new byte[subtask.getRequestSize()], subtask.getResponseSize(),
                subtask.getResponseDelay());
            // basic verification of response: is the payload a byte array of expected size?
            byte[] responseBytes = (byte[]) response;
            if (responseBytes == null || responseBytes.length != subtask.getResponseSize()) {
                throw new IllegalStateException("Unexpected benchmark response payload");
            }
        }
    }

    private List<BenchmarkSubtaskImpl> subtasks;

    private TextOutputReceiver outputReceiver;

    private CommunicationService communicationService;

    @SuppressWarnings("unchecked")
    public BenchmarkProcess(BenchmarkSetup setup, TextOutputReceiver outputReceiver, CommunicationService communicationService) {
        this.subtasks = new ArrayList<BenchmarkSubtaskImpl>();
        // cast to expected BenchmarkSubtask implementation; rework if necessary
        subtasks.addAll((Collection<? extends BenchmarkSubtaskImpl>) setup.getSubtasks());
        this.communicationService = communicationService;
        this.outputReceiver = outputReceiver;
    }

    @Override
    @TaskDescription("Communication Layer: benchmark main task")
    public void run() {
        outputReceiver.onStart();

        // initialize and start
        printOutput("Starting " + subtasks.size() + " benchmark task(s)");
        int index = 1;
        for (BenchmarkSubtaskImpl subtask : subtasks) {
            printOutput("  Task " + (index++) + ": " + subtask.formatDescription());
            subtask.recordStartTime();
            for (InstanceNodeSessionId nodeId : subtask.getTargetNodes()) {
                AtomicInteger messageCounter = new AtomicInteger(subtask.getNumMessages());
                for (int senderIndex = 0; senderIndex < subtask.getThreadsPerTarget(); senderIndex++) {
                    SenderTask sender = new SenderTask(subtask, nodeId, messageCounter);
                    ConcurrencyUtils.getAsyncTaskService().execute(sender);
                }
            }
        }

        // TODO spawn progress watcher thread

        // await completion
        printOutput("Awaiting benchmark results...");
        for (BenchmarkSubtaskImpl subtask : subtasks) {
            try {
                subtask.awaitTermination();
            } catch (InterruptedException e) {
                log.warn("Benchmark subtask interrupted", e);
                outputReceiver.onFatalError(e);
            }
        }

        // print results
        printOutput("Benchmark results:");
        index = 1;
        for (BenchmarkSubtaskImpl subtask : subtasks) {
            printOutput("  Task " + (index++) + ": " + subtask.formatDescription());
            for (String line : subtask.formatResults()) {
                printOutput("    " + line);
            }
        }

        outputReceiver.onFinished();
    }

    /**
     * Sends a line of output to the configured receiver.
     * 
     * @param line the output line
     */
    private void printOutput(String line) {
        outputReceiver.addOutput(line);
    }
}
