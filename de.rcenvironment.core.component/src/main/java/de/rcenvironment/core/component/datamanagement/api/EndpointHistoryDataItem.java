/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.datamanagement.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Encapsulate information of an endpoint, which is needed if the endpoint is part of an {@link ComponentHistoryDataItem}.
 * 
 * @author Doreen Seider
 */
public class EndpointHistoryDataItem {

    private long timestamp;

    private String endpointName;

    private TypedDatum value;

    private Integer counter;

    public EndpointHistoryDataItem(long timestamp, String endpointName, TypedDatum value) {
        this.timestamp = timestamp;
        this.endpointName = endpointName;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public TypedDatum getValue() {
        return value;
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }
}
