/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import de.rcenvironment.extras.testscriptrunner.internal.TestScenarioExecutionContext;

/**
 * Definitions of test life-cycle hooks.
 * 
 * @author Robert Mischke
 */
public class RceTestLifeCycleHooks {

    private final TestScenarioExecutionContext scenarioContext;

    public RceTestLifeCycleHooks(TestScenarioExecutionContext context) {
        this.scenarioContext = context;
    }

    /**
     * Common before-scenario hook.
     * 
     * @param scenario the {@link Scenario} object
     */
    @Before
    public void before(Scenario scenario) {
        scenarioContext.beforeEach(scenario);
    }

    /**
     * Common after-scenario hook.
     * 
     * @param scenario the {@link Scenario} object
     */
    @After
    public void after(Scenario scenario) {
        scenarioContext.afterEach(scenario);
    }

}
