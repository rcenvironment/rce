/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * {@link LogUtils} unit tests.
 * 
 * @author Robert Mischke
 */
public class LogUtilsTest {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Calls all utility methods once, and verifies that they return unique markers/ids. Currently, the actual log output is not checked
     * automatically, but can be inspected in the test logs if necessary (e.g. when changing the output formatting).
     */
    @Test
    public void testExceptionVariants() {
        // TODO could be improved by mocking the logger instance
        IOException testException = new IOException("test exception");
        final String testMessage = "test message";
        String id1 = LogUtils.logErrorAndAssignUniqueMarker(log, testMessage);
        String id2 = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log, testMessage, testException);
        String id3 = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(log, testMessage, testException);

        assertFalse(id1.equals(id2));
        assertFalse(id1.equals(id3));
        assertFalse(id2.equals(id3));
    }
}
