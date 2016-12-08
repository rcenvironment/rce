/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.List;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.model.endpoint.api.InitialDynamicEndpointDefinition;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Default mock for {@link EndpointDefinition}.
 * 
 * @author Doreen Seider
 */
public class EndpointDefinitionDefaultStub implements EndpointDefinition {

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isNameReadOnly() {
        return false;
    }

    @Override
    public List<DataType> getPossibleDataTypes() {
        return null;
    }

    @Override
    public DataType getDefaultDataType() {
        return null;
    }

    @Override
    public List<InputDatumHandling> getInputDatumOptions() {
        return null;
    }

    @Override
    public InputDatumHandling getDefaultInputDatumHandling() {
        return null;
    }

    @Override
    public List<InputExecutionContraint> getInputExecutionConstraintOptions() {
        return null;
    }

    @Override
    public InputExecutionContraint getDefaultInputExecutionConstraint() {
        return null;
    }

    @Override
    public EndpointType getEndpointType() {
        return null;
    }

    @Override
    public EndpointMetaDataDefinition getMetaDataDefinition() {
        return null;
    }

    @Override
    public List<InitialDynamicEndpointDefinition> getInitialDynamicEndpointDefinitions() {
        return null;
    }

    @Override
    public String getParentGroupName() {
        return null;
    }

    @Override
    public LogicOperation getLogicOperation() {
        return null;
    }

    @Override
    public EndpointCharacter getEndpointCharacter() {
        return EndpointCharacter.SAME_LOOP;
    }

}
