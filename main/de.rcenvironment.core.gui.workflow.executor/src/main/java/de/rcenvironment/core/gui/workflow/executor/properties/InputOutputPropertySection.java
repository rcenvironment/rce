/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * Property section for defining inputs and outputs for files.
 * 
 * @author Doreen Seider
 */
public class InputOutputPropertySection extends EndpointPropertySection {

    public InputOutputPropertySection() {

        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.input,
            EndpointType.INPUT, this, false,
            EndpointDefinitionConstants.DEFAULT_DYNAMIC_ENDPOPINT_IDENTIFIER, true);
        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.output,
            EndpointType.OUTPUT, this, false,
            EndpointDefinitionConstants.DEFAULT_DYNAMIC_ENDPOPINT_IDENTIFIER, true);
        setColumns(1);

        EndpointSelectionPane[] panes = new EndpointSelectionPane[] { inputPane, outputPane };
        setPanes(panes);
    }

}
