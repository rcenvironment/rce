/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view.timeline;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import de.rcenvironment.core.gui.resources.api.ColorManager;
import de.rcenvironment.core.gui.resources.api.StandardColors;

/**
 * The complete navigation overview control.
 * 
 * @author Hendrik Abbenhaus
 * 
 */
public class TimelineNavigationControl extends Canvas implements PaintListener, MouseMoveListener, MouseListener {

    /**
     * 
     * 
     * @author Hendrik Abbenhaus
     */
    private enum POSITION {
        LEFT, RIGHT, MIDDLE, NONE
    }

    private boolean mouseDown = false;

    private final List<AreaChangedListener> areaListeners = new ArrayList<AreaChangedListener>();

    private TimelineComponentRow[] allrows = null;

    private Date workflowStartTime = null;

    private Date workflowEndTime = null;

    private Date visibleStartTime = null;

    private Date visibleEndTime = null;

    private final int sliderLineWidth = 2;

    private POSITION currentChoosedPosition = POSITION.NONE;

    private int startPositionBuffer = 0;

    private final int accuracy = 6;

    private final double timeTextFactor = 5.3;

    private int timeTextWidth;

    private final int defaultTextHeight = 15;

    public TimelineNavigationControl(Composite parent) {
        super(parent, SWT.SINGLE);
        this.addPaintListener(this);
        this.addMouseMoveListener(this);
        this.addMouseListener(this);
        FontData fontData = getFont().getFontData()[0];
        timeTextWidth = (int) (timeTextFactor * fontData.getHeight());
    }

    public Date getVisibleStartTime() {
        return visibleStartTime;
    }

    public Date getVisibleEndTime() {
        return visibleEndTime;
    }

    @Override
    public void paintControl(PaintEvent e) {
        if (this.workflowStartTime == null
            || this.workflowEndTime == null
            || this.allrows == null || this.allrows.length == 0) {
            this.setEnabled(false);
            return;
        }
        this.setEnabled(true);

        // get width of canvas
        int canvasSizeX = this.getSize().x;
        // getHeigh of canvass
        int canvasSizeY = this.getSize().y;

        if (allrows != null && allrows.length != 0) {
            int rowhigh = (int) ((float) canvasSizeY / (float) allrows.length);
            for (int i = 0; i < allrows.length; i++) {
                TimelineComponentRow currentrow = allrows[i];
                for (int j = 0; j < currentrow.getActivities().length; j++) {
                    TimelineActivityPart currentactivity = currentrow.getActivities()[j];
                    if (currentactivity.getType() == null) {
                        continue;
                    }
                    if (currentactivity.getType().getColor() == null) {
                        continue;
                    }
                    if (currentactivity.getEndDate() == null) {
                        currentactivity.setEndtime(workflowEndTime);
                    }
                    // FIXME: resource leak: The color object is never disposed!
                    e.gc.setBackground(new Color(null, currentactivity.getType().getPreviewColor()));

                    long startdraw =
                        TimelineView.convertDateToPixel(currentactivity.getDate(), canvasSizeX, workflowStartTime, workflowEndTime);
                    long enddraw =
                        TimelineView.convertDateToPixel(currentactivity.getEndDate(), canvasSizeX, workflowStartTime, workflowEndTime)
                            - startdraw;
                    int ystart = (i * rowhigh);
                    int yend = ((i + 1) * rowhigh) - ystart;
                    e.gc.fillRectangle((int) Math.floor(startdraw), ystart, (int) Math.floor(enddraw), yend);
                }
            }
            if (visibleStartTime == null || visibleEndTime == null) {
                return;
            }

            int sliderPositionleft = TimelineView.convertDateToPixel(
                visibleStartTime, canvasSizeX - (this.sliderLineWidth),
                this.workflowStartTime, this.workflowEndTime)
                + (this.sliderLineWidth / 2);
            int sliderPositionRight = TimelineView.convertDateToPixel(
                visibleEndTime, canvasSizeX - (this.sliderLineWidth),
                this.workflowStartTime, this.workflowEndTime)
                + (this.sliderLineWidth / 2);

            if (!mouseDown) {
                e.gc.setAlpha(TimelineViewConstants.CANVAS_SELECTION_AREA_OPACITY);
                e.gc.setBackground(ColorManager.getInstance().getSharedColor(TimelineViewConstants.CANVAS_COLOR_SELECTION_AREA));
                e.gc.fillRectangle(sliderPositionleft, -canvasSizeY,
                    sliderPositionRight - sliderPositionleft, 2 * canvasSizeY + sliderLineWidth / 2);
            }
            e.gc.setAlpha(TimelineViewConstants.CANVAS_DEFAULT_OPACITY);
            e.gc.setForeground(ColorManager.getInstance().getSharedColor(StandardColors.RCE_BLACK));
            e.gc.setLineWidth(sliderLineWidth);
            e.gc.drawRectangle(sliderPositionleft, -canvasSizeY,
                sliderPositionRight - sliderPositionleft, 2 * canvasSizeY + sliderLineWidth / 2);

            DateFormat dfmt = new SimpleDateFormat("dd.MM.yy");
            int span = 0;
            if (dfmt.format(visibleStartTime).equals(dfmt.format(visibleEndTime))) {
                dfmt = new SimpleDateFormat("HH:mm:ss");
                span = defaultTextHeight / 2;
            } else {
                dfmt = new SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss");
                span = defaultTextHeight;
            }
            e.gc.drawText(dfmt.format(visibleStartTime), sliderPositionleft + sliderLineWidth, canvasSizeY / 2 - span, true);
            e.gc.drawText(
                dfmt.format(visibleEndTime), sliderPositionRight - sliderLineWidth - timeTextWidth, canvasSizeY / 2 - span, true);
        }
    }

