/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.testutils;

import java.util.Random;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A very basic throughput limiter that simply waits for a certain number of milliseconds every time a certain amount of traffic has
 * occurred. To prevent waiting deterministically at the exact same times every time, the traffic counter is initialized with a random value
 * between 0 and limit-1.
 *
 * @author Robert Mischke
 */
public class SimpleThroughputLimiter implements ThroughputLimiter {

    private static final int MSEC_TO_SEC_FACTOR = 1000;

    private static final float FALLBACK_VALUE_THROUGHPUT_STATISTICS = -1.0f;

    // prevent excessively high values leading to integer overflow
    private static final int MAX_INCREMENT_AND_COUNTER = Integer.MAX_VALUE / 2 - 1;

    private final int waitEveryNBytes;

    private final int waitDurationMsec;

    private int trafficCounter;

    private long statsCreationTimestamp;

    private long statsTotalBytes;

    private int statsNumInterrupts;

    private String verboseLoggingPrefix; // also serves as the on/off flag

    public SimpleThroughputLimiter(int waitEveryNBytes, int waitDurationMsec) {
        if (waitEveryNBytes <= 0 || waitEveryNBytes > MAX_INCREMENT_AND_COUNTER || waitDurationMsec <= 0) {
            throw new IllegalArgumentException();
        }
        this.waitEveryNBytes = waitEveryNBytes;
        this.waitDurationMsec = waitDurationMsec;
        this.trafficCounter = new Random().nextInt(waitEveryNBytes); // see class JavaDoc
        this.statsCreationTimestamp = System.currentTimeMillis();
    }

    @Override
    // synchronized to allow safe reuse of the same limiter for multiple streams; obviously, this setup can lead to additional slowdown
    public synchronized void beforeTraffic(int numBytes) {
        if (numBytes == 0) {
            return;
        }

        if (numBytes <= 0 || numBytes > MAX_INCREMENT_AND_COUNTER) {
            throw new IllegalArgumentException(Integer.toString(numBytes));
        }

        statsTotalBytes += numBytes;

        trafficCounter += numBytes;
        while (trafficCounter > waitEveryNBytes) {
            trafficCounter -= waitEveryNBytes;
            try {
                if (verboseLoggingPrefix != null) {
                    LogFactory.getLog(getClass())
                        .debug(StringUtils.format(
                            "%sThrottling connection for %d msec after a total of %d bytes of traffic (last delta: %d bytes)",
                            verboseLoggingPrefix, waitDurationMsec, statsTotalBytes, numBytes));
                }
                Thread.sleep(waitDurationMsec);
                statsNumInterrupts++;
            } catch (InterruptedException e) {
                LogFactory.getLog(getClass()).debug("Interrupted while waiting for throughput limiting");
            }
        }
    }

    @Override
    public synchronized String getStatisticsLine() {
        long lifetimeMsec = System.currentTimeMillis() - statsCreationTimestamp;
        float averageThroughput = FALLBACK_VALUE_THROUGHPUT_STATISTICS;
        if (lifetimeMsec > 0) {
            averageThroughput = ((float) statsTotalBytes) / lifetimeMsec * MSEC_TO_SEC_FACTOR;
        }

        return StringUtils.format(
            "Total traffic: %d bytes, waited for %d times (%d msec total), average throughput since creation: %f bytes/sec",
            statsTotalBytes, statsNumInterrupts, statsNumInterrupts * waitDurationMsec, averageThroughput);
    }

    public void enableVerboseLogging(String logPrefix) {
        this.verboseLoggingPrefix = logPrefix; // also serves as the on/off flag
    }
}
