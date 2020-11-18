/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.scripting.python.PythonScriptEngine;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.legacy.FileSupport;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * Class to communicate with a python instance. It contains a socket object and the belonging streams.
 * 
 * @author Adrian Stock
 * @author Niklas Foerst
 * @author Alexander Weinert
 *
 */
public class PythonAgent implements Runnable {

    private static final String SIMPLEJSON = "simplejson.zip";

    private static final String RESOURCES = "/resources/";

    private static final String PYTHON_BRIDGE = "RCE_Channel.py";

    private static final String PYTHON_CLIENT = "RCE_PythonClient.py";

    private static final String INPUT_FILE_FACTORY_PY = "input_file_factory.py";

    private static final String ESCAPESLASH = "\\\\";

    private static final String SLASH = "/";

    private static final int HUGE_NUMBER = 999999;

    private static final Log LOGGER = LogFactory.getLog(PythonScriptEngine.class);

    private static final String STANDARD_ERROR_MESSAGE = "Error occured when receiving a message.";

    protected Scanner inputScanner;

    protected ComponentContext compCtx;

    private LocalApacheCommandLineExecutor executor;

    private Long authentificationNumber;

    private PrintWriter out;

    private File tempDir;

    private int number;

    private PythonAgentInstanceManager instanceManager;

    private String pythonInstallationPath;

    private ServerSocket serverSocket;

    private CountDownLatch initializationSignal;

    private CountDownLatch pythonStartSignal;

    private boolean pythonInstanceIsRunning;

    private boolean initializationWasSuccessful;

    private final Semaphore lock = new Semaphore(1);

    // @taskDescription Python Agent
    public PythonAgent(PythonAgentInstanceManager instanceManager, String pythonInstallationPath,
        int number, ServerSocket serverSocket, CountDownLatch initializationSignal, ComponentContext compCtx) throws ScriptException {
        this.instanceManager = instanceManager;
        this.number = number;
        this.serverSocket = serverSocket;
        this.pythonInstallationPath = pythonInstallationPath;
        this.initializationSignal = initializationSignal;
        this.compCtx = compCtx;

        createFiles();
    }

    @Override
    public void run() {
        boolean successfulInitialization;
        successfulInitialization = init();
        if (successfulInitialization) {
            try {
                initializationWasSuccessful = registerPythonInstance();
            } catch (IOException e) {
                initializationWasSuccessful = false;
            }
        }
        if (initializationWasSuccessful) {
            initializationSignal.countDown();
            writeNewLineToLog();
        } else {
            stopPythonInstance();
            initializationSignal.countDown();
        }
    }

    /**
     * Returns a boolean which says, if the initialization of the {@link PythonAgent} was successful.
     * 
     * @return true, if initialization was successful
     */
    public boolean wasInitializationSuccessful() {
        return initializationWasSuccessful;
    }

