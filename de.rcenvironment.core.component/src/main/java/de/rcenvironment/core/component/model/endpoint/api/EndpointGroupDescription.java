/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.io.Serializable;

/**
 * Provides information about a single endpoint group.
 * 
 * @author Doreen Seider
 */
public class EndpointGroupDescription implements Serializable, Comparable<EndpointGroupDescription> {

    private static final long serialVersionUID = -4855835405561620711L;

    private String parentGroupName;

    private String dynamicEndpointId;

    private String name;

    private EndpointGroupDefinition endpointGroupDefinition;

    @Deprecated
    public EndpointGroupDescription() {}

    public EndpointGroupDescription(EndpointGroupDefinition newEndpointGroupDefinition) {
        this.endpointGroupDefinition = newEndpointGroupDefinition;
        if (endpointGroupDefinition != null) {
            name = endpointGroupDefinition.getName();
            dynamicEndpointId = endpointGroupDefinition.getIdentifier();
            parentGroupName = endpointGroupDefinition.getParentGroupName();
        }
    }

    /**
     * @return backing {@link EndpointGroupDefinition}
     */
    public EndpointGroupDefinition getEndpointGroupDefinition() {
        return endpointGroupDefinition;
    }

    /**
     * Do not confuse the name of a group (a human-readable string) with its identifier (a UUID). The latter is returned by
     * #getIdentifier().
     * 
     * @return The human-readable name of this endpoint group.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name name to set
     * @throws UnsupportedOperationException of this description belongs to a static one
     */
    public void setName(String name) throws UnsupportedOperationException {
        if (endpointGroupDefinition != null && endpointGroupDefinition.getName() != null) {
            throw new UnsupportedOperationException("name of static endpoint group can not be changed");
        }
        this.name = name;
    }

    public String getParentGroupName() {
        return parentGroupName;
    }

    public void setParentGroupName(String parentGroupName) {
        this.parentGroupName = parentGroupName;
    }

    /**
     * @return identifier of dynamic endpoint or <code>null</code> if it is a static endpoint
     */
    public String getDynamicEndpointIdentifier() {
        return dynamicEndpointId;
    }

    public void setDynamicEndpointIdentifier(String dynamicEndpointIdentifier) {
        dynamicEndpointId = dynamicEndpointIdentifier;
    }

    @Override
    public int compareTo(EndpointGroupDescription o) {
        return getName().compareTo(o.getName());
    }

}
