/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

/**
 * Identifier for an end point data object.
 * 
 * @author Jan Flink
 */
public class EndpointData implements Serializable, Comparable<EndpointData> {

    private static final long serialVersionUID = -5420792048630146556L;

    private final EndpointInstance endpointInstance;

    private final Integer counter;

    private final String serializedDatum;

    public EndpointData(EndpointInstance endpointInstance, Integer count, String typedDatum) {
        this.endpointInstance = endpointInstance;
        this.counter = count;
        this.serializedDatum = typedDatum;
    }

    public Integer getCounter() {
        return counter;
    }

    public String getDatum() {
        return serializedDatum;
    }

    public EndpointInstance getEndpointInstance() {
        return endpointInstance;
    }

    @Override
    public int compareTo(EndpointData endpointData) {
        if (endpointInstance.getEndpointName().equals(endpointData.getEndpointInstance().getEndpointName())) {
            return counter.compareTo(endpointData.getCounter());
        }
        return endpointInstance.getEndpointName().compareTo(endpointData.getEndpointInstance().getEndpointName());
    }
}
