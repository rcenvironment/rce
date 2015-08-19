/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Identifier for an endpoint instance.
 * 
 * @author Jan Flink
 */
public class EndpointInstance implements Serializable {

    private static final long serialVersionUID = 6873708196676499699L;

    private final String endpointName;

    private final EndpointType endpointType;

    public EndpointInstance(String endpointName, EndpointType endpointType) {
        this.endpointName = endpointName;
        this.endpointType = endpointType;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }
}
