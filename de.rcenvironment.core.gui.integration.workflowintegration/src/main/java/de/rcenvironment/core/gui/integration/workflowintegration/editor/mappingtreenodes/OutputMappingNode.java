/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;

/**
 * 
 * {@link MappingNode} extension representing output mapping nodes.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class OutputMappingNode extends EndpointMappingNode {

    protected OutputMappingNode(ComponentNode parent, String inputName, DataType dataType, String mappedName) {
        super(parent, EndpointAdapter.outputAdapterBuilder().dataType(dataType).externalEndpointName(mappedName)
            .internalEndpointName(inputName).workflowNodeIdentifier(parent.getWorkflowNodeIdentifier().toString()).build());
        setMappingType(MappingType.OUTPUT);
    }

    @Override
    public String getDetails() {
        return "";
    }
}