    /**
     * Initializes the socket and its streams.
     * 
     * @return true, if initialization was successful
     */
    public boolean init() {
//      Create the python instance and connect it to the socket
        Socket socket;

        pythonStartSignal = new CountDownLatch(1);
        startPythonInstance();

        try {
            pythonStartSignal.await();
        } catch (InterruptedException e) {
            return false;
        }

        if (!pythonInstanceIsRunning) {
            return false;
        }

        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
            stopPythonInstance();
            return false;
        }

//      Create streams to communicate with the python instance
        try {
            inputScanner = new Scanner(socket.getInputStream(), StandardCharsets.UTF_8.displayName());
            inputScanner.useDelimiter("\0");
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            stopPythonInstance();
            return false;
        }
        return true;
    }

    private void stopPythonInstance() {
        executor.cancel();
    }

    /**
     * Starts the python instance which will connect to the opened socket.
     */
    private void startPythonInstance() {

        executor = instanceManager.createNewExecutor();
        executor.setWorkDir(tempDir);

        ConcurrencyUtils.getAsyncTaskService().execute("PythonInstance", new Runnable() {

            @Override
            public void run() {

                authentificationNumber = (long) (Math.random() * HUGE_NUMBER);

                String pythonScriptPath = tempDir.getAbsolutePath() + "/" + PYTHON_CLIENT;

                String command = pythonInstallationPath + " -u " + pythonScriptPath + " "
                    + Integer.toString(serverSocket.getLocalPort()) + " " + authentificationNumber.toString();

                try {
                    executor.setWorkDir(new File(tempDir.getAbsolutePath()));
                    executor.start(pythonInstallationPath + " -V");
                    if (executor.waitForTermination() == 0) {
                        try {

                            executor.start(command);

                            pythonInstanceIsRunning = true;

                            pythonStartSignal.countDown();
                        } catch (IOException e) {
                            pythonInstanceIsRunning = false;
                            pythonStartSignal.countDown();
                        }

                    } else {
                        pythonInstanceIsRunning = false;
                        pythonStartSignal.countDown();
                    }

                } catch (IOException | InterruptedException e1) {
                    pythonInstanceIsRunning = false;
                    pythonStartSignal.countDown();
                }

                ConsoleRowUtils.logToWorkflowConsole(compCtx.getLog(), executor.getStdout(),
                    ConsoleRow.Type.TOOL_OUT, null, false);
                ConsoleRowUtils.logToWorkflowConsole(compCtx.getLog(), executor.getStderr(),
                    ConsoleRow.Type.TOOL_ERROR, null, false);
            }
        });
    }

    /**
     * Each python instance receives a token at its call for the authorization.
     */
    private boolean registerPythonInstance() throws IOException {
        sendMessage("Token request");
        String input = recvMessage();
        if (input == null) {
            return false;
        }
        if (!authentificationNumber.toString().equals(input)) {
            sendMessage("Token declined.");
            return false;
        }
        sendMessage("Token accepted.");
        return true;
    }

    public ScriptJSONObject scriptToJSON(String pythonCommand, String script)
        throws JsonMappingException, FileNotFoundException, IOException {

        ScriptJSONObject jsonObject = new ScriptJSONObject();

        jsonObject.setPythonComamnd(pythonCommand);
        jsonObject.setScript(script);

        return jsonObject;

    }

    /**
     * Sends the script to the Python instance. After receiving the script, Python will execute it.
     * 
     * @param script which will be executed.
     * @throws IOException if the communication between Java and the Python instance fails.
     * @throws ComponentException if the script couldn't be executed successfully.
     */

    public void executeScript(String script) throws IOException, ComponentException {

        // String[] scriptAsArray = script.split("\n");
        compCtx.getLog().componentInfo("Execute Python Script:");
        sendMessage("executeUserscript");

        recvMessage(); // ReadyToExecuteScript
//        sendScriptLength(scriptAsArray);
//        recvMessage(); // ReceivedScriptLength
        ScriptJSONObject jsonObject = scriptToJSON("execute", script);
        // LOGGER.debug("executeScript: " + jsonObject.toString());
        sendScript(jsonObject);
        String successfulExecution = recvMessage(); // Finished script execution successfully. Waiting for next task.
        if ("Error when executing the script. Waiting for next task.".equals(successfulExecution)) {
            throwGenericException();
        } else if ("Error when writing output to temporary folder.".equals(successfulExecution)) {
            throwGenericException();
        }
        writeNewLineToLog();
    }

    private void throwGenericException() throws ComponentException {
        throw new ComponentException("Failed to execute script.");
    }

    protected void writeNewLineToLog() {
        compCtx.getLog().componentInfo("");
    }

    private void sendScript(ScriptJSONObject scriptObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        sendMessage(objectMapper.writeValueAsString(scriptObject));
        out.flush();

    }

    /**
     * Stops the Python instance.
     * 
     * @throws IOException while receiving a message from the python instance.
     */
    public void stopInstance() {
        try {
            sendMessage("stopInstance");
            recvMessage();
        } catch (IOException e) {
            LOGGER.error("Error occured when closing the python instance.");
            stopPythonInstance();
        }

        int exitCode;
        try {
            exitCode = executor.waitForTermination();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while waiting for termination of python instance: " + e.getMessage());
            return;
        } 

        if (exitCode != 0) {
            LOGGER.error("Python instance terminated with non-zero exit code: " + exitCode);
        }
    }

    /**
     * Stops the Python instance when an error occured during the script execution.
     */
    public void stopInstanceRun() {
        try {
            sendMessage("stopInstanceRun");
            recvMessage();
        } catch (IOException e) {
            LOGGER.error("Error occured when closing the python instance.");
            stopPythonInstance();
        }
    }

    protected void sendMessage(String message) {
        //LOGGER.debug("Java Class : " + message);
        out.print(message + '\0');
        out.flush();

    }

    protected String recvMessage() throws IOException {
        //LOGGER.debug("Waiting for answer from python");
        final String input = inputScanner.next();
        //LOGGER.debug("Python Instance: " + input);
        return input;
    }

    /**
     * Creates the wrapper for the python script. Creates the script for the python instance, which communicates with RCE.
     * 
     * @throws IOException if the resources can't be loaded.
     */
    private void createFiles() throws ScriptException {

        try {
            tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("pythonAgent");
        } catch (final IOException e) {
            LOGGER.error("Could not create managed temp directory, falling back to default");
            try {
                final File tmp = File.createTempFile("prefix", "suffix");
                tempDir = tmp.getParentFile();
                tmp.delete(); // not needed
            } catch (final IOException e1) {
                LOGGER.error("Failed to fall back.");
                throw new ScriptException("Unable to create temp file and directory");
            }
        }
        try {
            File wrapperBridge = new File(tempDir, PYTHON_BRIDGE);
            try (InputStream wrapperScriptInputBridge = PythonScriptEngine.class
                .getResourceAsStream(RESOURCES + PYTHON_BRIDGE)) {
                FileUtils.copyInputStreamToFile(wrapperScriptInputBridge, wrapperBridge);
                try (InputStream simpleJsonFiles = PythonScriptEngine.class
                    .getResourceAsStream(RESOURCES + SIMPLEJSON)) {
                    FileSupport.unzip(simpleJsonFiles, tempDir);
                    File pythonClient = new File(tempDir, PYTHON_CLIENT);
                    try (InputStream wrapperScriptInputClient = PythonScriptEngine.class
                        .getResourceAsStream(RESOURCES + PYTHON_CLIENT)) {
                        FileUtils.copyInputStreamToFile(wrapperScriptInputClient, pythonClient);
                    }
                    File inputFileFactory = new File(tempDir, INPUT_FILE_FACTORY_PY);
                    try (InputStream wrapperScriptInputFactory = PythonScriptEngine.class
                        .getResourceAsStream(RESOURCES + INPUT_FILE_FACTORY_PY)) {
                        FileUtils.copyInputStreamToFile(wrapperScriptInputFactory, inputFileFactory);

                        String path = "\n" + "InputFileFactory.p = " + "'" + tempDir.getPath().replaceAll(ESCAPESLASH, SLASH) + "/'";

                        FileUtils.writeStringToFile(inputFileFactory, path, true);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create temporary Python scripts.");
            throw new ScriptException("Unable to create temporary Python scripts");
        }
    }

    public File getDirectory() {
        return tempDir;
    }

    public InputStream getStdout() {
        return executor.getStdout();
    }

    public InputStream getStderr() {
        return executor.getStderr();
    }

    public String getInstallationPath() {
        return this.pythonInstallationPath;
    }

    public void acquireLock() throws InterruptedException {
        this.lock.acquire();
    }

    public void releaseLock() {
        this.lock.release();
    }
}
