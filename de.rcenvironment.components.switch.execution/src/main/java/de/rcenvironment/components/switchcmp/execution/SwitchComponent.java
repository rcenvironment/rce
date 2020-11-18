/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.text.WordUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.switchcmp.common.Messages;
import de.rcenvironment.components.switchcmp.common.ScriptValidation;
import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.common.SwitchComponentHistoryDataItem;
import de.rcenvironment.components.switchcmp.common.SwitchCondition;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.scripting.ScriptingService;
import de.rcenvironment.core.scripting.ScriptingUtils;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Switch component.
 * 
 * @author David Scholz
 * @author Doreen Seider
 * @author Kathrin Schaffert
 */
public class SwitchComponent extends DefaultComponent {

    private static final String QUOTE = "\\\\Q";

    private static final String ENDQUOTE = "\\\\E";

    private static final String EXCEPTION_MESSAGE_WRITING = "Unexpected Exception occured, while writing JSON content String.";

    protected ScriptingService scriptingService;

    private ScriptLanguage scriptLanguage;

    private ScriptEngine engine;

    private String condition;

    private CloseOutputBehavior closeOutputBehavior;

    private ComponentContext componentContext;

    private SwitchComponentHistoryDataItem historyDataItem;

    private String closeOnCondition;

    /**
     * Whether outputs should be closed.
     * 
     * @author Doreen Seider
     * 
     */
    private enum CloseOutputBehavior {
        NEVER_CLOSE_OUTPUTS,
        CLOSE_OUTPUTS_ON_CONDITION_NUMBER,
        CLOSE_OUTPUTS_ON_NO_MATCH;
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;

    }
    @Override
    public void start() throws ComponentException {
        condition = componentContext.getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);
        closeOutputBehavior = getCloseOutputBehavior();
        if (closeOutputBehavior.equals(CloseOutputBehavior.CLOSE_OUTPUTS_ON_CONDITION_NUMBER)) {
            closeOnCondition = componentContext.getConfigurationValue(SwitchComponentConstants.SELECTED_CONDITION);
        }
        scriptLanguage = ScriptLanguage.getByName(SwitchComponentConstants.SCRIPT_LANGUAGE);
        scriptingService = componentContext.getService(ScriptingService.class);
        engine = scriptingService.createScriptEngine(scriptLanguage);

        List<SwitchCondition> conditionArray;
        if (condition != null && !condition.equals("")) {
            conditionArray = SwitchCondition.getSwitchConditionList(condition);
        } else {
            throw new ComponentException(Messages.noConditionKey);
        }

