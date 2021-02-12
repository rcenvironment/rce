/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

// FILE COMMENTED OUT TO PREVENT ERRORS AND INTERFERENCE WITH FILES NOT IN THE ATTIC.

//package de.rcenvironment.extras.testscriptrunner.attic;
//
//import java.util.Objects;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.junit.Assert;
//
//import com.jcraft.jsch.Session;
//
//import cucumber.api.java.en.Given;
//import cucumber.api.java.en.When;
//import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListenerAdapter;
//import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
//import de.rcenvironment.core.communication.sshconnection.impl.SshConnectionSetupImpl;
//import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
//import de.rcenvironment.core.utils.common.StringUtils;
//import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
//import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
//import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.ExecutionResult;
//import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;
//import de.rcenvironment.extras.testscriptrunner.common.ShutdownHooks;
//
///**
// * Step definitions for opening/closing SSH connections, sending SSH console commands, and verifying their output.
// * 
// * @author Robert Mischke
// */
//public class SshCommandExecutionDefinitions {
//
//    private static SshConnectionSetupImpl currentConnection;
//
//    private Boolean connectResult;
//
//    private final Log log = LogFactory.getLog(getClass());
//
//    /**
//     * Opens a connection to the given host and port. If an equivalent connection is already open, it is reused by default.
//     * 
//     * As a future extensions, it may be useful to support multiple open connections at once.
//     * 
//     * @param host the host (IP or name) to connect to
//     * @param portString the port to connect to, in string form to allow placeholders
//     * @param loginName the SSH login name
//     * @param loginPassphrase the SSH plain-text password (no key file support yet)
//     */
//    @Given("^a SSH connection to ([^ ,]+)(?:,| on)? port ([^ ]+) with (?:default login|login \"([^\"]*)\" and password \"([^\"]*)\")$")
//    public void givenConnectSshPort(String host, String portString, String loginName, String loginPassphrase) {
//
//        // define the "default login" option
//        if (loginName == null && loginPassphrase == null) {
//            loginName = "test"; // TODO use placeholder when available
//            loginPassphrase = "test";
//        }
//
//        int port = Integer.parseInt(portString);
//
//        // The connection can only be reused if it was not closed by the server in the meantime.
//        if (currentConnection != null && currentConnection.isConnected()) {
//            // FIXME verify that the parameters are actually the same
//            log.info("Reusing established SSH connection");
//            return;
//        }
//
//        log.info(StringUtils.format("Opening SSH connection to %s@%s:%d", loginName, host, port));
//        currentConnection =
//            new SshConnectionSetupImpl(
//                "default", "", host, port, loginName, null, false, false, false, new SshConnectionListenerAdapter() {
//
//                @Override
//                public void onConnected(SshConnectionSetup setup) {
//                    connectResult = true;
//                }
//
//                @Override
//                public void onConnectionAttemptFailed(SshConnectionSetup setup, String reason, boolean firstConsecutiveFailure,
//                    boolean willAutoRetry) {
//                    log.error("Failed to open SSH connection: " + reason);
//                    connectResult = false;
//                }
//            });
//        Session session = currentConnection.connect(loginPassphrase);
//        Objects.requireNonNull(connectResult, "Unknown connection result");
//        if (!connectResult) {
//            throw new AssertionError("Failed to connect");
//        }
//        Objects.requireNonNull(session, "Null session");
//
//        ShutdownHooks.register("teardownSshConnections", new Runnable() {
//
//            @Override
//            public void run() {
//                if (currentConnection != null && currentConnection.isConnected()) {
//                    log.info("Closing SSH connections");
//                    currentConnection.disconnect();
//                    currentConnection = null;
//                }
//            }
//        });
//    }
//
//    /**
//     * Executes an RCE console command over a previously established SSH connection. The output can then be queried with the common output
//     * step definitions (e.g. "then the output should contain ..."). Optionally, this command can verify an expected output for brevity.
//     * 
//     * @param command the command to run
//     * @param expectedResponse (optional) an expected string that should be contained in at least one line of the command's output
//     * @throws Throwable on errors
//     */
//    @When("^executing the command \"(.+)\"(?: \\(expecting \"([^\"]+)\"\\))?$")
//    public void whenExecutingOverSSH(String command, String expectedResponse) throws Throwable {
//        log.info(StringUtils.format("Executing SSH command \"%s\"", command));
//        Session session = currentConnection.getSession();
//        Objects.requireNonNull(session, "Session is <null>");
//        JSchRCECommandLineExecutor executor = new JSchRCECommandLineExecutor(session);
//        executor.start(command);
//        CapturingTextOutReceiver stdoutCapture = new CapturingTextOutReceiver();
//        CapturingTextOutReceiver stderrCapture = new CapturingTextOutReceiver();
//        final TextStreamWatcher stdoutWatcher =
//            new TextStreamWatcher(executor.getStdout(), ConcurrencyUtils.getAsyncTaskService(), stdoutCapture).start();
//        final TextStreamWatcher stderrWatcher =
//            new TextStreamWatcher(executor.getStderr(), ConcurrencyUtils.getAsyncTaskService(), stderrCapture).start();
//        int exitCode = executor.waitForTermination();
//        stdoutWatcher.waitForTermination();
//        stderrWatcher.waitForTermination();
//        ExecutionResult executionResult =
//            new ExecutionResult(exitCode, stdoutCapture.getBufferedOutput(), stderrCapture.getBufferedOutput());
//        if (executionResult.stderr.length() != 0 || executionResult.stderrLines.size() != 0) {
//            Assert.fail("Unexpected state: received StdErr output when executing a SSH command");
//        }
//        log.info(StringUtils.format("Output for SSH command \"%s\" (%d lines):\n%s", command, executionResult.stdoutLines.size(),
//            executionResult.stdout));
//        CommonStateAndSteps.getCurrent().setCurrentExecutionResult(executionResult);
//        log.debug("Execution of SSH command \"" + command + "\" finished");
//
//        if (expectedResponse != null && !expectedResponse.isEmpty()) {
//            log.info("Expecting response: " + expectedResponse);
//            // delegate/chain to common "output contains" step
//            CommonStateAndSteps.getCurrent().thenOutputContains("", "should", expectedResponse);
//        }
//    }
//
//}
