/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;

/**
 * 
 * {@link MappingNode} extension representing input mapping nodes.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class InputMappingNode extends EndpointMappingNode {

    protected InputMappingNode(ComponentNode parent, String inputName, DataType dataType, String mappedName, InputDatumHandling handling,
        InputExecutionContraint constraint) {
        super(parent, EndpointAdapter.inputAdapterBuilder().dataType(dataType).internalEndpointName(inputName)
            .externalEndpointName(mappedName).inputHandling(handling).inputExecutionConstraint(constraint)
            .workflowNodeIdentifier(parent.getWorkflowNodeIdentifier().toString()).build());
        setMappingType(MappingType.INPUT);
        setCheckable(!constraint.equals(InputExecutionContraint.Required));

    }

    @Override
    public String getDetails() {
        return StringUtils.format("Handling: %s; Constraint: %s", getEndpointAdapter().getInputDatumHandling().getDisplayName(),
            getEndpointAdapter().getInputExecutionConstraint().getDisplayName());
    }
}
