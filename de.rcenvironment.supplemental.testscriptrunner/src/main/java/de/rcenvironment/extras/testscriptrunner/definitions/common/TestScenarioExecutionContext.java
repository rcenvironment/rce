/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    private static final ThreadLocal<File> THREAD_LOCAL_PARAMETER_SCRIPT_LOCATION = new ThreadLocal<>();

    private final TextOutputReceiver outputReceiver;

    private final String buildUnderTestId;

    // TODO rename or replace with multi-instance setup
    private ExecutionResult currentExecutionResult;

    private Scenario scenario;

    private final Log log = LogFactory.getLog(getClass());

    private File testScriptLocation;
    
    // TODO review: contains test state; move into separate class?
    private ManagedInstance lastInstanceWithSingleCommandExecution;

    // note: only supposed to be accessed from the main thread, so no synchronization is performed or necessary
    // TODO review: contains test state; move into separate class?
    private Map<String, ManagedInstance> instancesById = new HashMap<>();

    // note: only supposed to be accessed from the main thread, so no synchronization is performed or necessary
    private Set<ManagedInstance> enabledInstances = new HashSet<>();

    public TestScenarioExecutionContext() {
        this.outputReceiver = Objects.requireNonNull(THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.get());
        this.buildUnderTestId = Objects.requireNonNull(THREAD_LOCAL_PARAMETER_BUILD_ID.get());
        this.testScriptLocation = Objects.requireNonNull(THREAD_LOCAL_PARAMETER_SCRIPT_LOCATION.get());
    }

    /**
     * Deposits parameters for the initialization of the actual {@link TestScenarioExecutionContext} instance, which is instantiated by a
     * dependency injection framework, and therefore requires a default constructor.
     * 
     * @param outputReceiverParam the {@link TextOutputReceiver} for script output
     * @param buildUnderTestIdParam the id of the build to test with the script
     * @param testScriptDirectoryParam the root directory of test scripts; provided to access additional test files
     */
    public static void setThreadLocalParameters(TextOutputReceiver outputReceiverParam,
        String buildUnderTestIdParam, File testScriptDirectoryParam) {
        if (THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.get() != null) { // consistency check
            throw new IllegalStateException();
        }
        THREAD_LOCAL_PARAMETER_OUTPUT_RECEIVER.set(outputReceiverParam);
        THREAD_LOCAL_PARAMETER_BUILD_ID.set(buildUnderTestIdParam);
        THREAD_LOCAL_PARAMETER_SCRIPT_LOCATION.set(testScriptDirectoryParam);
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
        outputReceiver.addOutput("> Starting test scenario \"" + newScenario.getName() + "\"");
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
            outputReceiver.addOutput("< Completed test scenario \"" + scenario.getName() + "\"");
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

    public File getTestScriptLocation() {
        return testScriptLocation;
    }

    public ManagedInstance getLastInstanceWithSingleCommandExecution() {
        return lastInstanceWithSingleCommandExecution;
    }

    public void setLastInstanceWithSingleCommandExecution(ManagedInstance lastInstanceWithSingleCommandExecution) {
        this.lastInstanceWithSingleCommandExecution = lastInstanceWithSingleCommandExecution;
    }
    
    /**
     * @param id id of instance
     * @param instance instance to be identified by id
     */
    public void putInstance(String id, ManagedInstance instance) {
        instancesById.put(id, instance);
    }
    
    /**
     * @param id key for map
     * @return instance belonging to key
     */
    public ManagedInstance getInstanceFromId(String id) {
        return instancesById.get(id);
    }

    public Map<String, ManagedInstance> getInstancesById() {
        return instancesById;
    }
    
    /**
     * @param instance instance to add to set of all enabled instances
     */
    public void addInstance(ManagedInstance instance) {
        enabledInstances.add(instance);
    }

    public Set<ManagedInstance> getEnabledInstances() {
        return enabledInstances;
    }

    public void setEnabledInstances(Set<ManagedInstance> enabledInstances) {
        this.enabledInstances = enabledInstances;
    }
}
