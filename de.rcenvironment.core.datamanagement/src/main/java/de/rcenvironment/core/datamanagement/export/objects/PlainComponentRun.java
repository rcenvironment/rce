/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.export.matching.MatchResult;
import de.rcenvironment.core.datamanagement.export.matching.Matchable;
import de.rcenvironment.core.datamanagement.export.matching.Matcher;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Representation of an executed component run. Can be serialized using Jackson.
 *
 * @author Tobias Brieden
 */
public class PlainComponentRun implements Comparable<PlainComponentRun>, Matchable<PlainComponentRun> {

    @JsonProperty
    private Integer runCounter;

    @JsonProperty
    private FinalComponentRunState finalState;

    @JsonProperty
    private List<PlainEndpoint> inputs;

    @JsonProperty
    private List<PlainEndpoint> outputs;

    // needed for Jackson deserialization
    public PlainComponentRun() {}

    public PlainComponentRun(ComponentRun componentRun, TypedDatumSerializer typedDatumSerializer) {
        this.finalState = componentRun.getFinalState();
        this.runCounter = componentRun.getRunCounter();

        inputs = new LinkedList<>();
        outputs = new LinkedList<>();

        // TODO why does this return a set instead of a list? The order of the list should be defined by the arrival of the components at
        // the input/output. This would allow to remove the sorting step.
        Set<EndpointData> endpoints = componentRun.getEndpointData();
        if (endpoints != null) {
            for (EndpointData endpoint : endpoints) {

                switch (endpoint.getEndpointInstance().getEndpointType()) {
                case INPUT:
                    inputs.add(new PlainEndpoint(endpoint, typedDatumSerializer));
                    break;
                case OUTPUT:
                    outputs.add(new PlainEndpoint(endpoint, typedDatumSerializer));
                    break;
                default:
                    // TODO log error
                    break;
                }

            }
            // the lists needs to be sorted to be easily comparable
            inputs.sort(null);
            outputs.sort(null);
        }
    }

    // implemented to achieve an ordering of PlainComponentRuns
    @Override
    public int compareTo(PlainComponentRun o) {
        return this.runCounter.compareTo(o.runCounter);
    }

    @Override
    public MatchResult matches(Map<DataType, Matcher> matchers, PlainComponentRun expected) {
        MatchResult result = new MatchResult();

        if (!runCounter.equals(expected.runCounter)) {
            result.addFailureCause(StringUtils.format("The run counter %d does not match %d.", runCounter, expected.runCounter));
        }

        if (!finalState.equals(expected.finalState)) {
            result.addFailureCause(
                StringUtils.format("The final state %s does not match the expected final state %s.", finalState, expected.finalState));
        }

        matchEndpointDatums(matchers, result, inputs, expected.inputs, "input");
        matchEndpointDatums(matchers, result, outputs, expected.outputs, "output");

        return result;
    }

    private MatchResult matchEndpointDatums(Map<DataType, Matcher> matchers,
        MatchResult result, List<PlainEndpoint> actual, List<PlainEndpoint> expected, String typeAsString) {

        if (actual.size() == expected.size()) {
            for (int i = 0; i < actual.size(); i++) {
                MatchResult nestedMatchResult = actual.get(i).matches(matchers, expected.get(i));

                if (!nestedMatchResult.hasMatched()) {
                    result.addFailureCause(StringUtils.format("The actual %s datum %d does not match the expected %s datum",
                        typeAsString, i, typeAsString), nestedMatchResult);
                }
            }
        } else {
            result.addFailureCause(
                StringUtils.format("The number of %s datums %d do not match the expected number of %s datums %d",
                    typeAsString, actual.size(), typeAsString, expected.size()));
        }

        return result;
    }
}
