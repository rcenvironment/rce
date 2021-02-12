/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.channel.MessageChannelTrafficListener;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link MessageChannelTrafficListener} intended for keeping track of all message traffic in virtual
 * network tests. Can be used to wait until no message has been sent for a certain time using 
 * {@link #waitForNetworkSilence(int, int).
 * 
 * @author Robert Mischke
 */
public class TestNetworkTrafficListener implements MessageChannelTrafficListener {

    private long lastTrafficTimestamp = 0;

    private long requestCount;

    private long lsaMessages = 0;

    private long routedMessages = 0;

    private long largestObservedHopCount = 0;

    private long unsuccessfulResponses = 0;

    private Semaphore trafficOccured = new Semaphore(0);

    @Override
    public void onRequestSentIntoChannel(NetworkRequest request) {
        onTraffic(true);
    }

    @Override
    public void onRequestReceivedFromChannel(NetworkRequest request, InstanceNodeSessionId sourceId) {
        onTraffic(false);
    }

    @Override
    public void onResponseSentIntoChannel(NetworkResponse response, NetworkRequest request, InstanceNodeSessionId sourceId) {
        // note: strictly speaking, the traffic has not happened yet, but is about to

        // TODO restore statistics?
        // if (MessageMetaData.createLsaMessage().matches(request.accessRawMetaData())) {
        // lsaMessages++;
        // }
        //
        // if (MessageMetaData.createRoutedMessage().matches(request.accessRawMetaData())) {
        // routedMessages++;
        // }
        //
        // if (MessageMetaData.wrap(request.accessRawMetaData()).getHopCount() >
        // largestObservedHopCount) {
        // largestObservedHopCount =
        // MessageMetaData.wrap(request.accessRawMetaData()).getHopCount();
        // }
        //
        // if (!response.isSuccess()) {
        // unsuccessfulResponses++;
        // }
        onTraffic(false);
    }

    private synchronized void onTraffic(boolean isRequest) {
        lastTrafficTimestamp = System.currentTimeMillis();
        if (isRequest) {
            requestCount++;
            trafficOccured.release();
        }
    }

    public synchronized long getRequestCount() {
        return requestCount;
    }

    public synchronized long getLastTrafficTimestamp() {
        return lastTrafficTimestamp;
    }

    /**
     * Clears the flag that indicates that traffic has occured.
     */
    public synchronized void clearCustomTrafficFlag() {
        trafficOccured.drainPermits();
    }

    /**
     * Waits until traffic has occured, or the timeout has expired.
     * 
     * @param maxWait the timeout
     * @throws TimeoutException on timeout
     * @throws InterruptedException on interruption
     */
    public void waitForCustomTrafficFlag(int maxWait) throws TimeoutException, InterruptedException {
        if (!trafficOccured.tryAcquire(maxWait, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Maximum wait time for custom traffic flag (" + maxWait + " msec) exceeded");
        }
    }

    /**
     * Waits until no network traffic has been reported for (at least) the given timespan.
     * 
     * @param minSilenceTime the minimum no-traffic time
     * @param maxWait the maximum time to wait for the no-traffic condition
     * @throws TimeoutException on timeout
     * @throws InterruptedException on interruption
     */
    public void waitForNetworkSilence(int minSilenceTime, int maxWait) throws TimeoutException, InterruptedException {
        int pollInterval = minSilenceTime / 2;
        int totalWait = 0;
        while (true) {
            if (totalWait > maxWait) {
                throw new TimeoutException("Maximum wait time for network silence (" + maxWait + " msec) exceeded");
            }
            // note: synchronized through getter
            if (System.currentTimeMillis() - getLastTrafficTimestamp() >= minSilenceTime) {
                return;
            }
            Thread.sleep(pollInterval);
            totalWait += pollInterval;
        }
    }

    /**
     * @return the number of received LSA messages (?)
     * 
     *         TODO verify description
     */
    public long getLsaMessages() {
        return lsaMessages;
    }

    /**
     * @return Returns the largest observed hop count.
     */
    public long getLargestObservedHopCount() {
        return largestObservedHopCount;
    }

    /**
     * @return the number of unsuccessful requests
     * 
     *         TODO verify semantics and current behavior
     */
    public long getUnsuccessfulResponses() {
        return unsuccessfulResponses;
    }

    /**
     * @return the number of routed messages
     * 
     *         TODO verify semantics and current behavior
     */
    public long getRoutedMessages() {
        return routedMessages;
    }

    /**
     * Formats traffic statistics to a string.
     * 
     * @param topologySize The number of instances.
     * @return A string representation for debugging/displaying.
     */
    public String getFormattedTrafficReport(int topologySize) {

        if (topologySize <= 0) {
            throw new IllegalArgumentException("Argument must be >=1");
        }

        return StringUtils.format("Total requests sent:                %d\n"
            + "Average requests sent per node:     %d\n"
            + "Total LSA messages sent:            %d\n"
            + "Average LSA messages sent per node: %d\n"
            + "Total routed messages sent:         %d\n"
            + "Largest observed hop count:         %d (%d)\n"
            + "Unsuccessful responses received:    %d\n",
            getRequestCount(),
            getRequestCount() / topologySize,
            getLsaMessages(),
            getLsaMessages() / topologySize,
            getRoutedMessages(),
            getLargestObservedHopCount(),
            topologySize,
            getUnsuccessfulResponses());
    }
}
