/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils;
import de.rcenvironment.core.utils.executor.testutils.IntegrationTestExecutorUtils.ExecutionResult;
import de.rcenvironment.core.utils.testing.ParameterizedTestUtils;
import de.rcenvironment.core.utils.testing.TestParametersProvider;

/**
 * Integration tests for the standalone Remote Access client. Requires a running RCE instance to test against, and the location of the
 * standalone client must be configured.
 * 
 * @author Robert Mischke
 */
public class RemoteAccessStandaloneClientParametrizedTests {

    private static final String TEST_TOOL_1_VERSION = "1.0";

    private static final String TEST_TOOL_1_ID = "testTool";

    private static final String TEST_TOOL_2_VERSION = "1 .,_-+()";

    private static final String TEST_TOOL_2_ID = "tool .,_-+() valid name test";

    private static final String DEFAULT_TESTFILE_CONTENT = "testContent";

    private static final String DEFAULT_TEST_FILENAME = "test.txt";

    private static final String TOOL_LIST_COLUMN_SEPARATOR = " / ";

    private static final String EXPECTED_PROTOCOL_VERSION = "7.0.0-pre1";

    private static final String PW_TEST_USER_ACCOUNT = "pwtest";

    private static final String PW_TEST_PASSWORD = "the_pw";

    // use "-Drce.network.overrideNodeId=12561256125612561256125612561256" on the test server to set this id
    private static final String TEST_SERVER_NODE_ID = "12561256125612561256125612561256";

    private static final String DOUBLE_QUOTE = "\"";

    private static final String QUOTED_EMPTY_STRING = DOUBLE_QUOTE + DOUBLE_QUOTE;

    private static final String OPERATION_ID_RUN_TOOL = "run-tool";

    private static final String OPERATION_ID_LIST_TOOLS = "list-tools";

    private static final String OPERATION_ID_PROTOCOL_VERSION = "protocol-version";

    private static final String HOST_PARAMETER = "-h";

    private static final String PORT_PARAMETER = "-p";

    private static final String USER_PARAMETER = "-u";

    private static final String PASSWORD_FILE_PARAMETER = "-f";

    private static final String INPUT_DIR_PARAMETER = "-I";

    private static final String OUTPUT_DIR_PARAMETER = "-O";

    private static final String TOOL_NODE_ID_PARAMETER = "-n";

    private static final String AUTHENTICATION_FAILURE_OUTPUT_TEXT_PART = "Authentication failure";

    private static final String UNEXPECTED_LINE_COUNT_ON_STD_ERR = "Unexpected line count on StdErr";

    private static final String UNEXPECTED_LINE_COUNT_ON_STD_OUT = "Unexpected line count on StdOut";

    private static final String UNEXPECTED_EXIT_CODE = "Unexpected exit code";

    private static final String TEST_FAILED_COMPLETE_OUTPUT_FOR = "Test failure: Printing complete non-debug output for ";

    private static final String STDERR_LOG_PREFIX = "StdErr: ";

    private static final String STDOUT_LOG_PREFIX = "StdOut: ";

    private File standaloneDirectory;

    private File standaloneExecutable;

    private TestParametersProvider testParameters;

    private IntegrationTestExecutorUtils executorUtils;

    private List<String> filteredStdout;

    private List<String> filteredStderr;

    private String testIP;

    private String testHostName;

    private String testPort;

    private final Log log = LogFactory.getLog(getClass());

    private File inputDir;

    private String inputDirPath;

    private File outputDir;

    private String outputDirPath;

    private final TempFileService tempFileService;

    public RemoteAccessStandaloneClientParametrizedTests() {
        tempFileService = TempFileServiceAccess.getInstance();
    }

