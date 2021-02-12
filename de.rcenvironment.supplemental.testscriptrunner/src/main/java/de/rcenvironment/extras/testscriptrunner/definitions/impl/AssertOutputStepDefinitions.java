/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;

/**
 * Steps for asserting output conditions.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke (based on code from)
 */
public class AssertOutputStepDefinitions extends InstanceManagementStepDefinitionBase {

    public AssertOutputStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Class implementing InstanceIterator for asserting last command output.
     * 
     * @author Marlon Schroeter
     */
    private class AssertLastCommandOutput implements InstanceIterator {

        private boolean shouldContain;

        private boolean useRegex;

        private String subString;

        AssertLastCommandOutput(boolean shouldContain, boolean useRegex, String subString) {
            this.shouldContain = shouldContain;
            this.useRegex = useRegex;
            this.subString = subString;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws Throwable {
            assertPropertyOfLastCommandOutput(instance, shouldContain, useRegex, subString);
        }

    }

    /**
     * Class implementing InstanceIterator for asserting file.
     * 
     * @author Marlon Schroeter
     */
    private class AssertFileContains implements InstanceIterator {

        private boolean shouldContain;

        private boolean useRegex;

        private String subString;

        private String relativeFilePath;

        AssertFileContains(boolean shouldContain, boolean useRegex, String subString, String relativeFilePath) {
            this.shouldContain = shouldContain;
            this.useRegex = useRegex;
            this.subString = subString;
            this.relativeFilePath = relativeFilePath;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws Throwable {
            assertRelativeFileContains(instance, relativeFilePath, shouldContain, useRegex, subString);

        }
    }

    /**
     * Class implementing InstanceIterator for asserting relative file is empty.
     * 
     * @author Marlon Schroeter
     */
    private class AssertFileEmpty implements InstanceIterator {

        private String relativeFilePath;

        AssertFileEmpty(String relativeFilePath) {
            this.relativeFilePath = relativeFilePath;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws Throwable {
            assertRelativeFileIsEmpty(instance, relativeFilePath);
        }
    }

    /**
     * Class implementing InstanceIterator for asserting error log of instance conforms/consists of entries in errorTable.
     * 
     * @author Marlon Schroeter
     */
    private class AssertErrorLog implements InstanceIterator {

        private DataTable errorTable;

        private boolean unspecifiedAccepted;

        AssertErrorLog(DataTable errorTable, boolean unspecifiedAccepted) {
            this.errorTable = errorTable;
            this.unspecifiedAccepted = unspecifiedAccepted;
        }

        @Override
        public void iterateActionOverInstance(ManagedInstance instance) throws Throwable {
            assertErrorContents(instance, errorTable, unspecifiedAccepted);
        }
    }

    /**
     * Verifies the output of the last command executed on the given instance via IM SSH.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param negationFlag a flag that changes the expected outcome to "substring NOT present"
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param subString the substring or pattern expected to be present or absent in the command's output
     * @throws Throwable on failure
     */
    @Then("^the(?: last)? output(?: of(?: (all|each))?(?: instance[s]?)?(?: \"([^\"]*)\")?)? should( not)? contain"
        + "( the pattern)? \"([^\"]*)\"$")
    public void thenLastOutputContains(String allFlag, String instanceIds, String negationFlag, String useRegexpMarker, String subString)
        throws Throwable {
        final boolean shouldContain = negationFlag == null;
        final boolean useRegex = useRegexpMarker != null;
        if (allFlag == null && instanceIds == null) {
            assertPropertyOfLastCommandOutput(executionContext.getLastInstanceWithSingleCommandExecution(), shouldContain, useRegex,
                subString);
        } else {
            AssertLastCommandOutput assertLastCommandOutput = new AssertLastCommandOutput(shouldContain, useRegex, subString);
            iterateInstances(assertLastCommandOutput, allFlag, instanceIds);
        }
    }

    /**
     * Checks the log file(s) of one or more instances for the presence or absence of a given substring.
     * 
     * @param relativeFilePathList comma-separated list of profile-relative paths to the file to check
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param notFlag either null or "not " to test for absence of the given substring
     * @param useRegexpMarker a flag that causes "substring" to be treated as a regular expression if present
     * @param subString the subString to look for in the specified log files
     * @throws Throwable on failure
     */
    @Then("^the file[s]? \"([^\"]*)\"(?: of(?: (all|each))?(?: instance[s]?)?(?: \"([^\"]*)\")?)? should( not)? contain"
        + "( the pattern)? \"([^\"]+)\"$")
    public void thenFilesContain(String relativeFilePathList, String allFlag, String instanceIds, String notFlag, String useRegexpMarker,
        String subString) throws Throwable {
        final boolean shouldContain = notFlag == null;
        final boolean useRegex = useRegexpMarker != null;
        for (String relativeFilePath : parseCommaSeparatedList(relativeFilePathList)) {
            AssertFileContains assertFileContains = new AssertFileContains(shouldContain, useRegex, subString, relativeFilePath);
            iterateInstances(assertFileContains, allFlag, instanceIds);
        }
    }

