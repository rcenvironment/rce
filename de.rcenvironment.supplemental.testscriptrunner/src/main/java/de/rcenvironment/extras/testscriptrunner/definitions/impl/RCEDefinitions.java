/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.rcenvironment.core.configuration.bootstrap.profile.Profile;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.ExecutionResult;
import de.rcenvironment.core.utils.testing.TestParametersProvider;
import de.rcenvironment.extras.testscriptrunner.common.CommonTestConfiguration;
import de.rcenvironment.extras.testscriptrunner.common.CommonUtils;

/**
 * Step definitions for testing RCE.
 * 
 * @author Tobias Rodehutskors
 */
public class RCEDefinitions {

    private CommonStateAndSteps commonState;

    private Map<String, String> dynamicSubstitutionMap;

    /**
     * This directory will be used to store the RCE profiles during testing.
     */
    private File testDir;

    public RCEDefinitions() throws IOException {
        // TODO ensure clean directory for each run?
        testDir = new File(CommonUtils.getValidatedSystemTempDir(), "rce-testing");
        testDir.mkdirs();
        if (!testDir.isDirectory()) {
            throw new IOException("Failed to create test dir");
        }

        commonState = CommonStateAndSteps.getCurrent();
        dynamicSubstitutionMap = new HashMap<String, String>();
        commonState.setParameterSubstitutionMap(dynamicSubstitutionMap);
    }

    /**
     * Creates a path to a random directory, which can be used as a profile directory, and stores this path with the given placeholder as
     * its key in a map for later substitution.
     * 
     * @param profilePlaceholder The key for the substitution mapping.
     * @param locked if not null the profile will be locked
     * @throws Throwable on failure
     */
    @Given("^a random profile path with placeholder \\$\\{(.*?)\\}( which is locked)?$")
    public void createRandomDirAsProfileDir(String profilePlaceholder, String locked) throws Throwable {

        File profileDir = new File(testDir, UUID.randomUUID().toString());
        dynamicSubstitutionMap.put(profilePlaceholder, profileDir.getAbsolutePath());

        if (locked != null) {
            new Profile.Builder(profileDir).create(true).migrate(true).buildUserProfile().attemptToLockProfileDirectory();
        }
    }

    /**
     * Launches RCE with the specified parameters.
     * 
     * @param parameters the parameter string to pass to the RCE executable
     * @throws Throwable on failure
     */
    @When("^calling RCE with parameters$")
    public void callingRceWithParameters(String parameters) throws Throwable {
        invokeRCE(CommonUtils.substitute(parameters, dynamicSubstitutionMap));
    }

    /**
     * Launches RCE without parameters.
     * 
     * @throws Throwable on failure
     */
    @When("^calling RCE$")
    public void callingRce() throws Throwable {
        invokeRCE("");
    }

    private void invokeRCE(String parameters) throws IOException, InterruptedException {

        TestParametersProvider testParameters = CommonTestConfiguration.getParameters();
        File rceExeLocation = testParameters.getExistingFile("testexecutable.path");
        String command = rceExeLocation.getAbsolutePath() + " " + parameters;

        // TODO This code is copied from IntegrationTestExecutorUtils in order to be able to set a custom CapturingTextOutReceiver
        final LocalApacheCommandLineExecutor executor = new LocalApacheCommandLineExecutor(rceExeLocation.getParentFile());
        executor.start(command);

        // create a CapturingTextOutReceiver which cancels the execution if a specific string is given in the output
        CapturingTextOutReceiver stdoutCapture = new CapturingTextOutReceiver() {

            private boolean cancelRequested = false;

            @Override
            public synchronized void addOutput(String line) {
                super.addOutput(line);

                if (!cancelRequested && this.getBufferedOutput().contains("Early startup complete, running main application")) {
                    cancelRequested = true;
                    executor.cancel();
                }
            }
        };
        CapturingTextOutReceiver stderrCapture = new CapturingTextOutReceiver();
        final TextStreamWatcher stdoutWatcher =
            new TextStreamWatcher(executor.getStdout(), ConcurrencyUtils.getAsyncTaskService(), stdoutCapture).start();
        final TextStreamWatcher stderrWatcher =
            new TextStreamWatcher(executor.getStderr(), ConcurrencyUtils.getAsyncTaskService(), stderrCapture).start();

        int exitCode = executor.waitForTermination();
        stdoutWatcher.waitForTermination();
        stderrWatcher.waitForTermination();

        commonState.setCurrentExecutionResult(
            new ExecutionResult(exitCode, stdoutCapture.getBufferedOutput(), stderrCapture.getBufferedOutput()));
    }

    /**
     * Starts a new RCE instance as SSH server.
     * 
     * TODO this method shares a lot of code with invokeRCE
     * 
     * TODO this is currently implemented using a Then step. This shut be replaced with Cucumber hooks.
     * http://zsoltfabok.com/blog/2012/09/cucumber-jvm-hooks/
     * 
     * @Before("@RCESSHServer")
     * 
     * @throws Throwable on failure
     */
    @Given("^a RCE instance running as SSH server$")
    public void startRCEinstanceRunningAsSSHserver() throws Throwable {
        TestParametersProvider testParameters = CommonTestConfiguration.getParameters();
        File rceExeLocation = testParameters.getExistingFile("testexecutable.path");

        // TODO we need to fix this lazy mans approach
        URL ressources = RCEDefinitions.class.getResource("");
        File profilePath = new File(new File(ressources.toURI()), "../../../../../profiles/sshServer");

        // TODO to investigate: if this is not started headless, shutdown fails in most cases
        String command = rceExeLocation.getAbsolutePath() + " --profile " + profilePath.getCanonicalPath() + " --headless";

        // TODO This code is copied from IntegrationTestExecutorUtils in order to be able to set a custom CapturingTextOutReceiver
        final LocalApacheCommandLineExecutor executor = new LocalApacheCommandLineExecutor(rceExeLocation.getParentFile());
        executor.start(command);

        final CountDownLatch earlyStartupCompleteLatch = new CountDownLatch(1);

        // create a CapturingTextOutReceiver which cancels the execution if a specific string is given in the output
        CapturingTextOutReceiver stdoutCapture = new CapturingTextOutReceiver() {

            private boolean earlyStartupComplete = false;

            @Override
            public synchronized void addOutput(String line) {
                super.addOutput(line);

                if (!earlyStartupComplete && this.getBufferedOutput().contains("Early startup complete, running main application")) {
                    earlyStartupComplete = true;
                    earlyStartupCompleteLatch.countDown();

                }
            }
        };

        new TextStreamWatcher(executor.getStdout(), ConcurrencyUtils.getAsyncTaskService(), stdoutCapture).start();

        earlyStartupCompleteLatch.await();
    }

    /**
     * Closes the running RCE instance using the established SSH connection.
     * 
     * TODO this is currently implemented using a Then step. This shut be replaced with Cucumber hooks.
     * http://zsoltfabok.com/blog/2012/09/cucumber-jvm-hooks/
     * 
     * @After("@RCESSHServer")
     * 
     * @throws Throwable on failure
     */
    @Then("^shutdown the RCE instance$")
    public void shutdownTheRCEinstance() throws Throwable {
        // the established connection is static, therefore we can just create an new SshCommandExecutionDefinitions object
        new SshCommandExecutionDefinitions().executeAndWait("shutdown", null);
    }
}
