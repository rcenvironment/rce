/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.management.BenchmarkSubtask;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Default {@link BenchmarkSubtask} implementation.
 * 
 * @author Robert Mischke
 */
public class BenchmarkSubtaskImpl implements BenchmarkSubtask {

    /**
     * Default/fallback value when there is no actual result, for example the "average duration on success" when all requests have failed.
     */
    private static final int NO_DURATION_AVAILABLE_VALUE = -1;

    private final List<InstanceNodeSessionId> targetNodes;

    private final int numMessages;

    private final int requestSize;

    private final int responseSize;

    private final int responseDelay;

    private final int threadsPerTarget;

    private Map<InstanceNodeSessionId, NodeResultContainer> nodeResults;

    private long subtaskStartTime;

    private CountDownLatch subtaskRequestCountdown;

    /**
     * Holds the benchmark results for a single target node.
     * 
     * @author Robert Mischke
     */
    private class NodeResultContainer {

        private long totalTime;

        private int numSuccess;

        private int numFinished;

        private long totalDuration;

        private long totalSuccessfulTime;

        private long totalFailureTime;

        long getTotalTime() {
            return totalTime;
        }

        void setTotalTime(long totalTime) {
            this.totalTime = totalTime;
        }

        int getNumSuccess() {
            return numSuccess;
        }

        void setNumSuccess(int numSuccess) {
            this.numSuccess = numSuccess;
        }

        int getNumFinished() {
            return numFinished;
        }

        void setNumFinished(int numFinished) {
            this.numFinished = numFinished;
        }

        long getTotalDuration() {
            return totalDuration;
        }

        void setTotalDuration(long totalDuration) {
            this.totalDuration = totalDuration;
        }

        long getTotalSuccessfulTime() {
            return totalSuccessfulTime;
        }

        void setTotalSuccessfulTime(long totalSuccessfulTime) {
            this.totalSuccessfulTime = totalSuccessfulTime;
        }

        long getTotalFailureTime() {
            return totalFailureTime;
        }

        void setTotalFailureTime(long totalFailureTime) {
            this.totalFailureTime = totalFailureTime;
        }
    }

    public BenchmarkSubtaskImpl(List<InstanceNodeSessionId> targetNodes, int numMessages, int requestSize, int responseSize,
        int responseDelay, int threadsPerTarget) {
        this.targetNodes = Collections.unmodifiableList(targetNodes);
        this.numMessages = numMessages;
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.responseDelay = responseDelay;
        this.threadsPerTarget = threadsPerTarget;

        this.nodeResults = Collections.synchronizedMap(new HashMap<InstanceNodeSessionId, BenchmarkSubtaskImpl.NodeResultContainer>());
        for (InstanceNodeSessionId node : targetNodes) {
            nodeResults.put(node, new NodeResultContainer());
        }
        subtaskRequestCountdown = new CountDownLatch(numMessages * targetNodes.size());
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.communication.management.BenchmarkSubtask#getTargetNodes()
     */
    @Override
    public List<InstanceNodeSessionId> getTargetNodes() {
        return targetNodes;
    }

    @Override
    public int getNumMessages() {
        return numMessages;
    }

    @Override
    public int getRequestSize() {
        return requestSize;
    }

    @Override
    public int getResponseSize() {
        return responseSize;
    }

    @Override
    public int getResponseDelay() {
        return responseDelay;
    }

    @Override
    public int getThreadsPerTarget() {
        return threadsPerTarget;
    }

    /**
     * @return a formatted string representing the subtask
     */
    public String formatDescription() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (InstanceNodeSessionId node : targetNodes) {
            if (!first) {
                builder.append(", ");
            } else {
                first = false;
            }
            builder.append(node);
        }
        String targetDescr = builder.toString();
        return StringUtils.format(
            "target nodes: {%s}, requests per target: %d, request size: %d, response size: %d, "
                + "response delay: %dms, threads per target: %d",
            targetDescr, numMessages, requestSize, responseSize, responseDelay, threadsPerTarget);
    }

    public long getRemainingRequestCount() {
        return subtaskRequestCountdown.getCount();
    }

    /**
     * Blocks until this subtasks has completed.
     * 
     * @throws InterruptedException on interruption
     */
    public void awaitTermination() throws InterruptedException {
        // TODO provide variant with timeout?
        subtaskRequestCountdown.await();
    }

    /**
     * @return an array of formatted strings representing the result of this subtask
     */
    public String[] formatResults() {
        String[] array = new String[targetNodes.size()];
        int i = 0;
        for (InstanceNodeSessionId targetNode : targetNodes) {
            NodeResultContainer resultContainer = nodeResults.get(targetNode);
            // synchronize for thread visibility
            synchronized (resultContainer) {
                long timeToFinish = toMsec(resultContainer.getTotalDuration());
                long avgRawSuccessMsec = NO_DURATION_AVAILABLE_VALUE;
                long avgActualSuccessMsec = NO_DURATION_AVAILABLE_VALUE;
                if (resultContainer.getNumSuccess() > 0) {
                    avgRawSuccessMsec = toMsec(resultContainer.getTotalSuccessfulTime() / resultContainer.getNumSuccess());
                    avgActualSuccessMsec = avgRawSuccessMsec - responseDelay;
                }
                int numFailures = numMessages - resultContainer.getNumSuccess();
                array[i++] =
                    StringUtils.format(
                        "%s: Avg actual time: %d ms, Avg raw time: %d ms, Failures: %d, Total time: %d ms",
                        targetNode, avgActualSuccessMsec, avgRawSuccessMsec, numFailures, timeToFinish);
            }
        }
        return array;

    }

    private long toMsec(long duration) {
        return TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
    }

    void recordStartTime() {
        subtaskStartTime = System.nanoTime();
    }

    /**
     * Records the outcome of a single benchmark request.
     * 
     * @param targetNode the target node thas was contacted
     * @param duration the total duration
     * @param error null on normal completion, or the generated exception
     */
    public void recordSingleResult(InstanceNodeSessionId targetNode, long duration, RemoteOperationException error) {
        NodeResultContainer resultContainer = nodeResults.get(targetNode);
        synchronized (resultContainer) {
            resultContainer.setNumFinished(resultContainer.getNumFinished() + 1);
            resultContainer.setTotalTime(resultContainer.getTotalTime() + duration);
            if (error == null) {
                resultContainer.setNumSuccess(resultContainer.getNumSuccess() + 1);
                resultContainer.setTotalSuccessfulTime(resultContainer.getTotalSuccessfulTime() + duration);
            } else {
                LogFactory.getLog(getClass()).warn("Error on benchmark request: " + error.getMessage());
                resultContainer.setTotalFailureTime(resultContainer.getTotalFailureTime() + duration);
            }
            if (resultContainer.getNumFinished() == numMessages) {
                resultContainer.setTotalDuration(System.nanoTime() - subtaskStartTime);
            }
        }
        subtaskRequestCountdown.countDown();
    }
}
