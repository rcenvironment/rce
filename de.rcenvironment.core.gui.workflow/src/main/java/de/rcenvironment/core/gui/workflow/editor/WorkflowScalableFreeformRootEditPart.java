/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor;

import java.util.Arrays;

import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;


/**
 * Workflow root edit part with enabled {@link WorkflowZoomManager}.
 *
 * @author Jan Flink
 */
public class WorkflowScalableFreeformRootEditPart extends ScalableFreeformRootEditPart {

    private static final double[] ZOOM_LEVELS = { WorkflowZoomManager.MIN_ZOOM, .25, .5, .75, 1.0, 1.5, WorkflowZoomManager.MAX_ZOOM };

    private WorkflowZoomManager zoomManager;

    public WorkflowScalableFreeformRootEditPart() {
        zoomManager = new WorkflowZoomManager((ScalableFreeformLayeredPane) getScaledLayers(), (Viewport) getFigure());
        zoomManager.setZoomAnimationStyle(ZoomManager.ANIMATE_NEVER);
        zoomManager.setZoomLevels(ZOOM_LEVELS);
        zoomManager.setZoomLevelContributions(Arrays.asList(WorkflowZoomManager.FIT_WORKFLOW_HEIGHT, WorkflowZoomManager.FIT_WORKFLOW_WIDTH,
            WorkflowZoomManager.FIT_WORKFLOW));
    }

    @Override
    public ZoomManager getZoomManager() {
        return zoomManager;
    }

}
