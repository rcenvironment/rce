/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.annotations.Inject;

import com.jcraft.jsch.JSchException;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.instancemanagement.InstanceConfigurationOperationSequence;
import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.instancemanagement.InstanceManagementService.InstallationPolicy;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.extras.testscriptrunner.internal.TestScenarioExecutionContext;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;

/**
 * Steps controlling and querying the RCE "instance management", which is used to provision and run test instances using configured RCE
 * installations.
 *
 * @author Robert Mischke
 */
public class InstanceManagementStepDefinitions {

    private static final int IM_OPERATION_TIMEOUT = 30000;

    private static final ManagedInstance[] EMPTY_INSTANCE_ARRAY = new ManagedInstance[0];

    // injected once via OSGi bind method
    private static InstanceManagementService instanceManagementService;

    // injected by test framework
    @Inject
    private TestScenarioExecutionContext executionContext;

    private TextOutputReceiver outputReceiver;

    private final AtomicInteger portNumberGenerator = new AtomicInteger(52100);

    private ManagedInstance lastInstanceWithSingleCommandExecution; // TODO check: actually helpful/necessary?

    // note: only supposed to be accessed from the main thread, so no synchronization is performed or necessary
    private final Map<String, ManagedInstance> instancesById = new HashMap<>();

    // note: only supposed to be accessed from the main thread, so no synchronization is performed or necessary
    private final Collection<ManagedInstance> enabledInstances = new HashSet<>();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Represents an instance (ie, a profile) managed by these test steps.
     * 
     * @author Robert Mischke
     */
    private final class ManagedInstance {

        private final String id; // the id of the instance/profile

        private String installationId; // the id of the installation to run this instance/profile with

        private Integer serverPort; // currently only supporting one server port; could be changed later

        private final List<String> configuredAutostartConnectionIds = new ArrayList<>();

        private String lastCommandOutput;

        // cache to avoid I/O on multiple check operations; only used while instance is stopped, and reset on startup
        private Map<String, String> cachedFileContent = new HashMap<>();

        private boolean potentiallyRunning;

        private ManagedInstance(String instanceId, String installationId) {
            this.id = instanceId;
            this.installationId = installationId;
        }

        @Override
        public String toString() {
            return getId();
        }

        public String getId() {
            return id; // immutable
        }

        public synchronized String getInstallationId() {
            return installationId;
        }

        @SuppressWarnings("unused") // for potential future use
        public synchronized void setInstallationId(String installationId) {
            this.installationId = installationId;
        }

        public synchronized Integer getServerPort() {
            return serverPort;
        }

        public synchronized void setServerPort(Integer serverPort) {
            this.serverPort = serverPort;
        }

        /**
         * @return the internal mutable list; not a copy!
         */
        public List<String> accessConfiguredAutostartConnectionIds() {
            return configuredAutostartConnectionIds; // the list reference itself is immutable
        }

        public synchronized String getLastCommandOutput() {
            return lastCommandOutput;
        }

        public synchronized void setLastCommandOutput(String lastCommandOutput) {
            this.lastCommandOutput = lastCommandOutput;
        }

        public synchronized String getProfileRelativeFileContent(String relativePath, boolean forceReload) throws IOException {
            if (!potentiallyRunning) {
                if (cachedFileContent.containsKey(relativePath)) {
                    return cachedFileContent.get(relativePath); // may be null if file does not exist
                }
            } else {
                log.warn("Requested file " + relativePath + " of running instance " + id + "; not using I/O cache");
            }

            final File fileLocation = instanceManagementService.resolveRelativePathWithinProfileDirectory(id, relativePath);
            final String content;
            if (!fileLocation.exists()) {
                content = null;
            } else {
                content = FileUtils.readFileToString(fileLocation, "UTF-8"); // no other information available; assume UTF8
            }

            if (!potentiallyRunning) {
                cachedFileContent.put(relativePath, content); // content may be null if file is missing
            }
            return content;
        }

        public synchronized void onStarting() {
            potentiallyRunning = true;
            cachedFileContent.clear();
        }

        public synchronized void onStopped() {
            potentiallyRunning = false;
        }

        @SuppressWarnings("unused") // for future use
        public synchronized boolean getPotentiallyRunning() {
            return potentiallyRunning;
        }
    }

    /**
     * OSGi bind method.
     * 
     * @param newService the new service instance
     */
    public void bindInstanceManagementService(InstanceManagementService newService) {
        instanceManagementService = newService;
    }

