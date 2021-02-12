/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputsSelectionPane;

/**
 * An extended "Properties" view tab for configuring endpoints (ie inputs and outputs) and using initial Variables.
 * 
 * @author Sascha Zur
 */
public class OptimizerEndpointPropertySection extends EndpointPropertySection {

    public OptimizerEndpointPropertySection() {
        super();
        OptimizerEndpointSelectionPane objectivePane = new OptimizerEndpointSelectionPane(Messages.targetFunction,
            EndpointType.INPUT, OptimizerComponentConstants.ID_OBJECTIVE, this, false);
        OptimizerEndpointSelectionPane constraintsPane = new OptimizerEndpointSelectionPane(Messages.constraints,
            EndpointType.INPUT, OptimizerComponentConstants.ID_CONSTRAINT, this, false);
        OptimizerEndpointSelectionPane designVariablePane = new OptimizerEndpointSelectionPane(Messages.designVariables,
            EndpointType.OUTPUT, OptimizerComponentConstants.ID_DESIGN, this, false);
        OptimizerEndpointSelectionPane gradientsPane = new OptimizerEndpointSelectionPane("Gradients (Inputs)",
            EndpointType.INPUT, OptimizerComponentConstants.ID_GRADIENTS, this, true);
        EndpointSelectionPane startValuesPane = new EndpointSelectionPane(Messages.startValueInput,
            EndpointType.INPUT, OptimizerComponentConstants.ID_STARTVALUES, new String[] { OptimizerComponentConstants.ID_STARTVALUES },
            new String[] {}, this, EndpointSelectionPane.NAME_AND_TYPE_READ_ONLY, true);
        OptimizerEndpointSelectionPane optimumPane = new OptimizerEndpointSelectionPane(Messages.optimalSolutionOutput,
            EndpointType.OUTPUT, OptimizerComponentConstants.ID_OPTIMA, this, true);

        EndpointSelectionPane outputForwardedPane =
            new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                new String[] { LoopComponentConstants.ENDPOINT_ID_FINAL_TO_FORWARD }, new String[] {}, this, true, true);

        InputCoupledWithAnotherInputAndOutputsSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputsSelectionPane("Inputs (to forward)", LoopComponentConstants.ENDPOINT_ID_TO_FORWARD,
                LoopComponentConstants.ENDPOINT_ID_START_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                LoopComponentConstants.ENDPOINT_ID_FINAL_TO_FORWARD, OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX, this,
                outputForwardedPane);

        EndpointSelectionPane outputPaneOthers = new EndpointSelectionPane("Outputs (other)", EndpointType.OUTPUT,
            null, new String[] {}, new String[] { LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE,
                OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME, OptimizerComponentConstants.DERIVATIVES_NEEDED },
            this, false, true);

        setColumns(1);
        setPanes(objectivePane, constraintsPane, designVariablePane, startValuesPane, optimumPane, gradientsPane,
            inputToForwardPane, outputForwardedPane, outputPaneOthers);
        objectivePane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
        constraintsPane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
        designVariablePane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });

    }
}
