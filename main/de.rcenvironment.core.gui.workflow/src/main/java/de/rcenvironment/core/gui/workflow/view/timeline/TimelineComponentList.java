/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.timeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Represents a Base {@link ScrolledComposite} for a complete List of {@link TimlineComponentRows}.
 *
 * @author Hendrik Abbenhaus
 */
public class TimelineComponentList extends ScrolledComposite implements ControlListener {
    
    private List<ResizeListener> resizeListener = new ArrayList<ResizeListener>();

    private Composite left = null;

    private Composite right = null;

    private SashForm list = null;
    
    private Map<Label, String> oldTextBuffer = new HashMap<>();

    public TimelineComponentList(Composite parent) {
        super(parent, SWT.V_SCROLL);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalSpan = 3;
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessVerticalSpace = true;

        this.list = new SashForm(this, SWT.HORIZONTAL);
        this.list.setLayoutData(gridData);

        this.setContent(this.list);
        this.setAlwaysShowScrollBars(true);
        this.setExpandVertical(true);
        this.setExpandHorizontal(true);
        this.addControlListener(this);
        this.setShowFocusedControl(true);

        FormLayout form = new FormLayout();
        this.setLayout(form);

        this.left = new Composite(list, SWT.NONE);
        this.left.addControlListener(this);
        GridLayout gridLayout = new GridLayout(1, false);
        left.setLayout(gridLayout);

        this.right = new Composite(list, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        right.setLayout(gridLayout);
        this.setWeights(new int[] { 1, 7 });
    }

    /**
     * Sets new Weights.
     * @param weights the weights
     */
    public void setWeights(int[] weights) {
        this.list.setWeights(weights);
        //checkLabelabbreviate();
    }

    public int[] getWeights() {
        return this.list.getWeights();
    }

    // TODO fix label abbreaviation not to use pack() as it can cause problems
//    private void checkLabelabbreviate() {
//        //on first start the left Composite is not created and has no Size. Then do not short anything!
//        if (this.left.getSize().x < 1){
//            return;
//        }
//        for (Object child : this.left.getChildren()) {
//            if (child instanceof Label) {
//                Label current = (Label) child;
//                //if the current Label 
//                if (!current.getText().equals("")) {
//                    //set to original text
//                    if (oldTextBuffer.containsKey(current)){
//                        current.setText(oldTextBuffer.get(current));
//                        current.getParent().layout();
//                        current.pack();
//                    }
//                    abbreviateLabel(current, current.getText());
//                }
//            }
//        }
//    }

//    private void abbreviateLabel(Label label, String labelText) {
//        if (label.getSize().x < 1 || label.getSize().y < 1 || labelText.length() < 6){
//            return;
//        }
//        //IconLabelSizeX + NameLabelSizeY + x (multiple border width) > WholeLeftComposite
//        if (label.getSize().x + TimelineViewConstants.CANVAS_DEFAULT_HEIGHT_HINT + 10 > this.left.getSize().x){
//            //add Name, if there is no entry. Only the first "put" sets the complete name
//            if (!oldTextBuffer.containsKey(label)){
//                oldTextBuffer.put(label, label.getText());
//            }
//            String shorterLabelText = "";
//            shorterLabelText = StringUtils.abbreviateMiddle(labelText, "...", labelText.length() - 1);
//            label.setText(shorterLabelText);
//            //set the new size of the label. Otherwise it will always be the shortest String
//            label.pack();
//            abbreviateLabel(label, shorterLabelText);
//        }
//    }

    /**
     * Adds a {@link TimelineComponentRow} to the current List.
     * @param row contains the row
     */
    public void addComponentRow(TimelineComponentRow row) {
        final String tooltipText = row.getName() + " - " + TimelineView.getComponentNameFromId(row.getComponentID(), this);
       
        CLabel nameLabel = new CLabel(this.left, SWT.NONE);
        nameLabel.setImage(row.getIcon());
        nameLabel.setToolTipText(tooltipText);
        nameLabel.setText(row.getName());
        nameLabel.setBackground(getBackground());
        // remove margins to beautify layout
        nameLabel.setMargins(nameLabel.getLeftMargin(), 0, nameLabel.getRightMargin(), 0);
        int labelHeight = nameLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.heightHint = (labelHeight);

        TimelineComponentCanvas canvas = new TimelineComponentCanvas(this.right, row.getVisibleStartTime(), row.getVisibleEndTime());
        canvas.setLayoutData(gridData);
        canvas.setWorkflowEndTime(row.getWorkflowEndTime());
        canvas.setActivities(row.getActivities());
    }

    /**
     * Deletes all {@link Label} and {@link TimelineComponentCanvas} elements and clears the oldTextBuffer.
     */
    public void clear() {
        for (Control current : this.left.getChildren()) {
            current.dispose();
            if (current instanceof Label) {
                current.dispose();
            }
        }
        for (Control current : this.right.getChildren()) {
            if (current instanceof TimelineComponentCanvas) {
                current.dispose();
            }
        }
        this.left.layout();
        this.right.layout();
        this.oldTextBuffer.clear();
    }

    /**
     * Sets a new visible time area and carry this over to all {@link TimelineComponentCanvas} .
     * @param startTime the current startTime
     * @param endTime the current endTime
     */
    public void setTimeArea(Date startTime, Date endTime) {
        for (Control currentRow : right.getChildren()) {
            if (currentRow instanceof TimelineComponentCanvas) {
                TimelineComponentCanvas currentCanvas = (TimelineComponentCanvas) currentRow;
                currentCanvas.setVisibleTimeArea(startTime, endTime);
            }
        }
    }

    /**
     * Clears the view an sets an new {@link Array} of {@link TimelineComponentRows} to show.
     * @param rows the rows
     */
    public void setTimeTableComponentRows(TimelineComponentRow[] rows) {
        //first clean all
        this.clear();
        //if there are no rows, do nothing
        if (rows == null || rows.length == 0) {
            return;
        }
        List<TimelineComponentRow> sortedRows = Arrays.asList(rows);
        Collections.sort(sortedRows);
        for (TimelineComponentRow currentRow : sortedRows) {
            addComponentRow(currentRow);
        }
        this.left.layout();
        this.right.layout();

    }

    @Override
    public void controlMoved(ControlEvent arg0) {

    }

    @Override
    public void controlResized(ControlEvent arg0) {
//        checkLabelabbreviate();
        if (arg0.getSource().equals(this)) {
            Rectangle r = this.getClientArea();
            this.setMinSize(this.list.computeSize(r.width, SWT.DEFAULT));
        } else {
            notifyResizeListener();
        }
        this.redraw();

    }

    /**
     * Adds a {@link ResizeListener} to collection.
     * @param a a new listener
     */
    public void addResizeListener(ResizeListener a) {
        resizeListener.add(a);
    }

    /**
     * Notifies all connected {@link ResizeListener}.
     */
    public void notifyResizeListener() {
        for (ResizeListener current : resizeListener) {
            current.resized();
        }
    }
    
    /**
     * Setting a new background-color in each area.
     * @param color the new background-color
     */
    @Override
    public void setBackground(Color color){
        super.setBackground(color);
        this.left.setBackground(color);
        this.right.setBackground(color);
    }

}

/**
 * Base for notify via a Listener when resized.
 * @author Hendrik Abbenhaus
 */
interface ResizeListener {
    void resized();
}
