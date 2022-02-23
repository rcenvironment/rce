/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import cucumber.api.java.en.When;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;

/**
 * Step definitons for executing commands on instances.
 * 
 * @author Marlon Schroeter
 */
public class InstanceCommandStepDefinitions extends InstanceManagementStepDefinitionBase {

    public InstanceCommandStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Class implementing InstanceAction of executing commands on one instance.
     * 
     * @author Marlon Schroeter
     */
    private class ExecuteCommandOnInstanceAction implements InstanceAction {

        private List<String> commands;

        private boolean mainAction;

        ExecuteCommandOnInstanceAction(List<String> commands, boolean mainAction) {
            this.commands = commands;
            this.mainAction = mainAction;
        }

        @Override
        public void performActionOnInstance(ManagedInstance instance, long timeout) throws IOException {
            for (String command : commands) {
                String commandOutput = executeCommandOnInstance(instance, command, mainAction);
                instance.setLastCommandOutput(commandOutput);
            }
            executionContext.setLastInstanceWithSingleCommandExecution(instance);
        }

    }

    /**
     * Executes one on more commands on one or more instances.
     * 
     * @param commandList comma separated list of commands.
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param executionDesc a string indicating the mode in which the given commands are executed on the instances. This does not refer to
     *        the order or concurrence of the commands, but the order or concurrence of the instances on which the commands are executed.
     *        The commands are executed in the given ordering. Can be choosen from "in the given order","concurrently","in a random order". 
     *        If null sequentially is the default.
     */
    @When("^executing(?: the)? command[s]? \"([^\"]*)\" on( all)?(?: instance[s])?(?: \"([^\"]*)\")?"
        + "(?: (in the given order|concurrently|in a random order))?$")
    public void whenExecutingCommandOnInstances(String commandList, String allFlag, String instanceIds, String executionDesc) {
        performActionOnInstances(
            new ExecuteCommandOnInstanceAction(parseCommaSeparatedList(commandList), true),
            resolveInstanceList(allFlag != null, instanceIds),
            resolveExecutionMode(executionDesc, InstanceActionExecutionType.RANDOM));
    }

    /**
     * Executes command order on top layer UI screen.
     * 
     * @param operations comma-separated list of operations to execute
     */
    @When("^executing command order \"([^\"]*)\"(?: on(?: the)? top layer(?: UI)?)?$")
    public void whenClosingConfigureUIAfterStartUp(String operations) {

        try {
            final Robot robot = new Robot();
            List<Integer> keys = new LinkedList<Integer>();
            List<String> commands = parseCommaSeparatedList(operations);
            for (String command : commands) {
                switch (command) {
                case ("up"):
                    keys.add(KeyEvent.VK_UP);
                    break;
                case ("left"):
                    keys.add(KeyEvent.VK_LEFT);
                    break;
                case ("right"):
                    keys.add(KeyEvent.VK_RIGHT);
                    break;
                case ("down"):
                    keys.add(KeyEvent.VK_DOWN);
                    break;
                case ("enter"):
                    keys.add(KeyEvent.VK_ENTER);
                    break;
                default:
                    fail(StringUtils.format("Command %s is not a valid execution command.", command));
                }
            }

            performKeyboardActions(robot, keys);

        } catch (AWTException e) {
            fail("Error attempting to execute commands");
        }
    }

    private void performKeyboardActions(Robot robot, List<Integer> keys) {
        keys.forEach((key) -> {
            robot.keyPress(key);
            robot.keyRelease(key);
        });
    }

}