    /**
     * Checks the log file(s) of one or more instances for their absence or emptiness.
     * 
     * @param relativeFilePathList comma-separated list of profile-relative paths to the file to check
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @throws Throwable on failure
     */
    @Then("^the file[s]? \"([^\"]*)\"(?: of(?: (all|each))?(?: instance[s]?)?(?: \"([^\"]*)\")?)? should be absent or empty$")
    public void thenFilesExist(String relativeFilePathList, String allFlag, String instanceIds) throws Throwable {
        for (String relativeFilePath : parseCommaSeparatedList(relativeFilePathList)) {
            AssertFileEmpty assertFileEmpty = new AssertFileEmpty(relativeFilePath);
            iterateInstances(assertFileEmpty, allFlag, instanceIds);
        }
    }

    /**
     * Convenience shortcut to test all relevant log files for a clean shutdown.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @throws Throwable on failure
     */
    @Then("^the log output of( all)?(?: instance[s]?)?(?: \"([^\"]*)\")? should indicate a clean shutdown with no warnings or errors$")
    public void thenLogOutputCleanShutdown(String allFlag, String instanceIds) throws Throwable {
        AssertFileEmpty warningLogEmpty = new AssertFileEmpty(StepDefinitionConstants.WARNINGS_LOG_FILE_NAME);
        AssertFileContains debugLogContainsNoneUnfinished =
            new AssertFileContains(true, false, "Known unfinished operations on shutdown: <none>",
                StepDefinitionConstants.DEBUG_LOG_FILE_NAME);
        AssertFileContains debugLogContainsExitCode0 =
            new AssertFileContains(true, false, "Main application shutdown complete, exit code: 0",
                StepDefinitionConstants.DEBUG_LOG_FILE_NAME);
        iterateInstances(warningLogEmpty, allFlag, instanceIds);
        iterateInstances(debugLogContainsNoneUnfinished, allFlag, instanceIds);
        iterateInstances(debugLogContainsExitCode0, allFlag, instanceIds);
    }

    /**
     * Verifies the presence or absence of certain output in the debug.log file of an instance.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param negationFlag a flag that changes the expected outcome to "subString NOT present"
     * @param useRegexpMarker a flag that causes "subString" to be treated as a regular expression if present
     * @param subString the subString or pattern expected to be present or absent in the command's output
     * @throws Throwable on failure
     */
    @Then("^the log output of( all)?(?: instance[s]?)?(?: \"([^\"]*)\")? should (not )?contain (the pattern )?\"([^\"]*)\"$")
    public void thenLogOutputContains(String allFlag, String instanceIds, String negationFlag, String useRegexpMarker, String subString)
        throws Throwable {
        thenFilesContain(StepDefinitionConstants.DEBUG_LOG_FILE_NAME, allFlag, instanceIds, negationFlag, useRegexpMarker, subString);
    }

    /**
     * Verifies given instances do not contain any errors in their corresponding warning log.
     * 
     * @param allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the value of
     *        {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param type currently supports error oder warning
     * @throws Throwable on failure
     */
    @Then("^the log output of( all)?(?: instance[s]?)?(?: \"([^\"]*)\")? should not contain(?: any) (error|warning)[s]?$")
    public void thenLogOutputNoErrors(String allFlag, String instanceIds, String type) throws Throwable {
        thenLogOutputNumberErrors(allFlag, instanceIds, 0, null, type);
    }

