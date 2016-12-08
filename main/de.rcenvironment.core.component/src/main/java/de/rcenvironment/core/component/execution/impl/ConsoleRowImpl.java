/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.impl;

import java.text.SimpleDateFormat;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.utils.common.ComparatorUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link ConsoleRow}.
 * 
 * @author Doreen Seider
 */
public class ConsoleRowImpl implements ConsoleRow {

    private static final long serialVersionUID = 5725183929182175975L;

    private String workflowIdentifier;

    private String componentIdentifier;

    private String workflowName;

    private String componentName;

    private Type type;

    private String payload;
    
    private long index;

    private long timestamp;
    
    private int componentRun;
    
    private long sequenceNumber;
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public String getWorkflowIdentifier() {
        return workflowIdentifier;
    }

    @Override
    public String getComponentIdentifier() {
        return componentIdentifier;
    }

    @Override
    public String getWorkflowName() {
        return workflowName;
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getPayload() {
        return payload;
    }
    
    @Override
    public int getComponentRun() {
        return componentRun;
    }
    
    @Override
    public long getSequenzNumber() {
        return sequenceNumber;
    }
    
    public void setWorkflowIdentifier(String workflowIdentifier) {
        this.workflowIdentifier = workflowIdentifier;
    }

    
    public void setComponentIdentifier(String componentIdentifier) {
        this.componentIdentifier = componentIdentifier;
    }

    
    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    /**
     * Sets the payload of the {@link ConsoleRow}.
     * 
     * @param payload payload to set
     */
    public void setPayload(String payload) {
        // cleanup step 1: replace all sequences of "special" characters with single spaces
        payload = payload.replaceAll("[\\n\\r\\f\\a\\e\\x00]+", " ");
        // cleanup step 2: replace tabs with a fixed number of spaces (4 for now)
        this.payload = payload.replaceAll("\\t", "    ");
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public void setComponentRun(int componentRun) {
        this.componentRun = componentRun;
    }
    
    @Override
    public void setIndex(long index) {
        this.index = index;
    }

    @Override
    public int compareTo(ConsoleRow o) {
        final int equal = 0;

        int compareResult = ComparatorUtils.compareLong(timestamp, o.getTimestamp());
        if (compareResult == equal) {
            compareResult = ComparatorUtils.compareLong(index, o.getIndex());
            if (compareResult == equal) {
                compareResult = workflowIdentifier.compareTo(o.getWorkflowIdentifier());
                if (compareResult == equal) {
                    compareResult = componentIdentifier.compareTo(o.getComponentIdentifier());
                    if (compareResult == equal) {
                        compareResult = payload.compareTo(o.getPayload());
                    }
                }
            }
        }
        return compareResult;
    }
    
    @Override
    public String toString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH:mm:ss,SSS");
        return StringUtils.format("%s: %s - %s (%s@%s)", df.format(timestamp), type.toString(), payload, componentName, workflowName);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConsoleRow) {
            return compareTo((ConsoleRow) o) == 0;
        }
        return false;
    }

}
