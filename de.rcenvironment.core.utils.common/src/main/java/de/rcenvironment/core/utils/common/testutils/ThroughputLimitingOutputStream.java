/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.testutils;

import java.io.OutputStream;

import org.apache.commons.io.output.CountingOutputStream;

/**
 * A simple stream wrapper to simulate a low-throughput connection.
 *
 * @author Robert Mischke
 */
public class ThroughputLimitingOutputStream extends CountingOutputStream {

    private ThroughputLimiter limiter;

    public ThroughputLimitingOutputStream(OutputStream wrappedStream, ThroughputLimiter limiter) {
        super(wrappedStream);
        this.limiter = limiter;
    }

    @Override
    protected synchronized void beforeWrite(int numBytes) {
        limiter.beforeTraffic(numBytes);
        super.beforeWrite(numBytes);
    }
}
