/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.properties;

import java.util.Deque;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.gui.workflow.EndpointContentProvider.Endpoint;


/**
 * Provides helper methods used if editing inputs.
 *
 * @author Doreen Seider
 */
public final class InputEditingHelper {

    private InputEditingHelper() {}
    
    /**
     * @param workflowInformation of affected workflow
     * @param endpoint of affected input
     * @return input value belonging to given endpoint
     */
    public static String getLatestInputValue(WorkflowExecutionInformation workflowInformation, Endpoint endpoint) {
        return getLatestInputValueFromEndpoint(workflowInformation.getExecutionIdentifier(), 
            getComponentIdentifier(workflowInformation, endpoint),
            endpoint);
    }
    
    private static String getComponentIdentifier(WorkflowExecutionInformation workflowInformation, Endpoint endpoint) {
        WorkflowNode workflowNode = endpoint.getWorkflowNode();
        return workflowInformation.getComponentExecutionInformation(workflowNode.getIdentifierAsObject()).getExecutionIdentifier();
    }
    
    /**
     * 
     * @param workflowId identifier of affected workflow
     * @param componentId identefier of affected component
     * @param endpoint of affected input
     * @return current input value belonging to given endpoint
     */
    public static String getLatestInputValueFromEndpoint(String workflowId, String componentId, Endpoint endpoint) {
        Deque<EndpointDatum> inputs = InputModel.getInstance().getInputs(workflowId, componentId, endpoint.getName());
        EndpointDatum input = null;
        if (!inputs.isEmpty()) {
            input = InputModel.getInstance().getInputs(workflowId, componentId, endpoint.getName()).getLast();
        }
        return getValueFromInput(input);
    }
    
    private static String getValueFromInput(EndpointDatum input) {
        final int maxLength = 50;
        String inputValue;
        if (input != null && input.getValue() != null) {
            TypedDatum rawInputValue = input.getValue();
            switch (input.getValue().getDataType()) {
            case FileReference:
                inputValue = ((FileReferenceTD) rawInputValue).getFileName();
                break;
            case DirectoryReference:
                inputValue = ((DirectoryReferenceTD) rawInputValue).getDirectoryName();
                break;
            case Integer:
            case Float:
            case Boolean:
                inputValue = rawInputValue.toString();
                break;
            case ShortText:
                inputValue = ((ShortTextTD) rawInputValue).toLengthLimitedString(maxLength);
                break;
            case Vector:
                inputValue = ((VectorTD) rawInputValue).toLengthLimitedString(maxLength);
                break;
            case Matrix:
                inputValue = ((MatrixTD) rawInputValue).toLengthLimitedString(maxLength);
                break;
            case SmallTable:
                inputValue = ((SmallTableTD) rawInputValue).toLengthLimitedString(maxLength);
                break;
            default:
                inputValue = input.getValue().toString();
                break;
            }
        } else {
            inputValue = "-";
        }
        return inputValue;
    }
    
}
