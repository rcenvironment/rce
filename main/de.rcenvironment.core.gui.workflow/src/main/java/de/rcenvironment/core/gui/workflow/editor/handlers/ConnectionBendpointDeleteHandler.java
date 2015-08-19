/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.gui.workflow.editor.commands.BendpointDeleteAllCommand;
import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;

/**
 * Handler to delete bendpoints from a connection.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionBendpointDeleteHandler extends AbstractHandler {

    protected GraphicalViewer viewer;

    protected CommandStack commandStack;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        final IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof WorkflowEditor) {
            WorkflowEditor editor = (WorkflowEditor) activePart;
            viewer = editor.getViewer();
            commandStack = (CommandStack) editor.getAdapter(CommandStack.class);
        }

        WorkflowDescription model = (WorkflowDescription) viewer.getContents().getModel();
        
        ConnectionWrapper connectionWrapper = null;
        @SuppressWarnings("rawtypes") List selections = viewer.getSelectedEditParts();
        for (Object element : selections) {
            if (element instanceof ConnectionPart) {
                ConnectionPart part = (ConnectionPart) element;
                connectionWrapper = (ConnectionWrapper) part.getModel();
                BendpointDeleteAllCommand bendpointDeleteCommand = new BendpointDeleteAllCommand();
                bendpointDeleteCommand.setConnections(ConnectionUtils.getConnectionsBetweenNodes(
                    connectionWrapper.getSource(), connectionWrapper.getTarget(), model));
                bendpointDeleteCommand.setReferencedModel(connectionWrapper);
                bendpointDeleteCommand.setWorkflowDescription(model);
                commandStack.execute(bendpointDeleteCommand);
                break;
            }
        }

        return null;
    }

}
