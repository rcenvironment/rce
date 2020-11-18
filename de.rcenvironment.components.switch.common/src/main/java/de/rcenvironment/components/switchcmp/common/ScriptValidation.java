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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
 * @author Kathrin Schaffert
 */
public final class ScriptValidation {

    private static ScriptEngine engine = null;

    private static final Log LOGGER = LogFactory.getLog(ScriptValidation.class);

    private static final int COLUMNNOTFOUND = -1;

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

        // This check is primarily intended for the headless usecase, since no SwitchComponentValidator is executed before the workflow is
        // started. Otherwise there would be no user feedback that no conditions are defined.
        // K. Schaffert, 22.06.2020
        if (script.trim().isEmpty()) {
            return Messages.noConditionString;
        }

        ArrayList<String> errorMessage = collectErrorMessage(script, inputsAndConnectionStatus, inputsAndDataTypes);
        Set<String> errorMessagePython = collectPythonErrorMessages(script, scriptEngine, inputsAndDataTypes);
        errorMessage.addAll(errorMessagePython);

        return createErrorMessageString(errorMessage);
    }

    private static Set<String> collectPythonErrorMessages(String script, ScriptEngine scriptEngine,
        Map<String, DataType> inputsAndDataTypes) {
        Set<String> errorMessagePython = new HashSet<>();

        StringBuilder varDefScript = buildVariableDefinitionScript(inputsAndDataTypes);

        try {
            // As the Jython script engine is not thread safe (console outputs of multiple script
            // executions are mixed), we must ensure that at most one script is executed at the same
            // time
            synchronized (ScriptingUtils.SCRIPT_EVAL_LOCK_OBJECT) {
                String evalScript = varDefScript.toString() + "if " + script + ":\n    returnValue=True\nelse:\n    returnValue=False";
                scriptEngine.eval(evalScript); // test for jython specific syntax errors
            }
        } catch (ScriptException e) { // generate own exception message to hide the use of jython
            if (e.getColumnNumber() != COLUMNNOTFOUND) {
                errorMessagePython.add("Syntax error: mismatched input at position "
                    + (e.getColumnNumber() - 3)); // subtract 3 columns for the character string "if "
            } else {
                errorMessagePython.add("Syntax error: unknown position");
            }
        }
        return errorMessagePython;
    }

    private static ArrayList<String> collectErrorMessage(String script, Map<String, Boolean> inputsAndConnectionStatus,
        Map<String, DataType> inputsAndDataTypes) {
        ArrayList<String> errorMessage = new ArrayList<>();
        Set<String> errorMessageNotSupported = new HashSet<>();
        Set<String> errorMessageNotDefined = new HashSet<>();
        Set<String> errorMessageNotConnected = new HashSet<>();

        List<String> operatorList = new ArrayList<>(Arrays.asList(SwitchComponentConstants.OPERATORS));
        operatorList.addAll(Arrays.asList(SwitchComponentConstants.OPERATORS_FOR_VALIDATION));

        Pattern operatorPattern = Pattern.compile(createValidationRegex(operatorList));
        Matcher operatorMatcher = operatorPattern.matcher(script);

        if (!inputsAndConnectionStatus.keySet().isEmpty()) {
            for (String inputName : inputsAndConnectionStatus.keySet()) {
                DataType inputToForwardDataType = inputsAndDataTypes.get(inputName);
                while (operatorMatcher.find()) {
                    if (operatorMatcher.group(0).equals(inputName) && inputToForwardDataType != null
                        && !Arrays.asList(SwitchComponentConstants.CONDITION_SCRIPT_DATA_TYPES).contains(inputToForwardDataType)) {
                        errorMessageNotSupported.add(StringUtils.format("Data type '%s' of input '%s' not supported in script",
                            inputToForwardDataType, inputName));
                    }
                    if (!inputsAndConnectionStatus.containsKey(operatorMatcher.group(0)) && !operatorList.contains(operatorMatcher.group(0))
                        && !operatorMatcher.group(0).trim().isEmpty()
                        && !org.apache.commons.lang3.StringUtils.isNumeric(operatorMatcher.group())) {
                        errorMessageNotDefined.add(StringUtils.format("'%s' is not defined", operatorMatcher.group(0)));
                    }
                }
            }
        } else {
            while (operatorMatcher.find()) {
                if (!operatorList.contains(operatorMatcher.group(0)) && !operatorMatcher.group(0).trim().isEmpty()
                    && !org.apache.commons.lang3.StringUtils.isNumeric(operatorMatcher.group())) {
                    errorMessageNotDefined.add(StringUtils.format("'%s' is not defined", operatorMatcher.group(0)));
                }
            }
        }

        errorMessage.addAll(errorMessageNotSupported);
        errorMessage.addAll(errorMessageNotDefined);
        errorMessage.addAll(errorMessageNotConnected);
        return errorMessage;
    }

    private static StringBuilder buildVariableDefinitionScript(Map<String, DataType> inputsAndDataTypes) {
        StringBuilder varDefScript = new StringBuilder();
        String str;
        for (Entry<String, DataType> entry : inputsAndDataTypes.entrySet()) {
            switch (entry.getValue()) {
            case Integer:
                str = entry.getKey() + " = 11\n";
                varDefScript.append(str);
                break;
            case Float:
                str = entry.getKey() + " = 11.1\n";
                varDefScript.append(str);
                break;
            case Boolean:
                str = entry.getKey() + " = True\n";
                varDefScript.append(str);
                break;
            default:
                break;
            }
        }
        return varDefScript;
    }

    private static String createErrorMessageString(ArrayList<String> errorMessage) {
        if (!errorMessage.isEmpty()) {
            if (errorMessage.size() == 1) {
                return errorMessage.get(0);
            } else {
                String lastErrorMessage = errorMessage.remove(errorMessage.size() - 1);
                String string = org.apache.commons.lang3.StringUtils.join(errorMessage, "; \n");
                return string + "; \n" + lastErrorMessage;
            }
        } else {
            return "";
        }
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
