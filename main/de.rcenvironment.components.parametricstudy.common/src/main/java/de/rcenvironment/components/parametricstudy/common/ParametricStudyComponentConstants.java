/*
 * Copyright (C) 2006-2015 DLR, Germany
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

    /** Suffix used for configuration value name. */
    public static final String OUTPUT_METATDATA_FROMVALUE = "FromValue";

    /** Suffix used for configuration value name. */
    public static final String OUTPUT_METATDATA_TOVALUE = "ToValue";

    /** Suffix used for configuration value name. */
    public static final String OUTPUT_METATDATA_STEPSIZE = "StepSize";

    /** Dynamic inputs id. */
    public static final String DYNAMIC_INPUT_IDENTIFIER = "parameters";

    /** Output name. */
    public static final String OUTPUT_NAME = "Design Variable";

    /** Constant. */
    public static final String OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS = "fitStepSizeToBounds";

    private ParametricStudyComponentConstants() {}

}
