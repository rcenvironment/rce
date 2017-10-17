/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.tiglviewer.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * "Properties" view tab for endpoints.
 * 
 * @author Doreen Seider
 */
public class TiglViewerEndpointSection extends DefaultEndpointPropertySection {
    
    public TiglViewerEndpointSection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, null,
            new String[] {}, new String[] { TiglViewerComponentConstants.ENDPOINT_NAME }, this);
        
        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, null,
            new String[] {}, new String[] { TiglViewerComponentConstants.ENDPOINT_NAME }, this, true);
        
        setColumns(2);
        setPanes(inputPane, outputPane);
        
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
    
}

