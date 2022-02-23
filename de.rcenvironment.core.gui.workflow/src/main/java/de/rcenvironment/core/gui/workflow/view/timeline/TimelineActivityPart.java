/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view.timeline;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Represents an object of a elemental activity area.
 * @author Hendrik Abbenhaus
 */
public class TimelineActivityPart {
    private String componentName = null;
    private TimelineActivityType type = null;
    private Date date = null;
    private String comment = "";
    private Date endtime = null;

    private String run;

    public TimelineActivityPart(String componentName, TimelineActivityType type, Date date, String comment) {
        this.componentName = componentName;
        this.date = date;
        this.type = type;
        this.comment = comment;
    }


    public TimelineActivityPart(String componentName, TimelineActivityType type, Date date, String run, String comment) {
        this.componentName = componentName;
        this.date = date;
        this.type = type;
        this.run = run;
        this.comment = comment;
    }

    /**
     * Returns the start time as a {@link Date} of this current activity area.
     * @return the time
     */
    public Date getDate() {
        return this.date;
    }
    
    public Date getEndDate(){
        return this.endtime;
    }


    /**
     * Returns the type as a {@link TimelineActivityType} of this current activity area.
     * @return the type
     */
    public TimelineActivityType getType() {
        return this.type;
    }
    
    /**
     * Returns the comment of an activity area.
     * @return the comment
     */
    public String getComment(){
        return this.comment;
    }
    
    /**
     * Returns a complete tooltip text of this activity area instance.
     * @param newWFEndTime the end time of workflow
     * @return a text for a tooltip
     */
    public String getTooltipText(Date newWFEndTime){
        String text = componentName + ": ";
        if (this.type != null){
            text += this.type.getDisplayName() + "\n";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        text += "Start: " + dateFormat.format(this.date) + "\n";
        
        Date currentCalcTime = this.endtime;
        if (currentCalcTime == null){
            currentCalcTime = newWFEndTime;
        }
        
        if (currentCalcTime != null){
            text += "End: " + dateFormat.format(currentCalcTime) + "\n";
            text += "Duration: " + getDurationText(currentCalcTime) + "\n";
        }
        if (run != null) {
            text += "Run: " + run + "\n";
        }
        if (this.comment != null && !this.comment.equals("")){
            text += "Comment: " + this.comment;
        }
        return text;
    }


    /**
     * Formatter beautifies input dates to readable Strings.
     * @param time The input date
     * @return a readable date string
     */
    public String getDurationText(Date time) {
        if (time ==  null){
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTime(this.date);

        long t1 = cal.getTimeInMillis();
        cal.setTime(time);
        
        return DurationFormatUtils.formatDurationHMS(Math.abs(cal.getTimeInMillis() - t1));
    }
    
    /**
     * Sets a new Endtime of this activity area instance.
     * @param newEndTime the current activity
     */
    public void setEndtime(Date newEndTime){
        this.endtime = newEndTime;
    }
    

}
