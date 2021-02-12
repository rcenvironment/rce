/*
 * Copyright 2006-2021 DLR, Germany
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

import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.export.matching.MatchResult;
import de.rcenvironment.core.datamanagement.export.matching.Matchable;
import de.rcenvironment.core.datamanagement.export.matching.Matcher;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Representation of an executed component instance. Can be serialized using Jackson.
 *
 * @author Tobias Brieden
 */
public class PlainComponentInstance implements Comparable<PlainComponentInstance>, Matchable<PlainComponentInstance> {

    @JsonProperty("component")
    private String id;

    // The name of a component needs to be unique within a workflow. Therefore, this property is used to for ordering.
    @JsonProperty
    private String name;

    @JsonProperty("runs")
    private List<PlainComponentRun> plainComponentRuns;

    // needed for Jackson deserialization
    public PlainComponentInstance() {}

    public PlainComponentInstance(ComponentInstance instance, Set<ComponentRun> componentRuns, TypedDatumSerializer typedDatumSerializer) {

        id = instance.getComponentID();
        name = instance.getComponentInstanceName();

        plainComponentRuns = new LinkedList<>();
        for (ComponentRun componentRun : componentRuns) {
            plainComponentRuns.add(new PlainComponentRun(componentRun, typedDatumSerializer));
        }
        // the list needs to be sorted to be easily comparable
        plainComponentRuns.sort(null);
    }

    // implemented to achieve an ordering of PlainComponentInstances
    @Override
    public int compareTo(PlainComponentInstance o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public MatchResult matches(Map<DataType, Matcher> matchers, PlainComponentInstance expected) {

        MatchResult result = new MatchResult();

        if (!id.equals(expected.id)) {
            result.addFailureCause(StringUtils.format("The id %s does not match %s", id, expected.id));
        }

        if (!name.equals(expected.name)) {
            result.addFailureCause(StringUtils.format("The name %s does not match %s", name, expected.name));
        }

        if (plainComponentRuns.size() == expected.plainComponentRuns.size()) {
            for (int i = 0; i < plainComponentRuns.size(); i++) {
                MatchResult nestedMatchResult = plainComponentRuns.get(i).matches(matchers, expected.plainComponentRuns.get(i));

                if (!nestedMatchResult.hasMatched()) {
                    result.addFailureCause(StringUtils.format("Component run %d does not match", i), nestedMatchResult);
                }
            }
        } else {
            result.addFailureCause(
                StringUtils.format("The number of runs %d does not match the expected number of runs %d", plainComponentRuns.size(),
                    expected.plainComponentRuns.size()));
        }

        return result;
    }

    public String getName() {
        return name;
    }
}