    /**
     * Once-for-all-tests setup.
     */
    @BeforeClass
    public static void initOnce() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Common setup.
     * 
     * @throws IOException on setup exceptions, e.g. invalid client path
     */
    @Before
    public void setUp() throws IOException {
        // read test parameters
        testParameters = new ParameterizedTestUtils().readDefaultPropertiesFile(getClass());
        standaloneDirectory = testParameters.getExistingDir("remoteaccess.standalone.location");
        testIP = testParameters.getNonEmptyString("testserver.ip");
        testHostName = testParameters.getNonEmptyString("testserver.hostname");
        testPort = testParameters.getNonEmptyString("testserver.port");

        // validate
        Assert.assertTrue("Invalid path", standaloneDirectory.isDirectory());
        if (OSFamily.isWindows()) {
            standaloneExecutable = new File(standaloneDirectory, "rce-remote.exe");
        } else {
            standaloneExecutable = new File(standaloneDirectory, "rce-remote");
        }
        Assert.assertTrue("Executable file not found", standaloneExecutable.isFile());
        Assert.assertTrue("'Executable' file not actually executable", standaloneExecutable.canExecute());

        executorUtils = new IntegrationTestExecutorUtils(standaloneDirectory);
        // prevent accidental reuse
        filteredStdout = null;
        filteredStderr = null;
    }

    /**
     * Common teardown.
     */
    @After
    public void tearDown() {
        // visual separation
        log.debug("--- End of test case --------------------------------------------------------------------");
    }

