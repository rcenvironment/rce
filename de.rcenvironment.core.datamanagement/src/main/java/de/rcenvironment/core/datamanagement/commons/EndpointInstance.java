/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    private final Map<String, String> metaData;

    public EndpointInstance(String endpointName, EndpointType endpointType) {
        this(endpointName, endpointType, new HashMap<String, String>());
    }

    public EndpointInstance(String endpointName, EndpointType endpointType, Map<String, String> metaData) {
        this.endpointName = endpointName;
        this.endpointType = endpointType;
        this.metaData = metaData;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }
    
    public Map<String, String> getMetaData() {
        return metaData;
    }
    
}
