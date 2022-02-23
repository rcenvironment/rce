/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand} removing dynamic endpoints from a <code>WorkflowNode</code> of type switch component.
 * 
 * @author Kathrin Schaffert
 */

public class SwitchRemoveDynamicEndpointCommand extends RemoveDynamicEndpointCommand {

    public SwitchRemoveDynamicEndpointCommand(EndpointType type, String dynamicEndpointId, List<String> names,
        Refreshable[] refreshable) {
        super(type, dynamicEndpointId, names, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        final WorkflowNode workflowNode = getWorkflowNode();
        String conTable = workflowNode.getConfigurationDescription().getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);
        int numOfCon = SwitchConditionSection.getTableContentLength(conTable);
        for (String name : names) {
            for (int i = 1; i <= numOfCon; i++) {
                workflowNode.getOutputDescriptionsManager()
                    .removeDynamicEndpointDescription(name + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + i);
                if (i == 1) {
                    workflowNode.getOutputDescriptionsManager()
                        .removeDynamicEndpointDescription(name + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH);
                }
            }
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
        final WorkflowNode workflowNode = getWorkflowNode();

        Map<String, String> outputMetaData = new HashMap<>();
        String conTable = workflowNode.getConfigurationDescription().getConfigurationValue(SwitchComponentConstants.CONDITION_KEY);
        int numOfCon = SwitchConditionSection.getTableContentLength(conTable);
        if (numOfCon != 0) {
            for (String name : names) {
                DataType type = oldDescriptions.get(name).getDataType();

                for (int i = 1; i <= numOfCon; i++) {
                    workflowNode.getOutputDescriptionsManager().addDynamicEndpointDescription(
                        SwitchComponentConstants.DATA_OUTPUT_ID, name + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " " + i,
                        type, outputMetaData);
                    if (i == 1) {
                        workflowNode.getOutputDescriptionsManager()
                            .addDynamicEndpointDescription(SwitchComponentConstants.DATA_OUTPUT_ID,
                                name + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH, type, outputMetaData);
                    }
                }
            }
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }
}
