/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.xml.loader.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Common constants to connect the properties from component and view.
 * 
 * @author Markus Kunde
 */
public final class XmlLoaderComponentConstants  {
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "xmlloader";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.dlr.sc.chameleon.rce.cpacssource.component.Source_CPACS Loading" };
    
    /** Constant. */
    public static final String XMLCONTENT = "xmlContent";
    
    /** Constant. */
    public static final String OUTPUT_NAME_XML = "XML";
    
    private XmlLoaderComponentConstants(){}
}
