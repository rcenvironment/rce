/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.export.objects;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.export.matching.MatchResult;
import de.rcenvironment.core.datamanagement.export.matching.Matchable;
import de.rcenvironment.core.datamanagement.export.matching.Matcher;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Representation of an executed workflow run. Can be serialized using Jackson.
 * 
 * TODO We should check if we unify this structure with the structure in de.rcenvironment.core.datamanagment.
 * 
 * TODO Currently, the exported workflow run, does not contain referenced files and directories. If a workflow run contains such data types
 * the comparison will fail, as the references differ.
 *
 * @author Tobias Brieden
 */
public class PlainWorkflowRun implements Matchable<PlainWorkflowRun> {

    // the title is not compared during matching. nevertheless, we store it in the file for better identification.
    @JsonProperty
    private String title;

    @JsonProperty
    private FinalWorkflowState finalState;

    @JsonProperty("components")
    private List<PlainComponentInstance> plainComponentInstances;    
    
    // needed for Jackson deserialization
    public PlainWorkflowRun() {}

    //TODO is there a better way to inject the TypedDatumSerializer? We could use the TypedDatumSerializerStub.
    public PlainWorkflowRun(WorkflowRun workflowRun, TypedDatumSerializer serializer) {

        title = workflowRun.getWorkflowTitle();
        finalState = workflowRun.getFinalState();

        plainComponentInstances = new LinkedList<>();
        Map<ComponentInstance, Set<ComponentRun>> componentRunsMap = workflowRun.getComponentRuns();
        for (Entry<ComponentInstance, Set<ComponentRun>> entry : componentRunsMap.entrySet()) {

            plainComponentInstances.add(new PlainComponentInstance(entry.getKey(), entry.getValue(), serializer));
        }
        // the list needs to be sorted to be easily comparable
        plainComponentInstances.sort(null);

    }

    @Override
    public MatchResult matches(Map<DataType, Matcher> matchers, PlainWorkflowRun expected) {

        MatchResult result = new MatchResult();

        if (!finalState.equals(expected.finalState)) {
            result.addFailureCause(StringUtils.format("The final state %s does not match %s", finalState, expected.finalState));
        }

        if (plainComponentInstances.size() == expected.plainComponentInstances.size()) {
            for (int i = 0; i < plainComponentInstances.size(); i++) {
                MatchResult nestedMatchResult = plainComponentInstances.get(i).matches(matchers, expected.plainComponentInstances.get(i));

                if (!nestedMatchResult.hasMatched()) {
                    result.addFailureCause(StringUtils.format("Instance \"%s\" does not match the expected instance \"%s\".",
                        plainComponentInstances.get(i).getName(), expected.plainComponentInstances.get(i).getName()), nestedMatchResult);
                }
            }
        } else {
            result.addFailureCause(StringUtils.format("The number of instances %d do not match %d",
                plainComponentInstances.size(), expected.plainComponentInstances.size()));
        }

        return result;
    }

}
