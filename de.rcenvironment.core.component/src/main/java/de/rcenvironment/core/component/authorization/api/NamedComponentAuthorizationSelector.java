/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.authorization.api;

/**
 * Convenience extension of {@link ComponentAuthorizationSelector} to transport an associated display name along with it.
 *
 * @author Robert Mischke
 */
public interface NamedComponentAuthorizationSelector
    extends ComponentAuthorizationSelector, Comparable<NamedComponentAuthorizationSelector> {

    /**
     * @return the associated display name
     */
    String getDisplayName();

    /**
     * * Compares this object with the specified object for order by ignoring cases in the object´s name.
     * 
     * @param o the object to compare
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    int compareToIgnoreCase(NamedComponentAuthorizationSelector o);
    
    /**
     * * Compares this object with the specified object for order by ignoring cases in the object´s internal name.
     * 
     * @param o the object to compare
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    int compareToIgnoreCaseInternal(NamedComponentAuthorizationSelector o);
}
