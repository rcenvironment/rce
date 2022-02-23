/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * Step definitions altering the state of an instance (e.g starting/stopping/...).
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke (based on code from)
 */
public class InstanceStateStepDefinitions extends InstanceManagementStepDefinitionBase {

    public InstanceStateStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
        // TODO Auto-generated constructor stub
    }

    /**
     * Class implementing InstanceIterator for asserting state of instance.
     * 
     * @author Marlon Schroeter
     */
    private class AssertStateIterator implements InstanceIterator {

        // can be changed to String or enum, when more states than running/not running are to be asseted
        private boolean shouldBeRunning;

        AssertStateIterator(boolean shouldBeRunning) {
            this.shouldBeRunning = shouldBeRunning;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws Exception {
            boolean isRunning = INSTANCE_MANAGEMENT_SERVICE.isInstanceRunning(instance.getId());
            if (isRunning != shouldBeRunning) {
                throw new AssertionError(StringUtils.format(
                    "Instance state did not match expectation: Instance \"%s\" was in "
                        + "running state [%s] when it should have been [%s]",
                    instance, isRunning, shouldBeRunning));
            }
        }

    }

    /**
     * Class implementing InstanceAction of starting instance.
     * 
     * @author Marlon Schroeter
     */
    private class StartInstanceAction implements InstanceAction {

        private boolean startWithGUI;

        private String startWithCommands;

        StartInstanceAction(boolean startWithGUI) {
            this.startWithGUI = startWithGUI;
            this.startWithCommands = "";
        }

        StartInstanceAction(boolean startWithGUI, String startWithCommands) {
            this.startWithGUI = startWithGUI;
            this.startWithCommands = startWithCommands;
        }

        @Override
        public void performActionOnInstance(ManagedInstance instance, long timeout) throws IOException {
            startSingleInstance(instance, startWithGUI, startWithCommands, timeout);
        }

    }

    /**
     * Class implementing InstanceAction of stopping one instance.
     * 
     * @author Marlon Schroeter
     */
    private class StopInstanceAction implements InstanceAction {

        @Override
        public void performActionOnInstance(ManagedInstance instance, long timeout) throws IOException {
            stopSingleInstance(instance);
        }

    }

    /**
     * Launches instances with the associated installation; will fail if no installation has been associated with one of the given
     * instances. It is not an error to start an already-runnning instance.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param executionDesc a string indicating the mode in which the instances can be started. Can be choosen from "in the given
     *        order","concurrently","sequentially". If null sequentially is the default.
     * @param startWithGuiFlag a phrase that is present (non-null) if the instances should be started with GUIs
     * @param commandArguments console arguments that are used to start the instance.
     */
    @When("^starting( all)? instance[s]?(?: \"([^\"]*)\")?(?: (in the given order|concurrently|in a random order))?"
        + "( in GUI mode)?(?: with console command[s]? (-{1,2}.+))?$")
    public void whenStartingInstances(String allFlag, String instanceIds, String executionDesc, String startWithGuiFlag,
        String commandArguments) {
        StartInstanceAction startInstanceAction;
        if (startWithGuiFlag == null) {
            startInstanceAction = new StartInstanceAction(startWithGuiFlag != null);
        } else {
            startInstanceAction = new StartInstanceAction(startWithGuiFlag != null, commandArguments);
            log.debug(commandArguments);
        }

        performActionOnInstances(
            startInstanceAction,
            resolveInstanceList(allFlag != null, instanceIds),
            resolveExecutionMode(executionDesc, InstanceActionExecutionType.RANDOM));
    }

    /**
     * Stops (shuts down) instances. It is not an error to stop an already-stopped instance.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param executionDesc a string indicating the mode in which the instances are stopped. Can be choosen from "in the given
     *        order","concurrently","sequentially". If null sequentially is the default.
     */
    @When("^stopping( all)? instance[s]?(?: \"([^\"]*)\")?(?: (in the given order|concurrently|in a random order))?$")
    public void whenStoppingInstances(String allFlag, String instanceIds, String executionDesc) {
        performActionOnInstances(
            new StopInstanceAction(),
            resolveInstanceList(allFlag != null, instanceIds),
            resolveExecutionMode(executionDesc, InstanceActionExecutionType.RANDOM));
    }

    /**
     * Schedules a state change event for a single instance; intended for tests where instances start/stop concurrently to other (blocking)
     * test steps. May be reworked to a multi-instance step/task in the future.
     * 
     * @param action a phrase indicating what should happen with the instance; currently supported: "shutdown", "restart"
     * @param instanceId the id of the instance to modify
     * @param delaySeconds the delay, in seconds, after which the action should be performed
     */
    @When("^scheduling (?:a|an instance) (shutdown|restart|reconnect) of \"([^\"]+)\" after (\\d+) second[s]?$")
    public void whenSchedulingNodeActionsAfterDelay(final String action, final String instanceId, final int delaySeconds)
        {

        // TODO ensure proper integration with test cleanup
        // TODO as this is the first asynchronous test action, check thread safety
        // TODO support "restart" action as well

        final ManagedInstance instance = resolveInstance(instanceId);
        final String taskDesc = StringUtils.format("delayed %s of instance %s", action, instanceId);
        ConcurrencyUtils.getAsyncTaskService().scheduleAfterDelay(taskDesc, new Runnable() {

            @Override
            public void run() {
                try {
                    switch (action) {
                    case "shutdown":
                        stopSingleInstance(instance);
                        break;
                    case "restart":
                     // assuming headless mode here
                        stopSingleInstance(instance);
                        startSingleInstance(instance, false, "", StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS); 
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
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param state the expected state (stopped/running)
     */
    @Then("^(all )?(?:instance[s]? )?(?:\"([^\"]*)\" )?should be (stopped|running)$")
    public void thenInstancesShouldBeInState(String allFlag, String instanceIds, String state) throws Exception {
        AssertStateIterator assertStateIterator = new AssertStateIterator("running".equals(state));
        iterateInstances(assertStateIterator, allFlag, instanceIds);
        printToCommandConsole("Verified the status \"" + state + "\" of instance \"" + instanceIds + "\"");
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

    private void startSingleInstance(final ManagedInstance instance, boolean withGUI, String commandArguments, long timeout)
        throws IOException {
        instance.onStarting();

        final String installationId = instance.getInstallationId();
        printToCommandConsole(StringUtils.format("Launching instance \"%s\" using installation \"%s\"", instance, installationId));
        INSTANCE_MANAGEMENT_SERVICE.startInstance(installationId, listOfSingleStringElement(instance.getId()),
            getTextoutReceiverForIMOperations(), timeout, withGUI, commandArguments);
    }

    private void stopSingleInstance(ManagedInstance instance) throws IOException {
        printToCommandConsole(StringUtils.format("Stopping instance \"%s\"", instance));
        INSTANCE_MANAGEMENT_SERVICE.stopInstance(listOfSingleStringElement(instance.getId()),
            getTextoutReceiverForIMOperations(), TimeUnit.SECONDS.toMillis(StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS));

        instance.onStopped();
    }

}
