/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.converger.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants.
 * 
 * @author Sascha Zur
 */
public final class ConvergerComponentConstants {

    /** Component ID. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "converger";
    
    /** Constant. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.converger.execution.ConvergerComponent_Converger" };

    /** Constant. */
    public static final String META_HAS_STARTVALUE = "hasStartValue";

    /** Constant. */
    public static final String META_STARTVALUE = "startValue";

    /** Constant. */
    public static final String KEY_EPS_R = "epsR";

    /** Constant. */
    public static final String KEY_EPS_A = "epsA";

    /** Constant. */
    public static final String KEY_MAX_CONV_CHECKS = "maxConvChecks";
    
    /** Constant. */
    public static final String KEY_ITERATIONS_TO_CONSIDER = "iterationsToConsider";
    
    /** Constant. */
    public static final String DEFAULT_VALUE_ITERATIONS_TO_CONSIDER = "1";

    /** Constant. */
    public static final String CONVERGED_OUTPUT_SUFFIX = "_converged";

    /** Constant. */
    public static final String IS_CONVERGED_OUTPUT_SUFFIX = "_is_converged";

    /** Constant. */
    public static final String CONVERGED = "Converged";

    /** Constant. */
    public static final String CONVERGED_ABSOLUTE = "Converged absolute";

    /** Constant. */
    public static final String CONVERGED_RELATIVE = "Converged relative";

    /** Constant. */
    public static final String ENDPOINT_ID_START_TO_CONVERGE = "startToConverge";
    
    /** Constant. */
    public static final String ENDPOINT_ID_TO_CONVERGE = "valueToConverge";
    
    /** Constant. */
    public static final String ENDPOINT_ID_FINAL_TO_CONVERGE = "finalToConverge";

    /** Constant. */
    public static final String ENDPOINT_ID_AUXILIARY = "auxiliaryValue";

    /** Constant. */
    public static final String NOT_CONVERGED_IGNORE = "notConvIgnore";

    /** Constant. */
    public static final String NOT_CONVERGED_FAIL = "notConvFail";

    /** Constant. */
    public static final String NOT_CONVERGED_NOT_A_VALUE = "notConvNotAValue";

    private ConvergerComponentConstants() {

    }
}