    /**
     * Tests handling of an unknown command.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testNoConnectionAttemptOnUnknownCommand() throws Exception {

        String unknownCommand = "unknownCmd";
        String command = buildCommand(new String[] { unknownCommand });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            parseAndValidateExecutionResult(result, 2, 0, 1);
            String errorMessage = filteredStderr.get(0);
            assertTrue(errorMessage.startsWith("Unknown operation parameter"));
            assertTrue(errorMessage.contains(unknownCommand));
            assertFalse(errorMessage.contains("Error connecting to"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests the protocol version query, using a server IP.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testVersionQueryWithIP() throws Exception {

        String command = buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, OPERATION_ID_PROTOCOL_VERSION });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            parseAndValidateExecutionResult(result, 0, 1, 0);
            assertEquals(EXPECTED_PROTOCOL_VERSION, filteredStdout.get(0));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests the protocol version query, using a server host name.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testVersionQueryWithHostName() throws Exception {

        String command =
            buildCommand(new String[] { HOST_PARAMETER, testHostName, PORT_PARAMETER, testPort, OPERATION_ID_PROTOCOL_VERSION });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            parseAndValidateExecutionResult(result, 0, 1, 0);
            assertEquals(EXPECTED_PROTOCOL_VERSION, filteredStdout.get(0));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests handling of a non-existing host name.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testInvalidHost() throws Exception {
        String command =
            buildCommand(new String[] { HOST_PARAMETER, "invalid.example.org", PORT_PARAMETER, testPort, OPERATION_ID_PROTOCOL_VERSION });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            parseAndValidateExecutionResult(result, 1, 0, 1);
            assertTrue(filteredStderr.get(0).contains("Error connecting to invalid.example.org:" + testPort));
            assertTrue(filteredStderr.get(0).contains("Failed to resolve hostname invalid.example.org"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests that the "password test" account does not work without specifying a password file.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testDefaultPasswordFailure() throws Exception {

        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, USER_PARAMETER, PW_TEST_USER_ACCOUNT,
                OPERATION_ID_PROTOCOL_VERSION });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            parseAndValidateExecutionResult(result, 1, 0, 1);
            assertTrue("Expected 'Authentication failure' message",
                filteredStderr.get(0).contains(AUTHENTICATION_FAILURE_OUTPUT_TEXT_PART));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests handling of a non-existing password file.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testNonExistingPasswordFile() throws Exception {

        File bogusPwFile = new File(standaloneDirectory, "bogusPwFile.txt").getAbsoluteFile();
        assertFalse(bogusPwFile.exists());

        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, USER_PARAMETER, PW_TEST_USER_ACCOUNT,
                PASSWORD_FILE_PARAMETER, DOUBLE_QUOTE + bogusPwFile.getAbsolutePath() + DOUBLE_QUOTE, OPERATION_ID_PROTOCOL_VERSION });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            parseAndValidateExecutionResult(result, 2, 0, 1);
            assertEquals("The specified password file does not exist", filteredStderr.get(0));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests handling of an existing password file containing a wrong password.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testExistingWrongPasswordFile() throws Exception {
        runPasswordFileScenario("wrongPW", 1);
    }

    /**
     * Tests handling of an existing password file exactly containing the correct password.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testExistingCorrectPasswordFileWithNoTrailingLineBreakChars() throws Exception {
        runPasswordFileScenario(PW_TEST_PASSWORD, 0);
    }

    /**
     * Tests handling of an existing password file containing the correct password, followed by a newline and a line feed (\n, 0x0A).
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testExistingCorrectPasswordFileWithTrailingLF() throws Exception {
        runPasswordFileScenario(PW_TEST_PASSWORD + "\n", 0);
    }

    /**
     * Tests handling of an existing password file containing the correct password, followed by a newline and a line feed (\r\n, 0x0D0A).
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testExistingCorrectPasswordFileWithTrailingCRLFOnWindows() throws Exception {
        // note: currently, \r\n is *not* expected to work on Linux: \r is taken as part of the first line, as Linux line endings
        // are always \n. maybe add a specific safety check for this (check for last character == '\r') and warn the user?
        if (OSFamily.isLinux()) {
            log.debug("Skipping Windows-specific test on Linux");
            return;
        }
        runPasswordFileScenario(PW_TEST_PASSWORD + "\r\n", 0);
    }

    /**
     * Tests handling of an existing password file containing the correct password, followed by a newline and a line feed (\n, 0x0A).
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testExistingCorrectPasswordFileWithExtraContentAfterTrailingLF() throws Exception {
        runPasswordFileScenario(PW_TEST_PASSWORD + "\ndummyContent", 2);
    }

    /**
     * Tests handling of an existing password file containing the correct password, followed by a newline and a line feed (\r\n, 0x0D0A).
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testExistingCorrectPasswordFileWithExtraContentAfterTrailingCRLF() throws Exception {
        runPasswordFileScenario(PW_TEST_PASSWORD + "\r\ndummyContent", 2);
    }

    private void runPasswordFileScenario(String passwordFileContent, int expectedExitCode) throws IOException, InterruptedException,
        AssertionError {
        // TODO extract common code
        File pwFile = tempFileService.createTempFileFromPattern("pwfile-*.tmp");
        // TODO add an additional test for newline/content after the password
        FileUtils.writeStringToFile(pwFile, passwordFileContent);

        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, USER_PARAMETER, PW_TEST_USER_ACCOUNT,
                PASSWORD_FILE_PARAMETER, DOUBLE_QUOTE + pwFile.getAbsolutePath() + DOUBLE_QUOTE, OPERATION_ID_PROTOCOL_VERSION });
        ExecutionResult result = executorUtils.executeAndWait(command);

        try {
            if (expectedExitCode == 0) {
                parseAndValidateExecutionResult(result, 0, 1, 0);
                // check normal behavior; any operation would do here
                assertEquals(EXPECTED_PROTOCOL_VERSION, filteredStdout.get(0));
            } else {
                parseAndValidateExecutionResult(result, expectedExitCode, 0, 1);
                if (expectedExitCode == 1) {
                    assertTrue("Expected 'Authentication failure' message",
                        filteredStderr.get(0).contains(AUTHENTICATION_FAILURE_OUTPUT_TEXT_PART));
                } else {
                    assertFalse("Unexpected 'Authentication failure' message",
                        filteredStderr.get(0).contains(AUTHENTICATION_FAILURE_OUTPUT_TEXT_PART));
                }
            }
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests fetching a list of available tools, and checks that exactly the configured test tool is available.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void listToolsAndFindExpectedTestTools() throws Exception {
        String command = buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, OPERATION_ID_LIST_TOOLS });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            parseAndValidateExecutionResult(result, 0, 2, 0);
            assertTrue(filteredStdout.get(0).startsWith(
                TEST_TOOL_1_ID + TOOL_LIST_COLUMN_SEPARATOR + TEST_TOOL_1_VERSION + TOOL_LIST_COLUMN_SEPARATOR
                    + TEST_SERVER_NODE_ID + TOOL_LIST_COLUMN_SEPARATOR));
            assertTrue(filteredStdout.get(1).startsWith(
                TEST_TOOL_2_ID + TOOL_LIST_COLUMN_SEPARATOR + TEST_TOOL_2_VERSION + TOOL_LIST_COLUMN_SEPARATOR
                    + TEST_SERVER_NODE_ID + TOOL_LIST_COLUMN_SEPARATOR));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests a valid case of remote tool execution; node id is left empty, so the only available installation should be used.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithValidDirectoriesAndNoNodeId() throws Exception {
        setupTempInputDir();
        setupTempOutputDir();
        String cmdParameters = "123 - 546";
        generateInputFile(DEFAULT_TEST_FILENAME, DEFAULT_TESTFILE_CONTENT);
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, TEST_TOOL_1_ID, TEST_TOOL_1_VERSION,
                DOUBLE_QUOTE + cmdParameters + DOUBLE_QUOTE });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutLines = 15;
            parseAndValidateExecutionResult(result, 0, expectedStdoutLines, 0);
            validateOutputFile("params.txt", DOUBLE_QUOTE + cmdParameters + DOUBLE_QUOTE, true);
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests a remote tool execution that is valid except for an unsafe character in the parameter string.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testValidRunToolCommandWithInsecureParameterString() throws Exception {
        setupTempInputDir();
        setupTempOutputDir();
        // TODO once this test has a better parameterization setup, add more test cases for this
        String cmdParameters = "123 - 546`x";
        generateInputFile(DEFAULT_TEST_FILENAME, DEFAULT_TESTFILE_CONTENT);
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, TEST_TOOL_1_ID, TEST_TOOL_1_VERSION,
                DOUBLE_QUOTE + cmdParameters + DOUBLE_QUOTE });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutLines = 5; // side effect of no pre-validation in client, so execution is attempted
            parseAndValidateExecutionResult(result, 1, expectedStdoutLines, 1);
            // check for correct error message
            assertTrue(filteredStderr.get(0).contains("forbidden character"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests with the second test tool (which contains all allowed characters in its id and version).
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithTool2AndValidDirectoriesAndNoNodeId() throws Exception {
        setupTempInputDir();
        setupTempOutputDir();
        String cmdParameters = "123 546";
        generateInputFile(DEFAULT_TEST_FILENAME, DEFAULT_TESTFILE_CONTENT);
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, DOUBLE_QUOTE + TEST_TOOL_2_ID + DOUBLE_QUOTE,
                DOUBLE_QUOTE + TEST_TOOL_2_VERSION + DOUBLE_QUOTE, DOUBLE_QUOTE + cmdParameters + DOUBLE_QUOTE });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutLines = 15;
            parseAndValidateExecutionResult(result, 0, expectedStdoutLines, 0);
            validateOutputFile("params.txt", DOUBLE_QUOTE + cmdParameters + DOUBLE_QUOTE, true);
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests a valid case of remote tool execution; node id is left empty, so the only available installation should be used.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithValidDirectoriesAndCorrectNodeId() throws Exception {
        setupTempInputDir();
        setupTempOutputDir();
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, TOOL_NODE_ID_PARAMETER, TEST_SERVER_NODE_ID, TEST_TOOL_1_ID,
                TEST_TOOL_1_VERSION, QUOTED_EMPTY_STRING });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutLines = 15;
            parseAndValidateExecutionResult(result, 0, expectedStdoutLines, 0);
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests a valid case of remote tool execution; node id is left empty, so the only available installation should be used.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithValidDirectoriesAndWrongNodeId() throws Exception {
        setupTempInputDir();
        setupTempOutputDir();
        String wrongNodeId = "98989898989898989898989898989898";
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, TOOL_NODE_ID_PARAMETER, wrongNodeId,
                TEST_TOOL_1_ID, TEST_TOOL_1_VERSION, QUOTED_EMPTY_STRING });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutLines = 5;
            parseAndValidateExecutionResult(result, 1, expectedStdoutLines, 1);
            String errorMessage = filteredStderr.get(0);
            assertTrue(errorMessage.contains("No matching tool"));
            assertTrue("Error message should contain the tool id", errorMessage.contains(TEST_TOOL_1_ID));
            assertTrue("Error message should contain the tool version", errorMessage.contains(TEST_TOOL_1_VERSION));
            assertTrue("Error message should contain the wrong node id",
                errorMessage.contains("running on a node with id '" + wrongNodeId + "'"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests the behavior if no input directory is given, and the default does not exist.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithMissingInputDirectory() throws Exception {
        assertNoDefaultInputOutputDirectoriesExist();
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, OPERATION_ID_RUN_TOOL, TEST_TOOL_1_ID,
                TEST_TOOL_1_VERSION, QUOTED_EMPTY_STRING });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            parseAndValidateExecutionResult(result, 1, 0, 1);
            String errorMessage = filteredStderr.get(0);
            assertTrue("Expected input directory error message", errorMessage.contains("Input (upload) directory"));
            assertTrue("Expected input directory error message", errorMessage.contains("does not exist"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests the behavior if no output directory is given, and the default does not exist.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithMissingOutputDirectory() throws Exception {
        assertNoDefaultInputOutputDirectoriesExist();
        setupTempInputDir();
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OPERATION_ID_RUN_TOOL, TEST_TOOL_1_ID, TEST_TOOL_1_VERSION, QUOTED_EMPTY_STRING });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            parseAndValidateExecutionResult(result, 1, 0, 1);
            String errorMessage = filteredStderr.get(0);
            assertTrue("Expected output directory error message", errorMessage.contains("Output (download) directory"));
            assertTrue("Expected output directory error message", errorMessage.contains("does not exist"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests a valid case of remote tool execution.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithDisabledUploadAndDownload() throws Exception {
        generateInputFile(DEFAULT_TEST_FILENAME, "test content");
        String toolParameters = "test parameters";
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, QUOTED_EMPTY_STRING,
                OUTPUT_DIR_PARAMETER, QUOTED_EMPTY_STRING, OPERATION_ID_RUN_TOOL, TEST_TOOL_1_ID, TEST_TOOL_1_VERSION, DOUBLE_QUOTE
                    + toolParameters + DOUBLE_QUOTE });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutLines = 13;
            parseAndValidateExecutionResult(result, 0, expectedStdoutLines, 0);
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Triggers execution of a non-existing tool, and checks the error handling.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithInvalidToolId() throws Exception {
        String bogusToolId = "bogusId";
        String bogusToolVersion = TEST_TOOL_1_VERSION; // intentionally one of an existing tool
        setupTempInputDir();
        setupTempOutputDir();
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, bogusToolId, bogusToolVersion, "xyz" });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStdoutSize = 5;
            parseAndValidateExecutionResult(result, 1, expectedStdoutSize, 1);
            String errorMessage = filteredStderr.get(0);
            assertTrue("Error message should contain the invalid tool's id", errorMessage.contains(bogusToolId));
            assertTrue("Error message should contain the invalid tool's version", errorMessage.contains(bogusToolVersion));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Tests validation of the tool id with an invalid character.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithInvalidCharacterInToolId() throws Exception {
        String bogusToolId = "bogus#Id";
        String bogusToolVersion = TEST_TOOL_1_VERSION; // intentionally one of an existing tool
        setupTempInputDir();
        setupTempOutputDir();
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, bogusToolId, bogusToolVersion,
                DOUBLE_QUOTE + "xyz 2 #%§" + DOUBLE_QUOTE });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            final int expectedStderrSize = 1;
            parseAndValidateExecutionResult(result, 1, 0, expectedStderrSize);
            String errorMessage = filteredStderr.get(0);
            assertTrue("Error message should contain 'tool id'", errorMessage.contains("tool id"));
            assertTrue("Error message should describe the error", errorMessage.contains("invalid character at position 6"));
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    /**
     * Triggers execution of a non-existing tool with unusual characters in the tool id and version.
     * 
     * @throws Exception on unhandled test exceptions
     */
    @Test
    public void testRunToolCommandWithMalformedToolId() throws Exception {
        // TODO also test double quotes, \0, line breaks, ... -> check how to properly pass to tool via command line first
        String bogusToolId = "äöüÄÖÜß`´'  endOfToolId";
        String bogusToolVersion = "äöüÄÖÜß`´' endOfToolVersion";
        setupTempInputDir();
        setupTempOutputDir();
        String command =
            buildCommand(new String[] { HOST_PARAMETER, testIP, PORT_PARAMETER, testPort, INPUT_DIR_PARAMETER, inputDirPath,
                OUTPUT_DIR_PARAMETER, outputDirPath, OPERATION_ID_RUN_TOOL, bogusToolId, bogusToolVersion, "xyz" });
        ExecutionResult result = executorUtils.executeAndWait(command);
        try {
            parseAndValidateExecutionResult(result, 2, 0, 1);
            // String errorMessage = filteredStderr.get(0);
            // TODO add more conditions for error message
        } catch (AssertionError e) {
            logFullOutputOnTestFailureAndRethrow(filteredStdout, filteredStderr, e);
        }
    }

