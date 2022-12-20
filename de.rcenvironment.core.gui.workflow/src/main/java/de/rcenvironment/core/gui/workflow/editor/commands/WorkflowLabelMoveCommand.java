/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;

/**
 * Command to move a WorkflowLabel to a new location on the screen.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabelMoveCommand extends Command {

    /** The workflow label. **/
    private final WorkflowLabel label;

    /** the request. **/
    private final ChangeBoundsRequest request;

    /** the new location. **/
    private final Point newLocation;

    /** saved old location. **/
    private final Point oldLocation;

    private final Dimension oldSize;

    private final Dimension newSize;

    private final boolean resize;

    /**
     * Constructor.
     * 
     * @param label The workflow label.
     * @param req The request.
     * @param newBounds The rectangle with the new location.
     */
    public WorkflowLabelMoveCommand(WorkflowLabel label, ChangeBoundsRequest req, Rectangle newBounds) {
        this.label = label;
        request = req;
        resize = !req.getSizeDelta().equals(0, 0);
        newLocation = newBounds.getLocation();
        newSize = newBounds.getSize();
        oldLocation = new Point(label.getX(), label.getY());
        oldSize = new Dimension(label.getWidth(), label.getHeight());
    }

    @Override
    public boolean canExecute() {
        Object type = request.getType();
        boolean execute = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() instanceof WorkflowEditor;

        return (execute && (RequestConstants.REQ_MOVE.equals(type) || RequestConstants.REQ_MOVE_CHILDREN.equals(type)
            || RequestConstants.REQ_RESIZE.equals(type) || RequestConstants.REQ_RESIZE_CHILDREN.equals(type)));
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void redo() {
        label.setSize(newSize.width, newSize.height);
        label.setLocation(newLocation.x, newLocation.y);
        label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
    }

    @Override
    public void undo() {
        label.setSize(oldSize.width, oldSize.height);
        label.setLocation(oldLocation.x, oldLocation.y);
        label.firePropertyChange(WorkflowLabel.PROPERTY_CHANGE);
    }
}
