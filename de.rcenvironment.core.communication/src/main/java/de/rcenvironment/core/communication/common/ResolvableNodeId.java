/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * A marker interface for subclasses of {@link CommonIdBase} that can be resolved to an {@link InstanceNodeSessionId} as long as the given
 * id points to an entity that is available in the current network. This is used in place of {@link CommonIdBase} to support future
 * extensions and adaptations, and making method signatures and contracts more explicit.
 * 
 * @author Robert Mischke
 */
public interface ResolvableNodeId extends NetworkDestination {

    /**
     * @return the {@link IdType} of this generic id
     */
    IdType getType();

    /**
     * @return the string form of the (usually persistent) identifier of a referenced instance; see the main JavaDoc for its description
     */
    String getInstanceNodeIdString();

    /**
     * Tests whether this identifier represents the same instance (but <b>not</b> necessarily the same instance <b>session</b>) as the
     * provided identifier. Currently, this method simply compares their instance id strings for equality.
     * <p>
     * Note that this method should be used whenever possible instead of manually comparing string representations for semantic clarity and
     * future maintainability.
     * 
     * @param otherId the other identifier to compare with
     * @return true if both identifiers are related to the same (abstractly defined) "instance"
     */
    boolean isSameInstanceNodeAs(ResolvableNodeId otherId);

}
