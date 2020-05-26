/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDefinition;

/**
 * Describes scheduling-related information about an input group.
 * 
 * @author Doreen Seider
 */
public final class InputGroupMockInformation {

    protected final String name;

    protected final EndpointGroupDefinition.LogicOperation type;

    protected final String parentGroup;

    public InputGroupMockInformation(String name, EndpointGroupDefinition.LogicOperation type, String parentGroup) {
        this.name = name;
        this.type = type;
        this.parentGroup = parentGroup;
    }

    public InputGroupMockInformation(String name, EndpointGroupDefinition.LogicOperation type) {
        this(name, type, null);
    }
}
