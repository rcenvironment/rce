/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * Common superclass for test step definitions, providing common infrastructure and utility methods.
 *
 * @author Robert Mischke
 */
public abstract class AbstractStepDefinitionBase {

    protected final TestScenarioExecutionContext executionContext;

    protected final TextOutputReceiver outputReceiver;

    protected final InstanceManagementService instanceManagementService;

    protected final Log log = LogFactory.getLog(getClass());

    // note: only supposed to be accessed from the main thread, so no synchronization is performed or necessary
    // TODO review: contains test state; move into separate class?
    protected final Map<String, ManagedInstance> instancesById;

    // TODO review: contains test state; move into separate class?
    protected ManagedInstance lastInstanceWithSingleCommandExecution;

    public AbstractStepDefinitionBase(TestScenarioExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.outputReceiver = executionContext.getOutputReceiver();
        this.instancesById = executionContext.getSharedInstancesByIdMap();
        this.instanceManagementService = ExternalServiceHolder.getInstanceManagementService();
    }

    protected final void printToCommandConsole(String text) {
        outputReceiver.addOutput(text);
    }

    protected final void assertPropertyOfTextOutput(ManagedInstance instance, String negationFlag, String useRegexpMarker, String substring,
        final String output, final String outputType) {
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

    /**
     * @param isMainAction true if executing this command is the actual test action; false if this it is incidental, e.g. for testing state
     *        or performing cleanup
     */
    protected final String executeCommandOnInstance(final ManagedInstance instance, String commandString, boolean isMainAction) {
        final String instanceId = instance.getId();
        final String logPattern;
        if (isMainAction) {
            logPattern = "Executing command \"%s\" on instance \"%s\"";
        } else {
            logPattern = "  (Executing command \"%s\" on instance \"%s\")";
        }
        final String startInfoText = StringUtils.format(logPattern, commandString, instanceId);
        printToCommandConsole(startInfoText);
        log.debug(startInfoText);
        CapturingTextOutReceiver commandOutputReceiver = new CapturingTextOutReceiver();
        try {
            final int maxAttempts = 3;
            int numAttempts = 0;
            while (numAttempts < maxAttempts) {
                try {
                    instanceManagementService.executeCommandOnInstance(instanceId, commandString, commandOutputReceiver);
                    break; // exit retry loop on success
                } catch (JSchException e) {
                    if (!e.toString().contains("Connection refused: connect")) {
                        throw e; // rethrow and fail on other errors
                    }
                }
                numAttempts++;
            }
            if (numAttempts > 1) {
                String retrySuffix = " after retrying the SSH connection for " + (numAttempts - 1) + " times)";
                printToCommandConsole(
                    StringUtils.format("  (Executed command \"%s\" on instance \"%s\"%s", commandString, instanceId, retrySuffix));
            }
            String commandOutput = commandOutputReceiver.getBufferedOutput();
            instance.setLastCommandOutput(commandOutput);
            log.debug(StringUtils.format("Finished execution of command \"%s\" on instance \"%s\"", commandString, instanceId));
            return commandOutput;
        } catch (JSchException | SshParameterException | IOException | InterruptedException e) {
            fail(StringUtils.format("Failed to execute command \"%s\" on instance \"%s\": %s", commandString, instanceId, e.toString()));
            return null; // dummy command; never reached
        }

    }

    protected final ManagedInstance resolveInstance(String instanceId) {
        return instancesById.get(instanceId);
    }

    protected final List<String> parseInstanceList(String instanceList) {
        return Arrays.asList(instanceList.trim().split("\\s*,\\s*"));
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
}
