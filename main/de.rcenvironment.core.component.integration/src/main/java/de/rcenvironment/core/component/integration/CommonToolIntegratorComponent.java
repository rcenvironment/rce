/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.integration;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.exec.OS;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.datamanagement.history.ComponentHistoryDataItemConstants;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.scripting.WorkflowConsoleForwardingWriter;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.scripting.ScriptDataTypeHelper;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils;
import de.rcenvironment.core.utils.common.security.StringSubstitutionSecurityUtils.SubstitutionContext;
import de.rcenvironment.core.utils.common.textstream.TextStreamWatcher;
import de.rcenvironment.core.utils.executor.LocalApacheCommandLineExecutor;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Main class for the generic tool integration.
 * 
 * @author Sascha Zur
 */
public class CommonToolIntegratorComponent extends DefaultComponent {

    private static final Log LOG = LogFactory.getLog(CommonToolIntegratorComponent.class);

    private static final String SLASH = "/";

    private static final String ESCAPESLASH = "\\\\";

    private static final String COMMAND_SCRIPT_TERMINATED_ABNORMALLY_ERROR_MSG = "Command script terminated abnormally. Exit code = ";

    private static final String SCRIPT_TERMINATED_ABNORMALLY_ERROR_MSG = " execution script terminated abnormally.";

    private static final String PROPERTY_PLACEHOLDER = "${prop:%s}";

    private static final String OUTPUT_PLACEHOLDER = "${out:%s}";

    private static final String INPUT_PLACEHOLDER = "${in:%s}";

    private static final String SCRIPT_LANGUAGE = "Jython";

    private static final String DIRECTORY_PLACEHOLDER_TEMPLATE = "${dir:%s}";

    private static final String QUOTE = "\"";

    private static final String SUBSTITUTION_ERROR_MESSAGE_PREFIX = " can not be substituted in the script, because it contains"
        + " at least one unsecure character. See log message above to see which characters are affected";

    protected ComponentContext componentContext;

    protected ComponentDataManagementService datamanagementService;

    protected File executionToolDirectory;

    protected File inputDirectory;

    protected File outputDirectory;

    protected String toolName;

    protected IntegrationHistoryDataItem historyDataItem;

    protected Map<String, TypedDatum> lastRunStaticInputValues = null;

    protected Map<String, TypedDatum> lastRunStaticOutputValues = null;

    protected boolean needsToRun = true;

    protected String copyToolBehaviour;

    private ScriptingService scriptingService;

    private TypedDatumFactory typedDatumFactory;

    private File baseWorkingDirectory;

    private File iterationDirectory;

    private File configDirectory;

    private File sourceToolDirectory;

    private String rootWDPath;

    private int runCount;

    private File currentWorkingDirectory;

    private String deleteToolBehaviour;

    private boolean useIterationDirectories;

    private boolean dontCrashOnNonZeroExitCodes;

    private String jythonPath;

    private String workingPath;

    private boolean setToolDirectoryAsWorkingDirectory;

    private File stderrLogFile;

    private File stdoutLogFile;

    private TextStreamWatcher stdoutWatcher;

    private TextStreamWatcher stderrWatcher;

    private Writer stdoutWriter;

    private Writer stderrWriter;

    private Map<String, Object> stateMap;

    private boolean keepOnFailure;

