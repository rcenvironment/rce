/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.script.execution.jython;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 */
public class JythonScriptExecutor extends DefaultScriptExecutor {

    protected static ScriptingService scriptingService;

    private static final String QUOTE = "\"";

    private static final String ESCAPESLASH = "\\\\";

    private static final String SLASH = "/";

    private static final Log LOGGER = LogFactory.getLog(JythonScriptExecutor.class);

    protected String preHeader;

    protected String header;

    protected String body;

    protected String foot;

    protected String jythonPath = "";

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
        workingPath = scripts.getParentFile().getAbsolutePath().toString();
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
        loadScript(userScript);
        foot =
            "\nRCE_Dict_OutputChannels = RCE.get_output_internal()\nRCE_CloseOutputChannelsList = RCE.get_closed_outputs_internal()\n"
                + StringUtils.format("sys.stdout.write('%s')\nsys.stderr.write('%s')\nsys.stdout.flush()\nsys.stderr.flush()",
                    WorkflowConsoleForwardingWriter.CONSOLE_END, WorkflowConsoleForwardingWriter.CONSOLE_END);
    }

    private void loadScript(String userScript) throws ComponentException {
        // wrappingScript = wrappingScript + userScript;
        body = userScript;
        if (body == null || body.length() == 0) {
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
                // include two important paths wich the header script need
                scriptEngine.eval("RCE_Bundle_Jython_Path = " + QUOTE + jythonPath + QUOTE);
                scriptEngine.eval("RCE_Temp_working_path = " + QUOTE + workingPath + QUOTE);

                // execute the headerScript, this defines the RCE_Channel class and some important
                // imports, variables and
                // its changig the working directory.
                scriptEngine.eval(header);

            } catch (IOError | ScriptException e) {
                throw new ComponentException("Failed to execute script that is wrapped around the actual script", e);
            }
            try {
                // execute the script, written by the user.
                scriptEngine.eval(body);
            } catch (IOError | ScriptException e) {
                throw new ComponentException("Failed to execute script", e);
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
}
