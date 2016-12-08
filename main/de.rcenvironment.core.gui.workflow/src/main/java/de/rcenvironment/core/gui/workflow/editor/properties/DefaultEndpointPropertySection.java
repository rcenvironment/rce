/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * An extended "Properties" view tab for configuring endpoints (ie inputs and outputs) and using
 * initial Variables.
 * 
 * @author Sascha Zur
 */
public class DefaultEndpointPropertySection extends EndpointPropertySection {

    public DefaultEndpointPropertySection() {
        super();
        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs, EndpointType.INPUT, "default",
            new String[] {}, new String[] {}, this);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs, EndpointType.OUTPUT, "default",
            new String[] {}, new String[] {}, this);
        setColumns(2);
        setPanes(inputPane, outputPane);
    }
}
