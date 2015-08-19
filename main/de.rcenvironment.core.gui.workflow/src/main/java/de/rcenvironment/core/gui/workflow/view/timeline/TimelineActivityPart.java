/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view.timeline;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * @author Hendrik Abbenhaus
 *
 */
public class TimelineActivityPart {
    private String componentName = null;
    private TimelineActivityType type = null;
    private Date date = null;
    private String comment = "";
    private Date endtime = null;

    /**
     * 
     */
    public TimelineActivityPart(String componentName, TimelineActivityType type, Date date, String comment) {
        this.componentName = componentName;
        this.date = date;
        this.type = type;
        this.comment = comment;
    }


     /**
     * @return the time
     */
    public Date getDate() {
        return this.date;
    }
    
    public Date getEndDate(){
        return this.endtime;
    }


    /**
     * @return the type
     */
    public TimelineActivityType getType() {
        return this.type;
    }
    
    
    public String getComment(){
        return this.comment;
    }
    
    /**
     * 
     * @param newWFEndTime the endtime of workflow
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
        if (this.comment != null && !this.comment.equals("")){
            text += "Comment: " + this.comment;
        }
        return text;
    }


    /**
     * 
     * @param time The endtimes.
     * @return the duration map
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
    
    public void setEndtime(Date newEndTime){
        this.endtime = newEndTime;
    }
    

}
