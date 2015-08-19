/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.joiner.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants for Merger component.
 *
 * @author Sascha Zur
 */
public final class JoinerComponentConstants {

    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "joiner";
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.merger.execution.MergerComponent_Merger" };
    /**Constant. */
    public static final String DATATYPE = "dataType"; 
    /**Constant. */
    public static final String INPUT_COUNT = "inputCount"; 
    /**Constant. */
    public static final String OUTPUT_NAME = "Joined";
    /**Constant. */
    public static final String KEY_JOINED = "joined";
    /**Constant. */
    public static final String DYNAMIC_OUTPUT_ID = "joined";
    /**Constant. */
    public static final String DYNAMIC_INPUT_ID = "toJoin";
    /**Constant. */
    public static final String INPUT_NAME = "Input ";
    
    private JoinerComponentConstants() {}

}
