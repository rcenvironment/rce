/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.editparts.ZoomManager;


/**
 * Workflow zoom manager to provide custom zoom texts as well as custom min and max zoom levels.
 *
 * @author Jan Flink
 */
public class WorkflowZoomManager extends ZoomManager {

    /**
     * Zoom string to fit the worklfow into the viewport.
     */
    public static final String FIT_WORKFLOW = "Fit Workflow";

    /**
     * Zoom string to fit the worklfow height into the viewport.
     */
    public static final String FIT_WORKFLOW_HEIGHT = "Fit Height";

    /**
     * Zoom string to fit the worklfow width into the viewport.
     */
    public static final String FIT_WORKFLOW_WIDTH = "Fit Width";

    protected static final double MAX_ZOOM = 2;

    protected static final double MIN_ZOOM = .1;

    private double previousLevel = 1;

    public WorkflowZoomManager(ScalableFigure pane, Viewport viewport) {
        super(pane, viewport);
    }

    @Override
    public double getMaxZoom() {
        return MAX_ZOOM;
    }

    @Override
    public double getMinZoom() {
        return MIN_ZOOM;
    }

    @Override
    public void setZoomAsText(String zoomString) {
        String zString;
        if (FIT_WORKFLOW.equalsIgnoreCase(zoomString)) {
            zString = FIT_ALL;
        } else if (FIT_WORKFLOW_HEIGHT.equalsIgnoreCase(zoomString)) {
            zString = FIT_HEIGHT;
        } else if (FIT_WORKFLOW_WIDTH.equalsIgnoreCase(zoomString)) {
            zString = FIT_WIDTH;
        } else {
            zString = zoomString;
        }
        boolean fitMin = FIT_ALL.equalsIgnoreCase(zString) && getFitPageZoomLevel() < MIN_ZOOM;
        fitMin |= FIT_WIDTH.equalsIgnoreCase(zString) && getFitWidthZoomLevel() < MIN_ZOOM;
        fitMin |= FIT_HEIGHT.equalsIgnoreCase(zString) && getFitHeightZoomLevel() < MIN_ZOOM;
        if (fitMin) {
            super.setZoom(MIN_ZOOM);
            return;
        }
        super.setZoomAsText(zString);
    }

    /**
     * Restores the previous zoom level.
     * 
     */
    public void restorePreviousZoomLevel() {
        setZoom(previousLevel);
    }

    @Override
    public void setZoom(double zoom) {
        previousLevel = getZoom();
        super.setZoom(zoom);
    }

}
