/*
 * Copyright (c) 2007 IBM Corporation and others
 * Copyright 2016-2022 DLR, Germany
 *  
 * SPDX-License-Identifier: EPL-1.0
 *   
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

/******************************************************************************
 * Code adapted from Graphical Modeling Framework's
 * org.eclipse.gmf.runtime.gef.ui.internal.tools.ConnectionBendpointTrackerEx
 * Downloaded: February, 2016
 * 
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.BendpointRequest;
import org.eclipse.gef.tools.ConnectionBendpointTracker;
import org.eclipse.swt.SWT;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;

/**
 * A derived ConnectionBendpointTracker that overrides the updateSourceRequest method allowing bendpoints to snap to grid.
 * 
 * // * @author carson_li // Name commented out as checkstyle won't accept it otherwise
 * 
 * @author Oliver Seebach
 */
public class CustomConnectionBendpointTracker extends ConnectionBendpointTracker {

    private static final double NUMBER_1_5 = 1.5;

    private static final double NUMBER_0_5 = 0.5;

    private static final int MODIFIER_NO_SNAPPING = SWT.ALT;

    private PrecisionRectangle sourceRectangle;

    private Point originalLocation = null;

    private ConnectionEditPart host;

    public CustomConnectionBendpointTracker(ConnectionEditPart host, int index) {
        super(host, index);
        this.host = host;
    }

    /*
     * @see org.eclipse.gef.tools.SimpleDragTracker#updateSourceRequest()
     */
    @Override
    protected void updateSourceRequest() {
        BendpointRequest request = (BendpointRequest) getSourceRequest();

        if (originalLocation == null) {
            originalLocation = getStartLocation().getCopy();
        }

        Dimension delta = getDragMoveDelta();

        if (getCurrentInput().isShiftKeyDown()) {
            float ratio = 0;
            if (delta.width != 0) {
                ratio = (float) delta.height / (float) delta.width;
            }

            ratio = Math.abs(ratio);
            if (ratio > NUMBER_0_5 && ratio < NUMBER_1_5) {
                if (Math.abs(delta.height) > Math.abs(delta.width)) {
                    if (delta.height > 0) {
                        delta.height = Math.abs(delta.width);
                    } else {
                        delta.height = -Math.abs(delta.width);
                    }
                } else {
                    if (delta.width > 0) {
                        delta.width = Math.abs(delta.height);
                    } else {
                        delta.width = -Math.abs(delta.height);
                    }
                }
            } else {
                if (Math.abs(delta.width) > Math.abs(delta.height)) {
                    delta.height = 0;
                } else {
                    delta.width = 0;
                }
            }
        }
        Point moveDelta = new Point(delta.width, delta.height);

        SnapToHelper snapToHelper = (SnapToHelper) getConnectionEditPart().getAdapter(SnapToHelper.class);

        Rectangle rect = new Rectangle(originalLocation.x, originalLocation.y,
            1, 1);
        if (sourceRectangle == null) {
            sourceRectangle = new PrecisionRectangle(rect);
        }

        if (snapToHelper != null
            && !getCurrentInput().isModKeyDown(MODIFIER_NO_SNAPPING)) {
            PrecisionRectangle baseRect = sourceRectangle.getPreciseCopy();
            baseRect.translate(moveDelta);
            PrecisionPoint preciseDelta = new PrecisionPoint(moveDelta);
            snapToHelper.snapPoint(request, PositionConstants.HORIZONTAL
                | PositionConstants.VERTICAL,
                new PrecisionRectangle[] { baseRect }, preciseDelta);
            Point newLocation = originalLocation.getCopy().translate(
                preciseDelta);
            request.setLocation(newLocation);
        } else {
            request.setLocation(getLocation());
        }
    }

    /*
     * @see org.eclipse.gef.tools.AbstractTool#handleDragStarted()
     */
    @Override
    protected boolean handleDragStarted() {
        originalLocation = null;
        sourceRectangle = null;
        host.getViewer().getControl().setData(WorkflowEditor.DRAG_STATE_BENDPOINT, true);
        return super.handleDragStarted();
    }

    @Override
    protected void performDrag() {
        host.getViewer().getControl().setData(WorkflowEditor.DRAG_STATE_BENDPOINT, false);
        super.performDrag();
    }

}
