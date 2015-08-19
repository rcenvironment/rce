/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.cluster.gui.properties;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Messages;

/**
 * Showing pre-defined inputs and outputs.
 * 
 * @author Doreen Seider
 */
public class ClusterComponentEndpointSection extends EndpointPropertySection {

    public ClusterComponentEndpointSection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, false, null, false);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, true, null, false);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }

}
