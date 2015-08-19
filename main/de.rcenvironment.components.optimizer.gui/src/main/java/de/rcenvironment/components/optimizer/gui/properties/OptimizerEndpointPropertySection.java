/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;

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
        OptimizerEndpointSelectionPane gradientsPane = new OptimizerEndpointSelectionPane("Gradients",
            EndpointType.INPUT, this, OptimizerComponentConstants.ID_GRADIENTS, true);
        OptimizerEndpointSelectionPane startValuesPane = new OptimizerEndpointSelectionPane(Messages.startValueInput,
            EndpointType.INPUT, this, OptimizerComponentConstants.ID_STARTVALUES, true);
        OptimizerEndpointSelectionPane optimumPane = new OptimizerEndpointSelectionPane(Messages.optimalSolutionOutput,
            EndpointType.OUTPUT, this, OptimizerComponentConstants.ID_OPTIMA, true);
        setColumns(1);
        setPanes(objectivePane, constraintsPane, designVariablePane, startValuesPane, optimumPane, gradientsPane);
        objectivePane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
        constraintsPane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
        designVariablePane.setAllPanes(new EndpointSelectionPane[] { objectivePane, constraintsPane, designVariablePane, startValuesPane,
            optimumPane, gradientsPane });
    }
}
