/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.converger.gui;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
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

        EndpointSelectionPane outputToConvergePane =
            new EndpointSelectionPane("Outputs (to converge)", EndpointType.OUTPUT, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE,
                new String[] { ConvergerComponentConstants.ENDPOINT_ID_FINAL_TO_CONVERGE }, new String[] {}, this, true, true);

        EndpointSelectionPane outputAuxiliaryPane =
            new EndpointSelectionPane("Outputs (auxiliary)", EndpointType.OUTPUT, ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY,
                new String[] {}, new String[] {}, this, true, true);

        EndpointSelectionPane inputToConvergePane =
            new ConvergerEndpointSelectionPane("Inputs (to converge)", this, outputToConvergePane, outputAuxiliaryPane);

        EndpointSelectionPane outputForwardedPane =
            new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                new String[] { LoopComponentConstants.ENDPOINT_ID_FINAL_TO_FORWARD }, new String[] {}, this, true, true);

        InputCoupledWithAnotherInputAndOutputsSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputsSelectionPane("Inputs (to forward)", LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                LoopComponentConstants.ENDPOINT_ID_FINAL_TO_FORWARD, ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, this,
                outputForwardedPane);

        EndpointSelectionPane outputPaneOthers = new EndpointSelectionPane("Outputs (other)", EndpointType.OUTPUT, null,
            new String[] {}, new String[] { ConvergerComponentConstants.CONVERGED, ConvergerComponentConstants.CONVERGED_ABSOLUTE,
                ConvergerComponentConstants.CONVERGED_RELATIVE, LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE },
            this, true, true);

        setColumns(2);
        setPanes(inputToConvergePane, outputToConvergePane, inputToForwardPane, outputForwardedPane, outputAuxiliaryPane,
            outputPaneOthers);
    }

}
