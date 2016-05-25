/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.converger.gui;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputsSelectionPane;

/**
 * "Properties" view tab for configuring endpoints (i.e. inputs and outputs).
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ConvergerEndpointSection extends EndpointPropertySection {

    public ConvergerEndpointSection() {

        EndpointSelectionPane outputToConvergePane = new EndpointSelectionPane("Outputs (to converge)", EndpointType.OUTPUT,
            this, true, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, true, true);

        EndpointSelectionPane outputAuxiliaryPane = new EndpointSelectionPane("Outputs (auxiliary)", EndpointType.OUTPUT,
            this, true, ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY, true, true);

        EndpointSelectionPane inputToConvergePane = new ConvergerEndpointSelectionPane("Inputs (to converge)",
            ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY,
            this, outputToConvergePane, outputAuxiliaryPane);

        EndpointSelectionPane outputForwardedPane = new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT,
            this, true, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, true, true);

        InputCoupledWithAnotherInputAndOutputsSelectionPane inputToForwardPane
            = new InputCoupledWithAnotherInputAndOutputsSelectionPane("Inputs (to forward)",
                LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                this, outputForwardedPane);
        inputToForwardPane.setMetaDataInput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        inputToForwardPane.setMetaDataOutput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataOutputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        
        EndpointSelectionPane outputPaneOthers = new EndpointSelectionPane("Outputs (other)", EndpointType.OUTPUT,
            this, true, null, true, true);

        setColumns(2);
        setPanes(inputToConvergePane, outputToConvergePane, inputToForwardPane, outputForwardedPane, outputAuxiliaryPane,
            outputPaneOthers);
    }
    
}
