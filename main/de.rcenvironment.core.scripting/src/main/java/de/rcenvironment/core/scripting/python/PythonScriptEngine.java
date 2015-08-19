/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import java.util.UUID;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.history.ComponentHistoryDataItemConstants;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.scripting.ScriptDataTypeHelper;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.legacy.FileSupport;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;

/**
 * Implementation of {@link ScriptEngine} for the python language.
 * 
 * @author Sascha Zur
 */
public class PythonScriptEngine implements ScriptEngine {

    private static final String SIMPLEJSON = "simplejson.zip";

    private static final String RESOURCES = "/resources/";

    private static final String PYTHON_BRIDGE = "RCE_Channel.py";

    private static final String RUN_SCRIPT = "Run_python_script_in_rce.py";

    private static final Log LOGGER = LogFactory.getLog(PythonScriptEngine.class);

    private static final String ESCAPED_DOUBLE_QUOTE = "\"";

    private static ComponentDataManagementService componentDatamanagementService;

    private File tempDir;

    private ScriptContext context;

    private LocalApacheCommandLineExecutor executor;

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Serializable> output = new HashMap<String, Serializable>();

    private final List<File> tempFiles = new LinkedList<File>();

    private List<String> closeOutputChannelsList = new LinkedList<String>();

    private TextStreamWatcher stdoutWatcher;

    private TextStreamWatcher stderrWatcher;

    private CommonComponentHistoryDataItem historyDataItem;

    private File stdoutLogFile;

    private File stderrLogFile;

    private String stderrAsString;

    private Map<String, Object> stateOutput;

    private List<String> notAValueOutputsList = new LinkedList<String>();

    /**
     * Creates a new executor.
     * 
     * @param dataItem {@link ScriptComponentHistoryDataItem} object for this script execution
     */
    public void createNewExecutor(CommonComponentHistoryDataItem dataItem) {
        try {
            executor = new LocalApacheCommandLineExecutor(null);
        } catch (IOException e) {
            LOGGER.error("Failed to create executor for python.");
        }
        historyDataItem = dataItem;
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
        final String command;
        if (context.getAttribute(PythonComponentConstants.PYTHON_OS) == OSFamily.Windows) {
            command = ESCAPED_DOUBLE_QUOTE + (String) context.getAttribute(PythonComponentConstants.PYTHON_INSTALLATION)
                + ESCAPED_DOUBLE_QUOTE
                + " -u "
                + ESCAPED_DOUBLE_QUOTE + tempDir.getAbsolutePath() + File.separator + RUN_SCRIPT
                + ESCAPED_DOUBLE_QUOTE;
        } else { // Linux/Mac Type
            command = ((String) context.getAttribute(PythonComponentConstants.PYTHON_INSTALLATION)).replaceAll(" ", "\\ ")
                + " -u "
                + tempDir.getAbsolutePath().replaceAll(" ", "\\ ") + File.separator + RUN_SCRIPT;
        }
        LOGGER.debug("PythonExecutor executes command: " + command);
        int exitCode = 0;
        try {
            executor.start(command);
            prepareOutputForRun();
            try {
                exitCode = executor.waitForTermination();
            } catch (InterruptedException e) {
                LOGGER.error("ProgramBlocker: InterruptedException " + e.getMessage());
            }
            waitForConsoleOutputAndAddThemToHistoryDataItem();
        } catch (IOException e) {
            LOGGER.error("Something during Python execution failed. See exception for details", e);
        }
        readOutputFromPython();
        return exitCode;
    }

