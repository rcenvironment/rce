/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.gui.properties;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputSelectionPane;

/**
 * An extended "Properties" view tab for configuring endpoints (ie inputs and outputs) and using
 * initial Variables.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class DOEEndpointPropertySection extends EndpointPropertySection {

    public DOEEndpointPropertySection() {
        super();

        EndpointSelectionPane inputPane = new EndpointSelectionPane("Inputs (evaluation results received from loop)",
            EndpointType.INPUT, this, false, "default", true);

        DOEEndpointSelectionPane outputPane = new DOEEndpointSelectionPane("Outputs (values to evaluate and others)",
            EndpointType.OUTPUT, this, false, "default", true);

        EndpointSelectionPane outputForwardedPane = new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT,
            this, true, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, true, true);

        InputCoupledWithAnotherInputAndOutputSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputSelectionPane("Inputs (to forward)",
                LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this,
                outputForwardedPane);
        inputToForwardPane.setMetaDataInput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataOutput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));

        inputPane.setMetaDataInput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));

        setColumns(2);
        setPanes(inputPane, outputPane, inputToForwardPane, outputForwardedPane);
    }
}
