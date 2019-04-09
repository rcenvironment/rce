/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.TimelineIntervalType;

/**
 * Identifier for a timeline interval of a component run.
 * 
 * @author Jan Flink
 */
public class ComponentRunInterval extends TimelineInterval implements Serializable {

    private static final long serialVersionUID = 8007213873078241795L;

    private final String componentID;

    private final String componentInstanceName;

    public ComponentRunInterval(String componentID, String componentInstanceName, TimelineIntervalType type, Long starttime, Long endtime) {
        super(type, starttime, endtime);
        this.componentID = componentID;
        this.componentInstanceName = componentInstanceName;
    }

    public String getComponentID() {
        return componentID;
    }

    public String getComponentInstanceName() {
        return componentInstanceName;
    }
}
