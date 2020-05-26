/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import de.rcenvironment.core.gui.resources.api.ColorManager;

/**
 * Each Component has its own {@link TimelineComponentCanvas} showing the usage of the component during a workflow.
 * @author Hendrik Abbenhaus
 * 
 */
public class TimelineComponentCanvas extends Canvas implements PaintListener, MouseListener, MouseMoveListener {

    private TimelineActivityPart[] activities = null;

    private Date wfEndTime = null;

    private Date visibleStartTime = null;

    private Date visibleEndTime = null;

    public TimelineComponentCanvas(Composite parent, Date startTime, Date endTime) {
        super(parent, SWT.SINGLE);
        this.addPaintListener(this);
        this.addMouseMoveListener(this);
        this.setVisibleTimeArea(startTime, endTime);
    }

    @Override
    public void paintControl(PaintEvent e) {
        // get width of canvas
        int maxX = this.getSize().x;
        // getHeigh of canvass
        int maxY = this.getSize().y;

        e.gc.setBackground(ColorManager.getInstance().getSharedColor(TimelineViewConstants.CANVAS_COLOR_BACKGROUND));
        e.gc.fillRectangle(0, 0, maxX, maxY);
        e.gc.setForeground(ColorManager.getInstance().getSharedColor(TimelineViewConstants.CANVAS_COLOR_BACKGROUND_LINE));
        e.gc.drawLine(0, (int) (maxY / 2), maxX, (int) (maxY / 2));
        if (activities == null || activities.length == 0
            || visibleStartTime == null
            || visibleEndTime == null
            || wfEndTime == null) {
            return;
        }
        for (int i = 0; i < activities.length; i++) {
            TimelineActivityPart activity = activities[i];
            if (activity.getType() == null) {
                continue;
            }
            if (activity.getType().getColor() == null) {
                continue;
            }
            if (activity.getEndDate() == null) {
                activity.setEndtime(wfEndTime);
            }

            if (activity.getEndDate().before(this.visibleStartTime) || activity.getDate().after(this.visibleEndTime)) {
                continue;
            }
            e.gc.setBackground(new Color(null, activity.getType().getColor()));

            long startdraw =
                TimelineView.convertDateToPixel(activity.getDate(), maxX, visibleStartTime, visibleEndTime);
            long enddraw =
                TimelineView.convertDateToPixel(activity.getEndDate(), maxX, visibleStartTime, visibleEndTime)
                    - startdraw;

            e.gc.fillRectangle((int) startdraw, 3, (int) enddraw, maxY - 4);
        }

    }

    /**
     * Sets a new WorkflowEndTime.
     * @param newwfEndTime the new workflow end time
     */
    public void setWorkflowEndTime(Date newwfEndTime) {
        this.wfEndTime = newwfEndTime;
        this.redraw();
    }

    /**
     * Sets a new visible time area containing start and endTime.
     * @param startTime the current selected startTime
     * @param endTime the current selected endTime
     */
    public void setVisibleTimeArea(Date startTime, Date endTime) {
        this.visibleStartTime = startTime;
        this.visibleEndTime = endTime;
        this.redraw();
    }

    /**
     * Sets a new Content.
     * @param newActivities the activities
     */
    public void setActivities(TimelineActivityPart[] newActivities) {
        this.activities = newActivities;
        this.redraw();
    }

    /*
     * (non-Javadoc)
     * NO OP!
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
     */
    @Override
    public void mouseDoubleClick(MouseEvent arg0) {

    }

    /*
     * (non-Javadoc)
     * NO OP!
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
     */
    @Override
    public void mouseDown(MouseEvent arg0) {

    }

    /*
     * (non-Javadoc)
     * NO OP!
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
     */
    @Override
    public void mouseUp(MouseEvent arg0) {

    }

    /**
     * Refreshes the current tooltipText by getting the correct {@link TimelineActivityPart}.
     * 
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
     */
    @Override
    public void mouseMove(MouseEvent e) {
        String newToolTipText = "";
        long tmpdate =
            ((long) e.x * (this.visibleEndTime.getTime() - this.visibleStartTime.getTime()) / (long) this.getSize().x)
                + this.visibleStartTime.getTime();
        for (int i = 0; i < activities.length; i++) {
            TimelineActivityPart current = activities[i];
            if (current.getDate().getTime() < tmpdate) {
                newToolTipText = activities[i].getTooltipText(wfEndTime);
            }
        }
        this.setToolTipText(newToolTipText);
    }

}
