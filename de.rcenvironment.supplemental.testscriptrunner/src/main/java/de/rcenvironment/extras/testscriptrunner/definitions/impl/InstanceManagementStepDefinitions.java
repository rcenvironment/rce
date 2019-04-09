/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.instancemanagement.InstanceConfigurationOperationSequence;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;
import de.rcenvironment.extras.testscriptrunner.definitions.common.AbstractStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;

/**
 * Steps controlling and querying the RCE "instance management", which is used to provision and run test instances using configured RCE
 * installations.
 *
 * @author Robert Mischke
 */
public class InstanceManagementStepDefinitions extends AbstractStepDefinitionBase {

    private static final String WARNINGS_LOG_FILE_NAME = "warnings.log";

    private static final String DEBUG_LOG_FILE_NAME = "debug.log";

    private static final int IM_OPERATION_TIMEOUT = 30000;

    private static final ManagedInstance[] EMPTY_INSTANCE_ARRAY = new ManagedInstance[0];

    private static final Pattern INSTANCE_DEFINITION_PATTERN = Pattern.compile("(\\w+)( \\[.*\\])?");

    private static final Pattern INSTANCE_DEFINITION_ID_SUBPATTERN = Pattern.compile("Id=(\\w+)"); // length intentionally undefined

    private static final String ABSENT_COMPONENT_STRING = "(absent)";

    private final AtomicInteger portNumberGenerator = new AtomicInteger(52100);

    // note: only supposed to be accessed from the main thread, so no synchronization is performed or necessary
    private final Collection<ManagedInstance> enabledInstances = new HashSet<>();

    /**
     * Represents the expected vs. the actual visibility and authorization state of a local or remote component.
     *
     * @author Robert Mischke
     */
    private class ComponentVisibilityState {

        private String componentName;

        private String nodeName;

        private String expectedState;

        private String actualState;

        ComponentVisibilityState(String componentName, String nodeName, String expectedState, String actualState) {
            this.componentName = componentName;
            this.nodeName = nodeName;
            setExpectedState(expectedState);
            setActualState(actualState);
        }

        public void setExpectedState(String expectedState) {
            this.expectedState = Optional.ofNullable(expectedState).orElse(ABSENT_COMPONENT_STRING);
        }

        public void setActualState(String actualState) {
            this.actualState = Optional.ofNullable(actualState).orElse(ABSENT_COMPONENT_STRING);
        }

        public boolean stateMatches() {
            return expectedState.equals(actualState);
        }

        @Override
        public String toString() {
            return StringUtils.format("%s | %s | expected: %s | found: %s", nodeName, componentName, expectedState, actualState);
        }

    }

    public InstanceManagementStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Pre-scenario setup code.
     */
    @Before
    public void initialize() {
        // sanity checks
        assertTrue(instancesById.isEmpty());
        assertTrue(enabledInstances.isEmpty());
    }

    /**
     * Post-scenario cleanup code.
     */
    @After
    public void tearDown() {
        tearDownLeftoverRunningInstances();
    }