    private void waitForConsoleOutputAndAddThemToHistoryDataItem() throws IOException {
        ComponentContext componentContext = (ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT);

        stdoutWatcher.waitForTermination();
        if (historyDataItem != null) {
            if (!FileUtils.readFileToString(stdoutLogFile).isEmpty()) {
                String stdoutFileRef = componentDatamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    stdoutLogFile, stdoutLogFile.getName());
                historyDataItem.addLog(stdoutLogFile.getName(), stdoutFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stdoutLogFile);
        }

        stderrWatcher.waitForTermination();
        stderrAsString = FileUtils.readFileToString(stderrLogFile);
        if (historyDataItem != null && !stderrAsString.isEmpty()) {
            String stderrFileRef = componentDatamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                stderrLogFile, stderrLogFile.getName());
            historyDataItem.addLog(stderrLogFile.getName(), stderrFileRef);
        }
        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stderrLogFile);
    }

    private void prepareOutputForRun() {

        initializeLogFileForHistoryDataItem();
        stdoutWatcher =
            ConsoleRowUtils.logToWorkflowConsole((ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT),
                executor.getStdout(), ConsoleRow.Type.STDOUT, stdoutLogFile, false);
        stderrWatcher =
            ConsoleRowUtils.logToWorkflowConsole((ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT),
                executor.getStderr(), ConsoleRow.Type.STDERR, stderrLogFile, false);
    }

    public String getStderrAsString() {
        return stderrAsString;
    }

    private void initializeLogFileForHistoryDataItem() {
        if (historyDataItem != null) {
            try {
                stdoutLogFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                    ComponentHistoryDataItemConstants.STDOUT_LOGFILE_NAME);
            } catch (IOException e) {
                LOGGER.error("Creating temp file for console output failed. No log file add to component history data", e);
            }
        }
        // always write stderr in file to provide it on error in exception
        try {
            stderrLogFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                ComponentHistoryDataItemConstants.STDERR_LOGFILE_NAME);
        } catch (IOException e) {
            LOGGER.error("Creating temp file for console error output failed. No log file add to component history data", e);
        }
    }

    private void writeInputForPython() {
        ComponentContext compContext = (ComponentContext) context.getAttribute(PythonComponentConstants.COMPONENT_CONTEXT);
        Map<String, Object> inputsToWrite = new HashMap<String, Object>();
        for (String inputName : compContext.getInputsWithDatum()) {
            switch (compContext.getInputDataType(inputName)) {
            case FileReference:
                String path = "";
                try {
                    // Create Temp file, to handle the file referece
                    File file =
                        new File(tempDir + File.separator + inputName, "upload.python-" + UUID.randomUUID().toString()
                            + ".tmp");
                    componentDatamanagementService.copyFileReferenceTDToLocalFile(compContext,
                        (FileReferenceTD) compContext.readInput(inputName), file);
                    path =
                        file.getAbsolutePath().toString(); // Remembering File to delete it
                    tempFiles.add(file);
                } catch (IOException e) {
                    LOGGER.error("Could not load file from file reference");
                }
                path = path.replaceAll("\\\\", "/");
                inputsToWrite.put(inputName, path);
                break;
            case DirectoryReference:
                String dirPath = "";
                try { //

                    // reate Temp file, to handle the file referece
                    File file = new File(tempDir + File.separator + inputName + File.separator);
                    componentDatamanagementService.copyDirectoryReferenceTDToLocalDirectory(compContext,
                        (DirectoryReferenceTD) compContext.readInput(inputName), file);
                    file = new File(file, ((DirectoryReferenceTD) compContext.readInput(inputName)).getDirectoryName());
                    dirPath =
                        file.getAbsolutePath().toString(); // Remembering File to delete it
                    tempFiles.add(file);
                } catch (IOException e) {
                    LOGGER.error("Could not load file from file reference");
                }
                dirPath = dirPath.replaceAll("\\\\", "/");
                inputsToWrite.put(inputName, dirPath);
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
                inputsToWrite.put(inputName, ((FloatTD) compContext.readInput(inputName)).getFloatValue());
                break;
            case Vector:
                VectorTD vector = (VectorTD) compContext.readInput(inputName);
                Object[] resultVector = new Object[vector.getRowDimension()];
                for (int j = 0; j < vector.getRowDimension(); j++) {
                    resultVector[j] = vector.getFloatTDOfElement(j).getFloatValue();
                }
                inputsToWrite.put(inputName, resultVector);
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
        try {
            mapper.writeValue(new File(tempDir.getAbsolutePath(), "pythonInput.rced"), inputsToWrite);
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

        List<String> outputNames = new LinkedList<String>();
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
    private void readOutputFromPython() {
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
            if (new File(tempDir.getAbsolutePath() + File.separator
                + "pythonSetOutputsIndefinit.rceo").exists()) {
                notAValueOutputsList = mapper.readValue(new File(tempDir.getAbsolutePath() + File.separator
                    + "pythonSetOutputsIndefinit.rceo"), notAValueOutputsList.getClass());
            }
            stateOutput = new HashMap<String, Object>();
            if (new File(tempDir.getAbsolutePath() + File.separator
                + "pythonStateOutput.rces").exists()) {
                stateOutput = mapper.readValue(new File(tempDir.getAbsolutePath() + File.separator
                    + "pythonStateOutput.rces"), stateOutput.getClass());
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (JsonMappingException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
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
        InputStream wrapperScriptInputMain = PythonScriptEngine.class.getResourceAsStream(RESOURCES + RUN_SCRIPT);
        FileUtils.copyInputStreamToFile(wrapperScriptInputMain, wrapperMain);
        File wrapperBridge = new File(tempDir, PYTHON_BRIDGE);
        InputStream wrapperScriptInputBridge = PythonScriptEngine.class.getResourceAsStream(RESOURCES + PYTHON_BRIDGE);
        FileUtils.copyInputStreamToFile(wrapperScriptInputBridge, wrapperBridge);
        InputStream simpleJsonFiles = PythonScriptEngine.class.getResourceAsStream(RESOURCES + SIMPLEJSON);
        FileSupport.unzip(simpleJsonFiles, tempDir);
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

    public List<String> getNotAValueOutputsList() {
        return notAValueOutputsList;
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
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tempDir);
            for (File f : tempFiles) {
                if (f.exists()) {
                    FileUtils.forceDelete(f);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    protected void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        componentDatamanagementService = compDataManagementService;
    }

}
