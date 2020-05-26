/*
 * Copyright 2006-2020 DLR, Germany
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
    public static final String CONDITION_INPUT_ID = "conditionToInput";

    /**
     * Constants.
     */
    public static final String DATA_INPUT_NAME = "To_forward";

    /**
     * Constants.
     */
    public static final String[] OPERATORS = { "<", ">", "==", "<=", ">=", "not", "or", "and", "True", "False" };

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
    public static final String CONDITION_KEY = "conditionKey";

    /**
     * Constants.
     */
    public static final String NEVER_CLOSE_OUTPUTS_KEY = "neverCloseOutputs";

    /**
     * Constants.
     */
    public static final String CLOSE_OUTPUTS_ON_TRUE_KEY = "closeOutputsOnTrue";

    /**
     * Constants.
     */
    public static final String CLOSE_OUTPUTS_ON_FALSE_KEY = "closeOutputsOnFalse";

    /**
     * Constants.
     */
    public static final String SCRIPT_LANGUAGE = "Jython";

    /**
     * Constants.
     */
    public static final String FALSE_OUTPUT = "False";

    /**
     * Constants.
     */
    public static final String TRUE_OUTPUT = "True";

    private SwitchComponentConstants() {}

}
