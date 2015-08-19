/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;

/**
 * Describes scheduling-related information about an input.
 * 
 * @author Doreen Seider
 */
public final class InputMockInformation {

    protected final String name;

    protected final EndpointDefinition.InputDatumHandling inputDatumHandling;

    protected final EndpointDefinition.InputExecutionContraint inputExecutionContraint;
    
    protected final String parentGroup;

    protected final boolean connected;

    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint,
        String parentGroup, boolean connected) {
        this.name = name;
        this.inputDatumHandling = inputDatumHandling;
        this.inputExecutionContraint = inputExecutionContraint;
        this.parentGroup = parentGroup;
        this.connected = connected;
    }

    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint,
        boolean connected) {
        this(name, inputDatumHandling, inputExecutionContraint, null, connected);
    }
    
    public InputMockInformation(String name, InputDatumHandling inputDatumHandling, InputExecutionContraint inputExecutionContraint) {
        this(name, inputDatumHandling, inputExecutionContraint, true);
    }

    
}
