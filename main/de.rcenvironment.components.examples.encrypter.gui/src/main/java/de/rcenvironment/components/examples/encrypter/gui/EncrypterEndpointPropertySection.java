/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.examples.encrypter.gui;

import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * "Properties" view tab for configuring cells as additional endpoints (i.e. inputs and outputs).
 * For the Encoder component, the {@link DefaultEndpointPropertySection} is extended because it uses
 * its own version of {@link EndpointSelectionPane}.
 * 
 * @author Sascha Zur
 */
public class EncrypterEndpointPropertySection extends DefaultEndpointPropertySection {

    public EncrypterEndpointPropertySection() {
        EndpointSelectionPane inputPane = new EncrypterEndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, false, "default", false);
        EndpointSelectionPane outputPane = new EncrypterEndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, false, "default", false);

        setColumns(2);
        setPanes(inputPane, outputPane);
    }

}
