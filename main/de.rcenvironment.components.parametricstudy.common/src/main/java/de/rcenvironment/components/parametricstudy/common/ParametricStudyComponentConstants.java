/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants shared by GUI and Non-GUI implementations.
 * 
 * @author Markus Kunde
 */
public final class ParametricStudyComponentConstants {

    /** Name of the component as it is defined declaratively in OSGi component. */
    public static final String COMPONENT_NAME = "Parametric Study";

    /** Identifier of the parametric study component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "parametricstudy";

    /** Identifiers of the parametric study component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.rce.components.parametricstudy.ParametricStudyComponent_" + COMPONENT_NAME };

    /** Suffix used for publishing Parametric Study notifications. */
    public static final String NOTIFICATION_SUFFIX = ":rce.component.parametricstudy";
    
    /** Configuration value name. */
    public static final String OUTPUT_METADATA_USE_INPUT_AS_FROM_VALUE = "UseInputAsFromValue";
    
    /** Configuration value name. */
    public static final String OUTPUT_METADATA_USE_INPUT_AS_TO_VALUE = "UseInputAsToValue";
    
    /** Configuration value name. */
    public static final String OUTPUT_METADATA_USE_INPUT_AS_STEPSIZE_VALUE = "UseInputAsStepSizeValue";

    /** Configuration value name. */
    public static final String OUTPUT_METATDATA_FROMVALUE = "FromValue";

    /** Configuration value name. */
    public static final String OUTPUT_METATDATA_TOVALUE = "ToValue";

    /** Configuration value name. */
    public static final String OUTPUT_METATDATA_STEPSIZE = "StepSize";
    
    /** Dynamic inputs id. */
    public static final String DYNAMIC_INPUT_STUDY_PARAMETERS = "paramericStudyParameters";

    /** Dynamic inputs id. */
    public static final String DYNAMIC_INPUT_IDENTIFIER = "parameters";

    /** Output name. */
    public static final String OUTPUT_NAME_DV = "Design variable";

    /** Output name. */
    public static final String OUTPUT_NAME_DONE = "Done";
    
    /** Input name. */
    public static final String INPUT_NAME_FROM_VALUE = "From Value";
    
    /** Input name. */
    public static final String INPUT_NAME_TO_VALUE = "To Value";
    
    /** Input name. */
    public static final String INPUT_NAME_STEPSIZE_VALUE = "StepSize Value";
    
    /** Constant. */
    public static final String OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS = "fitStepSizeToBounds";

    private ParametricStudyComponentConstants() {}

}
