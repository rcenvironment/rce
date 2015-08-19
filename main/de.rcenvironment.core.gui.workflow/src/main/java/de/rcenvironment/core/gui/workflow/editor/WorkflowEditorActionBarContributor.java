/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;

/**
 * Make Edit->Undo/Redo/Delete work.
 * 
 * @author Heinrich Wendel
 * @author Sascha Zur
 */
@SuppressWarnings("restriction")
public class WorkflowEditorActionBarContributor extends ActionBarContributor {

    private static final String SHOW_NUMBER_OF_CONNECTIONS = "ShowNumberOfConnections";

    @Override
    protected void buildActions() {
        addRetargetAction(new DeleteRetargetAction());
        addRetargetAction(new UndoRetargetAction());
        addRetargetAction(new RedoRetargetAction());

        // Grid action with icon
        RetargetAction gridAction = new RetargetAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY,
            "Show grid", IAction.AS_CHECK_BOX);
        ImageDescriptor gridDescriptor = ImageDescriptor.createFromImage(
            new Image(Display.getDefault(), WorkflowEditorActionBarContributor.class
                .getResourceAsStream("/resources/icons/toolbar/snapToGrid.png")));
        gridAction.setImageDescriptor(gridDescriptor);
        addRetargetAction(gridAction);

        // Snap to geometry action with icon
        RetargetAction geometryAction = new RetargetAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY,
            "Snap to geometry", IAction.AS_CHECK_BOX);
        ImageDescriptor geometryDescriptor = ImageDescriptor.createFromImage(
            new Image(Display.getDefault(), WorkflowEditorActionBarContributor.class
                .getResourceAsStream("/resources/icons/toolbar/snapToGeometry.png")));
        geometryAction.setImageDescriptor(geometryDescriptor);
        addRetargetAction(geometryAction);

        // Show number of connections action with icon
        ShowNumberOfConnectionsAction showNumberOfConnectionAction =
            new ShowNumberOfConnectionsAction(SHOW_NUMBER_OF_CONNECTIONS, "Show number of connections in workflow editor");
        ImageDescriptor showNumbersDescriptor = ImageDescriptor.createFromImage(
            new Image(Display.getDefault(), WorkflowEditorActionBarContributor.class
                .getResourceAsStream("/resources/icons/toolbar/connectNumbers.gif")));
        showNumberOfConnectionAction.setImageDescriptor(showNumbersDescriptor);
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
    }

    @Override
    protected void declareGlobalActionKeys() {
        // currently none
    }
}