    private void setupTempInputDir() throws IOException {
        assertNull(inputDir);
        inputDir = tempFileService.createManagedTempDir("input");
        inputDirPath = DOUBLE_QUOTE + inputDir.getAbsolutePath() + DOUBLE_QUOTE; // for safe(r) CLI execution
    }

    private void setupTempOutputDir() throws IOException {
        assertNull(outputDir);
        outputDir = tempFileService.createManagedTempDir("output");
        outputDirPath = DOUBLE_QUOTE + outputDir.getAbsolutePath() + DOUBLE_QUOTE; // for safe(r) CLI execution
    }

    private void generateInputFile(String filename, String content) throws IOException {
        File file = new File(inputDir, filename);
        FileUtils.writeStringToFile(file, content);
    }

    private void validateOutputFile(String filename, String content, boolean ignoreOuterWhitespace) throws IOException {
        File file = new File(outputDir, filename);
        assertTrue("Expected output file " + filename + " does not exist", file.isFile());
        assertEquals("Content of output file " + filename + " does not match with expectation", content.trim(),
            FileUtils.readFileToString(file).trim());
    }

    private void assertNoDefaultInputOutputDirectoriesExist() {
        File defaultInputDir = new File(standaloneDirectory, "input");
        assertFalse("There is a default 'input' directory in the client directory; it must be removed for reliable testing",
            defaultInputDir.exists());
        File defaultOutputDir = new File(standaloneDirectory, "output");
        assertFalse("There is a default 'output' directory in the client directory; it must be removed for reliable testing",
            defaultOutputDir.exists());
    }

