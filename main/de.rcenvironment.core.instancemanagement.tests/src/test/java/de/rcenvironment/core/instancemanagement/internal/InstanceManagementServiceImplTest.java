/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;

/**
 * Unit tests for {@link InstanceManagementServiceImpl} that can be run without triggering heavy-weight I/O operations.
 * 
 * @author Robert Mischke
 */
public class InstanceManagementServiceImplTest {

    private static final String OK_ID = "ok";

    private static final String INVALID_ID_1 = "";

    private static final String INVALID_ID_2 = "..";

    private LoggingTextOutReceiver userOutputReceiver;

    private InstanceManagementServiceImpl imService;

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();
        imService = new InstanceManagementServiceImpl();
        userOutputReceiver = new LoggingTextOutReceiver("");
    }

    /**
     * Tests that malformed ids are properly rejected by all public methods, ie an exception with an appropriate text is thrown.
     */
    @Test
    public void invalidIdsAreProperlyRejected() {
        // will fail, but not due to validation errors
        testStartInstanceValidation(OK_ID, OK_ID, false);

        testStartInstanceValidation(INVALID_ID_1, OK_ID, true);
        testStartInstanceValidation(INVALID_ID_2, OK_ID, true);
        testStartInstanceValidation(OK_ID, INVALID_ID_1, true);
        testStartInstanceValidation(OK_ID, INVALID_ID_2, true);

        // will fail, but not due to validation errors
        testStopInstanceValidation(OK_ID, false);

        testStopInstanceValidation(INVALID_ID_1, true);
        testStopInstanceValidation(INVALID_ID_2, true);

        // will fail, but not due to validation errors
        testSetupInstallationValidation(OK_ID, false);

        testSetupInstallationValidation(INVALID_ID_1, true);
        testSetupInstallationValidation(INVALID_ID_2, true);

    }

    private void testStartInstanceValidation(String param1, String param2, boolean shouldBeValidationError) {
        try {
            imService.startinstance(param1, param2, userOutputReceiver);
            failWithMessage();
        } catch (IOException e) {
            if (shouldBeValidationError) {
                assertIsMalformedIdException(e);
            } else {
                assertIsOtherException(e);
            }
        }
    }

    private void testStopInstanceValidation(String param1, boolean shouldBeValidationError) {
        try {
            imService.stopInstance(param1, userOutputReceiver);
            failWithMessage();
        } catch (IOException e) {
            if (shouldBeValidationError) {
                assertIsMalformedIdException(e);
            } else {
                assertIsOtherException(e);
            }
        }
    }

    private void testSetupInstallationValidation(String param1, boolean shouldBeValidationError) {
        try {
            imService.setupInstallationFromUrlQualifier(param1, "urlQualifier", null, userOutputReceiver);
            failWithMessage();
        } catch (IOException e) {
            if (shouldBeValidationError) {
                assertIsMalformedIdException(e);
            } else {
                assertIsOtherException(e);
            }
        }
    }

    private void failWithMessage() {
        fail("Exception expected");
    }

    private void assertIsMalformedIdException(Exception e) {
        String message = e.getMessage();
        assertTrue("Expected a 'malformed id' exception, but found " + message, e.getMessage().startsWith("Malformed id: "));
    }

    private void assertIsOtherException(Exception e) {
        String message = e.getMessage();
        assertFalse("Received an unexpected 'malformed id' exception: " + message, e.getMessage().startsWith("Malformed id: "));
    }
}
