/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.testutils;

/**
 * Common interface for implementations that limit the throughput of data flows (e.g. streams) over time. Typically used for testing by
 * simulating slow streams/connections.
 *
 * @author Robert Mischke
 */
public interface ThroughputLimiter {

    /**
     * The main method that acts before any data transfer/traffic takes place. Implementations may, for example, decide to wait for a
     * certain time before returning from this method.
     * 
     * @param numBytes the number of bytes that will be written or read after this method returns
     */
    void beforeTraffic(int numBytes);

    /**
     * @return human-readable statistics as a single-line string, e.g. the actions performed, or the effective total throughput since
     *         creation
     */
    String getStatisticsLine();
}