    /**
     * Pre-scenario setup code.
     */
    @Before
    public void initialize() {
        outputReceiver = executionContext.getOutputReceiver();
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
     * @param buildOrInstallationId the URL part (e.g. "snapshots/trunk") defining the build to use, or a symbolic installation id (e.g.
     *        ":self" or "local:...")
     * @throws Throwable on failure
     */
    @Given("^(?:the )?(running )?instance[s]? \"([^\"]*)\" using (?:the default build|build \"([^\"]*)\")$")
    public void givenInstancesUsingBuild(String autoStartPhrase, String instanceList, String buildOrInstallationId) throws Throwable {

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

        final List<String> instanceIdList = parseInstanceList(instanceList);
        for (String instanceId : instanceIdList) {
            final ManagedInstance instance = new ManagedInstance(instanceId, installationId);
            instancesById.put(instanceId, instance);
            enabledInstances.add(instance);
            printToCommandConsole(StringUtils.format("Configuring test instance \"%s\"", instanceId));

            final int imSshPortNumber = portNumberGenerator.incrementAndGet(); // TODO check whether that port is actually free
            final InstanceConfigurationOperationSequence operationSequence = instanceManagementService.newConfigurationOperationSequence()
                .resetConfiguration().enableImSshAccess(imSshPortNumber).setName(instanceId);
            instanceManagementService.applyInstanceConfigurationOperations(instanceId, operationSequence, imOperationOutputReceiver);
        }
        printToCommandConsole(StringUtils.format("Auto-starting instance(s) \"%s\"", instanceList));
        if (autoStartPhrase != null) {
            instanceManagementService.startInstance(installationId, instanceIdList, imOperationOutputReceiver,
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
            final String connectionEntryName = serverInstanceId + "-" + serverPortString;

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
        String commandOutput = executeCommandOnInstanceInternal(resolveInstance(instanceId), "net info");

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
        String commandOutput = executeCommandOnInstanceInternal(instance, commandString);
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
            executeCommandOnInstanceInternal(instance, commandString);
        }
        lastInstanceWithSingleCommandExecution = null; // prevent inconsistent data
    }

    /**
     * Verifies the output of the last specific instance a command was executed on via IM SSH.
     * 
     * @param substring the substring expected in the command's output
     * @throws Throwable on failure
     */
    @Then("^the (?:last )?output should contain \"([^\"]*)\"$")
    public void thenTheLastOutputShouldContain(String substring) throws Throwable {
        assertTheLastCommandOutputOfInstanceContains(lastInstanceWithSingleCommandExecution, substring);
    }

    /**
     * Verifies the output of the last command executed on the given instance via IM SSH.
     * 
     * @param instanceId the instance of which the last command output should be tested
     * @param substring the substring expected in the command's output
     * @throws Throwable on failure
     */
    @Then("^the (?:last )?output of \"([^\"]*)\" should contain \"([^\"]*)\"$")
    public void thenTheLastOutputOfInstanceShouldContain(String instanceId, String substring) throws Throwable {
        ManagedInstance instance = resolveInstance(instanceId);
        assertTheLastCommandOutputOfInstanceContains(instance, substring);
    }

    /**
     * Verifies the output of the last command executed on all previously configured instances via IM SSH.
     * 
     * @param substring the substring expected in each command's output
     * @throws Throwable on failure
     */
    @Then("^the (?:last )?output of each instance should contain \"([^\"]*)\"$")
    public void thenTheLastOutputOfEachInstanceShouldContain(String substring) throws Throwable {
        for (ManagedInstance instance : enabledInstances) {
            assertTheLastCommandOutputOfInstanceContains(instance, substring);
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
            assertTheProfileRelativeFileOfInstanceIsMissingOrEmpty(instance, "warnings.log");
            assertTheProfileRelativeFileOfInstanceContains(instance, "debug.log", "Known unfinished operations on shutdown: <none>");
            // note: this will fail on instances below 8.2.0-snapshot
            assertTheProfileRelativeFileOfInstanceContains(instance, "debug.log", "Main application shutdown complete, exit code: 0");
        }
    }

    /**
     * Waits for the given number of seconds; usually used to wait for an asynchronous operation to complete.
     * 
     * @param secondsToWait the duration to wait, in seconds
     * @throws Throwable on failure
     */
    // TODO move to generic set of operations
    @When("^waiting for (\\d+) seconds$")
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

    private String executeCommandOnInstanceInternal(final ManagedInstance instance, String commandString) {
        final String instanceId = instance.getId();
        final String startInfoText = StringUtils.format("Executing command \"%s\" on instance \"%s\"", commandString, instanceId);
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

    private void assertTheLastCommandOutputOfInstanceContains(ManagedInstance instance, String substring) {
        final String output = instance.getLastCommandOutput();
        if (!output.contains(substring)) {
            fail(StringUtils.format("The output of instance \"%s\" did not contain \"%s\"; full output:\n-----\n%s\n-----", instance,
                substring, output.trim()));
        }
        printToCommandConsole(
            StringUtils.format("  Command output of instance \"%s\" contained expected text \"%s\"", instance, substring));
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

        String commandOutput = executeCommandOnInstanceInternal(instance, "cn list");
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

    private void printToCommandConsole(String text) {
        outputReceiver.addOutput(text);
    }

    private PrefixingTextOutForwarder getTextoutReceiverForIMOperations() {
        return new PrefixingTextOutForwarder("  (IM output) ", outputReceiver);
    }

    private ManagedInstance resolveInstance(String instanceId) {
        return instancesById.get(instanceId);
    }

    private List<String> parseInstanceList(String instanceList) {
        return Arrays.asList(instanceList.trim().split("\\s*,\\s*"));
    }

    private ManagedInstance[] detachedIterableCopy(final Collection<ManagedInstance> pendingInstances) {
        return pendingInstances.toArray(EMPTY_INSTANCE_ARRAY);
    }

}
