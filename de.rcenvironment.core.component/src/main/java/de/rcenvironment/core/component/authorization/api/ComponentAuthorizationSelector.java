/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.api;

import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.model.api.ComponentRevision;

/**
 * Abstract representation of equivalence groups of {@link Component}s that access permissions are assigned to. In other words, this
 * selector defines which {@link Component}s are "the same" as far as the authorization system is concerned.
 * 
 * Currently, the defining aspect of {@link Component}s is their component id; this means that different versions (more precisely,
 * {@link ComponentRevision}s) of a component share the same permission settings at the moment. This is intentional, as it is assumed that
 * this is more convenient and intuitive for end users than a more fine-grained assignment. If this assumption turns out wrong, however,
 * then this abstraction is the place to change it.
 *
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public interface ComponentAuthorizationSelector {

    /**
     * @return the unique identifier of this selector; instances with the same id are considered equal
     */
    String getId();
    
    /**
     * @return true iff the component should be assignable to authorization groups by the user
     */
    boolean isAssignable();

}
