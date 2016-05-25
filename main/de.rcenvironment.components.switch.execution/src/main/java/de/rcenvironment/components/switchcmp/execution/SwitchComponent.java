/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.text.WordUtils;

import de.rcenvironment.components.switchcmp.common.ScriptValidation;
import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.common.SwitchComponentHistoryDataItem;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Switch component.
 * 
 * @author David Scholz
 * @author Doreen Seider
 */
public class SwitchComponent extends DefaultComponent {

    protected static ScriptingService scriptingService;

    private static final String QUOTE = "\\\\Q";

    private static final String ENDQUOTE = "\\\\E";

    private ScriptLanguage scriptLanguage;

    private ScriptEngine engine;

    private String condition;
    
    private CloseOutputBehavior closeOutputBehavior;

    private ComponentContext componentContext;

    private SwitchComponentHistoryDataItem historyDataItem;
    
    /**
     * Whether outputs should be closed.
     * 
     * @author Doreen Seider
     */
    private enum CloseOutputBehavior {
        NeverCloseOutputs,
        CloseOutputsOnTrue,
        CloseOutputsOnFalse;
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        condition = componentContext.getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);
        closeOutputBehavior = getCloseOutputBehavior();
        scriptLanguage = ScriptLanguage.getByName(SwitchComponentConstants.SCRIPT_LANGUAGE);
        scriptingService = componentContext.getService(ScriptingService.class);
        engine = scriptingService.createScriptEngine(scriptLanguage);
        String errorMessage = ScriptValidation.validateScript(condition, engine, getInputAndConnectionStatus(),
                getInputsAndDataTypes());
        if (!errorMessage.isEmpty()) { // validation before workflowstart
            throw new ComponentException(errorMessage);
        }
    }

    // Use one key for each enum value in component config map to make use of the property handling in the GUI provided by
    // WorkflowNodePropertySection. It can't map a radio button group to enums. (But it can map one single radio button to one config key.
    private CloseOutputBehavior getCloseOutputBehavior() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(SwitchComponentConstants.CLOSE_OUTPUTS_ON_TRUE_KEY))) {
            return CloseOutputBehavior.CloseOutputsOnTrue;
        } else if (Boolean.valueOf(componentContext.getConfigurationValue(SwitchComponentConstants.CLOSE_OUTPUTS_ON_FALSE_KEY))) {
            return CloseOutputBehavior.CloseOutputsOnFalse;
        } else {
            return CloseOutputBehavior.NeverCloseOutputs;
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        String conditionWithActualValues = condition;
        TypedDatum switchDatum = componentContext.readInput(SwitchComponentConstants.DATA_INPUT_NAME);
        for (String inputName : componentContext.getInputsWithDatum()) {
            TypedDatum datum = componentContext.readInput(inputName);
            if (datum.getDataType().equals(DataType.Float) || datum.getDataType().equals(DataType.Integer)) {
                conditionWithActualValues = conditionWithActualValues.replace(inputName, Pattern.quote(datum.toString())
                    .replaceAll(QUOTE, "")).replaceAll(ENDQUOTE, "");
            }
            if (datum.getDataType().equals(DataType.Boolean)) {
                conditionWithActualValues =
                    conditionWithActualValues.replace(inputName, WordUtils.capitalize(Pattern.quote(datum.toString())
                        .replaceAll(QUOTE, ""))).replaceAll(ENDQUOTE, "");
            }
        }

        String evalScript = "if " + conditionWithActualValues + ":\n    returnValue=True\nelse:\n    returnValue=False";
        Object returnValue;
        try {
            // As the Jython script engine is not thread safe (console outputs of multiple script
            // executions are mixed), we must ensure that at most one script is executed at the same
            // time
            synchronized (ScriptingUtils.SCRIPT_EVAL_LOCK_OBJECT) {
                engine.eval(evalScript);
                returnValue = engine.get("returnValue");
            }
        } catch (ScriptException e) {
            throw new ComponentException(StringUtils.format("Failed to interpret condition '%s': %s", 
                conditionWithActualValues, e.getMessage()), e); // should never happen
        }
        
        componentContext.getLog().componentInfo(StringUtils.format("Evaluated '%s' -> %b", conditionWithActualValues, returnValue));

        if (historyDataItem != null) {
            historyDataItem.setActualCondition(StringUtils.format("%s -> %b", conditionWithActualValues, returnValue));
            historyDataItem.setConditionPattern(condition);
        }

        String outputName;
        if ((Boolean) returnValue) {
            componentContext.writeOutput(SwitchComponentConstants.TRUE_OUTPUT, switchDatum);
            if (closeOutputBehavior == CloseOutputBehavior.CloseOutputsOnTrue) {
                componentContext.closeAllOutputs();
            }
            outputName = SwitchComponentConstants.TRUE_OUTPUT;
        } else {
            componentContext.writeOutput(SwitchComponentConstants.FALSE_OUTPUT, switchDatum);
            if (closeOutputBehavior == CloseOutputBehavior.CloseOutputsOnFalse) {
                componentContext.closeAllOutputs();
            }
            outputName = SwitchComponentConstants.FALSE_OUTPUT;
        }
        componentContext.getLog().componentInfo(StringUtils.format("Wrote to '%s': %s", outputName, switchDatum));

        writeFinalHistoryDataItem();
    }
    
    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        writeFinalHistoryDataItem();
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new SwitchComponentHistoryDataItem(SwitchComponentConstants.COMPONENT_ID);
        }
    }

    private void writeFinalHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    private Map<String, Boolean> getInputAndConnectionStatus() {
        Map<String, Boolean> inputs = new HashMap<>();
        
        for (String name : componentContext.getInputs()) {
            inputs.put(name, true);
        }
        
        for (String name : componentContext.getInputsNotConnected()) {
            inputs.put(name, false);
        }

        return inputs;
    }
    
    private Map<String, DataType> getInputsAndDataTypes() {
        Map<String, DataType> inputs = new HashMap<>();
        
        for (String name : componentContext.getInputs()) {
            inputs.put(name, componentContext.getInputDataType(name));
        }
        for (String name : componentContext.getInputsNotConnected()) {
            inputs.put(name, componentContext.getInputDataType(name));
        }
        return inputs;
    }

}
