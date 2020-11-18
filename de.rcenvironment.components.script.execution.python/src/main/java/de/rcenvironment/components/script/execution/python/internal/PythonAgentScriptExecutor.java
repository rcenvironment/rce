/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.script.execution.python.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptException;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.components.script.common.pythonAgentInstanceManager.internal.PythonAgent;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.execution.DefaultScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.scripting.python.PythonComponentConstants;
import de.rcenvironment.core.scripting.python.PythonScriptContext;
import de.rcenvironment.core.scripting.python.PythonScriptEngine;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * Implementation of {@link ScriptExecutor} to execute python scripts. In contrast to the PythonScript, this implementation sends the
 * scripts to running python-instances, which serve to execute the scripts and send back the results.
 * 
 * @author Adrian Stock
 *
 */
public class PythonAgentScriptExecutor extends DefaultScriptExecutor {

    private static final String NOT_VALUE_UUID = "not_a_value_7fdc603e";

    private static final String OS = "os";

    private final PythonAgentInstanceManager pythonInstanceManagerService;

    private PythonAgent agent;

    private PythonScriptContext scriptContext;

    private ComponentContext compCtx;

    public PythonAgentScriptExecutor(PythonAgentInstanceManager pythonInstanceManager) {
        pythonInstanceManagerService = pythonInstanceManager;
    }

    @Override
    public boolean prepareExecutor(ComponentContext newCompCtx) throws ComponentException {
        compCtx = newCompCtx;
        super.prepareExecutor(compCtx);
        setComponentContext(compCtx);
      
        final String pythonInstallation = getPythonPathFromConfigurationOrThrowException();

        try {
            agent = pythonInstanceManagerService.getAgent(pythonInstallation, compCtx);
        } catch (IOException e) {
            throw new ComponentException("Unable to create python agent", e);
        }

        scriptContext = new PythonScriptContext();
        scriptContext.setAttribute(PythonComponentConstants.PYTHON_INSTALLATION, pythonInstallation, 0);
        scriptContext.setAttribute(OS, OSFamily.getLocal(), 0);
        scriptContext.setAttribute(PythonComponentConstants.COMPONENT_CONTEXT, componentContext, 0);
        stateMap = new HashMap<>();
        scriptingService = compCtx.getService(ScriptingService.class);
        prepareOutputForRun();
        return true;
    }

    private String getPythonPathFromConfigurationOrThrowException() throws ComponentException {

        final String pythonInstallation = getPythonConfigurationSegment().getString("binaryPath");
        if (pythonInstallation == null || pythonInstallation.isEmpty()) {
            throw new ComponentException(
                "No Python installation specified. Please add thirdPartyIntegration parameter to configuration file.");
        }
        return pythonInstallation;
    }

    private ConfigurationSegment getPythonConfigurationSegment() {
        final ConfigurationService configurationService = compCtx.getService(ConfigurationService.class);
        final ConfigurationSegment pythonConfiguration = configurationService.getConfigurationSegment("thirdPartyIntegration/python");

        return pythonConfiguration;

    }

    @Override
    public void prepareNewRun(ScriptLanguage scriptLanguage, String userScript, ScriptComponentHistoryDataItem dataItem)
        throws ComponentException {

        historyDataItem = dataItem;
        scriptEngine = scriptingService.createScriptEngine(scriptLanguage);
        wrappingScript = userScript;
        if (wrappingScript == null || wrappingScript.length() == 0) {
            throw new ComponentException("No Python script configured");
        }
        scriptEngine.setContext(scriptContext);
        scriptContext.removeAttribute(PythonComponentConstants.STATE_MAP, 0);
        scriptContext.setAttribute(PythonComponentConstants.STATE_MAP, stateMap, 0);
        scriptContext.removeAttribute(PythonComponentConstants.RUN_NUMBER, 0);
        scriptContext.setAttribute(PythonComponentConstants.RUN_NUMBER, getCurrentRunNumber(), 0);
        ((PythonScriptEngine) scriptEngine).createNewExecutor(historyDataItem);
    }

    @Override
    public void runScript() throws ComponentException {

        try {
            agent.acquireLock();
//          Executing script here
            componentContext.announceExternalProgramStart();
            ((PythonScriptEngine) scriptEngine).agentPrepareScriptExecution(wrappingScript, agent.getDirectory());

            agent.executeScript(wrappingScript);

            ((PythonScriptEngine) scriptEngine).agentReadOutputFromPython(agent.getDirectory());

        } catch (IOException e) {
            agent.stopInstanceRun();
            throw new ComponentException("Failed to execute script", e);
        } catch (ScriptException | InterruptedException e) {
            throw new ComponentException("Failed to execute script", e);
        } finally {
            componentContext.announceExternalProgramTermination();
            agent.releaseLock();
        }
    }

    @Override
    public boolean postRun() throws ComponentException {

        TypedDatumFactory factory = componentContext.getService(TypedDatumService.class).getFactory();
        for (String outputName : componentContext.getOutputs()) {
            DataType type = componentContext.getOutputDataType(outputName);
            @SuppressWarnings("unchecked") List<Object> resultList = (List<Object>) scriptEngine.get(outputName);
            TypedDatum outputValue = null;
            if (scriptEngine.get(outputName) != null) {
                for (Object o : resultList) {
                    if (o != null && !String.valueOf(o).equals(NOT_VALUE_UUID)) {

                        outputValue = ScriptingUtils.getOutputByType(o, type, outputName,
                            agent.getDirectory().getAbsolutePath(), componentContext);
                        componentContext.writeOutput(outputName, outputValue);

                    } else if (String.valueOf(o).equals(NOT_VALUE_UUID)) {
                        outputValue = factory.createNotAValue(); // "not a value" value
                        componentContext.writeOutput(outputName, outputValue);
                    }
                }
            }
        }
        stateMap = ((PythonScriptEngine) scriptEngine).getStateOutput();
        for (String outputName : ((PythonScriptEngine) scriptEngine).getCloseOutputChannelsList()) {
            componentContext.closeOutput(outputName);
        }
        this.deleteTempFiles();
        return true;
    }

    @Override
    public void cancelScript() {}

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public void tearDown() {

        final boolean agentStopped;
        if (agent != null) {
            agentStopped = pythonInstanceManagerService.stopAgent(agent);
        } else {
            agentStopped = false;
        }
        
        if (agentStopped && scriptEngine != null) {
            LogFactory.getLog(this.getClass()).debug("Disposing script engine");
            ((PythonScriptEngine) scriptEngine).dispose();
        }
    }

    @Override
    public void prepareOutputForRun() {
        ConsoleRowUtils.logToWorkflowConsole(((ComponentContext) scriptContext.getAttribute(
            PythonComponentConstants.COMPONENT_CONTEXT)).getLog(), agent.getStdout(),
            ConsoleRow.Type.TOOL_OUT, null, false);
        ConsoleRowUtils.logToWorkflowConsole(((ComponentContext) scriptContext.getAttribute(
            PythonComponentConstants.COMPONENT_CONTEXT)).getLog(), agent.getStderr(),
            ConsoleRow.Type.TOOL_ERROR, null, false);
    }
}