    /**
     * Test step that sets up a single shared installation and one or more instances, while also registering this installation for these
     * instances.
     * 
     * TODO add instance configuration; currently they are only registered
     * 
     * @param autoStartPhrase an optional phrase that triggers auto-starting the instances if present
     * @param instanceList comma-separated list of instance names
     * @param optionalTemplateId if set, this specifies a template configuration file to start from, instead of an empty configuration
     * @param buildOrInstallationId the URL part (e.g. "snapshots/trunk") defining the build to use, or a symbolic installation id (e.g.
     *        ":self" or "local:...")
     * @throws Throwable on failure
     */
    @Given("^(?:the )?(running )?instance[s]? \"([^\"]*)\" (?:based on template \"([^\"]*)\" )?"
        + "using (?:the default build|build \"([^\"]*)\")$")
    public void givenInstancesUsingBuild(String autoStartPhrase, String instanceList, String optionalTemplateId,
        String buildOrInstallationId) throws Throwable {

        if (buildOrInstallationId == null) {
            // if this parameter is null, the phrase "the default build" was found instead of a "build <...>" part,
            // so the user wants to use the default "build under test" id -- misc_ro
            buildOrInstallationId = executionContext.getBuildUnderTestId();
        }

        // The "build or installation id" can be one of two things here: either
        // - an actual standard *build* id, e.g. "snapshot/trunk", "releases/8.1.1"
        // - or a special *installation* id, e.g. ":self" or "local:c:\temp\build\rce".
        //
        // Standard build ids are converted to implicit installation ids by removing all special characters;
        // special build ids are passed on as they are.

        final PrefixingTextOutForwarder imOperationOutputReceiver = getTextoutReceiverForIMOperations();

        final String installationId;
        if (instanceManagementService.isSpecialInstallationId(buildOrInstallationId)) {
            installationId = buildOrInstallationId;
        } else {
            installationId = deriveImplicitInstallationIdFromBuildId(buildOrInstallationId);
            printToCommandConsole(
                StringUtils.format("Setting up installation \"%s\" using build \"%s\"", installationId, buildOrInstallationId));
            instanceManagementService.setupInstallationFromUrlQualifier(installationId, buildOrInstallationId,
                InstallationPolicy.IF_PRESENT_CHECK_VERSION_AND_REINSTALL_IF_DIFFERENT,
                imOperationOutputReceiver, IM_OPERATION_TIMEOUT);
        }

        final List<String> instanceDefinitionParts = parseInstanceList(instanceList);
        final List<String> instanceIds = new ArrayList<>();
        for (String instanceDefinition : instanceDefinitionParts) {
            final Matcher matcher = INSTANCE_DEFINITION_PATTERN.matcher(instanceDefinition);
            if (!matcher.matches()) {
                fail("Invalid instance definition part: " + instanceDefinition);
            }

            String instanceId = matcher.group(1);
            instanceIds.add(instanceId);

            String optionString = matcher.group(2);
            if (optionString == null) {
                optionString = "";
            }

            final ManagedInstance instance = new ManagedInstance(instanceId, installationId, instanceManagementService);
            instancesById.put(instanceId, instance);
            enabledInstances.add(instance);
            printToCommandConsole(StringUtils.format("Configuring test instance \"%s\"", instanceId));

            final int imSshPortNumber = portNumberGenerator.incrementAndGet(); // TODO check whether that port is actually free
            InstanceConfigurationOperationSequence setupSequence = instanceManagementService.newConfigurationOperationSequence();
            // base on template or start configuration from scratch?
            if (optionalTemplateId == null) {
                setupSequence = setupSequence.resetConfiguration();
            } else {
                final File testTemplateFile =
                    new File(executionContext.getTestScriptLocation(),
                        StringUtils.format("instance_templates/%s.json", optionalTemplateId));
                if (!testTemplateFile.isFile()) {
                    throw new IOException(
                        "Specified template file " + optionalTemplateId + " was not found at expected location " + testTemplateFile);
                }
                setupSequence = setupSequence.applyTemplateFile(testTemplateFile);
            }
            if (optionString.contains("Relay")) {
                setupSequence = setupSequence.setRelayFlag(true);
            }
            if (optionString.contains("WorkflowHost") || optionString.contains("WfHost") || optionString.contains("WFHost")) {
                setupSequence = setupSequence.setWorkflowHostFlag(true);
            }
            Matcher idMatcher = INSTANCE_DEFINITION_ID_SUBPATTERN.matcher(optionString);
            String customNodeId = null;
            if (idMatcher.find()) {
                customNodeId = idMatcher.group(1);
            }
            setupSequence = setupSequence.enableImSshAccess(imSshPortNumber).setName(instanceId);
            if (customNodeId != null) {
                setupSequence = setupSequence.setCustomNodeId(customNodeId);
            }
            instanceManagementService.applyInstanceConfigurationOperations(instanceId, setupSequence, imOperationOutputReceiver);
        }
        printToCommandConsole(StringUtils.format("Auto-starting instance(s) \"%s\"", instanceList));
        if (autoStartPhrase != null) {
            instanceManagementService.startInstance(installationId, instanceIds, imOperationOutputReceiver,
                IM_OPERATION_TIMEOUT, false);
        }
    }

