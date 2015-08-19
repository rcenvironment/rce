/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
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

    static final String RESOURCES_DAKOTA_SURROGATE_SAMPLE = "/resources/dakota_surrogate.sample";

    static final String RESOURCES_DAKOTA_STANDARD_SAMPLE = "/resources/dakota_standard.sample";

    static final String PLACEHOLDER_GRADIENT_2_SECTION = "%%GRADIENT_2_SECTION%%";

    static final String PLACEHOLDER_GRADIENT_SECTION = "%%GRADIENT_SECTION%%";

    static final String PLACEHOLDER_CONSTRAINT_UPPER = "%%CONSTRAINT_UPPER%%";

    static final String PLACEHOLDER_CONSTRAINT_LOWER = "%%CONSTRAINT_LOWER%%";

    static final String PLACEHOLDER_CONSTRAINT_COUNT = "%%CONSTRAINT_COUNT%%";

    static final String PLACEHOLDER_OBJECTIVES_WEIGHT = "%%OBJECTIVES_WEIGHT%%";

    static final String PLACEHOLDER_OBJECTIVE_FUNCTIONS_COUNT = "%%OBJECTIVE_FUNCTIONS_COUNT%%";

    static final String PLACEHOLDER_WORKDIR = "%%WORKDIR%%";

    static final String PLACEHOLDER_DRIVER_FOR_OS = "%%DRIVER_FOR_OS%%";

    static final String PLACEHOLDER_CDV_NAMES = "%%CDV_NAMES%%";

    static final String PLACEHOLDER_CDV_UPPER_BOUNDS = "%%CDV_UPPER_BOUNDS%%";

    static final String PLACEHOLDER_CDV_LOWER_BOUNDS = "%%CDV_LOWER_BOUNDS%%";

    static final String PLACEHOLDER_CDV_INITIAL_POINT = "%%CDV_INITIAL_POINT%%";

    static final String PLACEHOLDER_CONTINUOUS_DESIGN_COUNT = "%%CONTINUOUS_DESIGN_COUNT%%";

    static final String PLACEHOLDER_METHOD_3_PROPERTIES = "%%METHOD_3_PROPERTIES%%";

    static final String PLACEHOLDER_METHOD_3_CODE = "%%METHOD_3_CODE%%";

    static final String PLACEHOLDER_METHOD_2_PROPERTIES = "%%METHOD_2_PROPERTIES%%";

    static final String PLACEHOLDER_METHOD_2_CODE = "%%METHOD_2_CODE%%";

    static final String PLACEHOLDER_METHOD_PROPERTIES = "%%METHOD_PROPERTIES%%";

    static final String PLACEHOLDER_METHOD_CODE = "%%METHOD_CODE%%";

    static final int MINUS_ONE = -1;

    static final String WHITESPACES = "       ";

    static final Log LOGGER = LogFactory.getLog(DakotaAlgorithm.class);

    static final String DOT = ".";

    static final String INTERVAL_TYPE_KEY = "interval_type";

    static final String FD_HESSIAN_STEP_SIZE_KEY = "fd_hessian_step_size";

    static final String INTERVAL_TYPE_HESSIAN_KEY = "interval_type_hessian";

    static final String FD_GRADIENT_STEP_SIZE_KEY = "fd_gradient_step_size";

    static final String STRING = "String";

    static final String BOOL = "BOOL";

    static final String COMMA = ",";

    static final String NORMAL = "normal";

    static final String OUTPUT = "output";

    static final String HESSIANS_KEY = "hessians";

    static final String GRADIENTS_KEY = "gradients";

    static final String BACKSLASH = "\"";

    static final String TRUE = "true";

    static final String TABS = "\t";

    static final String NEWLINE = System.getProperty("line.separator");

    static final String RESOURCES_DAKOTA_GRADIENTS_BASE_SAMPLE = "/resources/dakota_gradients_base.sample";

    static final String RESOURCES_DAKOTA_HESSIANS_SAMPLE = "/resources/dakota_hessians.sample";

    static final String RESOURCES_DAKOTA_GRADIENTS_SAMPLE = "/resources/dakota_gradients.sample";

    static final String NO_GRADIENTS = "no_gradients";

    static final String NO_HESSIANS = "no_hessians";

    static final String PARAMETER_HESSIANS = "%%HESSIANS%%";

    static final String PARAMETER_GRADIENTS = "%%GRADIENTS%%";

    static final String HESSIAN_STEP_SIZE = "%%HESSIAN_STEP_SIZE%%";

    static final String HESSIAN_INTERVALL = "%%HESSIAN_INTERVALL%%";

    static final String HESSIANS_VALUE = "%%HESSIANS_VALUE%%";

    static final String GRADIENT_STEP_SIZE = "%%GRADIENT_STEP_SIZE%%";

    static final String GRADIANT_INTERVAL_TYPE = "%%INTERVAL_TYPE%%";

    static final String NUMERICAL_GRADIENTS = "numerical_gradients";

    static final String DAKOTA_SURROGATE_BASED_LOCAL_STRING = "Dakota Surrogate-Based Local";

    static final String FINISH_STRING_FROM_DAKOTA = "<<<<< Best data captured at function evaluation";

    static final String BEST_PARAMETERS_STRING_FROM_DAKOTA = "<<<<< Best parameters";

    static final String BEST_OBJECTIVE_STRING_FROM_DAKOTA = "<<<<< Best objective function";

    private DakotaConstants() {};

}
