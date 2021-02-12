/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.objects;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.export.JacksonizedTypedDatumDeserializer;
import de.rcenvironment.core.datamanagement.export.JacksonizedTypedDatumSerializer;
import de.rcenvironment.core.datamanagement.export.matching.MatchResult;
import de.rcenvironment.core.datamanagement.export.matching.Matchable;
import de.rcenvironment.core.datamanagement.export.matching.Matcher;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Representation of an executed endpoint. Can be serialized using Jackson.
 * 
 * TODO It would be cleaner to introduce an additional PlainEndpointInstance to mimic the structure in de.rcenvironment.core.datamanagment.
 *
 * @author Tobias Brieden
 */
public class PlainEndpoint implements Comparable<PlainEndpoint>, Matchable<PlainEndpoint> {

    @JsonProperty
    private String name;

    @JsonSerialize(using = JacksonizedTypedDatumSerializer.class)
    @JsonDeserialize(using = JacksonizedTypedDatumDeserializer.class)
    private TypedDatum datum;

    @JsonProperty
    private Integer counter;

    // needed for Jackson deserialization
    public PlainEndpoint() {}

    public PlainEndpoint(EndpointData endpoint, TypedDatumSerializer typedDatumSerializer) {
        name = endpoint.getEndpointInstance().getEndpointName();
        counter = endpoint.getCounter();
        datum = typedDatumSerializer.deserialize(endpoint.getDatum());
    }

    // implemented to achieve an ordering of PlainEndpoints
    @Override
    public int compareTo(PlainEndpoint o) {
        if (name.equals(o.name)) {
            return this.counter.compareTo(o.counter);
        }
        return this.name.compareTo(o.name);
    }

    @Override
    public MatchResult matches(Map<DataType, Matcher> matchers, PlainEndpoint expected) {

        MatchResult result = new MatchResult();

        if (!name.equals(expected.name)) {
            result.addFailureCause(
                StringUtils.format("The actual endpoint name %s does not match the expected name %s.", name, expected.name));
        }
        
        if (!datum.getDataType().equals(expected.datum.getDataType())) {
            result.addFailureCause(
                StringUtils.format("The actual TypedDatum's type %s does not match the expected TypedDatum's type %s",
                    datum.getDataType().toString(), expected.datum.getDataType().toString()));
        }

        Matcher<TypedDatum> matcher = matchers.get(datum.getDataType());
        if (matcher == null) {
            throw new IllegalArgumentException(StringUtils.format("No matcher found for type %s", datum.getDataType().toString()));
        }
        MatchResult nestedMatchResult = matcher.matches(datum, expected.datum);

        if (!nestedMatchResult.hasMatched()) {
            result.addFailureCause(
                StringUtils.format("The actual TypedDatum %s does not match the expected TypedDatum %s", datum, expected.datum),
                nestedMatchResult);
        }

        if (!counter.equals(expected.counter)) {
            result.addFailureCause(
                StringUtils.format("The actual counter %d does not match the expected counter %d.", counter, expected.counter));
        }

        return result;
    }

    @Override
    public String toString() {
        return StringUtils.format("Endpoint(%s,%s)", name, datum);
    }
}
