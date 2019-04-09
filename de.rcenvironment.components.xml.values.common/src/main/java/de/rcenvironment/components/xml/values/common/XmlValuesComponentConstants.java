/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.xml.values.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Common constants to connect the properties from component and view.
 * 
 * @author Adrian Stock
 * @author Jan Flink
 */
public final class XmlValuesComponentConstants {

    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX
        + "xmlvalues";

    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID };

    /** Constant. */
    public static final String ENDPOINT_NAME_XML = "XML";

    private XmlValuesComponentConstants() {}

}
