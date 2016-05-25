/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.properties;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputSelectionPane;

/**
 * 
 * Creates a "Properties" view tab for configuring endpoints (inputs and outputs).
 * 
 * @author Marc Stammerjohann
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class ParametricStudyPropertiesSection extends EndpointPropertySection {

    public ParametricStudyPropertiesSection() {

        EndpointSelectionPane inputPane = new EndpointSelectionPane("Inputs (evaluation results received from loop)",
            EndpointType.INPUT, this, false, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, true);

        EndpointSelectionPane outputPane = new EndpointSelectionPane("Output (values to evaluate)",
            EndpointType.OUTPUT, this, false, null, true);

        EndpointSelectionPane outputForwardedPane = new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT,
            this, true, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, true, true);

        InputCoupledWithAnotherInputAndOutputSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputSelectionPane("Inputs (to forward)",
                LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this,
                outputForwardedPane);
        inputToForwardPane.setMetaDataInput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataOutput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));

        setColumns(2);
        setPanes(inputPane, outputPane, inputToForwardPane, outputForwardedPane);
    }
}
