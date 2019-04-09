/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.dakota.execution.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Constants class for dakota integration.
 * 
 * @author Sascha Zur
 */
public final class DakotaConstants {

    protected static final String RESOURCES_DAKOTA_SURROGATE_SAMPLE = "/resources/dakota_surrogate.sample";

    protected static final String RESOURCES_DAKOTA_STANDARD_SAMPLE = "/resources/dakota_standard.sample";

    protected static final String PLACEHOLDER_GRADIENT_2_SECTION = "%%GRADIENT_2_SECTION%%";

    protected static final String PLACEHOLDER_GRADIENT_SECTION = "%%GRADIENT_SECTION%%";

    protected static final String PLACEHOLDER_CONSTRAINT_UPPER = "%%CONSTRAINT_UPPER%%";

    protected static final String PLACEHOLDER_CONSTRAINT_LOWER = "%%CONSTRAINT_LOWER%%";

    protected static final String PLACEHOLDER_CONSTRAINT_COUNT = "%%CONSTRAINT_COUNT%%";

    protected static final String PLACEHOLDER_OBJECTIVES_WEIGHT = "%%OBJECTIVES_WEIGHT%%";

    protected static final String PLACEHOLDER_OBJECTIVE_FUNCTIONS_COUNT = "%%OBJECTIVE_FUNCTIONS_COUNT%%";

    protected static final String PLACEHOLDER_WORKDIR = "%%WORKDIR%%";

    protected static final String PLACEHOLDER_DRIVER_FOR_OS = "%%DRIVER_FOR_OS%%";

    protected static final String PLACEHOLDER_CDV_NAMES = "%%CDV_NAMES%%";

    protected static final String PLACEHOLDER_CDV_UPPER_BOUNDS = "%%CDV_UPPER_BOUNDS%%";

    protected static final String PLACEHOLDER_CDV_LOWER_BOUNDS = "%%CDV_LOWER_BOUNDS%%";

    protected static final String PLACEHOLDER_CDV_INITIAL_POINT = "%%CDV_INITIAL_POINT%%";

    protected static final String PLACEHOLDER_CONTINUOUS_DESIGN_COUNT = "%%CONTINUOUS_DESIGN_COUNT%%";

    protected static final String PLACEHOLDER_DDV_NAMES = "%%DDV_NAMES%%";

    protected static final String PLACEHOLDER_DDV_UPPER_BOUNDS = "%%DDV_UPPER_BOUNDS%%";

    protected static final String PLACEHOLDER_DDV_LOWER_BOUNDS = "%%DDV_LOWER_BOUNDS%%";

    protected static final String PLACEHOLDER_DDV_INITIAL_POINT = "%%DDV_INITIAL_POINT%%";

    protected static final String PLACEHOLDER_DISCRETE_DESIGN_COUNT = "%%DISCRETE_DESIGN_COUNT%%";

    protected static final String PLACEHOLDER_METHOD_3_PROPERTIES = "%%METHOD_3_PROPERTIES%%";

    protected static final String PLACEHOLDER_METHOD_3_CODE = "%%METHOD_3_CODE%%";

    protected static final String PLACEHOLDER_METHOD_2_PROPERTIES = "%%METHOD_2_PROPERTIES%%";

    protected static final String PLACEHOLDER_METHOD_2_CODE = "%%METHOD_2_CODE%%";

    protected static final String PLACEHOLDER_METHOD_PROPERTIES = "%%METHOD_PROPERTIES%%";

    protected static final String PLACEHOLDER_METHOD_CODE = "%%METHOD_CODE%%";

    protected static final int MINUS_ONE = -1;

    protected static final String WHITESPACES = "       ";

    protected static final Log LOGGER = LogFactory.getLog(DakotaAlgorithm.class);

    protected static final String DOT = ".";

    protected static final String INTERVAL_TYPE_KEY = "interval_type";

    protected static final String FD_HESSIAN_STEP_SIZE_KEY = "fd_hessian_step_size";

    protected static final String INTERVAL_TYPE_HESSIAN_KEY = "interval_type_hessian";

    protected static final String FD_GRADIENT_STEP_SIZE_KEY = "fd_gradient_step_size";

    protected static final String STRING = "String";

    protected static final String BOOL = "BOOL";

    protected static final String COMMA = ",";

    protected static final String NORMAL = "normal";

    protected static final String OUTPUT = "output";

    protected static final String HESSIANS_KEY = "hessians";

    protected static final String GRADIENTS_KEY = "gradients";

    protected static final String BACKSLASH = "\"";

    protected static final String TRUE = "true";

    protected static final String TABS = "\t";

    protected static final String NEWLINE = System.getProperty("line.separator");

    protected static final String RESOURCES_DAKOTA_GRADIENTS_BASE_SAMPLE = "/resources/dakota_gradients_base.sample";

    protected static final String RESOURCES_DAKOTA_HESSIANS_SAMPLE = "/resources/dakota_hessians.sample";

    protected static final String RESOURCES_DAKOTA_GRADIENTS_SAMPLE = "/resources/dakota_gradients.sample";

    protected static final String NO_GRADIENTS = "no_gradients";

    protected static final String NO_HESSIANS = "no_hessians";

    protected static final String PARAMETER_HESSIANS = "%%HESSIANS%%";

    protected static final String PARAMETER_GRADIENTS = "%%GRADIENTS%%";

    protected static final String HESSIAN_STEP_SIZE = "%%HESSIAN_STEP_SIZE%%";

    protected static final String HESSIAN_INTERVALL = "%%HESSIAN_INTERVALL%%";

    protected static final String HESSIANS_VALUE = "%%HESSIANS_VALUE%%";

    protected static final String GRADIENT_STEP_SIZE = "%%GRADIENT_STEP_SIZE%%";

    protected static final String GRADIANT_INTERVAL_TYPE = "%%INTERVAL_TYPE%%";

    protected static final String NUMERICAL_GRADIENTS = "numerical_gradients";

    protected static final String DAKOTA_SURROGATE_BASED_LOCAL_STRING = "Dakota Surrogate-Based Local";

    protected static final String FINISH_STRING_FROM_DAKOTA = "<<<<< Best data captured at function evaluation ";

    protected static final String BEST_PARAMETERS_STRING_FROM_DAKOTA = "<<<<< Best parameters";

    protected static final String BEST_OBJECTIVE_STRING_FROM_DAKOTA = "<<<<< Best objective function";

    private DakotaConstants() {};

}
