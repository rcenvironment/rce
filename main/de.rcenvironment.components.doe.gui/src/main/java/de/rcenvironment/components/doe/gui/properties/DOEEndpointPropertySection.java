/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.gui.properties;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * An extended "Properties" view tab for configuring endpoints (ie inputs and outputs) and using
 * initial Variables.
 * 
 * @author Sascha Zur
 */
public class DOEEndpointPropertySection extends EndpointPropertySection {

    public DOEEndpointPropertySection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane("Inputs",
            EndpointType.INPUT, this, false, "default", false);
        DOEEndpointSelectionPane outputPane = new DOEEndpointSelectionPane("Outputs",
            EndpointType.OUTPUT, this, false, "default", false);

        setColumns(2);
        setPanes(inputPane, outputPane);
    }
}
