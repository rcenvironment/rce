/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.api;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;

/**
 * Represents a component that is available as part of the current distributed network, including the components available from the local
 * node. This interface is used instead of {@link ComponentInstallation} instances to provide additional data (including authorization
 * information), to allow for better API design, and to simplify future API changes.
 *
 * @author Robert Mischke
 * @author Alexander Weinert (added display name)
 */
public interface DistributedComponentEntry {

    /**
     * @return the {@link DistributedComponentEntryType} of this entry, which determines basic location and accessibility
     */
    DistributedComponentEntryType getType();

    /**
     * @return the associated permission settings for this component, as declared by the node providing it; note that not all access groups
     *         may be accessible for the local node
     */
    AuthorizationPermissionSet getDeclaredPermissionSet();

    /**
     * @return the access groups for this component that are also accessible for the local node. For local components, this set is always
     *         equal to {@link #getDeclaredPermissionSet()}. For remote components, the component is accessible if there is at least one
     *         group in this set.
     */
    AuthorizationPermissionSet getMatchingPermissionSet();

    /**
     * @return for local components, this is always true; for remote components, this is true if the component has public access, or the
     *         local node has access to at least one of the component's declared access groups
     */
    boolean isAccessible();

    /**
     * @return the {@link ComponentInstallation}; note that this may be either the actual object instance used in a local registration, or
     *         an object reconstructed from a serialized form. Note that this is <code>null</code> for remote components that are not
     *         accessible for the local node
     */
    ComponentInstallation getComponentInstallation();

    /**
     * Convenience shortcut for accessing the value of {@link ComponentInstallation#getComponentRevision()}.
     * 
     * @return the return value of calling {@link ComponentInstallation#getComponentRevision()} on the contained
     *         {@link ComponentInstallation}
     */
    ComponentRevision getComponentRevision();

    /**
     * Convenience shortcut for accessing the value of {@link ComponentInstallation#getComponentInterface()}.
     * 
     * @return the return value of calling {@link ComponentInstallation#getComponentInterface()} on the contained
     *         {@link ComponentInstallation}
     */
    ComponentInterface getComponentInterface();

    /**
     * @return the serialized form of the contained {@link ComponentInstallation}, if this entry's type is SHARED, and serializing the
     *         {@link ComponentInstallation} was successful; otherwise, null
     */
    String getPublicationData();

    /**
     * Convenience shortcut for accessing the value of {@link ComponentInstallation#getNodeId()}.
     * 
     * @return the return value of calling {@link ComponentInstallation#getNodeId()} on the contained {@link ComponentInstallation}
     */
    // TODO rename
    String getNodeId();

    /**
     * @return A human-readable name for this entry. Is never null.
     */
    String getDisplayName();

}
