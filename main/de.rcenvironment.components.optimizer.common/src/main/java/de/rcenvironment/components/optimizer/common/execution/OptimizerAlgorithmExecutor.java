/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.common.OptimizerComponentHistoryDataItem;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * Class to provide running an external program that has to be blocked for a RCE wf calculation.
 * 
 * @author Sascha Zur
 */
public abstract class OptimizerAlgorithmExecutor implements Runnable {

    /***/
    protected static final Log LOGGER = LogFactory.getLog(OptimizerAlgorithmExecutor.class);

    protected static final int SLEEPTIME = 10;

    protected static TypedDatumFactory typedDatumFactory;

    private static final int SOCKET_TIMEOUT = 0;

    protected File workingDir;

    protected String inputFileEnding;

    protected ClientMessage messageFromClient;

    protected String port;

    protected String inputFileName;

    protected ComponentContext compContext;

    protected AtomicBoolean initFailed = new AtomicBoolean(false);

    protected AtomicBoolean startFailed = new AtomicBoolean(false);

    protected Exception startFailedException = null;

    private LocalApacheCommandLineExecutor executor;

    private Boolean stop;

    private Thread serverThread;

    private String outputFilename;

    private ServerSocket serverSocket;

    private Socket client;

    private boolean initializationLoop;

    public OptimizerAlgorithmExecutor() {

    }

    public OptimizerAlgorithmExecutor(ComponentContext ci, String newInputFileName, String outputFilename) {
        try {
            stop = false;
            workingDir = TempFileServiceAccess.getInstance().createManagedTempDir();
            inputFileName = newInputFileName;
            this.outputFilename = outputFilename;
            this.compContext = ci;

            // the fragment bundle with the binaries should not have this executor bundle as host
            // because of the build process. Instead, the Optimizer.common bundle is host and so
            // the classpath of common + fragment are merged. For getting the resources stream a
            // class from the common bundle (here the CommonBundleClasspathStub) is needed.
            InputStream jarInput = OptimizerAlgorithmExecutor.class.getResourceAsStream(
                "/resources/de.rcenvironment.components.optimizer.simulator.jar");

            File jar = new File(workingDir.getAbsolutePath() + File.separator + "de.rcenvironment.components.optimizer.simulator.jar");
            FileUtils.copyInputStreamToFile(jarInput, jar);
            jar.setExecutable(true);
        } catch (IOException e) {
            LOGGER.error("Error in constructor", e);
        }
    }

    protected abstract void prepareProblem() throws ComponentException;

    /**
     * Reads the output of the optimizer. The values are used in the RCE workflow.
     * 
     * @param outputValues that are read.
     * @throws IOException reading the file
     */
    public abstract void readOutputFileFromExternalProgram(Map<String, TypedDatum> outputValues) throws IOException;

    protected abstract void writeInputFileforExternalProgram(Map<String, Double> inputVariables,
        Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables,
        String outputFileName) throws IOException;

    /**
     * Returns the optimum for the last optimization run, if the run was successful.
     * 
     * @return the double values for all design variable outputs from the optimum
     */
    public abstract Map<String, Double> getOptimalDesignVariables();

    @Override
    public abstract void run();

    private void writePortFile() {
        try {
            File portFile = new File(workingDir.getAbsolutePath() + File.separator + inputFileName + ".port");
            List<String> lines = new LinkedList<String>();
            lines.add("" + port);
            FileUtils.writeLines(portFile, lines, System.getProperty("line.separator"));
        } catch (IOException e) {
            LOGGER.error("Could not write port file", e);
        }
    }

