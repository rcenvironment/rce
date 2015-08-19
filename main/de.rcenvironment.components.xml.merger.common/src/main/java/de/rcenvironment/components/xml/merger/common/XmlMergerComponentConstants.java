/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.common;

import de.rcenvironment.core.component.api.ComponentConstants;


/**
 * Common constants to connect the properties from component and view.
 * 
 * @author Miriam Lenk
 * @author Markus Kunde
 * @author Arne Bachmann
 */
public final class XmlMergerComponentConstants {
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "xmlmerger";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.dlr.sc.chameleon.rce.cpacsjoiner.component.Joiner_CPACS Joiner" };
    
    /** Constant. */
    public static final String XMLCONTENT_CONFIGNAME = "xmlContent";
    
    /** Constant. */
    public static final String MAPPINGTYPE_CONFIGNAME = "mappingType";
    
    /** Constant. */
    public static final String MAPPINGTYPE_XSLT = "XSLT";
    
    /** Constant. */
    public static final String MAPPINGTYPE_CLASSIC = "Classic";
    
    /** Constant. */
    public static final String INPUT_NAME_XML_TO_INTEGRATE = "XML to integrate";
    
    /** Constant. */
    public static final String INTEGRATING_INPUT_PLACEHOLDER = "INTEGRATING_INPUT";
    
    /** Constant. */
    public static final String XMLFILEEND = "xml";
    
    /** Constant. */
    public static final String INPUT_NAME_XML = "XML";
    
    private XmlMergerComponentConstants() {}
 
}
