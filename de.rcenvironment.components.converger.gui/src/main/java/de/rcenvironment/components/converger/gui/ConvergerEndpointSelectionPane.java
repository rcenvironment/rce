/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.ProcessEndpointsGroupCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * Endpoint selection pane.
 * 
 * @author Doreen Seider
 * @author Caslav Ilic
 */
public class ConvergerEndpointSelectionPane extends EndpointSelectionPane {

    private final Executor executor;

    private final EndpointSelectionPane outputPane;

    private final EndpointSelectionPane auxiliaryPane;

    public ConvergerEndpointSelectionPane(String title, Executor executor, EndpointSelectionPane outputPane,
        EndpointSelectionPane auxiliaryPane) {
        super(title, EndpointType.INPUT, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE,
            new String[] { ConvergerComponentConstants.ENDPOINT_ID_START_TO_CONVERGE }, new String[] {}, executor, false, true);
        this.executor = executor;
        this.outputPane = outputPane;
        this.auxiliaryPane = auxiliaryPane;
    }

    @Override
    protected void onEditClicked(String name, EndpointEditDialog dialog, Map<String, String> newMetaData) {

        if (!endpointManager.getEndpointDescription(name).getDynamicEndpointIdentifier().equals(dynEndpointIdToManage)) {
            dialog.setReadOnlyType(EndpointSelectionPane.NAME_AND_TYPE_READ_ONLY);
        }
        super.onEditClicked(name, dialog, newMetaData);
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this, outputPane);
        groupCommand
            .add(new AddDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, name, type, metaData, this, outputPane));
        if (metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE) == null
            || !Boolean.parseBoolean(metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE))) {
            Map<String, String> startMetaData = new HashMap<>();
            startMetaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                EndpointDefinition.InputExecutionContraint.Required.name());
            groupCommand.add(new AddDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_START_TO_CONVERGE,
                name + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, type,
                startMetaData,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane));
        }
        groupCommand.add(new AddDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, name, type,
            Collections.<String, String> emptyMap(), this, outputPane));
        groupCommand.add(new AddDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY,
            name + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX, DataType.Boolean,
            Collections.<String, String> emptyMap(), this, auxiliaryPane));
        groupCommand.add(new AddDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_FINAL_TO_CONVERGE,
            name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, type,
            Collections.<String, String> emptyMap(), this, outputPane));
        execute(groupCommand);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        String oldName = oldDescription.getName();
        String newName = newDescription.getName();
        DataType newType = newDescription.getDataType();
        Map<String, String> newMetaData = newDescription.getMetaData();
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this, outputPane);
        groupCommand.add(new EditDynamicInputCommand(oldDescription.getDynamicEndpointIdentifier(),
            oldName, newName, newType, newMetaData, this, outputPane));
        if (oldDescription.getDynamicEndpointIdentifier().equals(dynEndpointIdToManage)) {
            boolean oldHasStartValue = oldDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
                && Boolean.parseBoolean(oldDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE));
            boolean newHasStartValue = newDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
                && Boolean.parseBoolean(newDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE));
            if (!oldHasStartValue && !newHasStartValue) {
                groupCommand.add(new EditDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_START_TO_CONVERGE,
                    oldName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                    newName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                    newType, new HashMap<String, String>(),
                    LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane));
            } else if (!oldHasStartValue && newHasStartValue) {
                groupCommand.add(new RemoveDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_START_TO_CONVERGE,
                    oldName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this, outputPane));
            } else if (oldHasStartValue && !newHasStartValue) {
                groupCommand.add(new AddDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_START_TO_CONVERGE,
                    newName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, newType,
                    new HashMap<String, String>(),
                    LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane));
            }
            groupCommand.add(new EditDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE,
                oldName, newName, newType, new HashMap<String, String>(),
                this, outputPane));
            groupCommand.add(new EditDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY,
                oldName + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
                newName + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
                DataType.Boolean, new HashMap<String, String>(),
                this, auxiliaryPane));
            groupCommand.add(new EditDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_FINAL_TO_CONVERGE,
                oldName + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                newName + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                newType, new HashMap<String, String>(),
                this, outputPane));
        }
        execute(groupCommand);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this, outputPane);
        for (String name : names) {
            groupCommand.add(new RemoveDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE,
                name, this, outputPane));
            groupCommand.add(new RemoveDynamicInputCommand(ConvergerComponentConstants.ENDPOINT_ID_START_TO_CONVERGE,
                name + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this, outputPane));
            groupCommand.add(new RemoveDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE,
                name, this, outputPane));
            groupCommand.add(new RemoveDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY,
                name + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX, this, auxiliaryPane));
            groupCommand.add(new RemoveDynamicOutputCommand(ConvergerComponentConstants.ENDPOINT_ID_FINAL_TO_CONVERGE,
                name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, this, outputPane));
        }
        execute(groupCommand);
    }

}
