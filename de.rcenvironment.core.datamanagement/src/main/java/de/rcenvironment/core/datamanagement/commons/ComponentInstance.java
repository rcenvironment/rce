/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

/**
 * Identifier for a component instance.
 * 
 * @author Jan Flink
 */
public class ComponentInstance implements Serializable, Comparable<ComponentInstance> {

    private static final long serialVersionUID = -1146463727557514649L;

    private final String componentExecutionID;

    private final String componentID;

    private final String componentInstanceName;

    private final String finalState;

    public ComponentInstance(String componentID, String componentInstanceName, String finalState) {
        this.componentExecutionID = null;
        this.componentID = componentID;
        this.componentInstanceName = componentInstanceName;
        this.finalState = finalState;
    }

    public ComponentInstance(String componentExecutionID, String componentID, String componentInstanceName, String finalState) {
        this.componentExecutionID = componentExecutionID;
        this.componentID = componentID;
        this.componentInstanceName = componentInstanceName;
        this.finalState = finalState;
    }

    public String getComponentID() {
        return componentID;
    }

    public String getComponentInstanceName() {
        return componentInstanceName;
    }

    public String getComponentExecutionID() {
        return componentExecutionID;
    }

    public String getFinalState() {
        return finalState;
    }

    @Override
    public int compareTo(ComponentInstance arg0) {
        return getComponentInstanceName().compareTo(arg0.getComponentInstanceName());
    }

    @Override
    public int hashCode() {
        return componentID.hashCode() + componentInstanceName.hashCode();

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ComponentInstance other = (ComponentInstance) obj;
        return getComponentID().equals(other.getComponentID()) && getComponentInstanceName().equals(other.getComponentInstanceName());
    }

}
