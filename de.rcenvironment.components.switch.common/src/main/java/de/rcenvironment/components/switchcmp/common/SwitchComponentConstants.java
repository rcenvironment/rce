/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.common;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Switch constants.
 * 
 * @author David Scholz
 * @author Kathrin Schaffert
 */
public final class SwitchComponentConstants {

    /**
     * Constants.
     */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "switchcmp";

    /**
     * Constants.
     */
    public static final String[] COMPONENT_IDS = { COMPONENT_ID,
        "de.rcenvironment.components.switchcmp.execution.SwitchComponent_Switch" };

    /**
     * Constants.
     */
    public static final String CONDITION_KEY_PROPERTY_ID = "properties.conditionKey";

    /**
     * Constants.
     */
    public static final String CONDITION_INPUT_ID = "conditionToInput";

    /**
     * Constants.
     */
    public static final String DATA_INPUT_ID = "dataToInput";

    /**
     * Constants.
     */
    public static final String DATA_OUTPUT_ID = "dataToOutput";

    /** Constant. */
    public static final String OUTPUT_VARIABLE_SUFFIX_CONDITION = "_condition";

    /** Constant. */
    public static final String OUTPUT_VARIABLE_SUFFIX_NO_MATCH = "_no match";

    /**
     * Constants.
     */
    public static final String[] OPERATORS = { "<", ">", "==", "<=", ">=", "not", "or", "and", "True", "False" };

    /**
     * Constants.
     */
    public static final String[] PYTHON_KEYWORDS =
        { "True", "False", "None", "and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try",
            "while", "with", "yield" };

    /**
     * Constants.
     */
    public static final String[] OPERATORS_FOR_VALIDATION = { "\b+\b", "\b-\b", "\b*\b", "\b/\b", "\b%\b", "\\s+", "\b(\b", "\b)\b" };

    /**
     * Constants.
     */
    public static final DataType[] CONDITION_INPUT_DATA_TYPES = { DataType.Float, DataType.Integer, DataType.Boolean };

    /**
     * Constants.
     */
    public static final DataType[] CONDITION_SCRIPT_DATA_TYPES = { DataType.Float, DataType.Integer, DataType.Boolean };

    /**
     * Constants.
     */
    public static final String CONDITION_KEY = "conditionKey";


    /**
     * Constants.
     */
    public static final String WRITE_OUTPUT_KEY = "writeOutputKey";

    /**
     * Constants.
     */
    public static final String NEVER_CLOSE_OUTPUTS_KEY = "neverCloseOutputs";

    /**
     * Constants.
     */
    public static final String CLOSE_OUTPUTS_ON_CONDITION_NUMBER_KEY = "closeOutputsOnConditionNumber";

    /**
     * Constants.
     */
    public static final String CLOSE_OUTPUTS_ON_NO_MATCH_KEY = "closeOutputsOnNoMatch";
    
    /**
     * Constants.
     */
    public static final String SELECTED_CONDITION = "selectedCondition";

    /**
     * Constants.
     */
    public static final String SCRIPT_LANGUAGE = "Jython";

    private SwitchComponentConstants() {}

}
