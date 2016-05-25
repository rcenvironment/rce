/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithAnotherInputAndOutputsSelectionPane;

/**
 * An extended "Properties" view tab for configuring endpoints (ie inputs and outputs) and using
 * initial Variables.
 * 
 * @author Sascha Zur
 */
public class OptimizerEndpointPropertySection extends EndpointPropertySection {

    public OptimizerEndpointPropertySection() {
        super();
        OptimizerEndpointSelectionPane objectivePane = new OptimizerEndpointSelectionPane(Messages.targetFunction,
            EndpointType.INPUT, this, OptimizerComponentConstants.ID_OBJECTIVE, false);
        OptimizerEndpointSelectionPane constraintsPane = new OptimizerEndpointSelectionPane(Messages.constraints,
            EndpointType.INPUT, this, OptimizerComponentConstants.ID_CONSTRAINT, false);
        OptimizerEndpointSelectionPane designVariablePane = new OptimizerEndpointSelectionPane(Messages.designVariables,
            EndpointType.OUTPUT, this, OptimizerComponentConstants.ID_DESIGN, false);
        OptimizerEndpointSelectionPane gradientsPane = new OptimizerEndpointSelectionPane("Gradients (Inputs)",
            EndpointType.INPUT, this, OptimizerComponentConstants.ID_GRADIENTS, true);
        OptimizerEndpointSelectionPane startValuesPane = new OptimizerEndpointSelectionPane(Messages.startValueInput,
            EndpointType.INPUT, this, OptimizerComponentConstants.ID_STARTVALUES, true);
        OptimizerEndpointSelectionPane optimumPane = new OptimizerEndpointSelectionPane(Messages.optimalSolutionOutput,
            EndpointType.OUTPUT, this, OptimizerComponentConstants.ID_OPTIMA, true);

        EndpointSelectionPane outputForwardedPane = new EndpointSelectionPane("Outputs (forwarded)", EndpointType.OUTPUT,
            this, true, LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, true, true);

        InputCoupledWithAnotherInputAndOutputsSelectionPane inputToForwardPane =
            new InputCoupledWithAnotherInputAndOutputsSelectionPane("Inputs (to forward)",
                LoopComponentConstants.ENDPOINT_ID_TO_FORWARD, LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                this, outputForwardedPane);

        EndpointSelectionPane outputPaneOthers = new EndpointSelectionPane("Other (Outputs)", EndpointType.OUTPUT,
            this, true, null, true, true);

        setColumns(1);
        setPanes(objectivePane, constraintsPane, designVariablePane, startValuesPane, optimumPane, gradientsPane,
            inputToForwardPane, outputForwardedPane, outputPaneOthers);
        objectivePane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
        constraintsPane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
        designVariablePane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });

        inputToForwardPane.setMetaDataInput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataInputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));
        inputToForwardPane.setMetaDataOutput(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        inputToForwardPane.setMetaDataOutputWithSuffix(LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint));

    }
}
