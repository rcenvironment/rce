/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
    public static final String ENDPOINT_NAME_XML = "XML";
    
    private XmlLoaderComponentConstants(){}
}
