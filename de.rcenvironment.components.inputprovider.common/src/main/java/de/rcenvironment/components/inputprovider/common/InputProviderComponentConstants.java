/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.inputprovider.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * InputProviderComponentConstants class.
 * 
 * @author Sascha Zur
 * @author Mark Geiger
 */
public final class InputProviderComponentConstants {

    /** Constant. */
    public static final String META_VALUE = "startValue";

    /** Constant. */
    public static final String META_FILESOURCETYPE = "fileSourceType";
    
    /** Constant value for the key META_FILESOURCETYPE. */
    public static final String META_FILESOURCETYPE_ATWORKFLOWSTART = "atWorkflowStart";
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "inputprovider";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.inputprovider.execution.InputProviderComponent_Input Provider" };

    /** Constant. */
    public static final String PLACEHOLDER_FOMRAT = "%s (%s)";

    private InputProviderComponentConstants() {

    }

}
