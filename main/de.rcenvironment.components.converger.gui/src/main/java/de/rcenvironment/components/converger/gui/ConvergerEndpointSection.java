/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.converger.gui;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * "Properties" view tab for configuring endpoints (i.e. inputs and outputs).
 * 
 * @author Sascha Zur
 */
public class ConvergerEndpointSection extends EndpointPropertySection {

    public ConvergerEndpointSection() {

        EndpointSelectionPane outputPane = new ConvergerEndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, true, null, null);
        EndpointSelectionPane inputPane = new ConvergerEndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, false,
            ConvergerComponentConstants.ID_VALUE_TO_CONVERGE, (ConvergerEndpointSelectionPane) outputPane);
        setColumns(1);
        setPanes(inputPane, outputPane);
    }
}
