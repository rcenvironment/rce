/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointSelectionPane;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand.Executor;

/**
 * 
 * Pane for static input which is meant to switch.
 *
 * @author David Scholz
 */
public class SwitchDataInputSelectionPane extends EndpointSelectionPane {

    private EndpointSelectionPane[] panes;

    public SwitchDataInputSelectionPane(String genericEndpointTitle, EndpointType direction, Executor executor, boolean readonly,
        String dynamicEndpointIdToManage) {
        super(genericEndpointTitle, direction, executor, readonly, dynamicEndpointIdToManage, true, true);
    }

    @Override
    protected void executeEditCommand(EndpointDescription oldDescription, EndpointDescription newDescription) {
        super.executeEditCommand(oldDescription, newDescription);
        WorkflowNodeCommand command = new SwitchEditDynamicEndpointCommand(endpointType, newDescription, newDescription, panes);
        execute(command);
    }

    /**
     * 
     * Changes enpoints if edited.
     *
     * @author David Scholz
     */
    private class SwitchEditDynamicEndpointCommand extends EditDynamicEndpointCommand {

        SwitchEditDynamicEndpointCommand(EndpointType direction, EndpointDescription oldDescription,
            EndpointDescription newDescription, Refreshable[] refreshable) {
            super(direction, oldDescription, newDescription, refreshable);
        }

        @Override
        public void execute() {
            final WorkflowNode workflowNode = getWorkflowNode();
            super.execute();

            for (EndpointDescription outputDesc : workflowNode.getOutputDescriptionsManager().getStaticEndpointDescriptions()) {
                workflowNode.getOutputDescriptionsManager().editStaticEndpointDescription(outputDesc.getName(), newDesc.getDataType(),
                    newDesc.getMetaData());
            }
            //refresh all pains if endpoints were changed
            if (refreshable != null) {
                for (Refreshable r : refreshable) {
                    r.refresh();
                }
            }

        }

    }

    public void setAllPanes(EndpointSelectionPane[] allPanes) {
        this.panes = allPanes;
    }

}
