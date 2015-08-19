/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.properties;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

/**
 * 
 * Creates a "Properties" view tab for configuring endpoints (inputs and outputs).
 * 
 * @author Marc Stammerjohann
 */
public class ParametricStudyPropertiesSection extends EndpointPropertySection {

    public ParametricStudyPropertiesSection() {

        EndpointSelectionPane inputPane = new EndpointSelectionPane(Messages.inputs,
            EndpointType.INPUT, this, false, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, true);

        EndpointSelectionPane outputPane = new EndpointSelectionPane(Messages.outputs,
            EndpointType.OUTPUT, this, false, null, false);
        setColumns(2);
        setPanes(inputPane, outputPane);

    }
}
