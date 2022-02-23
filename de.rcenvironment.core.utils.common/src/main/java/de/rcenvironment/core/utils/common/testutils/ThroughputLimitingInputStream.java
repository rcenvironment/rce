/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.testutils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.CountingInputStream;

/**
 * A simple stream wrapper to simulate a low-throughput connection.
 *
 * @author Robert Mischke
 */
public class ThroughputLimitingInputStream extends CountingInputStream {

    private ThroughputLimiter limiter;

    public ThroughputLimitingInputStream(InputStream wrappedStream, ThroughputLimiter limiter) {
        super(wrappedStream);
        this.limiter = limiter;
    }

    @Override
    protected void beforeRead(int numBytes) throws IOException {
        limiter.beforeTraffic(numBytes);
        super.beforeRead(numBytes);
    }
}