    /**
     * Verifies that a given number of errors is present in the warning log of the given instances.
     * 
     * @param allFlag allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the
     *        value of {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param lowerBoundInput minimum number of allowed errors in warning log
     * @param upperBoundInput if present maximum number of allowed errors in warning log. If not present, lowerBound becomes exact number of
     *        allowed errors.
     * @param type currently supports error oder warning
     * @throws Throwable on failure
     */
    @Then("^the log output of( all)?(?: instance[s]?)?(?: \"([^\"]*)\")? should contain (\\d+)(?: to (\\d+))? (error|warning)[s]?$")
    public void thenLogOutputNumberErrors(final String allFlag, final String instanceIds, final Integer lowerBoundInput,
        final Integer upperBoundInput, final String type)
        throws Throwable {

        final int[] assertReturn = assertBounds(lowerBoundInput, upperBoundInput);
        final int lowerBound = assertReturn[0];
        final int upperBound = assertReturn[1];

        final Pattern typePattern = Pattern.compile(StringUtils.format(
            getRegexBaseFormatForLogMatching(type),
            StepDefinitionConstants.ANY_PACKAGE,
            StepDefinitionConstants.ANY_STRING));

        for (ManagedInstance instance : resolveInstanceList(allFlag != null, instanceIds)) {
            final int numberErrorsOrWarnings = countErrorsOrWarnings(instance, typePattern);
            assertNumberErrorsOrWarningsInsideBoundaries(numberErrorsOrWarnings, lowerBound, upperBound, instance, type);
        }
    }

    /**
     * checks if error/warning is (not) present in warning log of given instance. Fails if opposite case is true.
     * 
     * @param allFlag allFlag a phrase whose presence (non-null) influences which instances are effected. How it does that depends on the
     *        value of {@code instanceIds} and is defined in {@link #resolveInstanceList()}
     * @param instanceIds a comma-separated list of instances, which when present (non-null) influences which instances are effected. How it
     *        does that depends on the value of {@code allFlag} and is defined in {@link #resolveInstanceList()}
     * @param mode decides how warnings/error not mentioned in errorTable are handled. If equal to "consists of" unexpected occurrences
     *        cause a failure. If equal to "conform to" unexpected occurrences are ignored.
     * @param errorTable a table of errors consisting of . Each row represents an error which is to be checked for (non) existence. If
     *        ErrorMessage is left blank, all messages are accepted.
     * @throws Throwable on failure
     */
    @Then("^the log output of( all)?(?: instance[s]?)?(?: \"([^\"]*)\")? should (consist of|conform to):$")
    public void thenLogOutputConformance(String allFlag, String instanceIds, String mode, DataTable errorTable) throws Throwable {
        AssertErrorLog assertErrorLog = new AssertErrorLog(errorTable, mode.equals("conform to"));
        iterateInstances(assertErrorLog, allFlag, instanceIds);
    }

    private void assertNumberErrorsOrWarningsInsideBoundaries(final int numberErrorsOrWarnings, final int lowerBound, final int upperBound,
        final ManagedInstance instance, final String type) {
        if (numberErrorsOrWarnings < lowerBound || numberErrorsOrWarnings > upperBound) {
            fail(StringUtils.format("Expected between %s and %s $ss on instance %s. Saw %s instead.", lowerBound, upperBound, type,
                instance, numberErrorsOrWarnings));
        } else {
            printToCommandConsole(StringUtils.format("Expected between %s and %s $ss on instance %s. Saw %s as expected.", lowerBound,
                upperBound, type, instance, numberErrorsOrWarnings));
        }
    }

    private int countErrorsOrWarnings(final ManagedInstance instance, final Pattern typePattern) {
        int numberErrorsOrWarnings = 0;
        String warningLog = null;
        try {
            warningLog = instance.getProfileRelativeFileContent(StepDefinitionConstants.WARNINGS_LOG_FILE_NAME, false);
        } catch (IOException e) {
            fail(StringUtils.format("Trying to acces warinings log of instance %s produced an error: %s", instance, e));
        }
        Matcher m = typePattern.matcher(warningLog);
        while (m.find()) {
            numberErrorsOrWarnings++;
        }
        return numberErrorsOrWarnings;
    }

    private int[] assertBounds(final Integer lowerBound, final Integer upperBound) {
        if (upperBound == null) {
            return new int[] { lowerBound, lowerBound };
        }

        if (upperBound < lowerBound) {
            fail(StringUtils.format("Upper bound %s is lower than lower bound %s.", upperBound, lowerBound));
        }

        return new int[] { lowerBound, upperBound };
    }

    private String getRegexBaseFormatForLogMatching(String type) {
        String baseFormat;
        if (type.equals("error") || type.equals("Error") || type.equals("ERROR")) {
            baseFormat = StepDefinitionConstants.LOG_CONTAINS_ERROR_FORMAT;
        } else if (type.equals("warning") || type.equals("Warning") || type.equals("WARNING") || type.equals("warn") || type.equals("Warn")
            || type.equals("WARN")) {
            baseFormat = StepDefinitionConstants.LOG_CONTAINS_WARNING_FORMAT;
        } else {
            fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, type));
            return null; // never reached
        }
        return baseFormat;
    }

