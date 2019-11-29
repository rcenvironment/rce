/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * {@link UplinkProtocolErrorType} test.
 *
 * @author Robert Mischke
 */
public class UplinkProtocolErrorTypeTest {

    /**
     * Tests normal operation.
     */
    @Test
    public void formattingAndParsing() {
        String testMessage = "my message";
        final String wrappedMessage = UplinkProtocolErrorType.INVALID_HANDSHAKE_DATA.wrapErrorMessage(testMessage);
        assertEquals(UplinkProtocolErrorType.INVALID_HANDSHAKE_DATA, UplinkProtocolErrorType.typeOfWrappedErrorMessage(wrappedMessage));
        assertEquals(testMessage, UplinkProtocolErrorType.unwrapErrorMessage(wrappedMessage));
    }

    /**
     * Tests behavior on an unrecognized/invalid wrapped message.
     */
    @Test
    public void unknownTypeFallback() {
        // just use an unwrapped message as invalid input
        String testMessage = "my message";
        assertEquals(UplinkProtocolErrorType.UNKNOWN_ERROR, UplinkProtocolErrorType.typeOfWrappedErrorMessage(testMessage));
        assertNotNull(UplinkProtocolErrorType.unwrapErrorMessage(testMessage));
        // the fallback for the "unwrapped" message text should include the whole invalid input
        assertTrue(UplinkProtocolErrorType.unwrapErrorMessage(testMessage).contains(testMessage));
    }

}
