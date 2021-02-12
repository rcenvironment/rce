/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Common constants to connect the properties from component and view.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class CpacsWriterComponentConstants {

    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "cpacswriter";

    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.dlr.sc.chameleon.rce.cpacsdestination.component.Destination_CPACS Saving" };

    /** Constant. */
    public static final String OUTPUT_NAME_CPACS = "CPACS";
    
    /** Constant. */
    public static final String INPUT_NAME_CPACS = "CPACS";
    
    /** Constant. */
    public static final String CPACS_FILENAME = "cpacs.xml";

    // configuration properties
    /** Constant. */
    public static final String SAVE_MODE = "saveMode";

    /** Constant. */
    public static final String LOCAL_STORE_FOLDER = "localFolder";

    // notification ids
    /** Constant. */
    public static final String RUNTIME_CPACS_UUIDS = ":rce.component.cpacswriter";

    private CpacsWriterComponentConstants() {}
}