    /**
     * Sets a new Workflow time area.
     * 
     * @param newWFEndTime the new endtime
     * @param newWFStartTime the new starttime
     */
    public void setWorflowStartEndTime(Date newWFStartTime, Date newWFEndTime) {
        this.workflowEndTime = newWFEndTime;
        this.workflowStartTime = newWFStartTime;
        this.redraw();
    }

    /**
     * Sets a new visible time area.
     * 
     * @param startVisibleTime The beginning of visibility
     * @param endVisibleTime The end of visibility
     */
    public void setVisibleArea(Date startVisibleTime, Date endVisibleTime) {
        this.visibleStartTime = startVisibleTime;
        this.visibleEndTime = endVisibleTime;
        this.redraw();
    }

    /**
     * Sets the content.
     * 
     * @param rows the rows
     */
    public void setTimeTableComponentRows(TimelineComponentRow[] rows) {
        this.allrows = rows;
        redraw();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
     */
    @Override
    public void mouseMove(MouseEvent e) {
        if (visibleStartTime == null || visibleEndTime == null) {
            return;
        }
        Cursor cursor = null;
        int sliderPositionleft = TimelineView.convertDateToPixel(
            visibleStartTime, this.getSize().x - this.sliderLineWidth,
            this.workflowStartTime, this.workflowEndTime) + (this.sliderLineWidth / 2);
        int sliderPositionRight = TimelineView.convertDateToPixel(
            visibleEndTime, this.getSize().x - this.sliderLineWidth,
            this.workflowStartTime, this.workflowEndTime) + (this.sliderLineWidth / 2);

        if ((e.x > (sliderPositionleft - (accuracy * sliderLineWidth)) && e.x < (sliderPositionleft + (accuracy * sliderLineWidth)))
            || ((e.x > (sliderPositionRight - (accuracy * sliderLineWidth)) && e.x < (sliderPositionRight + (accuracy
            * sliderLineWidth))))
            || (currentChoosedPosition == POSITION.LEFT) || (currentChoosedPosition == POSITION.RIGHT)) {

            cursor = new Cursor(this.getDisplay(), SWT.CURSOR_SIZEWE);
        } else if (e.x > sliderPositionleft && e.x < sliderPositionRight) {
            cursor = new Cursor(this.getDisplay(), SWT.CURSOR_SIZEALL);
        }
        if (currentChoosedPosition == POSITION.NONE) {
            this.setCursor(cursor);
        }
        setNewPosition(e.x, currentChoosedPosition);
    }

    @Override
    public void mouseDoubleClick(MouseEvent e) {

    }

    private void setNewPosition(int x, POSITION position) {
        if (visibleStartTime == null || visibleEndTime == null) {
            return;
        }
        int sliderPositionleft = TimelineView.convertDateToPixel(
            visibleStartTime, this.getSize().x - this.sliderLineWidth,
            this.workflowStartTime, this.workflowEndTime) + (this.sliderLineWidth / 2);
        int sliderPositionRight = TimelineView.convertDateToPixel(
            visibleEndTime, this.getSize().x - this.sliderLineWidth,
            this.workflowStartTime, this.workflowEndTime) + (this.sliderLineWidth / 2);
        switch (position) {
        case LEFT:
            // do not change position with right slider
            if (x - (this.sliderLineWidth / 2) >= sliderPositionRight - sliderLineWidth) {
                return;
            }
            // do not get out of space
            if (x - (this.sliderLineWidth / 2) < 0) {
                x = (this.sliderLineWidth / 2);
            }
            visibleStartTime = TimelineView.convertPixelToDate(
                x - (this.sliderLineWidth / 2), this.getSize().x - this.sliderLineWidth, this.workflowStartTime, this.workflowEndTime);
            break;
        case RIGHT:
            // do not change position with left slider
            if (x - (this.sliderLineWidth / 2) <= sliderPositionleft + sliderLineWidth) {
                return;
            }
            // do not go out of the space
            if (x - (this.sliderLineWidth / 2) > this.getSize().x) {
                x = this.getSize().x - (sliderLineWidth / 2);
            }
            visibleEndTime = TimelineView.convertPixelToDate(
                x - (this.sliderLineWidth / 2), this.getSize().x - this.sliderLineWidth, this.workflowStartTime, this.workflowEndTime);

            break;
        case MIDDLE:
            int newsliderPositionRight = sliderPositionRight + (x - this.sliderLineWidth / 2 - startPositionBuffer);
            int newsliderPositionleft = sliderPositionleft + (x - this.sliderLineWidth / 2 - startPositionBuffer);

            if (newsliderPositionRight > this.getSize().x - sliderLineWidth || newsliderPositionleft < 0 - 1) {
                return;
            }
            visibleStartTime = TimelineView.convertPixelToDate(
                newsliderPositionleft + (this.sliderLineWidth / 2), this.getSize().x - this.sliderLineWidth,
                this.workflowStartTime, this.workflowEndTime);
            visibleEndTime = TimelineView.convertPixelToDate(
                newsliderPositionRight + (this.sliderLineWidth / 2), this.getSize().x - this.sliderLineWidth,
                this.workflowStartTime, this.workflowEndTime);
            startPositionBuffer = x;
            break;
        default:
            // do not have to redraw, if nothing is selected
            return;
        }

        this.redraw();
        notifyAreaChangeListener();
    }

    @Override
    public void mouseDown(MouseEvent e) {
        if (visibleStartTime == null || visibleEndTime == null) {
            return;
        }
        int sliderPositionleft = TimelineView.convertDateToPixel(
            visibleStartTime, this.getSize().x - this.sliderLineWidth,
            this.workflowStartTime, this.workflowEndTime) - (this.sliderLineWidth / 2);
        int sliderPositionRight = TimelineView.convertDateToPixel(
            visibleEndTime, this.getSize().x - this.sliderLineWidth,
            this.workflowStartTime, this.workflowEndTime) - (this.sliderLineWidth / 2);
        if (e.x > (sliderPositionleft - (accuracy * sliderLineWidth)) && e.x < (sliderPositionleft + (accuracy * sliderLineWidth))) {
            currentChoosedPosition = POSITION.LEFT;
        } else if (e.x > (sliderPositionRight - (accuracy * sliderLineWidth))
            && e.x < (sliderPositionRight + (accuracy * sliderLineWidth))) {
            currentChoosedPosition = POSITION.RIGHT;
        } else if (e.x > sliderPositionleft && e.x < sliderPositionRight) {
            currentChoosedPosition = POSITION.MIDDLE;
            startPositionBuffer = e.x;
        } else {
            currentChoosedPosition = POSITION.NONE;
        }
        this.mouseDown = true;
    }

    @Override
    public void mouseUp(MouseEvent e) {
        setNewPosition(e.x, currentChoosedPosition);
        currentChoosedPosition = POSITION.NONE;
        this.mouseDown = false;
        this.redraw();
    }

    /**
     * 
     *
     */
    public void notifyAreaChangeListener() {
        for (AreaChangedListener hl : areaListeners) {
            hl.selectedAreaChanged(visibleStartTime, visibleEndTime);
        }
    }

    /**
     * Add {@link AreaChangedListener} to the List of Listener.
     * 
     * @param newListener the new listener
     */
    public void addAreaChangeListener(AreaChangedListener newListener) {
        areaListeners.add(newListener);
    }

}

/**
 * Interface contains area changed events.
 * 
 * @author Hendrik Abbenhaus
 */
interface AreaChangedListener {

    void selectedAreaChanged(Date selectedStartTime, Date selectedEndTime);
}
