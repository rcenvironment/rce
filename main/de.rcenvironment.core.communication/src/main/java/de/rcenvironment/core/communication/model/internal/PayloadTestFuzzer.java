/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility class to intentionally corrupt network request or response payloads for testing error handling and robustness.
 * 
 * @author Robert Mischke
 */
public final class PayloadTestFuzzer {

    /**
     * Whether the fuzz testing feature(s) should be enabled.
     * 
     * IMPORTANT: this setting must always be "false" on release!
     */
    public static final boolean ENABLED = false;

    // note: these values probably still need tweaking
    private static final float MIN_P_TO_CORRUPT_PAYLOAD = 0.001f;

    private static final float MAX_P_TO_CORRUPT_PAYLOAD = 0.01f;

    private static final int PAYLOAD_SIZE_CEILING = 50000; // at this size, MAX_CHANCE is reached

    private static final float P_INCREMENT_PER_BYTE = (MAX_P_TO_CORRUPT_PAYLOAD - MIN_P_TO_CORRUPT_PAYLOAD)
        / PAYLOAD_SIZE_CEILING;

    private static final Random sharedRandom = new Random();

    private static final Log sharedLog = LogFactory.getLog(PayloadTestFuzzer.class);

    private PayloadTestFuzzer() {}

    /**
     * May apply one or more random corruptions (byte randomizations) to this byte array; the probability of this happening is based on te
     * payload's length.
     * 
     * @param payload the payload array to potentially modify/corrupt
     */
    public static void apply(byte[] payload) {
        int relevantSize = Math.min(payload.length, PAYLOAD_SIZE_CEILING);
        float p = MIN_P_TO_CORRUPT_PAYLOAD + relevantSize * P_INCREMENT_PER_BYTE;

        int n = 0;
        while (sharedRandom.nextFloat() <= p) {
            int pos = sharedRandom.nextInt(payload.length);
            payload[pos] = (byte) sharedRandom.nextInt();
            n++;
        }
        if (n > 0) {
            sharedLog.debug(StringUtils.format("Corrupted %d bytes of a payload at p=%.4f", n, p));
        }
    }
}
