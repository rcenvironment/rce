/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting.python;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.scripting.ScriptDataTypeHelper;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.legacy.FileSupport;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * Implementation of {@link ScriptEngine} for the python language.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel (#14029)
 */
public class PythonScriptEngine implements ScriptEngine {

    private static final String SIMPLEJSON = "simplejson.zip";

    private static final String RESOURCES = "/resources/";

    private static final String PYTHON_BRIDGE = "RCE_Channel.py";

    private static final String RUN_SCRIPT = "Run_python_script_in_rce.py";

    private static final Log LOGGER = LogFactory.getLog(PythonScriptEngine.class);

    private static final String ESCAPED_DOUBLE_QUOTE = "\"";

    private static final int EXIT_CODE_FAILURE = 1;

    private static ComponentDataManagementService componentDatamanagementService;

    private File tempDir;

    private ScriptContext context;

    private LocalApacheCommandLineExecutor executor;

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private Map<String, Serializable> output = new HashMap<>();

    private final List<File> tempFiles = new LinkedList<>();

    private List<String> closeOutputChannelsList = new LinkedList<>();

    private TextStreamWatcher stdoutWatcher;

    private TextStreamWatcher stderrWatcher;

    private Map<String, Object> stateOutput;

    /**
     * This latch is used to ensure that a cancellation request is not performed during the preparation of the script execution is executed,
     * but after the initialization is completed.
     */
    private CountDownLatch initializationSignal;

    /**
     * Set to true, if the script engine was canceled before it is properly initialized.
     */
    private boolean canceledBeforeInitialization = false;

    /**
     * Creates a new executor.
     * 
     * @param dataItem {@link ScriptComponentHistoryDataItem} object for this script execution
     */
    public synchronized void createNewExecutor(CommonComponentHistoryDataItem dataItem) {

        if (canceledBeforeInitialization) {
            LOGGER.error("Failed to create executor for python, since the SriptEngine was already canceled.");
            return;
        }

        // This method is synchronized to avoid a race condition with cancel
        try {
            executor = new LocalApacheCommandLineExecutor(null);
            initializationSignal = new CountDownLatch(1);
        } catch (IOException e) {
            LOGGER.error("Failed to create executor for python.");
        }
    }

    @Override
    public Bindings createBindings() {
        return null;
    }

    @Override
    public Object eval(String script) throws ScriptException {
        // find temp directory for all intermediate files
        try {
            tempDir = TempFileServiceAccess.getInstance().createManagedTempDir("python");
            tempFiles.add(tempDir);
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
        writeInputForPython();
        // run script
        try {
            createTemporaryPythonScript(script);
        } catch (IOException e) {
            LOGGER.error("Failed to create temporary Python script.");
        }
        executor.setWorkDir(tempDir);
        final String command =
            ESCAPED_DOUBLE_QUOTE + ((String) context.getAttribute(PythonComponentConstants.PYTHON_INSTALLATION)) + ESCAPED_DOUBLE_QUOTE
                + " -u "
                + tempDir.getAbsolutePath() + File.separator + RUN_SCRIPT;
        LOGGER.debug("PythonExecutor executes command: " + command);

        int exitCode = 0;
        try {
            executor.start(command);
            prepareOutputForRun();

            // as soon as we reach this position the execution can be interrupted
            initializationSignal.countDown();

            try {
                exitCode = executor.waitForTermination();
                stdoutWatcher.waitForTermination();
                stderrWatcher.waitForTermination();

            } catch (InterruptedException e) {
                LOGGER.error("ProgramBlocker: InterruptedException " + e.getMessage());
                return EXIT_CODE_FAILURE;
            } catch (CancellationException e) {
                LOGGER.debug("Execution canceled while waiting for termination of TextStreamWatcher.");
                return EXIT_CODE_FAILURE;
            }
        } catch (IOException e) {
            LOGGER.error("Something during Python execution failed. See exception for details", e);
        }
        readOutputFromPython();
        return exitCode;
    }

    private void prepareOutputForRun() {

        stdoutWatcher =
            ConsoleRowUtils.logToWorkflowConsole(((ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT))
                .getLog(), executor.getStdout(), ConsoleRow.Type.TOOL_OUT, null, false);
        stderrWatcher =
            ConsoleRowUtils.logToWorkflowConsole(((ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT))
                .getLog(), executor.getStderr(), ConsoleRow.Type.TOOL_ERROR, null, false);
    }

    private void writeInputForPython() {
        ComponentContext compContext = (ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT);
        Map<String, Object> inputsToWrite = new HashMap<>();
        for (String inputName : compContext.getInputsWithDatum()) {
            switch (compContext.getInputDataType(inputName)) {
            case FileReference:
                FileReferenceTD fileReference = (FileReferenceTD) compContext.readInput(inputName);
                File fileInputDir = new File(tempDir, inputName);
                tempFiles.add(fileInputDir);
                File file = new File(fileInputDir, fileReference.getFileName());
                try {
                    componentDatamanagementService.copyFileReferenceTDToLocalFile(compContext, fileReference, file);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load input file from the data management", e);
                }
                inputsToWrite.put(inputName, file.getAbsolutePath().toString().replaceAll("\\\\", "/"));
                break;
            case DirectoryReference:
                DirectoryReferenceTD directoryReference = (DirectoryReferenceTD) compContext.readInput(inputName);
                File dirInputDir = new File(tempDir, inputName);
                tempFiles.add(dirInputDir);
                File dir = new File(dirInputDir, directoryReference.getDirectoryName());
                try {
                    componentDatamanagementService.copyDirectoryReferenceTDToLocalDirectory(compContext,
                        (DirectoryReferenceTD) compContext.readInput(inputName), dirInputDir);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load input directory from the data management", e);
                }
                inputsToWrite.put(inputName, dir.getAbsolutePath().toString().replaceAll("\\\\", "/"));
                break;
            case Boolean:
                boolean bool = (((BooleanTD) compContext.readInput(inputName)).getBooleanValue());
                if (bool) {
                    inputsToWrite.put(inputName, true);
                } else {
                    inputsToWrite.put(inputName, false);
                }
                break;
            case ShortText:
                inputsToWrite.put(inputName, ((ShortTextTD) compContext.readInput(inputName)).getShortTextValue());
                break;
            case Integer:
                inputsToWrite.put(inputName, ((IntegerTD) compContext.readInput(inputName)).getIntValue());
                break;
            case Float:
                if (compContext.readInput(inputName) instanceof FloatTD) {
                    inputsToWrite.put(inputName, ((FloatTD) compContext.readInput(inputName)).getFloatValue());
                } else if (compContext.readInput(inputName) instanceof IntegerTD) {
                    inputsToWrite.put(inputName, ((IntegerTD) compContext.readInput(inputName)).getIntValue());
                }
                break;
            case Vector:
                VectorTD vector = (VectorTD) compContext.readInput(inputName);
                Object[] resultVector = new Object[vector.getRowDimension()];
                for (int j = 0; j < vector.getRowDimension(); j++) {
                    resultVector[j] = vector.getFloatTDOfElement(j).getFloatValue();
                }
                inputsToWrite.put(inputName, resultVector);
                break;
            case Matrix:
                MatrixTD matrix = (MatrixTD) compContext.readInput(inputName);
                if (matrix.getRowDimension() > 1) {
                    Object[][] result = new Object[matrix.getRowDimension()][matrix.getColumnDimension()];
                    for (int i = 0; i < result.length; i++) {
                        for (int j = 0; j < result[0].length; j++) {
                            result[i][j] = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(matrix.getFloatTDOfElement(i, j));
                        }
                    }
                    inputsToWrite.put(inputName, result);
                } else {
                    Object[] result = new Object[matrix.getColumnDimension()];
                    for (int j = 0; j < matrix.getColumnDimension(); j++) {
                        result[j] = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(matrix.getFloatTDOfElement(0, j));
                    }
                    inputsToWrite.put(inputName, result);
                }
                break;
            case SmallTable:
                SmallTableTD table = (SmallTableTD) compContext.readInput(inputName);
                if (table.getRowCount() > 1) {
                    Object[][] result = new Object[table.getRowCount()][table.getColumnCount()];
                    for (int i = 0; i < table.getRowCount(); i++) {
                        for (int j = 0; j < table.getColumnCount(); j++) {
                            result[i][j] = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(table.getTypedDatumOfCell(i, j));
                        }
                    }
                    inputsToWrite.put(inputName, result);
                } else {
                    Object[] result = new Object[table.getColumnCount()];
                    for (int j = 0; j < table.getColumnCount(); j++) {
                        result[j] = ScriptDataTypeHelper.getObjectOfEntryForPythonOrJython(table.getTypedDatumOfCell(0, j));
                    }
                    inputsToWrite.put(inputName, result);
                }
                break;
            default:
                inputsToWrite.put(inputName, "None"); // Should not happen
                break;
            }
        }
        List<String> inputsNotConnected = new LinkedList<>();
        for (String input : compContext.getInputsNotConnected()) {
            if (compContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null
                && (compContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.RequiredIfConnected.name())
                || compContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.NotRequired.name()))) {
                inputsNotConnected.add(input);
            }
        }
        for (String input : compContext.getInputs()) {
            if (compContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null
                && compContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).equals(
                    InputExecutionContraint.NotRequired.name())
                && !compContext.getInputsWithDatum().contains(input)) {
                inputsNotConnected.add(input);
            }
        }
        try {
            mapper.writeValue(new File(tempDir.getAbsolutePath(), "pythonInput.rced"), inputsToWrite);
            mapper.writeValue(new File(tempDir.getAbsolutePath(), "pythonInputReqIfConnected.rced"), inputsNotConnected);
            mapper.writeValue(new File(tempDir.getAbsolutePath(), "pythonStateVariables.rces"),
                context.getAttribute(PythonComponentConstants.STATE_MAP));
            mapper.writeValue(new File(tempDir.getAbsolutePath(), "pythonRunNumber.rcen"),
                context.getAttribute(PythonComponentConstants.RUN_NUMBER));

        } catch (JsonGenerationException e) {
            LOGGER.error(e.getMessage());
        } catch (JsonMappingException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        List<String> outputNames = new LinkedList<>();
        for (String outputName : compContext.getOutputs()) {
            outputNames.add(outputName);
        }
        try {
            mapper.writeValue(new File(tempDir.getAbsolutePath() + File.separator + "outputs.rceo"), outputNames);
        } catch (JsonGenerationException e) {
            LOGGER.error(e.getMessage());
        } catch (JsonMappingException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void readOutputFromPython() throws ScriptException {
        try {
            if (new File(tempDir.getAbsolutePath() + File.separator
                + "pythonOutput.rced").exists()) {
                output = mapper.readValue(new File(tempDir.getAbsolutePath() + File.separator
                    + "pythonOutput.rced"), output.getClass());
            }

            if (new File(tempDir.getAbsolutePath() + File.separator
                + "pythonCloseOutputChannelsList.rced").exists()) {
                closeOutputChannelsList = mapper.readValue(new File(tempDir.getAbsolutePath() + File.separator
                    + "pythonCloseOutputChannelsList.rced"), closeOutputChannelsList.getClass());
            }
            stateOutput = new HashMap<>();
            if (new File(tempDir.getAbsolutePath() + File.separator
                + "pythonStateOutput.rces").exists()) {
                stateOutput = mapper.readValue(new File(tempDir.getAbsolutePath() + File.separator
                    + "pythonStateOutput.rces"), stateOutput.getClass());
            }
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    public Map<String, Object> getStateOutput() {
        return stateOutput;
    }

    /**
     * This creates a temporary file containing the wrapped python script.
     * 
     * @return The file handle to execute later with the python interpreter
     * @throws IOException For any file error
     */
    private void createTemporaryPythonScript(String script) throws IOException {
        final File temp = new File(tempDir, "userscript.py");
        final FileWriter writer = new FileWriter(temp);
        script = StringUtils.replace(script, "\r\n", "\n");
        writer.write(script);
        writer.close();
        File wrapperMain = new File(tempDir, RUN_SCRIPT);
        try (InputStream wrapperScriptInputMain = PythonScriptEngine.class.getResourceAsStream(RESOURCES + RUN_SCRIPT)) {
            FileUtils.copyInputStreamToFile(wrapperScriptInputMain, wrapperMain);
            File wrapperBridge = new File(tempDir, PYTHON_BRIDGE);
            try (InputStream wrapperScriptInputBridge = PythonScriptEngine.class.getResourceAsStream(RESOURCES + PYTHON_BRIDGE)) {
                FileUtils.copyInputStreamToFile(wrapperScriptInputBridge, wrapperBridge);
                try (InputStream simpleJsonFiles = PythonScriptEngine.class.getResourceAsStream(RESOURCES + SIMPLEJSON)) {
                    FileSupport.unzip(simpleJsonFiles, tempDir);
                }
            }
        }
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        String script = "";
        BufferedReader br = new BufferedReader(reader);
        String line;
        try {
            line = br.readLine();
            while (line != null) {
                script += line;
            }
        } catch (IOException e) {
            LOGGER.error("Could not read script");
        }
        return eval(script);
    }

    @Override
    public Object eval(String script, ScriptContext contextIn) throws ScriptException {
        context = contextIn;
        return eval(script);
    }

    @Override
    public Object eval(Reader reader, ScriptContext contextIn) throws ScriptException {
        context = contextIn;
        return eval(reader);
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException {
        return eval(script);
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws ScriptException {
        return eval(reader);
    }

    @Override
    public Object get(String key) {
        return output.get(key);
    }

    public List<String> getCloseOutputChannelsList() {
        return closeOutputChannelsList;
    }

    @Override
    public Bindings getBindings(int scope) {
        return null;
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return null;
    }

    @Override
    public void put(String key, Object value) {
        context.setAttribute(key, value, 0);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {

    }

    @Override
    public void setContext(ScriptContext context) {
        this.context = context;
    }

    public LocalApacheCommandLineExecutor getExecutor() {
        return executor;
    }

    /**
     * Disposes all created help files.
     */
    public void dispose() {
        try {
            if (tempDir != null) {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempDir);
            }
            if (tempFiles != null) {
                for (File f : tempFiles) {
                    if (f.exists()) {
                        FileUtils.forceDelete(f);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    protected void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        componentDatamanagementService = compDataManagementService;
    }

    /**
     * Cancels the execution of the Python script.
     */
    public synchronized void cancel() {
        // This method is synchronized to avoid a race condition with createNewExecutor

        if (initializationSignal == null) {
            canceledBeforeInitialization = true;
            return;
        }

        try {
            initializationSignal.await();
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted while waiting for the initialization to finish.", e);
            LOGGER.debug("Cancelling the cancellation.");
            return;
        }

        stdoutWatcher.cancel();
        stderrWatcher.cancel();
        executor.cancel();
    }
}
