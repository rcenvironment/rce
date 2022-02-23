/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

/**
 * 
 * Utility class for handling conditions in the {@link SwitchConditionSection}.
 * 
 * @author Kathrin Schaffert
 *
 */
public class SwitchCondition {

    private static final String EXCEPTION_MESSAGE_READING = "Unexpected Exception occured, while reading JSON content String.";

    private int conditionNumber;

    private String conditionScript;

    @JsonIgnore
    private String validationMessages;

    public SwitchCondition() {
        super();
    }

    public SwitchCondition(int conditionNumber, String conditionScript, String validationMessages) {
        this.conditionNumber = conditionNumber;
        this.conditionScript = conditionScript;
        this.validationMessages = validationMessages;
    }

    public SwitchCondition(int conditionNumber, String conditionScript) {
        this(conditionNumber, conditionScript, null);
    }

    public int getConditionNumber() {
        return conditionNumber;
    }

    public void setConditionNumber(int conditionNumber) {
        this.conditionNumber = conditionNumber;
    }

    public String getConditionScript() {
        return conditionScript;
    }

    public void setConditionScript(String conditionScript) {
        this.conditionScript = conditionScript;
    }

    public String getValidationMessages() {
        return validationMessages;
    }

    public void setValidationMessages(String validationMessages) {
        this.validationMessages = validationMessages;
    }

    /**
     * 
     * Returns the Switch Condition Table ArrayList from the configuration string.
     * 
     * @param configStr configuration string of the SwitchComponentConstants.CONDITION_KEY Property, must not be null and must describe a
     *        serialized list of SwitchConditions
     * @return switch condition table ArrayList
     */
    public static List<SwitchCondition> getSwitchConditionList(String configStr) {
        ObjectMapper mapper = new ObjectMapper();
        CollectionType mapCollectionType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, SwitchCondition.class);
        ArrayList<SwitchCondition> switchConditionArray = new ArrayList<>();
        try {
            switchConditionArray = mapper.readValue(configStr, mapCollectionType);
        } catch (IOException e) {
            throw new RuntimeException(EXCEPTION_MESSAGE_READING, e); // should never happen
        }

        return switchConditionArray;
    }
}
