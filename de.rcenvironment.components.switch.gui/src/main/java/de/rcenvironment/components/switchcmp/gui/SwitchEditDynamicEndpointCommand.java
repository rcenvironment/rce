/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * 
 * {@link WorkflowNodeCommand} for editing a dynamic endpoint of a <code>WorkflowNode</code> of type switch component.
 *
 * @author David Scholz
 * @author Kathrin Schaffert
 */
public class SwitchEditDynamicEndpointCommand extends EditDynamicEndpointCommand {

    SwitchEditDynamicEndpointCommand(EndpointType direction, EndpointDescription oldDescription,
        EndpointDescription newDescription, Refreshable[] refreshable) {
        super(direction, oldDescription, newDescription, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        if (!newDesc.getName().equals(oldDesc.getName()) || !newDesc.getDataType().equals(oldDesc.getDataType())) {
            setDataToOutput(oldDesc.getName(), newDesc.getName(), newDesc.getDataType());
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    @Override
    public void undo() {
        super.undo();
        if (!newDesc.getName().equals(oldDesc.getName()) || !newDesc.getDataType().equals(oldDesc.getDataType())) {
            setDataToOutput(newDesc.getName(), oldDesc.getName(), oldDesc.getDataType());
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    /**
     * 
     * Sets the correspondent Output Streams (_CONDITION, _NO_MATCH) for an Input Stream.
     * 
     * @param oldName old name of the input stream
     * @param newName new name of the input stream
     * @param dataType data Type to set
     */
    public void setDataToOutput(String oldName, String newName, DataType dataType) {
        final WorkflowNode workflowNode = getWorkflowNode();
        String conTable = workflowNode.getConfigurationDescription().getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);
        int numOfCon = SwitchConditionSection.getTableContentLength(conTable);
        if (numOfCon != 0) {
            EndpointDescriptionsManager outputManager = workflowNode.getOutputDescriptionsManager();
            for (int i = 1; i <= numOfCon; i++) {
                EndpointDescription endpointCondition =
                    outputManager.getEndpointDescription(oldName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + i);
                workflowNode.getOutputDescriptionsManager().editDynamicEndpointDescription(
                    oldName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + i,
                    newName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + i,
                    dataType, endpointCondition.getMetaData());
                if (i == 1) {
                    EndpointDescription endpointNoMatch =
                        outputManager.getEndpointDescription(oldName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH);
                    workflowNode.getOutputDescriptionsManager().editDynamicEndpointDescription(
                        oldName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH,
                        newName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH,
                        dataType, endpointNoMatch.getMetaData());
                }
            }
        }
    }
}