    /**
     * Launches all previously registered instances with the associated installation; will fail if no installation has been associated with
     * one of the given instances. It is not an error to start an already-runnning instance.
     * 
     * @param startConcurrentlyFlag a phrase that is present (non-null) if the instances should be started in parallel
     * @param startWithGuiFlag a phrase that is present (non-null) if the instances should be started with GUIs
     * @throws Throwable on failure
     */
    @When("^starting all instances( concurrently)?( in GUI mode)?$")
    public void whenStartingAllInstances(String startConcurrentlyFlag, String startWithGuiFlag) throws Throwable {
        final boolean startConcurrently = startConcurrentlyFlag != null;
        final boolean startWithGui = startWithGuiFlag != null;
        if (startConcurrently) { // phrase present
            // start concurrently
            final RunnablesGroup runnablesGroup = ConcurrencyUtils.getFactory().createRunnablesGroup();
            for (final ManagedInstance instance : enabledInstances) {
                runnablesGroup.add(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            startSingleInstance(instance, startWithGui);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            executeRunnablesGroupAndHandlePotentialErrors(runnablesGroup, "starting an instance");
        } else {
            // start sequentially
            for (ManagedInstance instance : enabledInstances) {
                startSingleInstance(instance, startWithGui);
            }
        }
    }

    /**
     * Stops (shuts down) all previously registered instances. It is not an error to stop an already-stopped instance.
     * 
     * @param stopConcurrently a phrase that is present (non-null) if the instances should be stopped in parallel
     * @throws Throwable on failure
     */
    @When("^stopping all instances( concurrently)?$")
    public void whenStoppingAllInstances(String stopConcurrently) throws Throwable {
        if (stopConcurrently != null) { // phrase present
            // stop concurrently
            final RunnablesGroup runnablesGroup = ConcurrencyUtils.getFactory().createRunnablesGroup();
            for (final ManagedInstance instance : enabledInstances) {
                runnablesGroup.add(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            stopSingleInstance(instance);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            executeRunnablesGroupAndHandlePotentialErrors(runnablesGroup, "stopping an instance");
        } else {
            // stop sequentially
            for (ManagedInstance instance : enabledInstances) {
                stopSingleInstance(instance);
            }
        }
    }

    /**
     * Schedules a state change event for a single instance; intended for tests where instances start/stop concurrently to other (blocking)
     * test steps. May be reworked to a multi-instance step/task in the future.
     * 
     * @param action a phrase indicating what should happen with the instance; currently supported: "shutdown", "restart"
     * @param instanceId the id of the instance to modify
     * @param delaySeconds the delay, in seconds, after which the action should be performed
     * @throws Throwable on failure
     */
    @When("^scheduling (?:a|an instance) (shutdown|restart|reconnect) of \"([^\"]+)\" after (\\d+) seconds$")
    public void whenSchedulingNodeActionsAfterDelay(final String action, final String instanceId, final int delaySeconds)
        throws Throwable {

        // TODO ensure proper integration with test cleanup
        // TODO as this is the first asynchronous test action, check thread safety
        // TODO support "restart" action as well

        final ManagedInstance instance = resolveInstance(instanceId);
        ConcurrencyUtils.getAsyncTaskService().scheduleAfterDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    switch (action) {
                    case "shutdown":
                        stopSingleInstance(instance);
                        break;
                    case "restart":
                        stopSingleInstance(instance);
                        startSingleInstance(instance, false); // assuming headless mode here
                        break;
                    case "reconnect":
                        cycleAllOutgoingConnectionsOf(instance);
                        break;
                    default:
                        throw new IllegalArgumentException(action);
                    }
                } catch (IOException e) {
                    // TODO make outer script fail, probably on shutdown
                    log.error("Error while executing aynchonous action '" + action + "' on instance '" + instanceId + "'", e);
                }

            }

        }, TimeUnit.SECONDS.toMillis(delaySeconds));
        printToCommandConsole(
            StringUtils.format("Scheduling a '%s' action for instance '%s' after %d second(s)", action, instanceId, delaySeconds));
    }

    /**
     * Verifies the running/stopped state of one or more instances.
     * 
     * @param instanceList comma-separated list of instances to check
     * @param state the expected state (stopped/running)
     * @throws Throwable on failure
     */
    @Then("^instance[s]? \"([^\"]*)\" should be (stopped|running)$")
    public void thenInstancesShouldBeInState(String instanceList, String state) throws Throwable {
        boolean shouldBeRunning = ("running".equals(state));

        for (String instanceId : parseInstanceList(instanceList)) {
            boolean isRunning = instanceManagementService.isInstanceRunning(instanceId);
            if (isRunning != shouldBeRunning) {
                throw new AssertionError(StringUtils.format(
                    "Instance state did not match expectation: Instance \"%s\" was in "
                        + "running state [%s] when it should have been [%s]",
                    instanceId, isRunning, shouldBeRunning));
            }
        }
    }

