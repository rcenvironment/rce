/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.WorkflowActionBarContributor;

/**
 * Make Edit->Undo/Redo/Delete work.
 * 
 * @author Heinrich Wendel
 * @author Sascha Zur
 * @author Jan Flink
 */
@SuppressWarnings("restriction")
public class WorkflowEditorActionBarContributor extends WorkflowActionBarContributor {

    private static final String SHOW_NUMBER_OF_CONNECTIONS = "ShowNumberOfConnections";

    
    
    @Override
    protected void buildActions() {
        super.buildActions();
        addRetargetAction(new DeleteRetargetAction());
        addRetargetAction(new UndoRetargetAction());
        addRetargetAction(new RedoRetargetAction());

        // Grid action with icon
        RetargetAction gridAction = new RetargetAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY,
            "Show grid", IAction.AS_CHECK_BOX);
        gridAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.SNAP_TO_GRID));
        addRetargetAction(gridAction);

        // Snap to geometry action with icon
        RetargetAction geometryAction = new RetargetAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY,
            "Snap to geometry", IAction.AS_CHECK_BOX);
        geometryAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.SNAP_TO_GEOMETRY));
        addRetargetAction(geometryAction);

        // Show number of connections action with icon
        ShowNumberOfConnectionsAction showNumberOfConnectionAction =
            new ShowNumberOfConnectionsAction(SHOW_NUMBER_OF_CONNECTIONS, "Show number of connections in workflow editor");
        showNumberOfConnectionAction.setImageDescriptor(
            ImageManager.getInstance().getImageDescriptor(StandardImages.SHOW_CONNECTION_NUMBERS));
        showNumberOfConnectionAction.setToolTipText("Show number of connections in workflow editor");
        addRetargetAction(showNumberOfConnectionAction);

        addRetargetAction(new RetargetAction(ActionFactory.SELECT_ALL.getId(), GEFMessages.SelectAllAction_Label));
    }

    @Override
    public void contributeToToolBar(IToolBarManager toolBarManager) {
        toolBarManager.add(getAction(ActionFactory.UNDO.getId()));
        toolBarManager.add(getAction(ActionFactory.REDO.getId()));
        toolBarManager.add(new Separator());
        toolBarManager.add(getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY));
        toolBarManager.add(getAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY));
        toolBarManager.add(getAction(SHOW_NUMBER_OF_CONNECTIONS));
        toolBarManager.add(new Separator());
        super.contributeToToolBar(toolBarManager);
    }

    @Override
    protected void declareGlobalActionKeys() {
        super.declareGlobalActionKeys();
    }
    
}
