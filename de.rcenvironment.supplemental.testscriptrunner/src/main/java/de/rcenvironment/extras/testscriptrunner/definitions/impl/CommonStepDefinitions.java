/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import java.util.concurrent.TimeUnit;

import cucumber.api.java.en.When;
import de.rcenvironment.extras.testscriptrunner.definitions.common.AbstractStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;

/**
 * collection of common step definitions.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke (based on code from)
 */
public class CommonStepDefinitions extends AbstractStepDefinitionBase {

    public CommonStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Provides a wait command.
     * 
     * @param seconds the number of seconds to wait; may be null (but not negative)
     * @throws InterruptedException on thread interruption
     */
    @When("^[Ww]aiting for (\\d+) second[s]?$")
    public void whenWaiting(int seconds) throws InterruptedException {
        // 0 should be allowed for optional waits
        if (seconds < 0) {
            throw new IllegalArgumentException("Invalid wait time: " + seconds);
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    }
}