    private void assertErrorContents(ManagedInstance instance, DataTable errorTable, boolean unspecifiedAccepted) throws Throwable {
        for (String line : instance.getProfileRelativeFileContent(StepDefinitionConstants.WARNINGS_LOG_FILE_NAME, false).split("\\r?\\n")) {
            handleErrorLine(line, errorTable, unspecifiedAccepted);
        }
    }

    private void handleErrorLine(String line, DataTable errorTable, boolean unspecifiedAccepted) {
        for (Map<String, String> row : errorTable.asMaps(String.class, String.class)) {
            String presence = row.get("Presence");
            String type = row.get("Type");
            String origin = row.get("Origin");
            String message = row.get("Message");

            boolean positive = isRowPositive(presence);

            if (message.equals("") || message == null) {
                message = StepDefinitionConstants.ANY_STRING;
            } else {
                message = Pattern.quote(message);
            }

            if (origin.equals("") || origin == null) {
                origin = StepDefinitionConstants.ANY_PACKAGE;
            } else {
                origin = Pattern.quote(origin);
            }
            Pattern p = Pattern.compile(StringUtils.format(getRegexBaseFormatForLogMatching(type), origin, message));
            if (p.matcher(line).matches()) {
                if (positive) {
                    printToCommandConsole(StringUtils.format("Found and accepted expected error/warning %s", line));
                    return;
                } else {
                    fail(StringUtils.format("Found explicitly unwanted error/warning %s", line));
                }
            }
        }
        if (unspecifiedAccepted) {
            printToCommandConsole(StringUtils.format("Found unexpected error/warning %s. \n Accepted since in blacklisting mode.", line));
        } else {
            fail(StringUtils.format("Found unexpected error/warning %s. \n Failed since in whitelisting mode.", line));
        }
    }

    private boolean isRowPositive(String presence) {
        switch (presence) {
        case ("yes"):
        case ("y"):
            return true;
        case ("no"):
        case ("n"):
            return false;
        default:
            fail(StringUtils.format("%s is not a supported form of presence.", presence));
            return false;
        }
    }

    private void subStringContained(String string, String subString, boolean shouldContain, boolean useRegex, String errorBase) {
        String patternError = "";
        if (useRegex) {
            patternError = "the pattern ";
        }
        if (!stringContainsOrContainsNot(string, subString, shouldContain, useRegex)) {
            String containError = "";
            if (shouldContain) {
                containError = "not ";
            }
            fail(StringUtils.format("%s did %scontain %s\"%s\"", errorBase, containError, patternError, subString));
        } else {
            String containError = "not ";
            if (shouldContain) {
                containError = "";
            }
            printToCommandConsole(
                StringUtils.format("As expected %s did %scontain %s\"%s\"", errorBase, containError, patternError, subString));
        }
    }

    private void assertPropertyOfLastCommandOutput(ManagedInstance instance, boolean shouldContain, boolean useRegex,
        String subString) {
        subStringContained(instance.getLastCommandOutput(), subString, shouldContain, useRegex,
            StringUtils.format("  The command output of instance \"%s\"", instance));
    }

    private void assertRelativeFileContains(final ManagedInstance instance, final String relativeFilePath, final boolean shouldContain,
        final boolean useRegex, final String subString) throws IOException {
        final String fileContent = instance.getProfileRelativeFileContent(relativeFilePath, false);

        if (fileContent == null) {
            fail(StringUtils.format("The expected file \"%s\" in profile \"%s\" does not exist", relativeFilePath, instance));
        } else {
            subStringContained(fileContent, subString, shouldContain, useRegex,
                StringUtils.format("The file \"%s\" of instance \"%s\"", relativeFilePath, instance));
        }
    }

    private void assertRelativeFileIsEmpty(ManagedInstance instance, String relativeFilePath)
        throws IOException {
        final String fileContent = instance.getProfileRelativeFileContent(relativeFilePath, false);

        if (fileContent == null) {
            printToCommandConsole(
                StringUtils.format("As expected the file \"%s\" in profile \"%s\" is absent.", relativeFilePath, instance));
        } else {
            if (fileContent.isEmpty()) {
                printToCommandConsole(
                    StringUtils.format("As expected the file \"%s\" in profile \"%s\" is empty.", relativeFilePath, instance));
            } else {
                fail(StringUtils.format(
                    "The file \"%s\" in profile \"%s\" should have been absent or empty, but exists "
                        + "(content size: %d characters); full file content:\n%s",
                    relativeFilePath, instance, fileContent.length(), fileContent));
            }
        }
    }
}