    private Map<String, String> outputMapping;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getInputs().isEmpty();
    }

    @Override
    public void start() throws ComponentException {
        datamanagementService = componentContext.getService(ComponentDataManagementService.class);
        scriptingService = componentContext.getService(ScriptingService.class);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();

        // Create basic folder structure and prepare sandbox

        toolName = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_TOOL_NAME);
        rootWDPath = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_ROOT_WORKING_DIRECTORY);
        String toolDirPath = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_TOOL_DIRECTORY);

        sourceToolDirectory = new File(toolDirPath);
        if (!sourceToolDirectory.isAbsolute()) {
            try {
                sourceToolDirectory = new File(new File(Platform.getInstallLocation().getURL().toURI()), toolDirPath);
            } catch (URISyntaxException e) {
                sourceToolDirectory = new File(new File(Platform.getInstallLocation().getURL().getPath()), toolDirPath);
            }
        }
        useIterationDirectories = Boolean.parseBoolean(componentContext.getConfigurationValue(
            ToolIntegrationConstants.KEY_TOOL_USE_ITERATION_DIRECTORIES));
        copyToolBehaviour = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_COPY_TOOL_BEHAVIOUR);

        dontCrashOnNonZeroExitCodes = componentContext
            .getConfigurationValue(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES) != null
            && Boolean.parseBoolean(componentContext.getConfigurationValue(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES));
        setToolDirectoryAsWorkingDirectory =
            componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR) != null
                && Boolean.parseBoolean(componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR));
        if (copyToolBehaviour == null) {
            copyToolBehaviour = ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER;
        }
        getToolDeleteBehaviour();

        try {
            if (rootWDPath == null || rootWDPath.isEmpty()) {
                baseWorkingDirectory = TempFileServiceAccess.getInstance().createManagedTempDir(toolName);
            } else {
                baseWorkingDirectory = new File(rootWDPath + File.separator + toolName + "_" + UUID.randomUUID().toString()
                    + File.separator);
                baseWorkingDirectory.mkdirs();
            }
            if (!useIterationDirectories) {
                iterationDirectory = baseWorkingDirectory;
                currentWorkingDirectory = baseWorkingDirectory;
                createFolderStructure(baseWorkingDirectory);

            }
            if (copyToolBehaviour.equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ONCE)) {
                copySandboxTool(baseWorkingDirectory);
            }
            if (copyToolBehaviour.equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_NEVER)) {
                executionToolDirectory = sourceToolDirectory;
            }
        } catch (IOException e) {
            throw new ComponentException(toolName + ": Could not create working directory " + e.getMessage());
        }
        prepareJythonForUsingModules();
        initializeNewHistoryDataItem();

        stateMap = new HashMap<String, Object>();
        if (treatStartAsComponentRun()) {
            processInputs();
            if (historyDataItem != null) {
                historyDataItem.setWorkingDirectory(currentWorkingDirectory.getAbsolutePath());
            }
        }

    }

    private void getToolDeleteBehaviour() {
        if (componentContext.getConfigurationValue(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR) != null) {
            deleteToolBehaviour = componentContext.getConfigurationValue(ToolIntegrationConstants.CHOSEN_DELETE_TEMP_DIR_BEHAVIOR);
        } else {
            boolean deleteNeverActive =
                Boolean.parseBoolean(componentContext
                    .getConfigurationValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER));
            boolean deleteOnceActive =
                Boolean.parseBoolean(componentContext
                    .getConfigurationValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE));
            boolean deleteAlwaysActive =
                Boolean.parseBoolean(componentContext
                    .getConfigurationValue(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS));

            if (deleteAlwaysActive) {
                deleteToolBehaviour = ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS;
            } else if (deleteOnceActive) {
                deleteToolBehaviour = ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE;
            } else if (deleteNeverActive) {
                deleteToolBehaviour = ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_NEVER;
            }
        }
        keepOnFailure = false;
        if (componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_KEEP_ON_FAILURE) != null) {
            keepOnFailure =
                Boolean.parseBoolean(componentContext
                    .getConfigurationValue(ToolIntegrationConstants.KEY_KEEP_ON_FAILURE));
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        Map<String, TypedDatum> inputValues = new HashMap<>();
        if (componentContext != null && componentContext.getInputsWithDatum() != null) {
            for (String inputName : componentContext.getInputsWithDatum()) {
                inputValues.put(inputName, componentContext.readInput(inputName));
            }
        }

        try {
            // create iteration directory and prepare it
            if (useIterationDirectories) {
                iterationDirectory = new File(baseWorkingDirectory, "" + runCount++);
                currentWorkingDirectory = iterationDirectory;
                createFolderStructure(iterationDirectory);
                if (copyToolBehaviour.equals(ToolIntegrationConstants.VALUE_COPY_TOOL_BEHAVIOUR_ALWAYS)) {
                    copySandboxTool(iterationDirectory);
                }
            }
            // create a list with used input values to delete them afterwards
            Map<String, String> inputNamesToLocalFile = new HashMap<String, String>();
            for (String inputName : inputValues.keySet()) {
                if (componentContext.getInputDataType(inputName) == DataType.FileReference) {
                    inputNamesToLocalFile.put(inputName, copyInputFileToInputFolder(inputName, inputValues));
                } else if (componentContext.getInputDataType(inputName) == DataType.DirectoryReference) {
                    inputNamesToLocalFile.put(inputName, copyInputFileToInputFolder(inputName, inputValues));
                }
            }
            // Create Conf files
            Set<String> configFileNames = new HashSet<String>();
            for (String configKey : componentContext.getConfigurationKeys()) {
                String configFilename = componentContext.getConfigurationMetaDataValue(configKey,
                    ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME);
                if (configFilename != null && !configFilename.isEmpty()) {
                    configFileNames.add(configFilename);
                }
            }
            for (String filename : configFileNames) {
                File f = new File(configDirectory, filename);
                try {
                    if (f.exists()) {
                        f.delete();
                    }
                    f.createNewFile();
                    for (String configKey : componentContext.getConfigurationKeys()) {
                        String configFilename = componentContext.getConfigurationMetaDataValue(configKey,
                            ToolIntegrationConstants.KEY_PROPERTY_CONFIG_FILENAME);
                        if (configFilename != null && !configFilename.isEmpty() && configFilename.equals(filename)) {
                            List<String> lines = FileUtils.readLines(f);
                            lines.add(configKey + "=" + componentContext.getConfigurationValue(configKey));
                            FileUtils.writeLines(f, lines);
                        }
                    }
                } catch (IOException e) {
                    LOG.error(toolName + " could not write config file: ", e);
                }
            }

            String preScript = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_PRE_SCRIPT);
            beforePreScriptExecution(inputValues, inputNamesToLocalFile);
            int exitCode = 0;
            needsToRun = needToRun(inputValues, inputNamesToLocalFile);
            if (needsToRun) {
                lastRunStaticInputValues = new HashMap<String, TypedDatum>();
                lastRunStaticOutputValues = new HashMap<String, TypedDatum>();

                for (String inputName : inputValues.keySet()) {
                    if (componentContext.isStaticInput(inputName) && inputValues.containsKey(inputName)) {
                        lastRunStaticInputValues.put(inputName, inputValues.get(inputName));
                    }
                }
                runScript(preScript, inputValues, inputNamesToLocalFile, "Pre");
                beforeCommandExecution(inputValues, inputNamesToLocalFile);

                componentContext.printConsoleLine(WorkflowLifecyleEventType.TOOL_STARTING.name(), ConsoleRow.Type.LIFE_CYCLE_EVENT);
                exitCode = runCommand(inputValues, inputNamesToLocalFile);
                componentContext.printConsoleLine(WorkflowLifecyleEventType.TOOL_FINISHED.name(), ConsoleRow.Type.LIFE_CYCLE_EVENT);

                afterCommandExecution(inputValues, inputNamesToLocalFile);
                String postScript = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_POST_SCRIPT);
                if (postScript != null) {
                    postScript = ComponentUtils.replaceVariable(postScript, "" + exitCode, "exitCode", "${addProp:%s}");
                }
                runScript(postScript, inputValues, inputNamesToLocalFile, "Post");
            } else {
                for (String outputName : lastRunStaticOutputValues.keySet()) {
                    componentContext.writeOutput(outputName, lastRunStaticOutputValues.get(outputName));
                }
            }
            afterPostScriptExecution(inputValues, inputNamesToLocalFile);

            if (useIterationDirectories && deleteToolBehaviour.equals(
                ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS)) {
                try {
                    FileUtils.deleteDirectory(currentWorkingDirectory);
                    LOG.debug(toolName + ": Deleted directory " + currentWorkingDirectory);
                } catch (IOException e) {
                    LOG.error(toolName + ": Could not delete current working directory: ", e);
                }
            }
            try {
                if (needsToRun) {
                    closeConsoleWriters();
                } else {
                    writeNoNeedToRunInformationToStdout();
                }

                addLogsToHistoryDataItem();
            } catch (IOException e) {
                LOG.error("Closing or storing console log file failed", e);
            }

        } finally {
            // Note: To ensure history data items will be stored on error, a finally block is used
            // as long as no lifecycle method is present
            // for the case of an error (will be added in the future) - seid_do
            storeHistoryDataItem();

            // delete log files after each iteration if deleteToolBehavior is set so
            if (deleteToolBehaviour.equals(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS)) {
                deleteLogs();
            }
        }
    }

    // The before* and after* methods are for implementing own code in sub classes
    protected void afterPostScriptExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        // LOG.debug("after postscript execution");
    }

    protected void beforePreScriptExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        // LOG.debug("Before prescript execution");
    }

    protected void beforeCommandExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        // LOG.debug("before commad execution");
    }

    protected void afterCommandExecution(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        // LOG.debug("after command execution");
    }

    // The needToRun() method gives the ability to control if the integrated tool should run on some
    // conditions or not.
    protected boolean needToRun(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        // Standard case --> run always
        return true;
    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        switch (state) {
        case CANCELLED:
            deleteBaseWorkingDirectory(false);
            break;
        case FINISHED:
            deleteBaseWorkingDirectory(true);
            File parentFile = null;
            if (stderrLogFile != null) {
                parentFile = stderrLogFile.getParentFile();
            }
            // delete log files after workflow has finished if deleteToolBehavior is set so
            if (deleteToolBehaviour.equals(ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE)) {
                deleteLogs();
            }
            try {
                // to clean up delete all empty folders
                if (parentFile != null) {
                    for (File file : parentFile.listFiles()) {
                        if (file != null && file.isDirectory() && file.listFiles().length == 0) {
                            file.delete();
                        }
                    }
                }
            } catch (NullPointerException e) {
                if (parentFile != null) {
                    LOG.warn("Failed to delete temp files: " + parentFile.getAbsolutePath());
                } else {
                    LOG.warn("Failed to delete temp files");
                }

            }

            break;
        case FAILED:
            deleteBaseWorkingDirectory(false);
            break;
        default:
            break;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        deleteBaseWorkingDirectory(false);
    }

    private void deleteLogs() {
        if (stdoutLogFile != null && stdoutLogFile.exists()) {
            stdoutLogFile.delete();
        }
        if (stderrLogFile != null && stderrLogFile.exists()) {
            stderrLogFile.delete();
            if (stderrLogFile != null && stderrLogFile.getParentFile() != null && stderrLogFile.getParentFile().exists()) {
                stderrLogFile.getParentFile().delete();
            }
        }
    }

    private int runCommand(Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile)
        throws ComponentException {
        String commScript = null;
        int exitCode = 0;
        if (OS.isFamilyWindows() && (Boolean.parseBoolean(componentContext.getConfigurationValue(
            ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED)))) {
            commScript = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS);
            commScript = replacePlaceholder(commScript, inputValues, inputNamesToLocalFile, SubstitutionContext.WINDOWS_BATCH);
        } else if (OS.isFamilyUnix() && (Boolean.parseBoolean(componentContext.getConfigurationValue(
            ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED)))) {
            commScript = componentContext.getConfigurationValue(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX);
            commScript = replacePlaceholder(commScript, inputValues, inputNamesToLocalFile, SubstitutionContext.LINUX_BASH);
        } else {
            throw new ComponentException(toolName + ": Command script could not be found.");
        }

        try {
            componentContext.printConsoleLine(toolName + " command execution ...", ConsoleRow.Type.COMPONENT_OUTPUT);
            LocalApacheCommandLineExecutor executor = null;
            if (setToolDirectoryAsWorkingDirectory) {
                executor = new LocalApacheCommandLineExecutor(executionToolDirectory);
            } else {
                executor = new LocalApacheCommandLineExecutor(currentWorkingDirectory);
            }
            executor.startMultiLineCommand(commScript.split("\r?\n|\r"));
            initializeLogFileForHistoryDataItem();
            stdoutWatcher = ConsoleRowUtils.logToWorkflowConsole(componentContext, executor.getStdout(),
                ConsoleRow.Type.STDOUT, stdoutLogFile, false);
            stderrWatcher = ConsoleRowUtils.logToWorkflowConsole(componentContext, executor.getStderr(),
                ConsoleRow.Type.STDERR, stderrLogFile, false);
            try {
                exitCode = executor.waitForTermination();
            } catch (InterruptedException e) {
                deleteBaseWorkingDirectory(false);
                throw new ComponentException("Executor Interrupted: " + e.getMessage());
            }
            stdoutWatcher.waitForTermination();
            stderrWatcher.waitForTermination();
            componentContext.printConsoleLine("Command execution of " + toolName + " finished with exit code " + exitCode,
                ConsoleRow.Type.COMPONENT_OUTPUT);
            if (historyDataItem != null) {
                historyDataItem.setExitCode(exitCode);
            }
            if (!dontCrashOnNonZeroExitCodes && exitCode != 0) {
                componentContext.printConsoleLine(COMMAND_SCRIPT_TERMINATED_ABNORMALLY_ERROR_MSG + exitCode, ConsoleRow.Type.STDERR);
                deleteBaseWorkingDirectory(false);
                throw new ComponentException(COMMAND_SCRIPT_TERMINATED_ABNORMALLY_ERROR_MSG + exitCode);
            }
        } catch (IOException e) {
            deleteBaseWorkingDirectory(false);
            throw new ComponentException(toolName + ": Could not create Executor: " + e.getMessage());
        }
        return exitCode;
    }

    private void runScript(String script, Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile,
        String scriptPrefix) throws ComponentException {

        // As the Jython script engine is not thread safe (console outputs of multiple script
        // executions are mixed), we must ensure that at most one script is executed at the same
        // time
        synchronized (ScriptingUtils.SCRIPT_EVAL_LOCK_OBJECT) {
            Integer exitCode = null;
            if (script != null && !script.isEmpty()) {
                ScriptLanguage scriptLanguage = ScriptLanguage.getByName(SCRIPT_LANGUAGE);
                final ScriptEngine engine = scriptingService.createScriptEngine(scriptLanguage);
                Map<String, Object> scriptExecConfig = null;
                engine.put("config", scriptExecConfig);
                prepareScriptOutputForRun(engine);
                componentContext.printConsoleLine(toolName + " " + scriptPrefix + " script execution ... ",
                    ConsoleRow.Type.COMPONENT_OUTPUT);

                script = replacePlaceholder(script, inputValues, inputNamesToLocalFile, SubstitutionContext.JYTHON);
                try {
                    engine.eval("RCE_Bundle_Jython_Path = " + QUOTE + jythonPath + QUOTE);
                    engine.eval("RCE_Temp_working_path = " + QUOTE + workingPath + QUOTE);

                    String headerScript =
                        ScriptingUtils.prepareHeaderScript(stateMap, componentContext, TempFileServiceAccess.getInstance()
                            .createManagedTempDir(), new LinkedList<File>());
                    engine.eval(headerScript);
                    engine.eval(prepareTableInput(inputValues));
                    exitCode = (Integer) engine.eval(script);
                    String footerScript = "\nRCE_Dict_OutputChannels = RCE.get_output_internal()\nRCE_CloseOutputChannelsList = "
                        + "RCE.get_closed_outputs_internal()\n" + "RCE_NotAValueOutputList = RCE.get_indefinite_outputs_internal()\n"
                        + StringUtils.format("sys.stdout.write('%s')\nsys.stderr.write('%s')\nsys.stdout.flush()\nsys.stderr.flush()",
                            WorkflowConsoleForwardingWriter.CONSOLE_END, WorkflowConsoleForwardingWriter.CONSOLE_END);
                    engine.eval(footerScript);

                    ((WorkflowConsoleForwardingWriter) engine.getContext().getWriter()).awaitPrintingLinesFinished();
                    ((WorkflowConsoleForwardingWriter) engine.getContext().getErrorWriter()).awaitPrintingLinesFinished();

                    if (exitCode != null) {
                        componentContext.printConsoleLine(scriptPrefix + " execution script of " + toolName + " finished "
                            + " with exit code " + exitCode, ConsoleRow.Type.COMPONENT_OUTPUT);
                    } else {
                        componentContext.printConsoleLine(scriptPrefix + " execution script of " + toolName + " finished.",
                            ConsoleRow.Type.COMPONENT_OUTPUT);
                    }

                } catch (ScriptException | IOException e) {
                    componentContext.printConsoleLine(scriptPrefix + SCRIPT_TERMINATED_ABNORMALLY_ERROR_MSG, ConsoleRow.Type.STDERR);
                    componentContext.printConsoleLine(e.getMessage(), ConsoleRow.Type.STDERR);
                    deleteBaseWorkingDirectory(false);
                    throw new ComponentException("Pre or post script execution failed: " + e.toString());
                } catch (InterruptedException e) {
                    LOG.error("Waiting for stdout or stderr writer was interrupted", e);
                }
                if (exitCode != null && exitCode.intValue() != 0) {
                    deleteBaseWorkingDirectory(false);

                    throw new ComponentException(scriptPrefix + SCRIPT_TERMINATED_ABNORMALLY_ERROR_MSG + exitCode);
                }
                writeOutputValues(engine, scriptExecConfig);
            }
        }
    }

    private String prepareTableInput(Map<String, TypedDatum> inputValues) {
        String script = "";
        for (String inputName : inputValues.keySet()) {
            if (componentContext.getInputDataType(inputName) == DataType.Vector) {
                script += inputName + "= [";
                for (FloatTD floatEntry : ((VectorTD) inputValues.get(inputName)).toArray()) {
                    script += floatEntry.getFloatValue() + ",";
                }
                script = script.substring(0, script.length() - 1) + "]\n";
            }
        }
        return script;
    }

    @SuppressWarnings("unchecked")
    private void writeOutputValues(ScriptEngine engine, Map<String, Object> scriptExecConfig) {
        final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        /*
         * Extract all values calculated/set in the script to a custom scope so the calculated/set
         * values are accessible via the current Context.
         */
        for (final String key : bindings.keySet()) {
            Object value = bindings.get(key);
            if (value != null && value.getClass().getSimpleName().equals("NativeJavaObject")) {
                try {
                    value = value.getClass().getMethod("unwrap").invoke(value);
                } catch (IllegalArgumentException e) {
                    LOG.error(e);
                } catch (SecurityException e) {
                    LOG.error(e);
                } catch (IllegalAccessException e) {
                    LOG.error(e);
                } catch (InvocationTargetException e) {
                    LOG.error(e);
                } catch (NoSuchMethodException e) {
                    LOG.error(e);
                }
            }
            if (scriptExecConfig != null) {
                scriptExecConfig.put(key, value);
            }
            if (value != null && outputMapping.containsKey(key)) {
                if (componentContext.getOutputDataType(outputMapping.get(key)) == DataType.FileReference
                    || componentContext.getOutputDataType(outputMapping.get(key)) == DataType.DirectoryReference) {
                    File file = new File(value.toString());
                    if (!file.isAbsolute()) {
                        file = new File(currentWorkingDirectory, value.toString());
                    }
                    if (!file.exists()) {
                        componentContext.printConsoleLine("Could not find file for output " + outputMapping.get(key)
                            + ": " + file.getAbsolutePath(), ConsoleRow.Type.STDOUT);
                    }
                    try {
                        if (componentContext.getOutputDataType(outputMapping.get(key)) == DataType.FileReference) {
                            String metafilename = componentContext.getOutputMetaDataValue(outputMapping.get(key),
                                ToolIntegrationConstants.KEY_ENDPOINT_FILENAME);
                            String filename = file.getName();
                            if (metafilename != null && !metafilename.isEmpty()) {
                                filename = metafilename;
                            }
                            FileReferenceTD uuid = datamanagementService.createFileReferenceTDFromLocalFile(componentContext, file,
                                filename);
                            componentContext.writeOutput(outputMapping.get(key), uuid);
                            lastRunStaticOutputValues.put(outputMapping.get(key), uuid);
                        } else {
                            String metafilename = componentContext.getOutputMetaDataValue(outputMapping.get(key),
                                ToolIntegrationConstants.KEY_ENDPOINT_FILENAME);
                            String filename = file.getName();
                            if (metafilename != null && !metafilename.isEmpty()) {
                                filename = metafilename;
                            }
                            DirectoryReferenceTD uuid = datamanagementService.createDirectoryReferenceTDFromLocalDirectory(
                                componentContext, file, filename);
                            componentContext.writeOutput(outputMapping.get(key), uuid);
                            lastRunStaticOutputValues.put(outputMapping.get(key), uuid);
                        }
                    } catch (IOException e) {
                        LOG.error("Writing output from script: ", e);
                    }

                } else {
                    TypedDatum valueTD = ScriptDataTypeHelper.getTypedDatum(value, typedDatumFactory);
                    componentContext.writeOutput(outputMapping.get(key), valueTD);
                    lastRunStaticOutputValues.put(outputMapping.get(key), valueTD);
                }
            }
        }
        try {
            ScriptingUtils.writeAPIOutput(stateMap, componentContext, engine, workingPath, historyDataItem);
        } catch (ComponentException e) {
            LOG.error(e);
        }

        for (String outputName : (List<String>) engine.get("RCE_CloseOutputChannelsList")) {
            componentContext.closeOutput(outputName);
        }

        Map<String, Object> stateMapOutput = (Map<String, Object>) engine.get("RCE_STATE_VARIABLES");
        for (String key : stateMapOutput.keySet()) {
            stateMap.put(key, stateMapOutput.get(key));
        }

    }

    private String replacePlaceholder(String script, Map<String, TypedDatum> inputValues, Map<String, String> inputNamesToLocalFile,
        SubstitutionContext context) throws ComponentException {
        if (inputValues != null) {
            for (String inputName : inputValues.keySet()) {
                if (inputValues.containsKey(inputName) && script.contains(StringUtils.format(INPUT_PLACEHOLDER, inputName))) {
                    if (componentContext.getInputDataType(inputName) == DataType.FileReference) {
                        script = script.replace(StringUtils.format(INPUT_PLACEHOLDER, inputName),
                            inputNamesToLocalFile.get(inputName).replaceAll(ESCAPESLASH, SLASH));
                    } else if (componentContext.getInputDataType(inputName) == DataType.DirectoryReference) {
                        script = script.replace(StringUtils.format(INPUT_PLACEHOLDER, inputName),
                            inputNamesToLocalFile.get(inputName).replaceAll(ESCAPESLASH, SLASH));
                    } else if (componentContext.getInputDataType(inputName) == DataType.Vector) {
                        script = script.replace(StringUtils.format(INPUT_PLACEHOLDER, inputName), validate(inputName, context,
                            StringUtils.format("Name of Vector '%s'" + SUBSTITUTION_ERROR_MESSAGE_PREFIX, inputName)));

                    } else {
                        String value = inputValues.get(inputName).toString();
                        if (context == SubstitutionContext.JYTHON && componentContext.getInputDataType(inputName) == DataType.Boolean) {
                            value = value.substring(0, 1).toUpperCase() + value.substring(1);
                        }
                        script =
                            script
                                .replace(
                                    StringUtils.format(INPUT_PLACEHOLDER, inputName),
                                    validate(value, context, StringUtils.format("Value '%s' from input '%s'"
                                        + SUBSTITUTION_ERROR_MESSAGE_PREFIX, value, inputName)));
                    }
                }
            }
        }

        script = replaceOutputVariables(script, componentContext.getOutputs(), OUTPUT_PLACEHOLDER);
        Map<String, String> properties = new HashMap<>();
        // get properties!
        for (String configKey : componentContext.getConfigurationKeys()) {
            String value = componentContext.getConfigurationValue(configKey);
            validate(value, context, StringUtils.format("Value '%s' of property '%s'" + SUBSTITUTION_ERROR_MESSAGE_PREFIX,
                value, configKey));
            properties.put(configKey, componentContext.getConfigurationValue(configKey));
        }
        script = ComponentUtils.replacePropertyVariables(script, properties, PROPERTY_PLACEHOLDER);
        script = ComponentUtils.replaceVariable(script, configDirectory.getAbsolutePath(),
            ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[0], DIRECTORY_PLACEHOLDER_TEMPLATE);
        script = ComponentUtils.replaceVariable(script, currentWorkingDirectory.getAbsolutePath(),
            ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[1], DIRECTORY_PLACEHOLDER_TEMPLATE);
        script = ComponentUtils.replaceVariable(script, currentWorkingDirectory.getAbsolutePath(),
            ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[1] + "Dir", DIRECTORY_PLACEHOLDER_TEMPLATE);
        script = ComponentUtils.replaceVariable(script, inputDirectory.getAbsolutePath(),
            ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[2], DIRECTORY_PLACEHOLDER_TEMPLATE);
        script = ComponentUtils.replaceVariable(script, outputDirectory.getAbsolutePath(),
            ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[4], DIRECTORY_PLACEHOLDER_TEMPLATE);
        script = ComponentUtils.replaceVariable(script, executionToolDirectory.getAbsolutePath(),
            ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[3], DIRECTORY_PLACEHOLDER_TEMPLATE);

        return script;
    }

    private String replaceOutputVariables(String script, Set<String> outputs, String outputPlaceholder) {
        outputMapping = new HashMap<>();
        for (String outputName : outputs) {
            String outputID = "_RCE_OUTPUT_" + UUID.randomUUID().toString().replaceAll("-", "_");
            script = script.replace(StringUtils.format(outputPlaceholder, outputName), outputID);
            outputMapping.put(outputID, outputName);
        }
        return script;
    }

    private String validate(String key, SubstitutionContext context, String errorMsg) throws ComponentException {
        if (!StringSubstitutionSecurityUtils.isSafeForSubstitutionInsideDoubleQuotes(key, context)) {
            throw new ComponentException(errorMsg);
        }
        return key;
    }

    private String copyInputFileToInputFolder(String inputName, Map<String, TypedDatum> inputValues) {

        File targetFile = null;
        TypedDatum fileReference = inputValues.get(inputName);
        try {
            if (componentContext.getInputDataType(inputName) == DataType.FileReference) {
                String fileName = ((FileReferenceTD) fileReference).getFileName();
                if (componentContext.getInputMetaDataKeys(inputName).contains(ToolIntegrationConstants.KEY_ENDPOINT_FILENAME)
                    && !componentContext.getInputMetaDataValue(inputName, ToolIntegrationConstants.KEY_ENDPOINT_FILENAME).isEmpty()) {
                    fileName = componentContext.getInputMetaDataValue(inputName, ToolIntegrationConstants.KEY_ENDPOINT_FILENAME);
                }
                targetFile = new File(inputDirectory.getAbsolutePath(), inputName + File.separator + fileName);
                if (targetFile.exists()) {
                    FileUtils.forceDelete(targetFile);
                }
                datamanagementService.copyFileReferenceTDToLocalFile(componentContext, (FileReferenceTD) fileReference, targetFile);
            } else {
                String fileName = ((DirectoryReferenceTD) fileReference).getDirectoryName();
                targetFile = new File(inputDirectory.getAbsolutePath(), inputName);
                if (targetFile.exists()) {
                    FileUtils.forceDelete(targetFile);
                }
                datamanagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext,
                    (DirectoryReferenceTD) fileReference, targetFile);
                targetFile = new File(targetFile, ((DirectoryReferenceTD) fileReference).getDirectoryName());
                if (componentContext.getInputMetaDataValue(inputName, ToolIntegrationConstants.KEY_ENDPOINT_FILENAME) != null
                    && !componentContext.getInputMetaDataValue(inputName, ToolIntegrationConstants.KEY_ENDPOINT_FILENAME).isEmpty()) {
                    fileName = componentContext.getInputMetaDataValue(inputName, ToolIntegrationConstants.KEY_ENDPOINT_FILENAME);
                    File newTarget = new File(new File(inputDirectory.getAbsolutePath(), inputName), fileName);
                    targetFile.renameTo(newTarget);
                    targetFile = newTarget;
                }
            }
            componentContext.printConsoleLine(toolName + ": Copied " + targetFile.getName() + " to "
                + targetFile.getAbsolutePath() + "\"", ConsoleRow.Type.COMPONENT_OUTPUT);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return targetFile.getAbsolutePath();
    }

    private void copySandboxTool(File directory) throws ComponentException {
        File targetToolDir = new File(directory + File.separator + sourceToolDirectory.getName());
        boolean copiedToolDir = false;
        try {
            FileUtils.copyDirectory(sourceToolDirectory, targetToolDir);
            componentContext.printConsoleLine(toolName + ": Copied directory" + sourceToolDirectory.getName() + " to "
                + targetToolDir.getAbsolutePath(), ConsoleRow.Type.COMPONENT_OUTPUT);
            copiedToolDir = true;
        } catch (IOException e) {
            deleteBaseWorkingDirectory(false);
            throw new ComponentException(toolName + ": Could not copy tool directory " + sourceToolDirectory.getAbsolutePath()
                + " to sandbox", e);
        }
        if (copiedToolDir) {
            executionToolDirectory = targetToolDir;
            Iterator<File> it = FileUtils.iterateFiles(targetToolDir, null, true);
            while (it.hasNext()) {
                File f = it.next();
                f.setExecutable(true, false);
            }
        }

    }

    private void createFolderStructure(File directory) {
        inputDirectory = new File(directory.getAbsolutePath() + File.separator + ToolIntegrationConstants.COMPONENT_INPUT_FOLDER_NAME
            + File.separator);
        inputDirectory.mkdirs();
        outputDirectory = new File(directory.getAbsolutePath() + File.separator + ToolIntegrationConstants.COMPONENT_OUTPUT_FOLDER_NAME
            + File.separator);
        outputDirectory.mkdirs();
        configDirectory = new File(directory.getAbsolutePath() + File.separator + ToolIntegrationConstants.COMPONENT_CONFIG_FOLDER_NAME
            + File.separator);
        configDirectory.mkdirs();
        LOG.debug(toolName + ": Created folder structure in " + directory.getAbsolutePath());
    }

    private void deleteBaseWorkingDirectory(boolean workflowSuccess) {
        if ((ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ONCE.equals(deleteToolBehaviour)
            || ToolIntegrationConstants.KEY_TOOL_DELETE_WORKING_DIRECTORIES_ALWAYS.equals(deleteToolBehaviour))
            && !(keepOnFailure && !workflowSuccess)
            && baseWorkingDirectory.exists()) {
            try {
                if (rootWDPath == null || rootWDPath.isEmpty()) {
                    TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(baseWorkingDirectory);
                } else {
                    FileDeleteStrategy.FORCE.delete(baseWorkingDirectory);
                }
                LOG.debug(toolName + ": deleted directory: " + baseWorkingDirectory.getAbsolutePath());
            } catch (IOException e) {
                baseWorkingDirectory.deleteOnExit();
                LOG.warn(toolName + ": Could not delete base working directory: " + e.getMessage());
            }
        }
    }

    protected void bindScriptingService(final ScriptingService service) {
        scriptingService = service;
    }

    protected void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        datamanagementService = compDataManagementService;
    }

    private void prepareScriptOutputForRun(ScriptEngine scriptEngine) {
        final int buffer = 1024;
        StringWriter out = new StringWriter(buffer);
        StringWriter err = new StringWriter(buffer);
        stdoutWriter = new WorkflowConsoleForwardingWriter(out, componentContext, ConsoleRow.Type.STDOUT);
        stderrWriter = new WorkflowConsoleForwardingWriter(err, componentContext, ConsoleRow.Type.STDERR);
        scriptEngine.getContext().setWriter(stdoutWriter);
        scriptEngine.getContext().setErrorWriter(stderrWriter);
    }

    private void prepareJythonForUsingModules() {
        try {
            jythonPath = ScriptingUtils.getJythonPath();
        } catch (IOException e2) {
            LOG.error(e2);
        }

        File file = new File(baseWorkingDirectory.getAbsolutePath(), "jython-import-" + UUID.randomUUID().toString() + ".tmp");
        workingPath = file.getAbsolutePath().toString();
        workingPath = workingPath.replaceAll(ESCAPESLASH, SLASH);

        String[] splitted = workingPath.split(SLASH);
        workingPath = "";
        for (int i = 0; i < splitted.length - 1; i++) {
            workingPath += splitted[i] + SLASH;
        }

    }

    private void writeNoNeedToRunInformationToStdout() throws IOException {
        String message = "Tool did not run as input didn't change compared with the previous run.";
        componentContext.printConsoleLine(message, Type.STDOUT);
        if (historyDataItem != null) {
            FileUtils.writeStringToFile(stdoutLogFile, message);
        }
    }

    protected void closeConsoleWriters() throws IOException {
        if (stdoutWriter != null) {
            stdoutWriter.flush();
            stdoutWriter.close();
        }
        if (stderrWriter != null) {
            stderrWriter.flush();
            stderrWriter.close();
        }
    }

    protected void addLogsToHistoryDataItem() throws IOException {
        if (historyDataItem != null) {
            if (stdoutLogFile != null && stdoutLogFile.exists() && !FileUtils.readFileToString(stdoutLogFile).isEmpty()) {
                String stdoutFileRef = datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    stdoutLogFile, stdoutLogFile.getName());
                historyDataItem.addLog(stdoutLogFile.getName(), stdoutFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stdoutLogFile);
            if (stderrLogFile != null && stderrLogFile.exists() && !FileUtils.readFileToString(stderrLogFile).isEmpty()) {
                String stderrFileRef = datamanagementService.createTaggedReferenceFromLocalFile(componentContext,
                    stderrLogFile, stderrLogFile.getName());
                historyDataItem.addLog(stderrLogFile.getName(), stderrFileRef);
            }
            TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(stderrLogFile);
            historyDataItem.setWorkingDirectory(currentWorkingDirectory.getAbsolutePath());
        }

    }

    protected void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new IntegrationHistoryDataItem(componentContext.getComponentIdentifier());
        }
    }

    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    private void initializeLogFileForHistoryDataItem() {
        if (historyDataItem != null) {
            try {
                stdoutLogFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                    ComponentHistoryDataItemConstants.STDOUT_LOGFILE_NAME);
            } catch (IOException e) {
                LOG.error("Creating temp file for console output failed. No log file will be added to component history data", e);
            }
            try {
                stderrLogFile = TempFileServiceAccess.getInstance().createTempFileWithFixedFilename(
                    ComponentHistoryDataItemConstants.STDERR_LOGFILE_NAME);

            } catch (IOException e) {
                LOG.error("Creating temp file for console error output failed. No log file will be added to component history data", e);
            }
        }
    }

}
