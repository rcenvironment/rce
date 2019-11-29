/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;

/**
 * Describes scheduling-related information about an input.
 * 
 * @author Doreen Seider
 */
public final class InputMockInformation {

    protected final String name;
    
    // make configurable if needed
    protected final DataType dataType = DataType.Float;

    protected final EndpointDefinition.InputDatumHandling inputDatumHandling;

    protected final EndpointDefinition.InputExecutionContraint inputExecutionContraint;
    
    protected final EndpointCharacter endpointCharacter;
    
    protected final String parentGroup;

    protected final boolean connected;

    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint,
        EndpointCharacter endpointCharacter, String parentGroup, boolean connected) {
        this.name = name;
        this.inputDatumHandling = inputDatumHandling;
        this.inputExecutionContraint = inputExecutionContraint;
        this.endpointCharacter = endpointCharacter;
        this.parentGroup = parentGroup;
        this.connected = connected;
    }
    
    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint,
        EndpointCharacter endpointCharacter) {
        this(name, inputDatumHandling, inputExecutionContraint, endpointCharacter, null, true);
    }
    
    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint,
        String parentGroup, boolean connected) {
        this(name, inputDatumHandling, inputExecutionContraint, EndpointCharacter.SAME_LOOP, parentGroup, connected);
    }
    
    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint,
        boolean connected) {
        this(name, inputDatumHandling, inputExecutionContraint, EndpointCharacter.SAME_LOOP, null, connected);
    }
    
    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint) {
        this(name, inputDatumHandling, inputExecutionContraint, true);
    }

    
}
