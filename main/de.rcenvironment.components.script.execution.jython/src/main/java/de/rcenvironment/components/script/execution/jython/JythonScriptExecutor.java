/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.script.execution.jython;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.execution.DefaultScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.scripting.WorkflowConsoleForwardingWriter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
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

    protected String preHeader;

    protected String header;

    protected String body;

    protected String foot;

    protected String jythonPath = "";

    protected String workingPath = "";

    private Map<String, Object> stateMap;

    @Override
    public void reset() {
        stateMap = new HashMap<String, Object>();
    }

    @Override
    protected void bindScriptingService(final ScriptingService service) {
        scriptingService = service;
    }

    @Override
    protected void unbindScriptingService(final ScriptingService service) {
        /*
         * nothing to do here, this unbind method is only needed, because DS is throwing an
         * exception when disposing otherwise. probably a bug
         */
    }

    @Override
    protected void bindTypedDatumService(TypedDatumService newTypedDatumService) {
        typedDatumFactory = newTypedDatumService.getFactory();
    }

    @Override
    protected void unbindTypedDatumService(TypedDatumService noldTypedDatumService) {
        /*
         * nothing to do here, this unbind method is only needed, because DS is throwing an
         * exception when disposing otherwise. probably a bug
         */
    }

    @Override
    public boolean prepareExecutor(ComponentContext componentContext, DistributedNotificationService inNotificationService) {

        super.prepareExecutor(componentContext, inNotificationService);

        try {
            // this method determins the locaton of the jython.jar
            jythonPath = ScriptingUtils.getJythonPath();
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        File scripts = new File(tempDir, "scripts");
        File file =
            new File(scripts, "script.tmp");
        workingPath = scripts.getParentFile().getAbsolutePath().toString();
        workingPath = workingPath.replaceAll(ESCAPESLASH, SLASH);
        tempFiles.add(file);
        stateMap = new HashMap<String, Object>();
        return false;
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
                + "RCE_NotAValueOutputList = RCE.get_indefinite_outputs_internal()\n"
                + String.format("sys.stdout.write('%s')\nsys.stderr.write('%s')\nsys.stdout.flush()\nsys.stderr.flush()",
                    WorkflowConsoleForwardingWriter.CONSOLE_END, WorkflowConsoleForwardingWriter.CONSOLE_END);
    }

    private void loadScript(String userScript) {
        // wrappingScript = wrappingScript + userScript;
        body = userScript;
        if (body == null || body.length() == 0) {
            LOGGER.error("No Python script provided");
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
            } catch (ScriptException e) {
                throw new ComponentException("Failed to run Jython script", e);
            }
            try {
                // execute the script, written by the user.
                scriptEngine.eval(body);
            } catch (ScriptException e) {
                String line = "Script has errors!\n " + e.getCause().toString();
                componentContext.printConsoleLine(line, ConsoleRow.Type.STDERR);
                throw new ComponentException("Could not run script. Maybe the script has errors? \n\n", e);
            }
            try {
                // this script defines the outputChannel, so that all outputs sent with
                // RCE.write_output() work properly.
                scriptEngine.eval(foot);
                ((WorkflowConsoleForwardingWriter) scriptEngine.getContext().getWriter()).awaitPrintingLinesFinished();
                ((WorkflowConsoleForwardingWriter) scriptEngine.getContext().getErrorWriter()).awaitPrintingLinesFinished();
            } catch (ScriptException e) {
                throw new ComponentException("Failed reading Output Channels! \n\n", e);
            } catch (InterruptedException e) {
                LOGGER.error("Waiting for stdout or stderr writer was interrupted", e);
            }
        }
    }

    @Override
    public boolean postRun() throws ComponentException {
        ScriptingUtils.writeAPIOutput(stateMap, componentContext, scriptEngine, workingPath, historyDataItem);
        try {
            closeConsoleWritersAndAddLogsToHistoryDataItem();
        } catch (IOException e) {
            LOGGER.error("Closing or storing console log file failed", e);
        }

        return true;
    }

    @Override
    protected void bindComponentDataManagementService(ComponentDataManagementService compDataManagementService) {
        componentDatamanagementService = compDataManagementService;
    }
}
