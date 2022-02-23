/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */


// FILE COMMENTED OUT TO PREVENT ERRORS AND INTERFERENCE WITH FILES NOT IN THE ATTIC.


//package de.rcenvironment.extras.testscriptrunner.attic;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import cucumber.api.java.en.Given;
//import cucumber.api.java.en.When;
//import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils;
//import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.ExecutionResult;
//import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.LineFilter;
//import de.rcenvironment.core.utils.testing.TestParametersProvider;
//import de.rcenvironment.extras.testscriptrunner.common.CommonTestConfiguration;
//import de.rcenvironment.extras.testscriptrunner.common.CommonUtils;
//
///**
// * Step definitions for testing the standalone Remote Access client ("rce-remote").
// * 
// * @author Robert Mischke
// */
//public class RemoteAccessStandaloneClientDefinitions {
//
//    /**
//     * Configuration aspects that are specific for Remote Access tests, and are constant over all test or execution steps (e.g. the tool
//     * location, or the test server's host name and port).
//     * 
//     * @author Robert Mischke
//     */
//    private final class StaticSettings {
//
//        private final File rceRemoteExeLocation;
//
//        private final Map<String, String> substitutionMap;
//
//        private final IntegrationTestExecutorUtils executor;
//
//        private StaticSettings(TestParametersProvider testParameters) throws IOException {
//
//            log.info("Initializing static RemoteAccess-specific test settings");
//
//            rceRemoteExeLocation = testParameters.getExistingFile("testexecutable.path");
//
//            substitutionMap = new HashMap<String, String>();
//            substitutionMap.put("workdir", testDir.getAbsolutePath());
//            // substitutionMap.put("rce-remote-exe-path", new File(standaloneDirectory, "rce-remote.exe").getAbsolutePath());
//            substitutionMap.put("/", File.separator);
//            substitutionMap.put("CR", "\r");
//            substitutionMap.put("LF", "\n");
//            substitutionMap.put("list-tools-col-separator", " / ");
//            substitutionMap.put("testserver.host", testParameters.getNonEmptyString("testserver.host"));
//            substitutionMap.put("testserver.port", testParameters.getNonEmptyString("testserver.port"));
//            substitutionMap.put("testserver.nodeId", testParameters.getNonEmptyString("testserver.nodeId"));
//            substitutionMap.put("protocol.version.expected", testParameters.getNonEmptyString("protocol.version.expected"));
//            substitutionMap.put("actual-pw", "the_pw");
//
//            executor = new IntegrationTestExecutorUtils(rceRemoteExeLocation.getParentFile());
//        }
//
//        public String substitute(String input) {
//            return CommonUtils.substitute(input, substitutionMap);
//        }
//    }
//
//    private static StaticSettings staticSettings;
//
//    private File testDir;
//
//    private final Log log = LogFactory.getLog(getClass());
//
//    private CommonStateAndSteps commonState;
//
//    public RemoteAccessStandaloneClientDefinitions() throws IOException {
//        // TODO ensure clean directory for each run?
//        testDir = new File(CommonUtils.getValidatedSystemTempDir(), "rce-standalone-testing");
//        testDir.mkdirs();
//        if (!testDir.isDirectory()) {
//            throw new IOException("Failed to create test dir");
//        }
//
//        commonState = CommonStateAndSteps.getCurrent();
//    }
//
//    //TODO merge given password and given no password
//    
//    /**
//     * Creates a given password file with the given content. As a basic precaution, the filename can only consist of "word characters" and
//     * dots (e.g. no slashes).
//     * 
//     * TODO expand to general "a <...> file containing <...>"
//     * 
//     * @param filename the workdir-relative filename
//     * @param content the content
//     * @throws Throwable on failure
//     */
//    @Given("^a password file \"(.*?)\" containing \"(.*?)\"$")
//    public void givenSetUpPasswordFile(String filename, String content) throws Throwable {
//        content = getStaticSettings().substitute(content);
//        CommonUtils.validateStringMatches(filename, "[\\w\\.]+");
//        File pwfile = new File(testDir, filename);
//        FileUtils.writeStringToFile(pwfile, content);
//        log(String.format("Created password file '%s' containing '%s'", pwfile, content));
//    }
//
//    /**
//     * Ensures that there is no file with the given relative filename. As a basic precaution, the filename can only consist of
//     * "word characters" and dots (e.g. no slashes).
//     * 
//     * TODO expand to general "no <...> file at <...>" step?
//     * 
//     * @param filename the workdir-relative filename
//     * @throws Throwable on failure
//     */
//    @Given("^no password file at \"(.*?)\"$")
//    public void givenSetUpNoPasswordFile(String filename) throws Throwable {
//        CommonUtils.validateStringMatches(filename, "[\\w\\.]+");
//        File pwfile = new File(testDir, filename);
//        if (pwfile.delete()) {
//            log(String.format("Deleted password file '%s'", pwfile));
//        }
//    }
//
//    /**
//     * Invokes the platform-specific rce-remote executable in the configured installation folder, with the given parameters.
//     * 
//     * @param parameters the parameter string to pass to the rce-remote executable
//     * @throws Throwable on failure
//     */
//    @When("^calling rce-remote with parameters$")
//    public void whenCallingRCERemote(String parameters) throws Throwable {
//
//        // executablePath = CommonUtils.substitute(executablePath, substitutionMap);
//        parameters = getStaticSettings().substitute(parameters);
//        String command = getStaticSettings().rceRemoteExeLocation.getAbsolutePath() + " " + parameters;
//        log("Executing command: " + command);
//        ExecutionResult executionResult = getStaticSettings().executor.executeAndWait(command);
//        commonState.setParameterSubstitutionMap(getStaticSettings().substitutionMap);
//        commonState.setCurrentExecutionResult(executionResult.applyLineFilter(new LineFilter() {
//
//            @Override
//            public boolean accept(String line) {
//                return !line.startsWith("[DEBUG] ");
//            }
//        }));
//        log("Finished rce-remote invocation");
//    }
//
//    /**
//     * Fetches the {@link StaticSettings} singleton, initializing it on first access.
//     * 
//     * @return the single {@link StaticSettings} instance for all test runs
//     * @throws IOException on initialization failure
//     */
//    private synchronized StaticSettings getStaticSettings() throws IOException {
//        // lazy-init so these settings are loaded once, but only if a related command is actually used
//        if (staticSettings == null) {
//            staticSettings = new StaticSettings(CommonTestConfiguration.getParameters());
//        }
//        return staticSettings;
//    }
//
//    /**
//     * Convenience shortcut for INFO level logging.
//     */
//    private void log(String string) {
//        log.info(string);
//    }
//
//}
