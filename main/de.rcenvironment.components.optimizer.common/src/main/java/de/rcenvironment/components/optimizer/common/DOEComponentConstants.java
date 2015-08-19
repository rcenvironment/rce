/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
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
public final class DOEComponentConstants {

    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "doe";
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.rce.components.optimizer.OptimizerComponent_Design of Experiments" };

    private DOEComponentConstants() {}

}
