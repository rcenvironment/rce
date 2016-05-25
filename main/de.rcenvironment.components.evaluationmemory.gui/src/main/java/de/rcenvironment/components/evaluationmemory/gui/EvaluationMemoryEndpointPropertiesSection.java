/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithOutputSelectionPane;

/**
 * 
 * Endpoint section for Evaluation Memory component.
 *
 * @author Doreen Seider
 */
public class EvaluationMemoryEndpointPropertiesSection extends EndpointPropertySection {

    public EvaluationMemoryEndpointPropertiesSection() {
        EndpointSelectionPane outputPaneToEvaluate = new EndpointSelectionPane("Outputs (values to evaluate sent to loop)",
            EndpointType.OUTPUT, this, true, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, true, true);
        
        InputCoupledWithOutputSelectionPane inputPaneToEvaluate = new InputCoupledWithOutputSelectionPane(
            "Inputs (values to evaluate received from study component)",
            EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, this, outputPaneToEvaluate);
        
        EndpointSelectionPane outputPaneEvaluationResults = new EndpointSelectionPane(
            "Outputs (evaluation results sent to study component)",
            EndpointType.OUTPUT, this, true, EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, true, true);

        InputCoupledWithOutputSelectionPane inputPaneEvaluationResults = new InputCoupledWithOutputSelectionPane(
            "Inputs (evaluation results received from loop)", EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, this,
            outputPaneEvaluationResults);
        
        EndpointSelectionPane inputPaneOthers = new EndpointSelectionPane("Inputs (other)", EndpointType.INPUT, 
            this, true, null, true, true);
        
        setColumns(2);
        setPanes(inputPaneToEvaluate, outputPaneToEvaluate, inputPaneEvaluationResults, outputPaneEvaluationResults, inputPaneOthers);
    }

}
