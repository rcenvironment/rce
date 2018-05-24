/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.utils.common.textstream.receivers.LoggingTextOutReceiver;
import de.rcenvironment.extras.testscriptrunner.definitions.RceTestLifeCycleHooks;
import de.rcenvironment.extras.testscriptrunner.definitions.impl.InstanceManagementStepDefinitions;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter;
import de.rcenvironment.extras.testscriptrunner.internal.CucumberTestFrameworkAdapter.ExecutionResult;

/**
 * TestScriptRunner "self test"; current only intended for local execution as a non-plugin unit test.
 *
 * @author Robert Mischke
 */
public class TestScriptRunnerBackendTest {

    private static final String SEPARATOR_TEXT_LINE =
        "-----------------------------------------------------------------------------------------------";

    private final CucumberTestFrameworkAdapter testFrameworkAdapter;

    private final Log log = LogFactory.getLog(getClass());

    public TestScriptRunnerBackendTest() {
        testFrameworkAdapter = new CucumberTestFrameworkAdapter(
            RceTestLifeCycleHooks.class,
            InstanceManagementStepDefinitions.class,
            SelfTestSteps.class);
    }

    /**
     * Uses the test framework adapter to run scripts in src/test/resources/scripts.
     * 
     * @throws IOException on I/O errors
     */
    @Test
    public void executeSelfTestScripts() throws IOException {
        final File scriptDir = new File("src/test/resources/scripts/");

        new InstanceManagementStepDefinitions().bindInstanceManagementService(EasyMock.createNiceMock(InstanceManagementService.class));

        final String systemTempPath = System.getProperty("java.io.tmpdir");
        final File reportDir = new File(systemTempPath, "testscriptrunner-selftest");
        log.info("Test report output directory: " + reportDir);

        final LoggingTextOutReceiver outputReceiver = new LoggingTextOutReceiver("(Test output) ");
        final String buildUnderTestId = "dummyBuild";
        ExecutionResult result = testFrameworkAdapter.executeTestScripts(scriptDir, null, outputReceiver, buildUnderTestId, reportDir);

        List<String> reportLines = result.getReportFileLines();
        if (reportLines != null) {
            // dump generated text report to text console
            log.info("Test run complete, content of report file:");
            log.info(SEPARATOR_TEXT_LINE);
            for (String line : reportLines) {
                log.info(line);

            }
            log.info(SEPARATOR_TEXT_LINE);

        } else {
            fail("Test run complete (no report file found)");
        }

        List<String> stdOutLines = result.getCapturedStdOutLines();
        boolean errorCaseDetected = false;
        log.info("Captured Standard Output:");
        log.info(SEPARATOR_TEXT_LINE);
        for (String line : stdOutLines) {
            log.info(line);
            if (line.contains("Undefined scenarios")) {
                errorCaseDetected = true;
            }
        }
        log.info(SEPARATOR_TEXT_LINE);
        if (errorCaseDetected) {
            fail("Error case detected - check captured Standard Output for details");
        }
    }

}