        for (SwitchCondition con : conditionArray) {

            String errorMessage = ScriptValidation.validateScript(con.getConditionScript(), engine, getInputAndConnectionStatus(),
                getInputsAndDataTypes());
            if (!errorMessage.isEmpty()) { // validation before workflowstart
                throw new ComponentException(errorMessage);
            }
        }

    }

    // Use one key for each enum value in component config map to make use of the property handling in the GUI provided by
    // WorkflowNodePropertySection. It can't map a radio button group to enums. (But it can map one single radio button to one config key.
    private CloseOutputBehavior getCloseOutputBehavior() {
        if (Boolean.TRUE.equals(
            Boolean.valueOf(componentContext.getConfigurationValue(SwitchComponentConstants.CLOSE_OUTPUTS_ON_CONDITION_NUMBER_KEY)))) {
            return CloseOutputBehavior.CLOSE_OUTPUTS_ON_CONDITION_NUMBER;
        } else if (Boolean.TRUE
            .equals(Boolean.valueOf(componentContext.getConfigurationValue(SwitchComponentConstants.CLOSE_OUTPUTS_ON_NO_MATCH_KEY)))) {
            return CloseOutputBehavior.CLOSE_OUTPUTS_ON_NO_MATCH;
        } else {
            return CloseOutputBehavior.NEVER_CLOSE_OUTPUTS;
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ArrayList<SwitchCondition> conditionArrayWithPlaceholder =
            (ArrayList<SwitchCondition>) SwitchCondition.getSwitchConditionList(condition);

        Map<String, String> actualConditionArray = new HashMap<>();
        String writeToFirstCondition = componentContext.getConfigurationValue(SwitchComponentConstants.WRITE_OUTPUT_KEY);
        Boolean outputWritten = false;
        Boolean closeOutputs = false;

        List<Boolean> list =
            checkConditionsAndWriteOutput(conditionArrayWithPlaceholder, actualConditionArray, closeOutputs, outputWritten,
                writeToFirstCondition);

        outputWritten = list.get(0);
        closeOutputs = list.get(1);

        // Close Outputs, in case they should be closed after sending
        if (Boolean.TRUE.equals(closeOutputs)) {
            componentContext.closeAllOutputs();
        }

        // If No Match
        if (Boolean.FALSE.equals(outputWritten)) {
            // write outputs in case there is no match
            writeOutputsNoMatch();

            // close outputs, if parameter is set
            if (closeOutputBehavior == CloseOutputBehavior.CLOSE_OUTPUTS_ON_NO_MATCH) {
                componentContext.closeAllOutputs();
            }
        }

        if (historyDataItem != null) {
            try {
                historyDataItem.setActualCondition(mapper.writeValueAsString(actualConditionArray));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(EXCEPTION_MESSAGE_WRITING, e); // should never happen
            }
            historyDataItem.setConditionPattern(condition);
            historyDataItem.setWriteToFirstCondition(writeToFirstCondition);
            historyDataItem.setIdentifier(componentContext.getComponentIdentifier());
        }
        writeFinalHistoryDataItem();
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        writeFinalHistoryDataItem();
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.TRUE.equals(Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM)))) {
            historyDataItem = new SwitchComponentHistoryDataItem();
        }
    }

    private void writeFinalHistoryDataItem() {
        if (historyDataItem != null && Boolean.TRUE
            .equals(Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM)))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    /**
     * 
     * Proves, if there is a match and outputs should be written.
     * 
     * @param conditionArrayWithPlaceholder
     * @param actualConditionMap
     * @param closeOutputs
     * @param outputWritten
     * @param writeToFirstCondition
     * @return List of boolean parameter outputWritten and closeOutputs
     * @throws ComponentException
     */
    private List<Boolean> checkConditionsAndWriteOutput(ArrayList<SwitchCondition> conditionArrayWithPlaceholder,
        Map<String, String> actualConditionMap, Boolean closeOutputs, Boolean outputWritten, String writeToFirstCondition)
        throws ComponentException {
        Object returnValue = false;
        for (int i = 0; i < conditionArrayWithPlaceholder.size(); i++) {

            String conditionNumber = String.valueOf(i + 1);
            String conditionWithPlaceholder = conditionArrayWithPlaceholder.get(i).getConditionScript();

            if (conditionWithPlaceholder.isEmpty()) {
                continue;
            }

            String conditionWithActualValues = replacePlaceholder(conditionWithPlaceholder);
            String evalScript = "if " + conditionWithActualValues + ":\n    returnValue=True\nelse:\n    returnValue=False";

            returnValue = evaluateScript(evalScript, conditionWithActualValues, i + 1);

            componentContext.getLog().componentInfo(StringUtils.format("Evaluated '%s' -> %b", conditionWithActualValues, returnValue));
            actualConditionMap.put(conditionNumber, StringUtils.format("%s -> %b", conditionWithActualValues, returnValue));

            // write outputs, if script is true
            if (Boolean.TRUE.equals((Boolean) returnValue)) {

                outputWritten = writeOutputs(conditionNumber);

                // set closeOutputs parameter to true, if closing behavior and condition number fit
                if (closeOutputBehavior == CloseOutputBehavior.CLOSE_OUTPUTS_ON_CONDITION_NUMBER && closeOnCondition != null
                    && closeOnCondition.equals(conditionNumber)) {
                    closeOutputs = true;
                }
            }

            // break loop, if parameter writeToFirstCondition is true
            Boolean writeOutput = Boolean.parseBoolean(writeToFirstCondition);
            if (writeOutput && outputWritten) {
                break;
            }
        }

        List<Boolean> list = new ArrayList<>();
        list.add(outputWritten);
        list.add(closeOutputs);

        return list;
    }

    private Object evaluateScript(String evalScript, String conditionWithActualValues, int conditionNumber) throws ComponentException {
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
            throw new ComponentException(StringUtils.format("Failed to interpret condition %s '%s': %s",
                String.valueOf(conditionNumber), conditionWithActualValues, e.getMessage()), e); // should never happen
        }
        return returnValue;
    }

    private String replacePlaceholder(String conditionString) {

        for (String inputName : componentContext.getInputsWithDatum()) {
            TypedDatum datum = componentContext.readInput(inputName);
            if (datum.getDataType().equals(DataType.Float) || datum.getDataType().equals(DataType.Integer)) {
                conditionString = conditionString.replace(inputName, Pattern.quote(datum.toString())
                    .replaceAll(QUOTE, "")).replaceAll(ENDQUOTE, "");
            }
            if (datum.getDataType().equals(DataType.Boolean)) {
                conditionString =
                    conditionString.replace(inputName, WordUtils.capitalize(Pattern.quote(datum.toString())
                        .replaceAll(QUOTE, ""))).replaceAll(ENDQUOTE, "");
            }
        }
        return conditionString;
    }

    private Boolean writeOutputs(String conditionNumber) {
        boolean val = false;
        for (String input : componentContext.getInputsWithDatum()) {

            if (componentContext.getDynamicInputIdentifier(input).equals(SwitchComponentConstants.CONDITION_INPUT_ID)) {
                continue;
            }

            TypedDatum datum = componentContext.readInput(input);
            componentContext.writeOutput(input + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + conditionNumber, datum);
            String outputName = input + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + conditionNumber;
            componentContext.getLog().componentInfo(StringUtils.format("Wrote to '%s': %s", outputName, datum));

            val = true;
        }
        return val;
    }

    private void writeOutputsNoMatch() {
        for (String input : componentContext.getInputsWithDatum()) {

            if (componentContext.getDynamicInputIdentifier(input).equals(SwitchComponentConstants.CONDITION_INPUT_ID)) {
                continue;
            }

            TypedDatum datum = componentContext.readInput(input);
            componentContext.writeOutput(input + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH, datum);
            String outputName = input + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH;
            componentContext.getLog().componentInfo(StringUtils.format("Wrote to '%s': %s", outputName, datum));
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