    /**
     * Configures the network connections defined by the given description string.
     * 
     * @param connectionsSetup the comma-separated list of configurations to configure; example: "A->C, B->C"
     * @throws Throwable on failure
     */
    @Given("^configured network connection[s]? \"([^\"]*)\"$")
    public void givenConfiguredNetworkConnections(String connectionsSetup) throws Throwable {
        printToCommandConsole(StringUtils.format("Configuring network connections (\"%s\")", connectionsSetup));
        // parse the string defining the intended network connections
        Pattern p = Pattern.compile("\\s*(\\w+)->(\\w+)\\s*(?:\\[([\\w,\\s]*)\\])?\\s*");
        for (String connectionSetupPart : connectionsSetup.split(",")) {
            Matcher m = p.matcher(connectionSetupPart);
            if (!m.matches()) {
                fail("Syntax error in connection setup part: " + connectionSetupPart);
            }
            final String clientInstanceId = m.group(1);
            final String serverInstanceId = m.group(2);
            final String connectionOptions = m.group(3);

            final ManagedInstance serverInstance = resolveInstance(serverInstanceId);

            final String ipString = "127.0.0.1";
            Integer serverPort = serverInstance.getServerPort();
            if (serverPort == null) {
                serverPort = portNumberGenerator.incrementAndGet();
                final InstanceConfigurationOperationSequence operationSequence =
                    instanceManagementService.newConfigurationOperationSequence().addServerPort("default", ipString, serverPort);
                instanceManagementService.applyInstanceConfigurationOperations(serverInstanceId, operationSequence,
                    getTextoutReceiverForIMOperations());
                serverInstance.setServerPort(serverPort);
            }
            final String serverPortString = serverPort.toString();
            final String connectionEntryName = serverInstanceId + "_Port" + serverPortString;

            // note: as of release 8.1.0 and before, "cn list" does not output the connection id provided via IM configuration, but
            // "ip:port" for each connection; so this is the string needed to detect the connection's state from the output -- misc_ro
            final String cnListOutputConnectionName = StringUtils.format("%s:%d", ipString, serverPort);

            boolean enableAutoStart = connectionOptions != null && connectionOptions.contains("autoStart");
            // final String autoStartString = Boolean.toString(enableAutoStart);
            final InstanceConfigurationOperationSequence operationSequence =
                instanceManagementService.newConfigurationOperationSequence().addNetworkConnection(
                    connectionEntryName, ipString, serverPort, enableAutoStart, 5, 30, 1.5f);
            instanceManagementService.applyInstanceConfigurationOperations(clientInstanceId, operationSequence,
                getTextoutReceiverForIMOperations());
            if (enableAutoStart) {
                resolveInstance(clientInstanceId).accessConfiguredAutostartConnectionIds().add(cnListOutputConnectionName);
            }
        }
    }

    /**
     * Checks each of the connections configured using {@link #configuredNetworkConnections()} until it is in the CONNECTED state, or the
     * given timeout is reached.
     * 
     * @param maxWaitTimeSeconds the maximum time to wait for connections to be established
     * @throws Throwable on failure
     */
    @Then("^all auto-start network connections should be ready within (\\d+) seconds$")
    public void thenAllAutoStartNetworkConnectionsShouldBeReadyWithinSeconds(int maxWaitTimeSeconds) throws Throwable {
        printToCommandConsole("Waiting for all auto-start network connections to complete");

        final Set<ManagedInstance> pendingInstances = new HashSet<>();
        for (ManagedInstance instance : enabledInstances) {
            if (!instance.accessConfiguredAutostartConnectionIds().isEmpty()) {
                pendingInstances.add(instance);
            }
        }

        final long maximumTimestampForStandardAttempts = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitTimeSeconds);

        // printToCommandConsole("Waiting for " + maxWaitTimeSeconds + " seconds(s)...");
        while (!pendingInstances.isEmpty() && System.currentTimeMillis() <= maximumTimestampForStandardAttempts) {
            for (ManagedInstance instance : detachedIterableCopy(pendingInstances)) {
                boolean success = testIfConfiguredOutgoingConnectionsAreConnected(instance, false);
                if (success) {
                    pendingInstances.remove(instance);
                }
            }
        }

        if (!pendingInstances.isEmpty()) {
            // at least one instance was not successfully validated yet; make one final attempt and log any failures
            for (ManagedInstance instance : detachedIterableCopy(pendingInstances)) {
                boolean success = testIfConfiguredOutgoingConnectionsAreConnected(instance, true); // true = print failure info
                if (success) {
                    pendingInstances.remove(instance);
                }
            }
        }