    private String buildCommand(String[] cmdParts) {
        StringBuilder buffer = new StringBuilder();
        if (OSFamily.isLinux()) {
            buffer.append("export LD_LIBRARY_PATH=. && ");
        }
        buffer.append(standaloneExecutable.getAbsolutePath());
        for (String cmdPart : cmdParts) {
            // TODO add escaping/quoting
            buffer.append(' ');
            buffer.append(cmdPart);
        }
        String command = buffer.toString();
        log.debug("Command line: " + command);
        return command;
    }

    private List<String> logOutputAndRemoveDebugLines(List<String> original, String prefix) {
        List<String> filtered = new ArrayList<>(original.size());
        for (String line : original) {
            log.debug(prefix + line);
            if (!line.startsWith("[DEBUG] ")) {
                filtered.add(line);
            }
        }
        return filtered;
    }

    private void parseAndValidateExecutionResult(ExecutionResult result, int exitCode, int stdOutSize, int stdErrSize)
        throws AssertionError {
        filteredStdout = logOutputAndRemoveDebugLines(result.stdoutLines, STDOUT_LOG_PREFIX);
        filteredStderr = logOutputAndRemoveDebugLines(result.stderrLines, STDERR_LOG_PREFIX);
        assertEquals(UNEXPECTED_EXIT_CODE, exitCode, result.exitCode);
        assertEquals(UNEXPECTED_LINE_COUNT_ON_STD_OUT, stdOutSize, filteredStdout.size());
        assertEquals(UNEXPECTED_LINE_COUNT_ON_STD_ERR, stdErrSize, filteredStderr.size());
    }

    private void logFullOutputOnTestFailureAndRethrow(List<String> stdout, List<String> stderr, AssertionError e)
        throws AssertionError {
        // log full output on test failure
        log.error(TEST_FAILED_COMPLETE_OUTPUT_FOR + STDOUT_LOG_PREFIX + stdout);
        log.error(TEST_FAILED_COMPLETE_OUTPUT_FOR + STDERR_LOG_PREFIX + stderr);
        throw e;
    }
}