    /**
     * Needed for the initialization loop.
     * 
     * @throws ComponentException if init fails
     * @return true, if loop was successful
     */
    public boolean initializationLoop() throws ComponentException {
        initializationLoop = true;
        boolean returnValue = false;
        try {
            if (!isStopped()) {
                runNewServer();
                while (client == null) {
                    try {
                        Thread.sleep(SLEEPTIME);
                    } catch (InterruptedException e) {
                        LOGGER.error("Error in blocker ", e);
                    }
                    if (initFailed.get()) {
                        throw (ComponentException) startFailedException;
                    }
                    if (startFailed.get()) {
                        break;
                    }
                }
                if (client != null) {
                    returnValue = readMessageFromClient();
                }
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        initializationLoop = false;
        return returnValue && !isStopped() && !isInitFailed() && !getStartFailed().get();
    }

    /**
     * Starts the program that shall be blocked.
     * 
     * @param executionCommand : to execute
     * @throws ComponentException
     */
    protected void startProgram(String executionCommand) throws ComponentException {
        startProgram(executionCommand, "");
    }

    /**
     * Starts the program that shall be blocked.
     * 
     * @param executionCommand : to execute
     * @throws ComponentException
     */
    protected void startProgram(String executionCommand, String previousCommand) throws ComponentException {
        prepareProblem();
        if (!initializationLoop) {
            startServer();
        }
        try {
            String javaHome = System.getProperty("java.home");
            if (!javaHome.contains("bin")) {
                javaHome += File.separator + "bin";
            }
            File javaHomefile = new File(workingDir.getAbsolutePath() + File.separator + "javaHome.txt");
            FileWriter fw = new FileWriter(javaHomefile);
            fw.write("" + javaHome);
            fw.close();
            String command = "";
            if (OS.isFamilyUnix()) {
                if (previousCommand.equalsIgnoreCase("")) {
                    command = executionCommand + inputFileName;
                } else {
                    command =
                        previousCommand + " " + workingDir.getAbsolutePath() + File.separator + executionCommand + inputFileName;
                }
            } else if (previousCommand == null || previousCommand.equalsIgnoreCase("")) {
                command = executionCommand + inputFileName;
            } else {
                command = previousCommand + " " + executionCommand + inputFileName;
            }
            executor = new LocalApacheCommandLineExecutor(new File(workingDir.getAbsolutePath() + File.separator));
            executor.start(command);
            executor.setEnv("PATH", System.getenv("PATH") + File.pathSeparator + javaHome);
            File consoleStdOutput = new File(workingDir, "consoleStdOutput.txt");
            File consoleErrOutput = new File(workingDir, "consoleErrOutput.txt");
            ConsoleRowUtils.logToWorkflowConsole(compContext, executor.getStdout(), ConsoleRow.Type.STDOUT,
                consoleStdOutput,
                false);
            ConsoleRowUtils.logToWorkflowConsole(compContext, executor.getStderr(), ConsoleRow.Type.STDERR,
                consoleErrOutput,
                false);
            try {
                int exitCode = executor.waitForTermination();
                if (exitCode != 0) {
                    if (!initFailed.get()) {
                        startFailed.set(true);
                        startFailedException = new ComponentException("Optimizer exited with a non zero exit code. "
                            + "Optimizer exit code = " + exitCode);
                        throw new ComponentException("Optimizer exited with a non zero exit code. "
                            + "Optimizer exit code = " + exitCode);
                    }
                }
                stop();
            } catch (InterruptedException e) {
                LOGGER.info("Optimizer closed while sleeping");
            }
        } catch (IOException e) {
            LOGGER.error("ProgramBlocker: IOException " + e.getMessage());
        }
        try {
            Thread.sleep(SLEEPTIME);
        } catch (InterruptedException e) {
            LOGGER.info("ProgramBlocker: InterruptedException " + e.getMessage());
        }
    }

    /**
     * Runs a single optimization step.
     * 
     * @param inputVariables : All target functions
     * @param inputVariablesGradients : all gradients for the target functions
     * @param constraintVariables : all constraints
     * @param constraintVariablesGradients : all gradients for the constraints
     * @param outputValues : all design variables
     */
    public void runStep(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables, Map<String, Double> constraintVariablesGradients,
        Map<String, TypedDatum> outputValues) {
        try {
            if (!isStopped()) {
                if (messageFromClient != null) {
                    writeInputFileforExternalProgram(inputVariables, inputVariablesGradients,
                        constraintVariables, outputFilename);
                    sendMessageToClient("Close");
                    try {
                        serverThread = null;
                        serverThread = runNewServer();
                        // Wait for client to connect or termination of program thread
                        while (client == null && !isStopped()) {
                            try {
                                Thread.sleep(SLEEPTIME);
                            } catch (InterruptedException e) {
                                LOGGER.error("Error in blocker ", e);
                            }
                        }
                        if (serverThread != null) {
                            synchronized (serverThread) {
                                serverThread.interrupt();
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error in blocker", e);
                    }
                    if (!stop) {
                        if (readMessageFromClient()) {
                            readOutputFileFromExternalProgram(outputValues);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error in blocker", e);
        }
    }

    private void sendMessageToClient(String message) throws IOException {
        if (client != null && !client.isClosed()) {
            PrintWriter printWriter =
                new PrintWriter(
                    new OutputStreamWriter(
                        client.getOutputStream()));
            printWriter.print(message);
            printWriter.flush();
            client.close();
            client = null;
        }
    }

    private boolean readMessageFromClient() throws IOException {
        if (!client.isClosed()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            final int buffersize = 1024;
            char[] buffer = new char[buffersize];
            int blockLength = bufferedReader.read(buffer, 0, buffersize);
            String nachricht = new String(buffer, 0, blockLength);
            if (!nachricht.equals("exit")) {
                String[] splitMessage = nachricht.split("&&");
                this.messageFromClient = new ClientMessage(splitMessage[0], splitMessage[1]);
                return true;
            }

        }
        return false;
    }

    private void startServer() {
        if (!stop) {
            try {
                if (port == null || port.equals("")) {
                    serverSocket = new ServerSocket(0);
                    port = "" + serverSocket.getLocalPort();
                } else {
                    serverSocket = new ServerSocket(Integer.valueOf(port));
                }
                serverSocket.setSoTimeout(SOCKET_TIMEOUT);
                writePortFile();
            } catch (IOException e) {
                LOGGER.error("Could not start server", e);
            }
        }
    }

    private Thread runNewServer() throws IOException {
        if (serverSocket == null) {
            startServer();
        }
        Thread newServerThread = new Thread() {

            @Override
            public void run() {
                try {
                    if (serverSocket != null) {
                        client = serverSocket.accept();
                    }
                } catch (IOException e) {
                    if (isStopped()) {
                        LOGGER.debug("Socket closed because program finished");
                    }
                }
            }
        };
        newServerThread.start();
        return newServerThread;
    }

    /**
     * Stops everything.
     */
    public void stop() {
        synchronized (stop) {
            stop = true;
        }
        try {
            if (serverThread != null) {
                synchronized (serverThread) {
                    if (port != null && serverSocket != null && serverThread.isAlive()) {
                        Socket server = new Socket("localhost", Integer.parseInt(port));
                        PrintWriter printWriter =
                            new PrintWriter(
                                new OutputStreamWriter(
                                    server.getOutputStream()));
                        printWriter.print("exit");
                        printWriter.flush();
                        server.close();
                    }
                }
            } else {
                startFailed.set(true);
            }
        } catch (IOException e) {
            LOGGER.debug("Stopping optimizer failed", e);
        }

    }

    /**
     * Terminates the connection to the client (jar blocker).
     */
    public void closeConnection() {
        try {
            sendMessageToClient("Close");
            if (executor != null) {
                executor.waitForTermination();
            }
        } catch (IOException e) {
            LOGGER.debug("Socket closed because program terminated");
        } catch (InterruptedException e) {
            LOGGER.debug("Socket closed because program terminated");
        }
    }

    /**
     * Gets rid of all tmp files.
     */
    public void dispose() {
        try {
            if (client != null) {
                this.client.close();
            }
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
            if (serverThread != null) {
                serverThread.interrupt();
            }
            if (executor != null) {
                executor.waitForTermination();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to dispose blocker tempfiles", e);
        } catch (InterruptedException e) {
            LOGGER.warn("Failed to dispose blocker tempfiles", e);
        }
    }

    public String getPort() {
        return port;
    }

    /**
     * 
     * @return true, if optimizer is stopped
     */
    public boolean isStopped() {
        synchronized (stop) {
            return stop;
        }
    }

    /**
     * Helper class to pass the information from the jar file.
     * 
     * @author Sascha Zur
     */
    public class ClientMessage {

        private String currentWorkingDir;

        private String filename;

        public ClientMessage(String currentWorkingDir, String filename) {
            super();
            this.setCurrentWorkingDir(currentWorkingDir);
            this.setFilename(filename);
        }

        public String getCurrentWorkingDir() {
            return currentWorkingDir;
        }

        public void setCurrentWorkingDir(String currentWorkingDir) {
            this.currentWorkingDir = currentWorkingDir;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

    }

    protected int countInput(Collection<String> input) {
        int result = 0;
        for (String e : input) {
            if (compContext.getDynamicInputIdentifier(e).equals(OptimizerComponentConstants.ID_OBJECTIVE)
                && !e.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                if (compContext.getInputDataType(e) == DataType.Vector) {
                    result += Integer.parseInt(compContext.getInputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE));
                } else {
                    result++;
                }
            }
        }
        return result;
    }

    protected int countConstraint(Collection<String> input) {
        int result = 0;
        for (String e : input) {
            if (compContext.getDynamicInputIdentifier(e).equals(OptimizerComponentConstants.ID_CONSTRAINT)
                && !e.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                if (compContext.getInputDataType(e) == DataType.Vector) {
                    result += Integer.parseInt(compContext.getInputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE));
                } else {
                    result++;
                }
            }
        }
        return result;
    }

    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumFactory = newTypedDatumService.getFactory();
    }

    protected void unbindTypedDatumService(TypedDatumService oldTypedDatumService) {}

    public boolean isInitFailed() {
        return initFailed.get();
    }

    /**
     * Checks if for the current run derivatives are needed from the workflow.
     * 
     * @return true, if they are needed.
     */
    public abstract boolean getDerivativedNeeded();

    /**
     * Individual writing the history data item.
     * 
     * @param historyItem to write
     */
    public abstract void writeHistoryDataItem(OptimizerComponentHistoryDataItem historyItem);

    public File getWorkingDir() {
        return workingDir;
    }

    public AtomicBoolean getStartFailed() {
        return startFailed;
    }

    public void setStartFailed(AtomicBoolean startFailed) {
        this.startFailed = startFailed;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }
}
