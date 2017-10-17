/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.examples.encrypter.gui;

import de.rcenvironment.components.examples.encrypter.common.EncrypterComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.DefaultEndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * "Properties" view tab for configuring cells as additional endpoints (i.e. inputs and outputs). For the Encoder component, the
 * {@link DefaultEndpointPropertySection} is extended because it uses its own version of {@link EndpointSelectionPane}.
 * 
 * @author Sascha Zur
 */
public class EncrypterEndpointPropertySection extends DefaultEndpointPropertySection {

    public EncrypterEndpointPropertySection() {

        EndpointSelectionPane inputPane = new EncrypterEndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, "default", new String[] { EncrypterComponentConstants.INPUT_NAME_TEXT }, this, false);
        EndpointSelectionPane outputPane = new EncrypterEndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, null, new String[] { EncrypterComponentConstants.OUTPUT_NAME_RESULT }, this, true);

        setColumns(2);
        setPanes(inputPane, outputPane);
    }
    
}
