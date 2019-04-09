/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

/**
 * Statistics on messages in the network.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke (added synchronization)
 */
public final class NetworkStats {

    private int failedCommunications = 0;

    private int successfulCommunications = 0;

    private int sentLSAs = 0;

    private int receivedLSAs = 0;

    private int rejectedLSAs = 0;

    private int hopCountOfSentLSAs = 0;

    private int hopCountOfReceivedLSAs = 0;

    private int hopCountOfRejectedLSAs = 0;

    private int maxReceivedHopCount = 0;

    private int maxTimeToLive = 0;

    private int shortesPathComputations = 0;

    /**
     * @return The average hop count of sent LSAs. 
     */
    public synchronized int averageHopCountOfSentLSAs() {
        if (sentLSAs > 0) {
            return hopCountOfSentLSAs / sentLSAs;
        } else {
            return 0;
        }
    }

    /**
     * @return The average hop count of received LSAs.
     */
    public synchronized int averageHopCountOfReceivedLSAs() {
        if (receivedLSAs > 0) {
            return hopCountOfReceivedLSAs / receivedLSAs;
        } else {
            return 0;
        }
    }

    /**
     * @return The average hop count of rejected LSAs.
     */
    public synchronized int averageHopCountOfRejectedLSAs() {
        if (rejectedLSAs > 0) {
            return hopCountOfRejectedLSAs / rejectedLSAs;
        } else {
            return 0;
        }
    }

    /**
     * @return The number of failed communications.
     */
    public synchronized int getFailedCommunications() {
        return failedCommunications;
    }

    /**
     * @return The incremented value.
     */
    public synchronized int incFailedCommunications() {
        return ++this.failedCommunications;
    }

    /**
     * @return The number of successful communications.
     */
    public synchronized int getSuccessfulCommunications() {
        return successfulCommunications;
    }

    /**
     * @return The incremented value.
     */
    public synchronized int incSuccessfulCommunications() {
        return ++this.successfulCommunications;
    }

    /**
     * @return The receivedLSAs.
     */
    public synchronized int getReceivedLSAs() {
        return receivedLSAs;
    }

    /**
     * @return The incremented value.
     */
    public synchronized int incReceivedLSAs() {
        receivedLSAs++;
        return receivedLSAs;
    }

    /**
     * @return Returns the getSentLSAs.
     */
    public synchronized int getSentLSAs() {
        return sentLSAs;
    }

    /**
     * @return The incremented value.
     */
    public synchronized int incSentLSAs() {
        sentLSAs++;
        return sentLSAs;
    }

    /**
     * @return The rejectedLSAs.
     */
    public synchronized int getRejectedLSAs() {
        return rejectedLSAs;
    }

    /**
     * @return The incremented value.
     */
    public synchronized int incRejectedLSAs() {
        rejectedLSAs++;
        return rejectedLSAs;
    }

    /**
     * @return The shortesPathComputations.
     */
    public synchronized int getShortestPathComputations() {
        return shortesPathComputations;
    }

    /**
     * Increment.
     * @return The incremented value.
     */
    public synchronized int incShortestPathComputations() {
        shortesPathComputations++;
        return shortesPathComputations;
    }

    /**
     * @return The hopCountOfReceivedLSAs.
     */
    public synchronized int getHopCountOfReceivedLSAs() {
        return hopCountOfReceivedLSAs;
    }

    /**
     * @param increment The increment.
     * @return The incremented value.
     */
    public synchronized int incHopCountOfReceivedLSAs(int increment) {
        hopCountOfReceivedLSAs += increment;
        setMaxReceivedHopCount(increment);
        return hopCountOfReceivedLSAs;
    }

    /**
     * @return The hopCountOfsentLSAs.
     */
    public synchronized int getHopCountOfSentLSAs() {
        return hopCountOfSentLSAs;
    }

    /**
     * @param increment The increment.
     * @return The incremented value.
     */
    public synchronized int incHopCountOfSentLSAs(int increment) {
        hopCountOfSentLSAs += increment;
        return hopCountOfSentLSAs;
    }

    /**
     * @return Returns the hopCountOfDeclinedLSAs.
     */
    public synchronized int getHopCountOfRejectedLSAs() {
        return hopCountOfRejectedLSAs;
    }

    /**
     * @param increment The increment.
     * @return The incremented value.
     */
    public synchronized int incHopCountOfRejectedLSAs(int increment) {
        hopCountOfRejectedLSAs += increment;
        return hopCountOfRejectedLSAs;
    }

    private int setMaxReceivedHopCount(int value) {
        if (value > maxReceivedHopCount) {
            maxReceivedHopCount = value;
        }
        return maxReceivedHopCount;
    }

    /**
     * @return Returns the maxReceivedHopCount.
     */
    public synchronized int getMaxReceivedHopCount() {
        return maxReceivedHopCount;
    }

    /**
     * Set maximum time to live for LSAs.
     * 
     * @param value The maximum time.
     * @return The maximum time.
     */
    public synchronized int setMaxTimeToLive(int value) {
        if (value > maxTimeToLive) {
            maxTimeToLive = value;
        }
        return maxTimeToLive;
    }

    /**
     * @return Returns the maxTimeToLive.
     */
    public synchronized int getMaxTimeToLive() {
        return maxTimeToLive;
    }

}
