/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.jython;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.execution.DefaultScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.scripting.WorkflowConsoleForwardingWriter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * Implementation of {@link ScriptExecutor} to execute default script engines.
 * 
 * @author Mark Geiger
 * @author Sascha Zur
 * @author Kathrin Schaffert (#14965, #17088)
 */
@Component
public class JythonScriptExecutor extends DefaultScriptExecutor {

    protected static ScriptingService scriptingService;

    private static final String QUOTE = "\"";

    private static final String ESCAPESLASH = "\\\\";

    private static final String SLASH = "/";

    private static final Log LOGGER = LogFactory.getLog(JythonScriptExecutor.class);

    protected String preHeader;

    protected String header;

    protected String inputFile;

    protected String orderedDictionary;

    protected String body;

    protected String foot;

    protected File jythonPath;

    protected String workingPath = "";

    @Override
    public boolean prepareExecutor(ComponentContext componentContext) throws ComponentException {
        boolean successful = super.prepareExecutor(componentContext);
        try {
            // this method determins the locaton of the jython.jar
            jythonPath = ScriptingUtils.getJythonPath();
        } catch (IOException e) {
            throw new ComponentException("Internal error: Failed to intialize Jython", e);
        }
        if (jythonPath == null) {
            throw new ComponentException("Internal error: Failed to intialize Jython");
        }
        File scripts = new File(tempDir, "scripts");
        File file =
            new File(scripts, "script.tmp");
        workingPath = scripts.getParentFile().getAbsolutePath();
        workingPath = workingPath.replaceAll(ESCAPESLASH, SLASH);
        tempFiles.add(file);
        stateMap = new HashMap<>();
        scriptingService = componentContext.getService(ScriptingService.class);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        return successful;
    }

    @Override
    public void prepareNewRun(ScriptLanguage scriptLanguage, String userScript,
        ScriptComponentHistoryDataItem dataItem) throws ComponentException {
        historyDataItem = dataItem;
        scriptEngine = scriptingService.createScriptEngine(scriptLanguage);
        body = "";
        foot = "";
        header = ScriptingUtils.prepareHeaderScript(stateMap, componentContext, tempDir, tempFiles);
        // with upgrade to Jython > 2.7, the orderedDictionary will be obsolete
        // K. Schaffert, 13.03.2020
        orderedDictionary = ScriptingUtils.prepareOrderedDictionaryScript();
        
        inputFile = ScriptingUtils.prepareInputFileFactoryScript(workingPath);
        loadScript(userScript);
        foot =
            "\nRCE_Dict_OutputChannels = RCE.get_output_internal()\nRCE_CloseOutputChannelsList = RCE.get_closed_outputs_internal()\n"
            + "RCE_writtenInputFiles = RCE.get_written_input_files()\n"
                + StringUtils.format("sys.stdout.write('%s')\nsys.stderr.write('%s')\nsys.stdout.flush()\nsys.stderr.flush()",
                    WorkflowConsoleForwardingWriter.CONSOLE_END, WorkflowConsoleForwardingWriter.CONSOLE_END);
    }

    private void loadScript(String userScript) throws ComponentException {
        body = userScript;
        if (body == null || body.trim().isEmpty()) {
            throw new ComponentException("No Python script configured");
        }
    }

    @Override
    public void runScript() throws ComponentException {

        // As the Jython script engine is not thread safe (console outputs of multiple script
        // executions are mixed), we must ensure that at most one script is executed at the same
        // time
        synchronized (ScriptingUtils.SCRIPT_EVAL_LOCK_OBJECT) {

            prepareOutputForRun();
            try {
                // include two important paths which the header script needs
                scriptEngine
                    .eval("RCE_Bundle_Jython_Path = " + QUOTE + jythonPath.getAbsolutePath().replaceAll(ESCAPESLASH, SLASH) + QUOTE);
                scriptEngine.eval("RCE_Temp_working_path = " + QUOTE + workingPath + QUOTE);

                // execute the headerScript, this defines the RCE_Channel class and some important
                // imports, variables and its changing the working directory.
                scriptEngine.eval(header);
                
                // with upgrade to Jython > 2.7, the orderedDictionary will be obsolete
                // K. Schaffert, 13.03.2020
                scriptEngine.eval(orderedDictionary);
                // execute the inputFile Script, which defines the InputFileFactory class
                scriptEngine.eval(inputFile);
                // for the InputFileFactory, we need the orderedDictionary script, 
                // which was was evaluated above.

            } catch (IOError | ScriptException e) {
                throw new ComponentException("Failed to execute script that is wrapped around the actual script", e);
            }
            try {
                // execute the script, written by the user.
                scriptEngine.eval(body);
            } catch (IOError e) {
                throw new ComponentException("Failed to execute script", e);
            } catch (ScriptException e) {
                if (e.getCause() != null) {
                    // expected case
                    throw new ComponentException("Script execution error: " + e.getMessage() + "\n" + e.getCause().toString());
                } else {
                    // fallback in unexpected cause == null case
                    throw new ComponentException("Script execution error: " + e.getMessage());
                }
            }
            try {
                // this script defines the outputChannel, so that all outputs sent with
                // RCE.write_output() work properly.
                scriptEngine.eval(foot);
                ((WorkflowConsoleForwardingWriter) scriptEngine.getContext().getWriter()).awaitPrintingLinesFinished();
                ((WorkflowConsoleForwardingWriter) scriptEngine.getContext().getErrorWriter()).awaitPrintingLinesFinished();
            } catch (IOError | ScriptException e) {
                throw new ComponentException("Failed to execute script that is wrapped around the actual script", e);
            } catch (InterruptedException e) {
                componentContext.getLog().componentError("Waiting for script output was interrupted. Some output might be missing");
                LOGGER.error("Waiting for stdout or stderr of Jython script execution was interrupted");
            }
        }
    }

    @Override
    public boolean postRun() throws ComponentException {
        ScriptingUtils.writeAPIOutput(stateMap, componentContext, scriptEngine, workingPath, historyDataItem);
        try {
            closeConsoleWriters();
        } catch (IOException e) {
            LOGGER.error("Failed to close stdout or stderr writer", e);
        }

        this.deleteTempFiles();

        return true;
    }

    @Override
    public void setWorkingPath(String workingPath) {
        this.workingPath = workingPath;
    }

    @Override
    public void cancelScript() {
        // We cannot cancel the execution of the script. Instead the thread needs to be interrupted.
    }

    @Override
    public boolean isCancelable() {
        // We cannot cancel the execution of the script. Instead the thread needs to be interrupted.
        return false;
    }

    @Override
    public void tearDown() {
        this.deleteTempFiles();
    }
}
