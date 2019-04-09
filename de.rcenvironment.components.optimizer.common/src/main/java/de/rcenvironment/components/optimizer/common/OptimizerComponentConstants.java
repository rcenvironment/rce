/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants shared by GUI and component.
 * 
 * @author Sascha Zur
 */
public final class OptimizerComponentConstants {

    /** Identifier of the Optimizer component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "optimizer";

    /** Identifiers of the Optimizer component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.rce.components.optimizer.OptimizerComponent_Optimizer" };

    /** Suffix used for publishing Optimizer notifications. */
    public static final String NOTIFICATION_SUFFIX = ":rce.component.optimizer";

    /** Configuration key denoting which algorithm should be used. */
    public static final String ALGORITHMS = "algorithm";

    /** Configuration key denoting which algorithm should be used. */
    public static final String METHODCONFIGURATIONS = "methodConfigurations";

    /** Configuration key denoting which algorithm should be used. */
    public static final String DAKOTAPATH = "dakotapath";

    /** Property key variable lower bound. */
    public static final String META_PACK = "pack";

    /** Property key for startvalue. */
    public static final String META_STARTVALUE = "startValue";

    /** Property key for weight. */
    public static final String META_WEIGHT = "weight";

    /** Property key for goal. */
    public static final String META_GOAL = "goal";

    /** Constant. */
    public static final String META_LOWERBOUND = "lower";

    /** Constant. */
    public static final String META_UPPERBOUND = "upper";

    /** Constant. */
    public static final String META_SOLVEFOR = "solve";

    /** Constant. */
    public static final String WIDGET_CHECK = "Check";

    /** Constant. */
    public static final String WIDGET_COMBO = "Combo";

    /** Constant. */
    public static final String GUI_ORDER_KEY = "GuiOrder";

    /** Constant. */
    public static final String SEPARATOR = ",";

    /** Constant. */
    public static final String TYPE_REAL = "REAL";

    /** Constant. */
    public static final String TYPE_INT = "INT";

    /** Constant. */
    public static final String TYPE_STRING = "STRING";

    /** Constant. */
    public static final String WIDGET_TEXT = "Text";

    /** Constant. */
    public static final String VALIDATION_KEY = "Validation";

    /** Constant. */
    public static final String DATA_TYPE_KEY = "dataType";

    /** Constant. */
    public static final String SWTWIDGET_KEY = "SWTWidget";

    /** Constant. */
    public static final String GUINAME_KEY = "GuiName";

    /** Constant. */
    public static final String VALUE_KEY = "Value";

    /** Constant. */
    public static final String DEFAULT_VALUE_KEY = "DefaultValue";

    /** Constant. */
    public static final String CHOICES_KEY = "Choices";

    /** Constant. */
    public static final String NO_LINEBREAK_KEY = "NoLinebreak";

    /** Constant. */
    public static final String NOKEYWORD_KEY = "NoKeyword";

    /** Constant. */
    public static final String DONT_SHOW_KEY = "doNotShow";

    /** Constant. */
    public static final String DACE_LIST_KEY = "dace_list";

    /** Constant. */
    public static final String DAKOTA_LHS = "Dakota Latin Hypercube Sampling";

    /** Constant. */
    public static final String APPROX_METHOD_KEY = "approx_method_list";

    /** Constant. */
    public static final String DONT_WRITE_KEY = "doNotWrite";

    /** Constant. */
    public static final String HAS_GRADIENT = "hasGradient";

    /** Constant how the gradient channels end. */
    public static final String GRADIENT_DELTA = "\u2202";

    /** Constant that channels for startvalues contain. */
    public static final String STARTVALUE_SIGNATURE = " - start value";

    /** Constant. */
    public static final String OPTIMIZER_PACKAGE = "optimizerPackageCode";

    /** Constant. */
    public static final String GENERIC_GUI_CONFIG = "gui_properties_configuration";

    /** Constant. */
    public static final String GENERIC_SOURCE = "source";

    /** Constant. */
    public static final String GENERIC_FLAG = "method";

    /** Constant. */
    public static final String ID_OBJECTIVE = "Objective";

    /** Constant. */
    public static final String ID_CONSTRAINT = "Constraint";

    /** Constant. */
    public static final String ID_DESIGN = "Design";

    /** Constant. */
    public static final String DAKOTA_SGB = "Dakota Surrogate-Based Local";

    /** Constant. */
    public static final String OPTIMUM_VARIABLE_SUFFIX = "_optimal";

    /** Constant. */
    public static final String ID_STARTVALUES = "startvalues";

    /** Constant. */
    public static final String ID_OPTIMA = "optima";

    /** Constant. */
    public static final String META_HAS_STARTVALUE = "hasStartValue";

    /** Constant. */
    public static final String OPTIMIZER_VECTOR_INDEX_SYMBOL = "_";

    /** Constant. */
    public static final String METADATA_VECTOR_SIZE = "vectorSize";

    /** Constant. */
    public static final String ITERATION_COUNT_ENDPOINT_NAME = "Iteration";

    /** Constant. */
    public static final String META_KEY_HAS_BOUNDS = "hasSingleBounds";

    /** Constant. */
    public static final String BOUNDS_STARTVALUE_LOWER_SIGNITURE = " - lower bounds";

    /** Constant. */
    public static final String BOUNDS_STARTVALUE_UPPER_SIGNITURE = " - upper bounds";

    /** Constant. */
    public static final String ID_GRADIENTS = "gradients";

    /** Constant. */
    public static final String DERIVATIVES_NEEDED = "Gradient request";

    /** Constant. */
    public static final String USE_CUSTOM_DAKOTA_PATH = "CustomDakotaPath";

    /** Constant. */
    public static final String CUSTOM_DAKOTA_PATH = "dakotaExecPath";

    /** Constant. */
    public static final String USE_RESTART_FILE = "usePrecalculation";

    /** Constant. */
    public static final String RESTART_FILE_PATH = "preCalcFilePath";

    /** Constant. */
    public static final String GENERIC_EVALUATION_FILE = "RCEOptimization.py";

    /** Constant. */
    public static final String GENERIC_ALGORITHMS_FILE = "algorithms.json";

    /** Constant. */
    public static final String GENERIC_MAIN_FILE = "generic_optimizer.py";

    /** Constant. */
    public static final String META_IS_DISCRETE = "isDiscrete";

    /** Constant. */
    public static final String META_STEP = "step";

    /** Constant. */
    public static final String META_USE_STEP = "useStep";

    /** Constant. */
    public static final String META_USE_UNIFIED_STEP = "useUnifiedStep";

    /** Constant. */
    public static final String STEP_VALUE_SIGNATURE = " - step value";

    /**
     * Hide the constructor.
     */
    private OptimizerComponentConstants() {}

}
