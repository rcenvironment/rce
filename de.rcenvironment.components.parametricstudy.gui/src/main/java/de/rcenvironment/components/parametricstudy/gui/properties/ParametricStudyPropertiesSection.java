/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.properties;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputSelectionPane;

/**
 * Creates a "Properties" view tab for configuring endpoints (inputs and outputs).
 * 
 * @author Marc Stammerjohann
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class ParametricStudyPropertiesSection extends EndpointPropertySection {

    public ParametricStudyPropertiesSection() {

        EndpointSelectionPane designValuesPane = new EndpointSelectionPane("Inputs (configuration)",
            EndpointType.INPUT, ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS, new String[] {}, new String[] {}, this,
            EndpointSelectionPane.NAME_AND_TYPE_READ_ONLY, true);

        EndpointSelectionPane inputPane = new EndpointSelectionPane("Inputs (evaluation results received from loop)",
            EndpointType.INPUT, ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER, new String[] {}, new String[] {}, this, false,
            true);

        EndpointSelectionPane outputPane = new ParametricStudyEndpointSelectionPane("Outputs (values to evaluate)",
            EndpointType.OUTPUT, this, designValuesPane);

        EndpointSelectionPane outputForwardedPane = new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT,
            LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, new String[] {}, new String[] {}, this, true, true);

        InputCoupledWithAnotherInputAndOutputSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputSelectionPane("Inputs (to forward)", LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this,
                outputForwardedPane);

        EndpointSelectionPane outputPaneOthers = new EndpointSelectionPane("Outputs (other)", EndpointType.OUTPUT, null,
            new String[] {}, new String[] { LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE }, this, true, true);

        setColumns(2);
        setPanes(inputPane, outputPane, inputToForwardPane, outputForwardedPane, designValuesPane, outputPaneOthers);
    }
}
