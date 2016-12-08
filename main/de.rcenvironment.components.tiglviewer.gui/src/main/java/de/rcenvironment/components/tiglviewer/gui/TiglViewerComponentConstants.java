/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.tiglviewer.gui;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants.
 * 
 * @author Doreen Seider
 */
public final class TiglViewerComponentConstants {

    /** Identifier of the parametric study component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "tiglviewer";
    
    /** Identifiers of the parametric study component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID,
        "de.rcenvironment.components.tiglviewer.execution.TiglViewerComponent_TiGL Viewer" };
    
    /** Name of the static input and output. */
    public static final String ENDPOINT_NAME = "TiGL Viewer File";

    private TiglViewerComponentConstants() {}

}
