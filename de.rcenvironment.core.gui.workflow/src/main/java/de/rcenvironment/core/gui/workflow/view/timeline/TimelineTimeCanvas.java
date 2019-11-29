/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Each {@link TimelineComponentRow} get its own Canvas.
 * @author Hendrik Abbenhaus
 *
 */
public class TimelineTimeCanvas extends Canvas implements PaintListener {

    private Date visibleStartTime = null;
    private Date visibleEndTime = null;


    public TimelineTimeCanvas(Composite parent) {
        super(parent, SWT.BORDER);
        this.addPaintListener(this);
        
    }

    @Override
    public void paintControl(PaintEvent e) {
        // get width of canvas
        int canvasSizeX = this.getSize().x;
        // getHeigh of canvass
        int canvasSizeY = this.getSize().y;
       
        e.gc.drawLine(0, canvasSizeY/2, canvasSizeX, canvasSizeY/2);
        
        if (visibleStartTime == null || visibleEndTime == null){
            return;
        }
        
    }
    
    /**
     * Sets visible area.
     * @param startVisibleTime The beginning of visibility
     * @param endVisibleTime The end of visibility
     */
    public void setVisibleArea(Date startVisibleTime, Date endVisibleTime) {
        this.visibleStartTime = startVisibleTime;
        this.visibleEndTime = endVisibleTime;
        this.redraw();
    }
    
    


}
