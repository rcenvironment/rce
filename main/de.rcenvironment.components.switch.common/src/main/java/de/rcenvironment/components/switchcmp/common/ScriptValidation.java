/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
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
     * Script validation in gui.
     * 
     * @param callerInstance instance for serviceregistry
     * @param script condition
     * @param allowedItems list with input names used in script
     * @param inputToSwitchDataType for testing use of invalid datatypes in script
     * @return error message or null if script is valid
     */
    public static String validateScript(String script, List<String> allowedItems, DataType inputToSwitchDataType, Object callerInstance) {
        if (engine == null) { // create engine for use in gui
            ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(callerInstance);
            ScriptingService service = serviceRegistryAccess.getService(ScriptingService.class);
            engine = service.createScriptEngine(ScriptLanguage.getByName(SwitchComponentConstants.SCRIPT_LANGUAGE));
        }
        return validateScript(script, engine, allowedItems, inputToSwitchDataType);
    }

    /**
     * 
     * Script validation in execution.
     * 
     * @param script condition
     * @param scriptEngine for script execution
     * @param allowedItems list with input names used in script
     * @param inputToSwitchDataType for testing use of invalid datatypes in script
     * @return error message or null if script is valid
     */
    public static String validateScript(String script, ScriptEngine scriptEngine, List<String> allowedItems,
        DataType inputToSwitchDataType) {
        if (script == null || script.trim().isEmpty()) {
            return "No condition is defined";
        }

        Set<String> scriptErrorMessage = new HashSet<>(); // very important that HashSet is used!
        String syntaxErrorMessage = "";
        String notDefinedMessage = "%s not defined";
        List<String> operatorList = new ArrayList<>(Arrays.asList(SwitchComponentConstants.OPERATORS));
        operatorList.addAll(Arrays.asList(SwitchComponentConstants.OPERATORS_FOR_VALIDATION));

        Pattern operatorPattern = Pattern.compile(createValidationRegex(operatorList));
        Matcher operatorMatcher = operatorPattern.matcher(script);

        while (operatorMatcher.find()) {
            if (operatorMatcher.group(0).equals(SwitchComponentConstants.DATA_INPUT_NAME) && inputToSwitchDataType != null) {
                if (!(inputToSwitchDataType.equals(DataType.Float) || inputToSwitchDataType.equals(DataType.Integer) 
                    || inputToSwitchDataType.equals(DataType.Boolean))) {
                    return "Input with unsupported data type used";
                }
            }
            if (!allowedItems.contains(operatorMatcher.group(0)) && !operatorList.contains(operatorMatcher.group(0))
                && !operatorMatcher.group(0).trim().isEmpty() && !StringUtils.isNumeric(operatorMatcher.group())) {
                scriptErrorMessage.add(operatorMatcher.group(0));
            }
        }
        if (!scriptErrorMessage.isEmpty()) {
            notDefinedMessage = String.format(notDefinedMessage, scriptErrorMessage.toString());
            return notDefinedMessage;
        }

        for (String name : allowedItems) {
            script = script.replace(name, Integer.toString(name.length()));
        }

        try {
            // As the Jython script engine is not thread safe (console outputs of multiple script
            // executions are mixed), we must ensure that at most one script is executed at the same
            // time
            synchronized (ScriptingUtils.SCRIPT_EVAL_LOCK_OBJECT) {
                String evalScript =
                    "if " + script + ":\n    returnValue=True\nelse:\n    returnValue=False";
                scriptEngine.eval(evalScript); // test for jython specific syntax errors
            }
        } catch (ScriptException e) { // generate own exception message to hide the use of jython
            syntaxErrorMessage =
                "Syntax error: mismatched input " + "at line number " + e.getLineNumber() + " at column number " + e.getColumnNumber();
        }

        return syntaxErrorMessage;
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
