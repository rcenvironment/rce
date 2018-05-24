/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.internal;

import static org.junit.Assert.assertEquals;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cucumber.api.Scenario;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.ExecutionResult;

/**
 * A context holder for execution of a single {@link Scenario}.
 *
 * @author Robert Mischke
 */
public final class TestScenarioExecutionContext {

    private static final ThreadLocal<TextOutputReceiver> THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER = new ThreadLocal<>();

    private static final ThreadLocal<String> THREAD_LOCAL_PARAMETER_BUILD_ID = new ThreadLocal<>();

    private final TextOutputReceiver outputReceiver;

    private final String buildUnderTestId;

    private ExecutionResult currentExecutionResult;

    private Scenario scenario;

    private final Log log = LogFactory.getLog(getClass());

    public TestScenarioExecutionContext() {
        this.outputReceiver = Objects.requireNonNull(THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.get());
        this.buildUnderTestId = Objects.requireNonNull(THREAD_LOCAL_PARAMETER_BUILD_ID.get());
    }

    /**
     * Deposits parameters for the initialization of the actual {@link TestScenarioExecutionContext} instance, which is instantiated by a
     * dependency injection framework, and therefore requires a default constructor.
     * 
     * @param outputReceiverParam the {@link TextOutputReceiver} for script output
     * @param buildUnderTestIdParam the id of the build to test with the script
     */
    public static void setThreadLocalParameters(TextOutputReceiver outputReceiverParam,
        String buildUnderTestIdParam) {
        if (THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.get() != null) { // consistency check
            throw new IllegalStateException();
        }
        THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.set(outputReceiverParam);
        THREAD_LOCAL_PARAMETER_BUILD_ID.set(buildUnderTestIdParam);
    }

    /**
     * Discards the {@link TestScenarioExecutionContext} instance for the current thread to release memory (especially when run from a
     * thread pool).
     */
    public static void discardThreadLocalParameters() {
        if (THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.get() == null) { // consistency check
            throw new IllegalStateException();
        }
        THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.set(null);
        THREAD_LOCAL_PARAMETER_BUILD_ID.set(null);
    }

    /**
     * Prints a log header before each test.
     * 
     * @param newScenario the injected {@link Scenario} object
     */
    public void beforeEach(Scenario newScenario) {
        this.scenario = newScenario;
        outputReceiver.addOutput("Starting test scenario \"" + newScenario.getName() + "\"");
    }

    /**
     * Prints a log footer after each test, and potentially appends error information to any generated reports.
     * 
     * @param finishedScenario the injected {@link Scenario} object
     */
    public void afterEach(Scenario finishedScenario) {
        assertEquals(finishedScenario, this.scenario); // internal consistency check
        if (scenario.isFailed()) {
            outputReceiver
                .addOutput("*** Error in test scenario \"" + scenario.getName() + "\"; dumping any captured StdOut/StdErr output");
            scenario.write("*** Error in test scenario \"" + scenario.getName() + "\"; dumping any captured StdOut/StdErr output");
            if (currentExecutionResult != null) {
                for (String line : currentExecutionResult.stdoutLines) {
                    String logLine = "[StdOut] " + line;
                    scenario.write(logLine);
                    log.error(logLine);
                }
                for (String line : currentExecutionResult.stderrLines) {
                    String logLine = "[StdErr] " + line;
                    scenario.write(logLine);
                    log.error(logLine);
                }
            }
        } else {
            outputReceiver.addOutput("Completed test scenario \"" + scenario.getName() + "\"");
        }
    }

    public TextOutputReceiver getOutputReceiver() {
        return outputReceiver;
    }

    public String getBuildUnderTestId() {
        return buildUnderTestId;
    }

    public String getScenarioName() {
        return scenario.getName();
    }
}
