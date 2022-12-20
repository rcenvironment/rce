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
 * Mapping nodes holding {@link EndpointAdapter}.
 *
 * @author Jan Flink
 */
public abstract class EndpointMappingNode extends MappingNode {

    private EndpointAdapter endpointAdapter;

    protected EndpointMappingNode(ComponentNode parent, EndpointAdapter endpointAdapter) {
        super(parent);
        this.endpointAdapter = endpointAdapter;
    }

    @Override
    public String getInternalName() {
        return endpointAdapter.getInternalName();
    }

    @Override
    public DataType getDataType() {
        return endpointAdapter.getDataType();
    }

    @Override
    public String getExternalName() {
        return endpointAdapter.getExternalName();
    }

    @Override
    public void setExternalName(String mappedName) {
        this.endpointAdapter.setExternalName(mappedName);
    }

    public EndpointAdapter getEndpointAdapter() {
        return endpointAdapter;
    }

}
