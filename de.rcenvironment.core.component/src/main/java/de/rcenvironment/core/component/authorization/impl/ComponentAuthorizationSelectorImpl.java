/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.impl;

import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;

/**
 * Default {@link ComponentAuthorizationSelector} implementation.
 *
 * @author Robert Mischke
 */
public class ComponentAuthorizationSelectorImpl implements ComponentAuthorizationSelector {

    private String id;
    
    private boolean assignable;

    public ComponentAuthorizationSelectorImpl(String identifier) {
        this(identifier, true);
    }
    
    public ComponentAuthorizationSelectorImpl(String identifier, boolean assignable) {
        this.id = identifier;
        this.assignable = assignable;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentAuthorizationSelector)) { // important: also accept subclasses
            return false;
        }
        final ComponentAuthorizationSelector other = (ComponentAuthorizationSelector) obj;
        return id.equals(other.getId());
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean isAssignable() {
        return assignable;
    }

}
