/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * Definitions of test life-cycle hooks.
 * 
 * @author Robert Mischke
 */
public class RceTestLifeCycleHooks extends InstanceManagementStepDefinitionBase {

    public RceTestLifeCycleHooks(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Common before-scenario hook.
     * 
     * @param scenario the {@link Scenario} object
     */
    @Before
    public void before(Scenario scenario) {
        initialize();
        executionContext.beforeEach(scenario);
    }

    /**
     * Common after-scenario hook.
     * 
     * @param scenario the {@link Scenario} object
     */
    @After
    public void after(Scenario scenario) {
        tearDownLeftoverRunningInstances();
        executionContext.afterEach(scenario);
    }
    
    
    private void initialize() {
        assertTrue(executionContext.getInstancesById().isEmpty());
        assertTrue(executionContext.getEnabledInstances().isEmpty());
    }

    private void tearDownLeftoverRunningInstances() {
        for (ManagedInstance instance : executionContext.getEnabledInstances()) {
            final String instanceId = instance.getId();
            try {
                if (INSTANCE_MANAGEMENT_SERVICE.isInstanceRunning(instanceId)) {
                    printToCommandConsole(StringUtils.format(
                        "Stopping instance \"%s\" after test scenario \"%s\"", instanceId,
                        executionContext.getScenarioName()));
                    INSTANCE_MANAGEMENT_SERVICE.stopInstance(listOfSingleStringElement(instanceId),
                        getTextoutReceiverForIMOperations(), TimeUnit.SECONDS.toMillis(StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS));
                }
            } catch (IOException e) {
                printToCommandConsole("Error shutting down instance " + instanceId + ": " + e.toString());
            }
        }

        // verify shutdown
        for (ManagedInstance instance : executionContext.getEnabledInstances()) {
            final String instanceId = instance.getId();
            try {
                if (INSTANCE_MANAGEMENT_SERVICE.isInstanceRunning(instanceId)) {
                    fail(StringUtils.format(
                        "Instance \"%s\" is still detected as \"running\" after the post-test shutdown for scenario \"%s\"", instanceId,
                        executionContext.getScenarioName()));
                }
            } catch (IOException e) {
                printToCommandConsole("Error verifying shutdown state of instance " + instanceId + ": " + e.toString());
            }
        }
    }
}
