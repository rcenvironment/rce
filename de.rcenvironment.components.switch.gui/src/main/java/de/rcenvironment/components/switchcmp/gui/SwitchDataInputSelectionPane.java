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

import org.eclipse.swt.widgets.Display;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * Pane for static input which is meant to switch.
 *
 * @author David Scholz
 * @author Kathrin Schaffert
 */
public class SwitchDataInputSelectionPane extends EndpointSelectionPane {

    private EndpointSelectionPane[] panes;

    public SwitchDataInputSelectionPane(Executor executor) {
        super(Messages.dataInputString, EndpointType.INPUT, SwitchComponentConstants.DATA_INPUT_ID, new String[] {},
            new String[] {}, executor);
    }
    
    @Override
    protected void onAddClicked() {
        SwitchEndpointEditDialog dialog =
            new SwitchEndpointEditDialog(Display.getDefault().getActiveShell(), EndpointActionType.ADD,
                configuration, endpointType, dynEndpointIdToManage, false,
                endpointManager.getDynamicEndpointDefinition(dynEndpointIdToManage)
                    .getMetaDataDefinition(),
                new HashMap<String, String>());
        onAddClicked(dialog);
    }
    
    @Override
    protected void onEditClicked() {
        final String name = (String) table.getSelection()[0].getData();
        EndpointDescription endpoint = endpointManager.getEndpointDescription(name);
        Map<String, String> newMetaData = cloneMetaData(endpoint.getMetaData());

        SwitchEndpointEditDialog dialog =
            new SwitchEndpointEditDialog(Display.getDefault().getActiveShell(),
                EndpointActionType.EDIT, configuration, endpointType,
                dynEndpointIdToManage, false, endpoint.getEndpointDefinition()
                    .getMetaDataDefinition(),
                newMetaData);
        onEditClicked(name, dialog);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        WorkflowNodeCommand command = new SwitchEditDynamicEndpointCommand(endpointType, oldDescription, newDescription,
            panes);
        execute(command);
    }

    @Override
    protected void executeAddCommand(String name, DataType type, Map<String, String> metaData) {
        WorkflowNodeCommand command = new SwitchAddDynamicEndpointCommand(endpointType, dynEndpointIdToManage, name,
            type, metaData, panes);
        execute(command);
    }

    @Override
    protected void executeRemoveCommand(List<String> names) {
        final WorkflowNodeCommand command = new SwitchRemoveDynamicEndpointCommand(endpointType, dynEndpointIdToManage, names, panes);
        execute(command);
    }

    public void setAllPanes(EndpointSelectionPane[] allPanes) {
        this.panes = allPanes;
    }
    
}
