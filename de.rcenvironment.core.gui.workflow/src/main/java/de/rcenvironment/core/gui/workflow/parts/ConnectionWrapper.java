/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Class that encapsulates all connections between two nodes into one connection.
 * 
 * @author Heinrich Wendel
 */
public class ConnectionWrapper {

    /** paint a sourceArrow? */
    private boolean sourceArrow;

    /** paint a targetArrow? */
    private boolean targetArrow;

    /** The source node. */
    private WorkflowNode source;

    /** The target node. */
    private WorkflowNode target;

    /** The number of connections/channels contains in this wrapper. */
    private int numberOfConnections = 0;
    
    /**
     * Constructor.
     * @param source See above.
     * @param target See above.
     */
    public ConnectionWrapper(WorkflowNode source, WorkflowNode target) {
        this.source = source;
        this.target = target;
    }

    /**
     * 
     */
    public void incrementNumberOfConnections(){
        numberOfConnections++;
    }
    
    /**
     * Setter.
     * @param value Setter.
     */
    public void setSourceArrow(boolean value) {
        this.sourceArrow = value;
    }

    /**
     * Getter.
     * @return Getter.
     */
    public boolean getSourceArrow() {
        return sourceArrow;
    }

    /**
     * Setter.
     * @param value Setter.
     */
    public void setTargetArrow(boolean value) {
        this.targetArrow = value;
    }

    /**
     * Getter.
     * @return Getter.
     */
    public boolean getTargetArrow() {
        return targetArrow;
    }

    /**
     * Getter.
     * @return Getter.
     */
    public WorkflowNode getSource() {
        return source;
    }

    /**
     * Getter.
     * @return Getter.
     */
    public WorkflowNode getTarget() {
        return target;
    }

    
    /**
     * 
     * @return Number of connections/channel within this wrapper.
     */
    public int getNumberOfConnections() {
        return numberOfConnections;
    }

}
