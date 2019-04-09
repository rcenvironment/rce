/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;

/**
 * Definitions of test life-cycle hooks.
 * 
 * @author Robert Mischke
 */
public class RceTestLifeCycleHooks extends AbstractStepDefinitionBase {

    public RceTestLifeCycleHooks(TestScenarioExecutionContext context) {
        super(context);
    }

    /**
     * Common before-scenario hook.
     * 
     * @param scenario the {@link Scenario} object
     */
    @Before
    public void before(Scenario scenario) {
        executionContext.beforeEach(scenario);
    }

    /**
     * Common after-scenario hook.
     * 
     * @param scenario the {@link Scenario} object
     */
    @After
    public void after(Scenario scenario) {
        executionContext.afterEach(scenario);
    }

}