        if (!pendingInstances.isEmpty()) {
            fail("On " + pendingInstances.size() + " instance(s), the configured outgoing connections "
                + "were not established after waiting/retrying for " + maxWaitTimeSeconds + " second(s)");
        }
    }

    /**
     * Verifies that a given set of instances is present in a given instance's visible network.
     * 
     * @param instanceId the node that should have its visible network inspected
     * @param testType whether to test for "should contain" or "should consist of" the given list of instances
     * @param listOfExpectedVisibleInstances the comma-separated list of expected instances
     * @throws Throwable on failure
     */
    @Then("^the visible network of \"([^\"]*)\" should (consist of|contain) \"([^\"]*)\"$")
    public void thenTheVisibleNetworkOfShouldConsistOf(String instanceId, String testType, String listOfExpectedVisibleInstances)
        throws Throwable {
        List<String> expectedVisibleInstances = parseInstanceList(listOfExpectedVisibleInstances);
        String commandOutput = executeCommandOnInstance(resolveInstance(instanceId), "net info", false);

        // TODO improve: actually parse the output and support "should consist of"
        for (String expectedInstanceId : expectedVisibleInstances) {
            if (!commandOutput.contains(expectedInstanceId)) {
                fail("The visible network of instance " + instanceId + " does not contain the expected instance " + expectedInstanceId);
            }
        }

        printToCommandConsole("Verified the visible network of instance \"" + instanceId + "\"");
        if ("consist of".equals(testType)) {
            printToCommandConsole(
                "  Note: the full 'should consist of' syntax is not implemented yet and was tested as 'should contain' instead");
        }
    }

    /**
     * Executes a console command via IM SSH access on the given instance.
     * 
     * TODO merge with multi-execution step
     * 
     * @param commandString the command string to execute
     * @param instanceId the instance to execute the command on
     * @throws Throwable on failure
     */
    @When("^executing (?:the )?command \"([^\"]*)\" on (?:instance )?\"([^\"]*)\"$")
    public void whenExecutingCommandOnSingleInstance(String commandString, String instanceId) throws Throwable {
        final ManagedInstance instance = resolveInstance(instanceId);
        String commandOutput = executeCommandOnInstance(instance, commandString, true);
        instance.setLastCommandOutput(commandOutput);
        lastInstanceWithSingleCommandExecution = instance;
    }

    /**
     * Executes a console command via IM SSH access on all previously configured instances.
     * 
     * @param commandString the command string to execute
     * @throws Throwable on failure
     */
    @When("^executing command \"([^\"]*)\" on all instances$")
    public void whenExecutingCommandOnAllInstances(String commandString) throws Throwable {
        for (ManagedInstance instance : enabledInstances) {
            executeCommandOnInstance(instance, commandString, true);
        }
        lastInstanceWithSingleCommandExecution = null; // prevent inconsistent data
    }

    /**
     * Verifies the output of the last specific instance a command was executed on via IM SSH.
     * 
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param substring the substring or pattern expected to be present or absent in the command's output
     * @throws Throwable on failure
     */
    @Then("^the (?:last )?output should (not )?contain (the pattern )?\"([^\"]*)\"$")
    public void thenTheLastOutputShouldContain(String negationFlag, String useRegexpMarker, String substring) throws Throwable {
        assertPropertyOfLastCommandOutput(lastInstanceWithSingleCommandExecution, negationFlag, useRegexpMarker, substring);
    }

    /**
     * Verifies the output of the last command executed on the given instance via IM SSH.
     * 
     * @param instanceId the instance of which the last command output should be tested
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param substring the substring or pattern expected to be present or absent in the command's output
     * @throws Throwable on failure
     */
    @Then("^the (?:last )?output of \"([^\"]*)\" should (not )?contain (the pattern )?\"([^\"]*)\"$")
    public void thenTheLastOutputOfInstanceShouldContain(String instanceId, String negationFlag, String useRegexpMarker, String substring)
        throws Throwable {
        ManagedInstance instance = resolveInstance(instanceId);
        assertPropertyOfLastCommandOutput(instance, negationFlag, useRegexpMarker, substring);
    }

    /**
     * Verifies the output of the last command executed on all previously configured instances via IM SSH.
     * 
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param substring the substring or pattern expected to be present or absent in each command's output
     * @throws Throwable on failure
     */
    @Then("^the (?:last )?output of each instance should (not )?contain (the pattern )?\"([^\"]*)\"$")
    public void thenTheLastOutputOfEachInstanceShouldContain(String negationFlag, String useRegexpMarker, String substring)
        throws Throwable {
        for (ManagedInstance instance : enabledInstances) {
            assertPropertyOfLastCommandOutput(instance, negationFlag, useRegexpMarker, substring);
        }
    }

    /**
     * Batch test for presence or absence of component installations and their properties.
     * 
     * TODO this should actually go into a separate step definition class; there should be some refactoring first, though
     * 
     * @param instanceId the instance to query
     * @param componentsTable the expected component data to see (or not see in case of the reserved "absent" marker)
     * @throws Throwable on failure
     */
    @Then("^instance \"([^\"]*)\" should see these components:$")
    public void thenTheVisibleComponentsOfAnInstanceShouldBe(String instanceId, DataTable componentsTable) throws Throwable {
        final ManagedInstance instance = resolveInstance(instanceId);

        Map<String, ComponentVisibilityState> visibilityMap = new HashMap<>();

        // parse expectations
        for (List<String> criteriaRow : componentsTable.cells(0)) {
            String argNodeName = criteriaRow.get(0);
            String argCompName = criteriaRow.get(1);
            String expectedState = criteriaRow.get(2);

            final String mapKey = argNodeName + "/" + argCompName;

            ComponentVisibilityState entry = new ComponentVisibilityState(argCompName, argNodeName, expectedState, null);
            visibilityMap.put(mapKey, entry);

            log.debug("Parsed component expectation: " + entry);
        }

        // parse actual state
        String output = executeCommandOnInstance(instance, "components list --as-table --auth", false);
        // log.debug(output);
        for (String line : output.split("\n")) {
            if (line.startsWith("Finished executing command")) {
                // synthetic final line; not part of the actual output
                continue;
            }
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            String[] lineParts = trimmedLine.split("\\|");
            if (lineParts.length != 6) {
                log.error("Ignoring output line with unexpected number of elements: " + line);
                continue;
            }
            final String componentRefName = lineParts[2];
            final String nodeName = lineParts[0];
            final String actualState = lineParts[5];

            final String mapKey = nodeName + "/" + componentRefName;
            ComponentVisibilityState existing = visibilityMap.get(mapKey);
            if (existing != null) {
                existing.setActualState(actualState);
            }
        }

        boolean hasMismatch = false;
        StringBuilder errorLines = new StringBuilder();
        for (ComponentVisibilityState entry : visibilityMap.values()) {
            if (!entry.stateMatches()) {
                final String errorLine = "  Unexpected component state: " + entry;
                errorLines.append("\n");
                errorLines.append(errorLine);
                printToCommandConsole(errorLine);
                hasMismatch = true;
            }
        }

        if (hasMismatch) {
            fail("At least one component had an unexpected visibility/authorization state: " + errorLines.toString());
        }
    }

    /**
     * Checks the log file(s) of one or more instances for the presence or absence of a given substring.
     * 
     * TODO actually support the parameter variations
     * 
     * @param relativeFilePath the profile-relative path to the file to check, e.g. "debug.log"
     * @param instances either "all instances" or "instance[s] \"<comma-separated list>\""
     * @param notFlag either null or "not " to test for absence of the given substring
     * @param substring the substring to look for in the specified log files
     * @throws Throwable on failure
     */
    @Then("^the ([^\"]+) file[s]? of (all instances) should (not )?contain \"([^\"]+)\"$")
    public void thenTheFilesOfInstancesShouldOrShouldNotContain(String relativeFilePath, String instances, String notFlag,
        String substring) throws Throwable {
        for (ManagedInstance instance : enabledInstances) {
            assertTheProfileRelativeFileOfInstanceContains(instance, relativeFilePath, substring);
        }
    }

    /**
     * Checks the log file(s) of one or more instances for the presence or absence of a given substring.
     * 
     * TODO actually support the parameter variations
     * 
     * @param relativeFilePath the profile-relative path to the file to check, e.g. "debug.log"
     * @param instances either "all instances" or "instance[s] \"<comma-separated list>\""
     * @throws Throwable on failure
     */
    @Then("^the ([^\"]+) file[s]? of (all instances) should be absent or empty$")
    public void thenTheFilesOfInstancesShouldNotExistOrBeEmpty(String relativeFilePath, String instances) throws Throwable {
        for (ManagedInstance instance : enabledInstances) {
            assertTheProfileRelativeFileOfInstanceIsMissingOrEmpty(instance, relativeFilePath);
        }
    }

    /**
     * Convenience shortcut to test all relevant log files for a clean shutdown.
     * 
     * @param instances either "all instances" or "instance[s] \"<comma-separated list>\""
     * @throws Throwable on failure
     */
    @Then("^the log output of (all instances) should indicate a clean shutdown with no unexpected warnings or errors$")
    public void thenTheLogOutputShouldIndicateACleanShutdown(String instances) throws Throwable {
        for (ManagedInstance instance : enabledInstances) {
            // expect empty or absent warnings.log file
            assertTheProfileRelativeFileOfInstanceIsMissingOrEmpty(instance, WARNINGS_LOG_FILE_NAME);
            assertTheProfileRelativeFileOfInstanceContains(instance, DEBUG_LOG_FILE_NAME,
                "Known unfinished operations on shutdown: <none>");
            // note: this will fail on instances below 8.2.0-snapshot
            assertTheProfileRelativeFileOfInstanceContains(instance, DEBUG_LOG_FILE_NAME,
                "Main application shutdown complete, exit code: 0");
        }
    }

    /**
     * Verifies the presence or absence of certain output in the debug.log file of an instance.
     * 
     * @param instanceId the id of the instance to test
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param substring the substring or pattern expected to be present or absent in the command's output
     * @throws Throwable on failure
     */
    @Then("^the log output of \"([^\"]*)\" should (not )?contain (the pattern )?\"([^\"]*)\"$")
    public void thenTheLogOutputShouldOrShouldNotContain(String instanceId, String negationFlag, String useRegexpMarker, String substring)
        throws Throwable {
        ManagedInstance instance = resolveInstance(instanceId);
        final String fileContent = instance.getProfileRelativeFileContent(DEBUG_LOG_FILE_NAME, false);
        if (fileContent == null || fileContent.isEmpty()) {
            fail(
                StringUtils.format("The expected file \"%s\" in profile \"%s\" does not exist or is empty", DEBUG_LOG_FILE_NAME, instance));
        }
        assertPropertyOfTextOutput(instance, negationFlag, useRegexpMarker, substring, fileContent, DEBUG_LOG_FILE_NAME + " content");
    }

    /**
     * Waits for the given number of seconds; usually used to wait for an asynchronous operation to complete.
     * 
     * @param secondsToWait the duration to wait, in seconds
     * @throws Throwable on failure
     */
    // TODO move to generic set of operations
    @When("^waiting for (\\d+) second[s]?$")
    public void whenWaitingForSeconds(int secondsToWait) throws Throwable {
        printToCommandConsole("Waiting for " + secondsToWait + " seconds(s)...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(secondsToWait));
    }

    private void startSingleInstance(final ManagedInstance instance, boolean withGUI) throws IOException {
        instance.onStarting();

        final String installationId = instance.getInstallationId();
        printToCommandConsole(StringUtils.format("Launching instance \"%s\" using installation \"%s\"", instance, installationId));
        instanceManagementService.startInstance(installationId, listOfSingleStringElement(instance.getId()),
            getTextoutReceiverForIMOperations(), IM_OPERATION_TIMEOUT, withGUI);
    }

    private void stopSingleInstance(ManagedInstance instance) throws IOException {
        printToCommandConsole(StringUtils.format("Stopping instance \"%s\"", instance));
        instanceManagementService.stopInstance(listOfSingleStringElement(instance.getId()),
            getTextoutReceiverForIMOperations(), IM_OPERATION_TIMEOUT);

        instance.onStopped();
    }

    private void cycleAllOutgoingConnectionsOf(ManagedInstance instance) {
        final String cnListOutput = executeCommandOnInstance(instance, "cn list", false);
        Pattern connectionIdPattern = Pattern.compile("^\\s*\\((\\d+)\\) ");
        final Matcher matcher = connectionIdPattern.matcher(cnListOutput);
        int count = 0;
        while (matcher.find()) {
            count++;
            final String connectionIndex = matcher.group(1);
            executeCommandOnInstance(instance, "cn stop " + connectionIndex, false);
            executeCommandOnInstance(instance, "cn start " + connectionIndex, false);
        }
        if (count == 0) {
            printToCommandConsole("  WARNING: Attempted to stop and restart all outgoing connections of " + instance.getId()
                + ", but no connections were found");
        } else {
            printToCommandConsole("  Stopped and restarted " + count + " outgoing connection(s) of " + instance.getId());
        }
    }

    private void executeRunnablesGroupAndHandlePotentialErrors(final RunnablesGroup runnablesGroup, String singleTaskDescription) {
        final List<RuntimeException> exceptions = runnablesGroup.executeParallel();
        boolean hasFailure = false;
        for (RuntimeException e : exceptions) {
            if (e != null) {
                log.warn("Exception while asynchronously " + singleTaskDescription, e);
                hasFailure = true;
            }
        }
        if (hasFailure) {
            // rethrow an arbitrary one
            for (RuntimeException e : exceptions) {
                if (e != null) {
                    throw e;
                }
            }
        }
    }

    private void assertPropertyOfLastCommandOutput(ManagedInstance instance, String negationFlag, String useRegexpMarker,
        String substring) {
        assertPropertyOfTextOutput(instance, negationFlag, useRegexpMarker, substring, instance.getLastCommandOutput(), "command output");
    }

    private void assertTheProfileRelativeFileOfInstanceContains(ManagedInstance instance, String relativeFilePath, String substring)
        throws IOException {
        final String fileContent = instance.getProfileRelativeFileContent(relativeFilePath, false);

        if (fileContent == null) {
            fail(StringUtils.format("The expected file \"%s\" in profile \"%s\" does not exist", relativeFilePath, instance,
                substring));
        } else {
            if (!fileContent.contains(substring)) {
                fail(StringUtils.format("The content of the file \"%s\" in profile \"%s\" did not contain \"%s\"", relativeFilePath,
                    instance, substring));
            } else {
                printToCommandConsole(
                    StringUtils.format("  The file \"%s\" in profile \"%s\" contained the expected text \"%s\"", relativeFilePath, instance,
                        substring));
            }
        }
    }

    private void assertTheProfileRelativeFileOfInstanceIsMissingOrEmpty(ManagedInstance instance, String relativeFilePath)
        throws IOException {
        final String fileContent = instance.getProfileRelativeFileContent(relativeFilePath, false);

        if (fileContent == null) {
            printToCommandConsole(
                StringUtils.format("  The file \"%s\" in profile \"%s\" is absent as expected", relativeFilePath, instance));
        } else {
            if (fileContent.isEmpty()) {
                printToCommandConsole(
                    StringUtils.format("  The file \"%s\" in profile \"%s\" is empty as expected", relativeFilePath, instance));
            } else {
                fail(StringUtils.format(
                    "The file \"%s\" in profile \"%s\" should have been absent or empty, but exists "
                        + "(content size: %d characters); full file content:\n%s",
                    relativeFilePath, instance, fileContent.length(), fileContent));
            }
        }
    }

    private boolean testIfConfiguredOutgoingConnectionsAreConnected(final ManagedInstance instance, boolean isFinalAttempt) {
        final List<String> connectionIds = instance.accessConfiguredAutostartConnectionIds();

        String commandOutput = executeCommandOnInstance(instance, "cn list", false);
        int matches = 0;
        for (String connectionId : connectionIds) {
            Matcher matcher = Pattern.compile("'" + connectionId + "'.*?- (\\w+)").matcher(commandOutput);
            if (!matcher.find()) {
                if (isFinalAttempt) {
                    fail(StringUtils.format(
                        "Unexpected state: Attempted to verify the state of connection \"%s\" on \"%s\", "
                            + "but it did not appear in the output of \"cn list\" at all; full command output:\n%s",
                        connectionId, instance, commandOutput));
                } else {
                    continue; // most likely, the tested instance has simply not registered the connection(s) yet, so just retry
                }
            }
            String state = matcher.group(1);
            if (matcher.find()) {
                fail(StringUtils.format(
                    "Unexpected state: Found more than one entry for connection \"%s\" on \"%s\" "
                        + "in the output of \"cn list\"; full command output:\n%s",
                    connectionId, instance, commandOutput));
            }
            if ("CONNECTED".equals(state)) {
                matches++;
            } else {
                if (isFinalAttempt) {
                    printToCommandConsole(StringUtils.format(
                        "Failed expectation: the connection \"%s\" on \"%s\" should be in state \"CONNECTED\", but is \"%s\"",
                        connectionId, instance, state));
                }
            }
        }
        boolean success = matches == connectionIds.size();
        return success;
    }

    private void tearDownLeftoverRunningInstances() {
        for (ManagedInstance instance : enabledInstances) {
            final String instanceId = instance.getId();
            try {
                if (instanceManagementService.isInstanceRunning(instanceId)) {
                    printToCommandConsole(StringUtils.format(
                        "Stopping instance \"%s\" after test scenario \"%s\"", instanceId,
                        executionContext.getScenarioName()));
                    instanceManagementService.stopInstance(listOfSingleStringElement(instanceId),
                        getTextoutReceiverForIMOperations(), IM_OPERATION_TIMEOUT);
                }
            } catch (IOException e) {
                printToCommandConsole("Error shutting down instance " + instanceId + ": " + e.toString());
            }
        }

        // verify shutdown
        for (ManagedInstance instance : enabledInstances) {
            final String instanceId = instance.getId();
            try {
                if (instanceManagementService.isInstanceRunning(instanceId)) {
                    fail(StringUtils.format(
                        "Instance \"%s\" is still detected as \"running\" after the post-test shutdown for scenario \"%s\"", instanceId,
                        executionContext.getScenarioName()));
                }
            } catch (IOException e) {
                printToCommandConsole("Error verifying shutdown state of instance " + instanceId + ": " + e.toString());
            }
        }
    }

    private List<String> listOfSingleStringElement(String element) {
        List<String> singleInstanceList = new ArrayList<>();
        singleInstanceList.add(element);
        return singleInstanceList;
    }

    private String deriveImplicitInstallationIdFromBuildId(String buildId) {
        return buildId.replaceAll("[^\\w]", "_");
    }

    private PrefixingTextOutForwarder getTextoutReceiverForIMOperations() {
        return new PrefixingTextOutForwarder("  (IM output) ", outputReceiver);
    }

    private ManagedInstance[] detachedIterableCopy(final Collection<ManagedInstance> pendingInstances) {
        return pendingInstances.toArray(EMPTY_INSTANCE_ARRAY);
    }

}
