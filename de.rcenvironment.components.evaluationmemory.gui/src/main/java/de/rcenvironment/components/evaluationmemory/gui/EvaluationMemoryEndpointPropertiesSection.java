/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointPropertySection;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.InputCoupledWithOutputSelectionPane;

/**
 * Endpoint section for Evaluation Memory component.
 *
 * @author Doreen Seider
 */
public class EvaluationMemoryEndpointPropertiesSection extends EndpointPropertySection {

    public EvaluationMemoryEndpointPropertiesSection() {
        
        EndpointSelectionPane outputPaneToEvaluate = new EndpointSelectionPane("Outputs (values to evaluate sent to loop)",
            EndpointType.OUTPUT, EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, new String[] {}, new String[] {}, this, true);

        InputCoupledWithOutputSelectionPane inputPaneToEvaluate = new EvaluationMemoryInputPaneToEvaluate(
            "Inputs (values to evaluate received from evaluation component)",
            EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE, this, outputPaneToEvaluate);
        
        EndpointSelectionPane outputPaneEvaluationResults = new EndpointSelectionPane(
            "Outputs (evaluation results sent to evaluation component)", EndpointType.OUTPUT,
            EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, new String[] {}, new String[] {}, this, true);

        InputCoupledWithOutputSelectionPane inputPaneEvaluationResults = new InputCoupledWithOutputSelectionPane(
            "Inputs (evaluation results received from loop)", EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS, this,
            outputPaneEvaluationResults);
        
        setColumns(2);
        setPanes(inputPaneToEvaluate, outputPaneToEvaluate, inputPaneEvaluationResults, outputPaneEvaluationResults);
    }

}
