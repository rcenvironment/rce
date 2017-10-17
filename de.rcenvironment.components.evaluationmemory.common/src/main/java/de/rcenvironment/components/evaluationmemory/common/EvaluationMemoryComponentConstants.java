/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.common;

import de.rcenvironment.core.component.api.ComponentConstants;


/**
 * Constants used by the memory component.
 * 
 * @author Doreen Seider
 */
public final class EvaluationMemoryComponentConstants {

    /** Constants. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "evaluationmemory";
    
    /** Constants. */
    public static final String CONFIG_SELECT_AT_WF_START = "select_at_wf_start";
    
    /** Constants. */
    public static final String CONFIG_MEMORY_FILE = "mem_file";
    
    /** Constants. */
    public static final String CONFIG_CONSIDER_LOOP_FAILURES = "store_failures";
    
    /** Constants. */
    public static final String CONFIG_MEMORY_FILE_WF_START = "mem_file_wf_start";
    
    /** Constants. */
    public static final String PLACEHOLDER_MEMORY_FILE_PATH = "${memory_file_path}";
    
    /** Constants. */
    public static final String ENDPOINT_ID_TO_EVALUATE = "to_evaluate";
    
    /** Constants. */
    public static final String ENDPOINT_ID_EVALUATION_RESULTS = "evaluation_results";
    
    private EvaluationMemoryComponentConstants() {}

}
