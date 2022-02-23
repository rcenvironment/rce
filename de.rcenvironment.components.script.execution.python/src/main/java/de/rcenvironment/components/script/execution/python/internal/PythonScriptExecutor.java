/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.script.execution.python.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.script.common.ScriptComponentHistoryDataItem;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.execution.DefaultScriptExecutor;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
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
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * Implementation of {@link ScriptExecutor} to execute python scripts. This is needed because Python is not part of the default ScriptEngine
 * and must be implemented manually. For this we require some different code for the {@link ScriptExecutor} methods.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel (#14029)
 * @author Kathrin Schaffert (#14965)
 */
public class PythonScriptExecutor extends DefaultScriptExecutor {

    private static final String NOT_VALUE_UUID = "not_a_value_7fdc603e";

    private static final String OS = "os";

    private PythonScriptContext scriptContext;

    private Log log = LogFactory.getLog(PythonScriptExecutor.class);

    @Override
    public boolean prepareExecutor(ComponentContext compCtx) throws ComponentException {
        super.prepareExecutor(compCtx);
        componentContext = compCtx;
        String pythonInstallation = componentContext.getConfigurationValue(PythonComponentConstants.PYTHON_INSTALLATION);
        if (pythonInstallation == null || pythonInstallation.isEmpty()) {
            throw new ComponentException("No Python installation specified.");
        }
        scriptContext = new PythonScriptContext();
        scriptContext.setAttribute(PythonComponentConstants.PYTHON_INSTALLATION, pythonInstallation, 0);
        scriptContext.setAttribute(OS, OSFamily.getLocal(), 0);
        scriptContext.setAttribute(PythonComponentConstants.COMPONENT_CONTEXT, componentContext, 0);
        stateMap = new HashMap<>();
        scriptingService = compCtx.getService(ScriptingService.class);

        return true;
    }

    @Override
    public void prepareOutputForRun() {}

    @Override
    public void prepareNewRun(ScriptLanguage scriptLanguage, String userScript, ScriptComponentHistoryDataItem dataItem)
        throws ComponentException {
        historyDataItem = dataItem;
        scriptEngine = scriptingService.createScriptEngine(scriptLanguage);
        wrappingScript = userScript;
        if (wrappingScript == null || wrappingScript.trim().isEmpty()) {
            throw new ComponentException("No Python script configured");
        }
        scriptEngine.setContext(scriptContext);
        scriptContext.removeAttribute(PythonComponentConstants.STATE_MAP, 0);
        scriptContext.setAttribute(PythonComponentConstants.STATE_MAP, stateMap, 0);
        scriptContext.removeAttribute(PythonComponentConstants.RUN_NUMBER, 0);
        scriptContext.setAttribute(PythonComponentConstants.RUN_NUMBER, getCurrentRunNumber(), 0);
        ((PythonScriptEngine) scriptEngine).createNewExecutor(historyDataItem);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
    }

    @Override
    public void runScript() throws ComponentException {
        int exitCode = 0;
        try {
            // Executing script here
            componentContext.announceExternalProgramStart();
            exitCode = (Integer) scriptEngine.eval(wrappingScript);
        } catch (ScriptException e) {
            throw new ComponentException("Failed to execute script", e);
        } finally {
            componentContext.announceExternalProgramTermination();
        }

        if (exitCode != 0) {
            throw new ComponentException("Failed to execute script; exit code: " + exitCode);
        }
    }

    @Override
    public boolean postRun() throws ComponentException {
        TypedDatumFactory factory = componentContext.getService(TypedDatumService.class).getFactory();
        for (String outputName : componentContext.getOutputs()) {
            DataType type = componentContext.getOutputDataType(outputName);
            @SuppressWarnings("unchecked") List<Object> resultList = (List<Object>) scriptEngine.get(outputName);
            TypedDatum outputValue = null;
            String workingPath = ((PythonScriptEngine) scriptEngine).getExecutor().getWorkDir().getAbsolutePath();
            if (scriptEngine.get(outputName) != null) {
                for (Object o : resultList) {
                    if (o != null && !String.valueOf(o).equals(NOT_VALUE_UUID)) {
                    
                        outputValue = ScriptingUtils.getOutputByType(o, type, outputName, workingPath, componentContext);              
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
        ((PythonScriptEngine) scriptEngine).dispose();
        this.deleteTempFiles();
        return true;
    }

    private TypedDatum convertBoolean(TypedDatumFactory factory, Object o) throws ComponentException {
        String stringValue = o.toString();
        boolean isNumber = true;
        TypedDatum outputValue = null;

        try {
            float numberValue = Float.parseFloat(stringValue);
            if (Math.abs(numberValue) > 0) {
                outputValue = factory.createBoolean(true);
            } else {
                outputValue = factory.createBoolean(false);
            }
        } catch (NumberFormatException e) {
            isNumber = false;
        }

        if (!isNumber && (stringValue.equalsIgnoreCase("0") || stringValue.equalsIgnoreCase("0L") || stringValue.equalsIgnoreCase("0.0")
            || stringValue.equalsIgnoreCase("0j") || stringValue.equalsIgnoreCase("()") || stringValue.equalsIgnoreCase("[]")
            || stringValue.isEmpty() || stringValue.equalsIgnoreCase("{}") || stringValue.equalsIgnoreCase("false")
            || stringValue.equalsIgnoreCase("none"))) {

            outputValue = factory.createBoolean(false);

        } else if (!isNumber) {
            outputValue = factory.createBoolean(true);
        }
        return outputValue;
    }

    private TypedDatum handleFileOrDirectoryOutput(String outputName, TypedDatum outputValue, String type, Object o)
        throws ComponentException {
        try {
            File file = new File(String.valueOf(o));
            if (!file.isAbsolute()) {
                file = new File(((PythonScriptEngine) scriptEngine).getExecutor().getWorkDir(), String.valueOf(o));
            }
            if (file.exists()) {
                if (type.equals("directory")) {
                    outputValue =
                        componentContext.getService(ComponentDataManagementService.class).createDirectoryReferenceTDFromLocalDirectory(
                            componentContext, file,
                            file.getName());
                } else {
                    outputValue =
                        componentContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(
                            componentContext, file,
                            file.getName());
                }
                if (file.getAbsolutePath().startsWith(
                    componentContext.getService(ConfigurationService.class)
                        .getParentTempDirectoryRoot().getAbsolutePath())) {
                    tempFiles.add(file);
                }
            } else {
                throw new ComponentException(StringUtils.format(
                    "Failed to write %s to output '%s' as it does not exist: %s", type, outputName, file.getAbsolutePath()));
            }
        } catch (IOException e) {
            throw new ComponentException(StringUtils.format(
                "Failed to store %s into the data management - "
                    + "if it is not stored in the data management, it can not be sent as output value",
                type), e);
        }
        return outputValue;
    }

    @Override
    public void deleteTempFiles() {
        super.deleteTempFiles();
        if (scriptEngine != null) {
            ((PythonScriptEngine) scriptEngine).dispose();
        }
    }

    @Override
    public void cancelScript() {
        if (scriptEngine == null) {
            // FIXME we need to delay the cancellation request, currently it is simple ignored in this case
            log.error("Cannot cancel the execution, as the script engine (Script Component) is not propertly prepared.");
            return;
        }
        ((PythonScriptEngine) scriptEngine).cancel();
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public void tearDown() {
        this.deleteTempFiles();
    }
}
