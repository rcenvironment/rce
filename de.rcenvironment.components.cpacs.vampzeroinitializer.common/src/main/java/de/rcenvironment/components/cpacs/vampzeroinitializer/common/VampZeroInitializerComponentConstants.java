/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.vampzeroinitializer.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Common constants to connect the properties from component and view.
 * 
 * @author Arne Bachmann
 * @author Markus Kunde
 */
public final class VampZeroInitializerComponentConstants {
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "vampzeroinitializer";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.dlr.sc.chameleon.rce.vampzero.component.VampZero_CPACS_VAMPzero GUI" };
    
    /** Constant. */
    public static final String COMPONENTNAME = "VAMPzero GUI";

    /** Constant. */
    public static final String XMLCONTENT = "xmlContent";
    
    /** Constant. */
    public static final String OUTPUT_NAME_CPACS = "CPACS";
    
    /** Constant. */
    public static final String CPACS_FILENAME = "cpacs.xml";
    
    private VampZeroInitializerComponentConstants() {}
}
