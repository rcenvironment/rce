/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.TableItem;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicInputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicOutputCommand;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.ProcessEndpointsGroupCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * Endpoint selection pane.
 * 
 * @author Doreen Seider
 * @author Caslav Ilic
 */
public class ConvergerEndpointSelectionPane extends EndpointSelectionPane {

    private final String dynamicEndpointIdToConverge;
    private final String dynamicEndpointIdAuxiliary;

    private final Executor executor;

    private final EndpointSelectionPane outputPane;
    private final EndpointSelectionPane auxiliaryPane;

    public ConvergerEndpointSelectionPane(String title, String idToConverge, String idAuxiliary, Executor executor,
        EndpointSelectionPane outputPane, EndpointSelectionPane auxiliaryPane) {
        super(title, EndpointType.INPUT, executor, false, idToConverge, true);
        this.dynamicEndpointIdToConverge = idToConverge;
        this.dynamicEndpointIdAuxiliary = idAuxiliary;
        this.executor = executor;
        this.outputPane = outputPane;
        this.auxiliaryPane = auxiliaryPane;
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        metaData.putAll(LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint));
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this, outputPane);
        groupCommand.add(new AddDynamicInputCommand(dynamicEndpointIdToConverge, name, type, metaData, this, outputPane));
        if (metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE) == null
            || !Boolean.parseBoolean(metaData.get(ConvergerComponentConstants.META_HAS_STARTVALUE))) {
            // LoopComponentConstants.createMetaData should be extended to take InputExecutionContraint
            Map<String, String> startMetaData = LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint);
            startMetaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                EndpointDefinition.InputExecutionContraint.Required.name());
            groupCommand.add(new AddDynamicInputCommand(dynamicEndpointIdToConverge,
                name + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, type,
                startMetaData,
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane));
        }
        groupCommand.add(new AddDynamicOutputCommand(dynamicEndpointIdToConverge, name, type,
            LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint), this, outputPane));
        groupCommand.add(new AddDynamicOutputCommand(dynamicEndpointIdAuxiliary,
            name + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX, DataType.Boolean,
            LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint), this, auxiliaryPane));
        groupCommand.add(new AddDynamicOutputCommand(dynamicEndpointIdToConverge,
            name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, type,
            LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint), this, outputPane));
        execute(groupCommand);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        String oldName = oldDescription.getName();
        String newName = newDescription.getName();
        DataType newType = newDescription.getDataType();
        Map<String, String> newMetaData = newDescription.getMetaData();
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this, outputPane);
        groupCommand.add(new EditDynamicInputCommand(dynamicEndpointIdToConverge,
            oldName, newName, newType, newMetaData, this, outputPane));
        boolean oldHasStartValue = oldDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
            && Boolean.parseBoolean(oldDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE));
        boolean newHasStartValue = newDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE) != null
            && Boolean.parseBoolean(newDescription.getMetaData().get(ConvergerComponentConstants.META_HAS_STARTVALUE));
        if (!oldHasStartValue && !newHasStartValue) {
            groupCommand.add(new EditDynamicInputCommand(dynamicEndpointIdToConverge,
                oldName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                newName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX,
                newType, LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint),
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane));
        } else if (!oldHasStartValue && newHasStartValue) {
            groupCommand.add(new RemoveDynamicInputCommand(dynamicEndpointIdToConverge,
                oldName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this, outputPane));
        } else if (oldHasStartValue && !newHasStartValue) {
            groupCommand.add(new AddDynamicInputCommand(dynamicEndpointIdToConverge,
                newName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, newType,
                LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint),
                LoopComponentConstants.ENDPOINT_STARTVALUE_GROUP, this, outputPane));
        }
        groupCommand.add(new EditDynamicOutputCommand(dynamicEndpointIdToConverge,
            oldName, newName, newType, LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint),
            this, outputPane));
        groupCommand.add(new EditDynamicOutputCommand(dynamicEndpointIdAuxiliary,
            oldName + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
            newName + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX,
            DataType.Boolean, LoopComponentConstants.createMetaData(LoopEndpointType.SelfLoopEndpoint),
            this, auxiliaryPane));
        groupCommand.add(new EditDynamicOutputCommand(dynamicEndpointIdToConverge,
            oldName + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
            newName + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
            newType, LoopComponentConstants.createMetaData(LoopEndpointType.OuterLoopEndpoint),
            this, outputPane));
        execute(groupCommand);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        List<String> removeNames = new LinkedList<>();
        for (String name : names) {
            if (name.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                removeNames.add(name);
            }
        }
        for (String remove : removeNames) {
            names.remove(remove);
        }
        ProcessEndpointsGroupCommand groupCommand = new ProcessEndpointsGroupCommand(executor, this, outputPane);
        for (String name : names) {
            groupCommand.add(new RemoveDynamicInputCommand(dynamicEndpointIdToConverge,
                name, this, outputPane));
            groupCommand.add(new RemoveDynamicInputCommand(dynamicEndpointIdToConverge,
                name + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX, this, outputPane));
            groupCommand.add(new RemoveDynamicOutputCommand(dynamicEndpointIdToConverge,
                name, this, outputPane));
            groupCommand.add(new RemoveDynamicOutputCommand(dynamicEndpointIdAuxiliary,
                name + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX, this, auxiliaryPane));
            groupCommand.add(new RemoveDynamicOutputCommand(dynamicEndpointIdToConverge,
                name + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX, this, outputPane));
        }
        execute(groupCommand);
    }

    @Override
    protected void updateButtonActivation() {
        super.updateButtonActivation();
        TableItem[] selection = table.getSelection();
        if (selection.length == 1 && selection[0].getText().endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
            buttonEdit.setEnabled(false);
            buttonRemove.setEnabled(false);
        }
    }
}
