/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;

/**
 * Common superclass for test step definitions, providing common infrastructure and utility methods.
 *
 * @author Robert Mischke
 */
public abstract class AbstractStepDefinitionBase {

    protected final TestScenarioExecutionContext executionContext;

    protected final TextOutputReceiver outputReceiver;

    protected final Log log = LogFactory.getLog(getClass());

    public AbstractStepDefinitionBase(TestScenarioExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.outputReceiver = executionContext.getOutputReceiver();
    }

    protected final void assertPropertyOfTextOutput(ManagedInstance instance, String negationFlag, String useRegexpMarker,
        String substring, final String output, final String outputType) {
        final boolean expectedPresence = (negationFlag == null); // true = step did NOT contain phrase "... should not contain ..."
        final boolean useRegexp = (useRegexpMarker != null); // step contained phrase "... the pattern ..."
        if (!useRegexp) {
            useRegexpMarker = ""; // prevent "null" in output below
        }
        final boolean found;
        if (useRegexp) {
            found = Pattern.compile(substring, Pattern.MULTILINE).matcher(output).find();
        } else {
            found = output.contains(substring);
        }
        if (expectedPresence && !found) {
            // on failure, write the examined output to a temp file and log its location as dumping a large file is slow in terminals
            fail(
                StringUtils.format("The %s of instance \"%s\" did not contain %s\"%s\";\n  saving the examined output as %s for inspection",
                    outputType, instance, useRegexpMarker, substring, writeOutputToTempFile(output)));
        }
        if (!expectedPresence && found) {
            // on failure, write the examined output to a temp file and log its location as dumping a large file is slow in terminals
            fail(StringUtils.format(
                "The %s of instance \"%s\" contained %s\"%s\" although it should not;\n  saving the examined output as %s for inspection",
                outputType, instance, useRegexpMarker, substring, writeOutputToTempFile(output)));
        }
        if (expectedPresence) {
            printToCommandConsole(
                StringUtils.format("  The %s of instance \"%s\" contained the expected text \"%s\"", outputType, instance, substring));
        } else {
            printToCommandConsole(
                StringUtils.format("  The %s of instance \"%s\" did not contain text \"%s\" (as expected)",
                    outputType, instance, substring));
        }
    }

    protected List<String> listOfSingleStringElement(String element) {
        List<String> singleInstanceList = new ArrayList<>();
        singleInstanceList.add(element);
        return singleInstanceList;
    }

    /**
     * Parses comma separated list to List of Strings removing whitespaces.
     */
    protected final List<String> parseCommaSeparatedList(String commaSeparatedList) {
        if (commaSeparatedList == null) {
            return new LinkedList<String>();
        }
        return Arrays.asList(commaSeparatedList.trim().split("\\s*,\\s*"));
    }

    protected final void printToCommandConsole(String text) {
        outputReceiver.addOutput(text);
    }

    protected final boolean stringContainsOrContainsNot(String string, String substring, boolean shouldContain, boolean useRegex) {
        final boolean found;
        if (useRegex) {
            found = Pattern.compile(substring, Pattern.MULTILINE).matcher(string).find();
        } else {
            found = string.contains(substring);
        }
        return shouldContain == found;
    }

    /**
     * @return the location of the generated temp file
     * @throws IOException
     */
    protected final String writeOutputToTempFile(final String output) {
        try {
            final File tempFile = File.createTempFile("bdd_test_failure_data", ".txt");
            FileUtils.write(tempFile, output);
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            fail("Unexpected error writing temp file: " + e.toString());
            return null; // never reached
        }
    }

    protected PrefixingTextOutForwarder getTextoutReceiverForIMOperations() {
        return new PrefixingTextOutForwarder("  (IM output) ", outputReceiver);
    }
}
