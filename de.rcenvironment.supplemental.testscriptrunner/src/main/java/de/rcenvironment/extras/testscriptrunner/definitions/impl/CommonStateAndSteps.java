/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.ExecutionResult;
import de.rcenvironment.extras.testscriptrunner.common.CommonUtils;

/**
 * Common steps and life cycle hooks for all application contexts, and a holder for common state like the an execution's output.
 * 
 * @author Robert Mischke
 */
// TODO several steps need to be migrated to the current multi-instance test approach -- misc_ro
public class CommonStateAndSteps {

    private static CommonStateAndSteps current;

    private ExecutionResult currentExecutionResult;

    private Map<String, String> substitutionMap;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * StdOut/StdErr selector.
     * 
     * @author Robert Mischke
     */
    private enum OutputType {
        STANDARD,
        ERROR;
    }

    public CommonStateAndSteps() {
        current = this;
    }

    public static CommonStateAndSteps getCurrent() {
        return current;
    }

    public void setParameterSubstitutionMap(Map<String, String> newSubstitutionMap) {
        this.substitutionMap = newSubstitutionMap;
    }

    public void setCurrentExecutionResult(ExecutionResult lastExecutionResult) {
        this.currentExecutionResult = lastExecutionResult;
    }



    /**
     * Provides a wait command.
     * 
     * @param seconds the number of seconds to wait; may be null (but not negative)
     * @throws InterruptedException on thread interruption
     */
    @When("^[Ww]aiting for (\\d+) seconds")
    public void wait(int seconds) throws InterruptedException {
        // 0 should be allowed for optional waits
        if (seconds < 0) {
            throw new IllegalArgumentException("Invalid wait time: " + seconds);
        }
        log.info("Waiting for " + seconds + " seconds");
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    }

    /**
     * After running some sort of execution, this method verifies that the exit code was the given value.
     * 
     * @param expected the expected exit code
     * @throws Throwable on failure
     */
    @Then("^the exit code should be (\\d+)$")
    public void assertExitCodeIs(int expected) throws Throwable {
        int actual = currentExecutionResult.exitCode;
        Assert.assertEquals("Wrong exit code", expected, actual);
    }

    /**
     * After running some sort of execution, this method verifies that the standard or error output consists of the given number of text
     * lines.
     * 
     * @param outputType the output to check: should be either "standard" or "error"
     * @param expectedLines the expected number of text lines
     * @throws Throwable on failure
     */
    @Then("^the (standard |error |)output should be (\\d+) lines? long$")
    public void assertLineCountOfSelectedOutputIs(String outputType, int expectedLines) throws Throwable {
        final List<String> output;
        output = getSelectedFilteredOutput(outputType);
        Assert.assertEquals("Wrong line count on " + outputType + " output", expectedLines, output.size());
    }

    /**
     * After running some sort of execution, this method verifies that the standard or error output contains the given string (as part of a
     * single line).
     * 
     * @param outputType the output to check: should be either "standard" or "error"
     * @param sign should be either "should" or "should not"
     * @param content the expected or unexpected substring of an output line
     * @throws Throwable on failure
     */
    @Then("^the (standard |error |)output (should |should not |)contain \"(.*?)\"$")
    public void assertSelectedOutputContains(String outputType, String sign, String content) throws Throwable {

        if (substitutionMap != null) {
            content = CommonUtils.substitute(content, substitutionMap);
        }
        final List<String> output;
        output = getSelectedFilteredOutput(outputType.trim());
        
        // check if the output contains the given content
        boolean contains = false;
        for (String line : output) {
            if (line.contains(content)) {
                contains = true;
                break;
            }
        }

        // decide if the assertion is satisfied and what error message should be printed
        boolean error = false;
        String errorMessageTemplate = "";
        boolean should = parseSignSelector(sign);
        if (should && !contains) {
            error = true;
            errorMessageTemplate = "The %soutput did not contain the expected string \"%s\"; the full output is:\n%s";
        } else if (!should && contains) {
            error = true;
            errorMessageTemplate = "The %soutput did contain the unexpected string \"%s\"; the full output is:\n%s";
        }
        if (error) {
            Assert.fail(StringUtils.format(errorMessageTemplate, outputType, content, getSelectedRawOutput(outputType)));
        }
    }

    /**
     * After running some sort of execution, this method verifies that the standard or error output contains the given string (as part of a
     * single line).
     * 
     * This method is equivalent to {@link #assertSelectedOutputContains(String, String)}, and is only required to support different test
     * script constructs.
     * 
     * @param outputType the output to check: should be either "standard" or "error"
     * @param sign should be either "should" or "should not"
     * @param expectedContent the expected substring of an output line
     * @throws Throwable on failure
     */
    @Then("^the (standard |error |)output (should |should not |)contain$")
    public void assertSelectedOutputContainsLongText(String outputType, String sign, String expectedContent) throws Throwable {
        // delegate
        assertSelectedOutputContains(outputType, sign, expectedContent);
    }

    /**
     * After running some sort of execution, this method verifies that the standard or error output is empty.
     * 
     * @param outputType the output to check: should be either "standard" or "error"
     * @throws Throwable on failure
     */
    @Then("^the (standard |error |)output should be empty$")
    public void assertSelectedOutputIsEmpty(String outputType) throws Throwable {
        // delegate
        assertLineCountOfSelectedOutputIs(outputType, 0);
    }

    private String getSelectedRawOutput(String outputType) {
        switch (parseOutputSelector(outputType)) {
        case STANDARD:
            return currentExecutionResult.stdout;
        case ERROR:
            return currentExecutionResult.stderr;
        default:
            throw new IllegalArgumentException(outputType);
        }
    }

    private List<String> getSelectedFilteredOutput(String outputType) {
        switch (parseOutputSelector(outputType)) {
        case STANDARD:
            return currentExecutionResult.filteredStdoutLines;
        case ERROR:
            return currentExecutionResult.filteredStderrLines;
        default:
            throw new IllegalArgumentException(outputType);
        }
    }

    private boolean parseSignSelector(String sign) {
        sign = sign.trim();
        switch (sign) {
        case "should":
            return true;
        case "should not":
            return false;
        default:
            throw new IllegalArgumentException(sign);
        }
    }

    private OutputType parseOutputSelector(String outputType) {
        // for convenience, assume "standard" if the type is null or empty
        if (outputType == null || outputType.isEmpty()) {
            return OutputType.STANDARD;
        }

        // normalize to allow simpler regexp patterns
        outputType = outputType.trim();

        switch (outputType) {
        case "standard":
            return OutputType.STANDARD;
        case "error":
            return OutputType.ERROR;
        default:
            throw new IllegalArgumentException(outputType);
        }
    }

}
