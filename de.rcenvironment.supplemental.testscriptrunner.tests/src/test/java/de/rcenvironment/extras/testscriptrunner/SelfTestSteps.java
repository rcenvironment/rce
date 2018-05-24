/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner;

import static org.junit.Assert.assertEquals;

import org.picocontainer.annotations.Inject;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import de.rcenvironment.extras.testscriptrunner.internal.TestScenarioExecutionContext;

/**
 * Step definitions for self-testing the TestScriptRunner plugin backend.
 *
 * @author Robert Mischke
 */
public class SelfTestSteps {

    private boolean testFlag;

    @Inject
    // injected by test framework
    private TestScenarioExecutionContext executionContext;

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
