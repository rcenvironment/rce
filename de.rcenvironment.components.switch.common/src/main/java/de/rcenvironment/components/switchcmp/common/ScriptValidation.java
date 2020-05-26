/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * 
 * Checks user script for syntax errors and not allowed jython constructs.
 *
 * @author David Scholz
 */
public final class ScriptValidation {

    private static ScriptEngine engine = null;

    private static final Log LOGGER = LogFactory.getLog(ScriptValidation.class);
    
    private ScriptValidation() {}

    /**
     * 
     * Script validation in GUI.
     * 
     * @param callerInstance instance for service registry
     * @param script condition
     * @param inputsAndConnectionStatus name of inputs and flag indicating whether input is connected
     * @param inputsAndDataTypes data types of inputs (input name -> data type)
     * @return error message or <code>null</code> if script is valid
     */
    public static String validateScript(String script, Map<String, Boolean> inputsAndConnectionStatus,
        Map<String, DataType> inputsAndDataTypes, Object callerInstance) {
        if (engine == null) { // create engine for use in gui
            ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(callerInstance);
            ScriptingService service = serviceRegistryAccess.getService(ScriptingService.class);
            engine = service.createScriptEngine(ScriptLanguage.getByName(SwitchComponentConstants.SCRIPT_LANGUAGE));
        }
        return validateScript(script, engine, inputsAndConnectionStatus, inputsAndDataTypes);
    }

    /**
     * 
     * Script validation in execution.
     * 
     * @param script condition
     * @param scriptEngine for script execution
     * @param inputsAndConnectionStatus name of inputs and flag indicating whether input is connected
     * @param inputsAndDataTypes name of inputs and their data types
     * @return error message or null if script is valid
     */
    public static String validateScript(String script, ScriptEngine scriptEngine, Map<String, Boolean> inputsAndConnectionStatus,
        Map<String, DataType> inputsAndDataTypes) {
        if (script == null || script.trim().isEmpty()) {
            return "No condition is defined";
        }

        String errorMessage = "";
        List<String> operatorList = new ArrayList<>(Arrays.asList(SwitchComponentConstants.OPERATORS));
        operatorList.addAll(Arrays.asList(SwitchComponentConstants.OPERATORS_FOR_VALIDATION));

        Pattern operatorPattern = Pattern.compile(createValidationRegex(operatorList));
        Matcher operatorMatcher = operatorPattern.matcher(script);

        DataType inputToForwardDataType = inputsAndDataTypes.get(SwitchComponentConstants.DATA_INPUT_NAME);
        while (operatorMatcher.find()) {
            if (operatorMatcher.group(0).equals(SwitchComponentConstants.DATA_INPUT_NAME) && inputToForwardDataType != null) {
                if (!(inputToForwardDataType.equals(DataType.Float) || inputToForwardDataType.equals(DataType.Integer)
                    || inputToForwardDataType.equals(DataType.Boolean))) {
                    errorMessage = appendErrorMessage(errorMessage,
                        StringUtils.format("Data type '%s' of input '%s' not supported in script",
                            inputToForwardDataType, SwitchComponentConstants.DATA_INPUT_NAME));
                }
            }
            if (!inputsAndConnectionStatus.containsKey(operatorMatcher.group(0)) && !operatorList.contains(operatorMatcher.group(0))
                && !operatorMatcher.group(0).trim().isEmpty() && !org.apache.commons.lang3.StringUtils.isNumeric(operatorMatcher.group())) {
                errorMessage = appendErrorMessage(errorMessage, StringUtils.format("'%s' is not defined", operatorMatcher.group(0)));
            }
            if (inputsAndConnectionStatus.containsKey(operatorMatcher.group(0))
                && !inputsAndConnectionStatus.get(operatorMatcher.group(0))) {
                errorMessage = appendErrorMessage(errorMessage, StringUtils.format("'%s' is not connected", operatorMatcher.group(0)));
            }
        }

        for (Entry<String, DataType> entry : inputsAndDataTypes.entrySet()) {
            switch (entry.getValue()) {
            case Integer:
                script = script.replace(entry.getKey(), "11");
                break;
            case Float:
                script = script.replace(entry.getKey(), "11.1");
                break;
            case Boolean:
                script = script.replace(entry.getKey(), "True");
                break;
            default:
                break;
            }
        }

        try {
            // As the Jython script engine is not thread safe (console outputs of multiple script
            // executions are mixed), we must ensure that at most one script is executed at the same
            // time
            synchronized (ScriptingUtils.SCRIPT_EVAL_LOCK_OBJECT) {
                String evalScript = "if " + script + ":\n    returnValue=True\nelse:\n    returnValue=False";
                scriptEngine.eval(evalScript); // test for jython specific syntax errors
            }
        } catch (ScriptException e) { // generate own exception message to hide the use of jython
            errorMessage = appendErrorMessage(errorMessage, 
                "Syntax error: mismatched input " + "at line number " + e.getLineNumber() + " at column number " + e.getColumnNumber());
        }

        return errorMessage;
    }
    
    private static String appendErrorMessage(String errorMessage, String errorMessageToAppend) {
        if (!errorMessage.isEmpty()) {
            errorMessage += "\n";
        }
        errorMessage += errorMessageToAppend;
        return errorMessage;
        
    }

    private static String createValidationRegex(List<String> operatorList) {
        StringBuilder op = new StringBuilder();
        op.append("(");
        for (String operator : operatorList) {

            if (operator.equals("<") || operator.equals(">")) {
                op.append(operator + "(?!=)");
            } else {
                op.append(operator);
            }

            op.append("|");
        }
        op.append("\\b\\w+\\b");
        op.append(")");

        try {
            Pattern.compile(op.toString());
        } catch (PatternSyntaxException e) {
            LOGGER.error("Invalid Regex!", e);
        }

        return op.toString();
    }
}
