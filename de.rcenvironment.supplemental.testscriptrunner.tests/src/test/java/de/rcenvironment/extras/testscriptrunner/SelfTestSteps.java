/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner;

import static org.junit.Assert.assertEquals;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import de.rcenvironment.extras.testscriptrunner.definitions.common.AbstractStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;

/**
 * Step definitions for self-testing the TestScriptRunner plugin backend.
 *
 * @author Robert Mischke
 */
public class SelfTestSteps extends AbstractStepDefinitionBase {

    private boolean testFlag;

    public SelfTestSteps(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Dummy/test command.
     * 
     * @throws Throwable on failure
     */
    @Given("^the initial state$")
    public void theInitialState() throws Throwable {}

    /**
     * Dummy/test command.
     * 
     * @throws Throwable on failure
     */
    @Given("^the flag was initialized$")
    public void theFlagWasInitialized() throws Throwable {
        testFlag = true;
    }

    /**
     * Dummy/test command.
     * 
     * @param value test parameter
     * @throws Throwable on failure
     */
    @Then("^the test flag should be (.*)$")
    public void theTestFlagShouldBeX(String value) throws Throwable {
        assertEquals(Boolean.parseBoolean(value), testFlag);
    }

    /**
     * Verifies that the build id is actually forwarded to step definitions.
     * 
     * @param expectedBuildId the build id provided by the test script
     * @throws Throwable on failure
     */
    @Then("^the default build id should be \"([^\"]*)\"$")
    public void theDefaultBuildIdShouldBe(String expectedBuildId) throws Throwable {
        assertEquals(expectedBuildId, executionContext.getBuildUnderTestId());
    }

}
