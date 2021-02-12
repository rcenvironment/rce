/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Service interface providing methods to register and unregister local component installations. Whether these components are made available
 * to other nodes depends on their related authorization settings; see {@link AuthorizationService}.
 * <p>
 * This service is responsible for:
 * <li>Keeping track of local component installations
 * <li>Fetching the initial authorization settings from the {@link AuthorizationService} and subscribing to updates
 * <li>Combining component registration data and authorization settings to a consistent model
 * <li>Reporting updates in this model to {@link DistributedComponentKnowledgeService} for distribution to other nodes
 * 
 * Regarding terminology: We say that a ComponentAuthorizationSelector is orphaned if it does not refer to any currently available
 * component. This may happen if the user removes an integration that was still published.
 *
 * @author Robert Mischke
 */
public interface LocalComponentRegistrationService {

    /**
     * Adds or updates a local component, using any already-present remote authorization information, or setting local-only access if there
     * is none.
     * 
     * @param componentInstallation {@link ComponentInstallation} to add
     */
    void registerOrUpdateLocalComponentInstallation(ComponentInstallation componentInstallation);

    /**
     * Adds or updates a local component and its associated authorization information.
     * <p>
     * Note that this is a convenience method that is ONLY secure if there can never be another component with the same id, but a different
     * version; otherwise, information can leak to user groups that were authorized for a previous version of the "same" component (in the
     * sense of "same component id").
     * 
     * @param componentInstallation {@link ComponentInstallation} to add
     * @param permissionSet the {@link AuthorizationPermissionSet} to apply
     */
    void registerOrUpdateSingleVersionLocalComponentInstallation(ComponentInstallation componentInstallation,
        AuthorizationPermissionSet permissionSet);

    /**
     * Removes a component.
     * 
     * @param compInstallationId identifier of {@link ComponentInstallation} to remove
     */
    void unregisterLocalComponentInstallation(String compInstallationId);

    /**
     * Derives the {@link ComponentAuthorizationSelector} from a {@link Component}'s internal identifier (without version). Note that this
     * method is less robust against future changes than {@link #getComponentSelector(ComponentInstallation)}; this String-based method
     * should only be used when no {@link ComponentInstallation} object is available!
     * 
     * @param componentIdentifier the component's internal identifier (without a version suffix)
     * @return the derived selector
     */
    ComponentAuthorizationSelector getComponentSelector(String componentIdentifier);

    /**
     * Derives the {@link ComponentAuthorizationSelector} from a {@link Component} instance.
     * 
     * @param component the component
     * @return the derived selector
     */
    ComponentAuthorizationSelector getComponentSelector(ComponentInstallation component);

    /**
     * Returns the permissions for a {@link ComponentAuthorizationSelector}.
     * 
     * @param component the {@link ComponentAuthorizationSelector} of the component to query
     * @param replaceNullResult if true, returns a "local only" permission set if the is no explicit setting for this selector; if false,
     *        null is returned instead
     * @return the permissions (ie the set of access groups), or an empty (local-only) permission set if none have been defined yet [TODO
     *         review: is this always appropriate? is it necessary to return null instead?]
     */
    AuthorizationPermissionSet getComponentPermissionSet(ComponentAuthorizationSelector component,
        boolean replaceNullResult);

    /**
     * Sets the complete set of permissions for a {@link ComponentAuthorizationSelector}.
     * 
     * @param selector the {@link ComponentAuthorizationSelector}
     * @param permissionSet the permissions (ie the set of access groups) to set
     */
    void setComponentPermissions(ComponentAuthorizationSelector selector, AuthorizationPermissionSet permissionSet);

    /**
     * Adds or removes a single permission entry for a {@link ComponentAuthorizationSelector} and an {@link AuthorizationAccessGroup}.
     * 
     * @param selector the {@link ComponentAuthorizationSelector}
     * @param accessGroup the {@link AuthorizationAccessGroup}
     * @param newState true if the component(s) should be authorized for this group; false if any existing permission should be removed
     */
    void setComponentPermissionState(ComponentAuthorizationSelector selector, AuthorizationAccessGroup accessGroup, boolean newState);

    /**
     * Sets the permissions for a {@link ComponentAuthorizationSelector} to the intersection of the current permission set and the provided
     * one.
     * <p>
     * This method is intended to avoid race conditions when updating a component with new data and new permissions at the same time. The
     * problem this solves is that when the old permission set is larger than the new one, new component data would leak to recipients that
     * are not in the new permission set. A trivial solution would be to completely de-publish the component before the update, but that
     * would cause unnecessary availability disruption for recipient that are both authorized before and after.
     * <p>
     * With this method, the intended approach is to reduce permissions to the intersection of both permission sets, then updating the
     * component, and then setting the new (full) permission set, which is safe at any point in time, and also minimizes the remote update
     * delta. -- misc_ro
     * 
     * @param selector the {@link ComponentAuthorizationSelector}
     * @param permissionSet the permissions (ie the set of access groups) to set
     */
    void restrictComponentPermissionsToIntersectionWith(ComponentAuthorizationSelector selector,
        AuthorizationPermissionSet permissionSet);

    /**
     * Converts a "component string id and version" pair currently used for referencing components into an abstract reference object. This
     * conversion decouples the authorization system from future component-specific design changes.
     * 
     * @param componentId the component's id (which is currently also its display name)
     * @param version the version string
     * @return the abstract component reference to use for authorization
     */
    // ComponentAuthorizationSelector getOrCreateComponentReference(String componentId, String version);
    /**
     * @return the {@link AuthorizationAccessGroup}s that are available for assigning them to {@link ComponentAuthorizationSelector}s in
     *         form of an {@link AuthorizationPermissionSet}
     */
    List<AuthorizationAccessGroup> listAvailableAuthorizationAccessGroups();

    /**
     * @return the list of "selectors", which are the "keys" that a user assigns component permissions to; currently, this is the
     *         version-less id of components, ie all versions of a component share the same permissions
     */
    List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForRemotableComponents();

    /**
     * @return the list of "selectors", which are the "keys" that a user assigns component permissions to; currently, this is the
     *         version-less id of components, ie all versions of a component share the same permissions. In contrast to
     *         {@link #listAuthorizationSelectorsForRemotableComponents()}, this list includes orphaned selectors.
     */
    List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForRemotableComponentsIncludingOrphans();

    /**
     * @param accessGroup The access group for which all component selectors are to be returned.
     * @param includeOrphanedSelectors If true, selectors that refer to non-integrated components are included in the result. If false,
     *        those selectors are not included.
     * @return A list of those selectors that are public for the given accessGroup. Is never null, but may be empty. Sorted alphabetically
     *         by the display name of the selector.
     */
    List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForAccessGroup(AuthorizationAccessGroup accessGroup,
        boolean includeOrphanedSelectors);

    /**
     * @return the current {@link AuthorizationPermissionSet}s that have been assigned to local components; note that local-only permission
     *         sets are treated as "no assigned permission", and are therefore not returned by this method
     */
    Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> listAssignedComponentPermissions();

    /**
     * Registers a new {@link PermissionMatrixChangeListener}.
     * 
     * @param listener the listener
     */
    void addPermissionMatrixChangeListener(PermissionMatrixChangeListener listener);

    /**
     * Unregisters a previously-registered {@link PermissionMatrixChangeListener}.
     * 
     * @param listener the listener
     */
    void removePermissionMatrixChangeListener(PermissionMatrixChangeListener listener);

    /**
     * Signals that all built-in components have been loaded. This is relevant as this means that all built-in component ids are known and
     * therefore available for authorization mapping.
     */
    void reportBuiltinComponentLoadingComplete();

    /**
     * Signals that all tool integration components have been loaded.
     */
    void reportToolIntegrationRegistrationComplete();

    /**
     * Waits for all local component initialization to complete, with a timeout. Behaves like
     * {@link java.util.concurrent.CountDownLatch#await(long, TimeUnit)}.
     * 
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return true if the wait completed without timeout
     * @throws InterruptedException on interruption
     */
    boolean waitForLocalComponentInitialization(int timeout, TimeUnit unit) throws InterruptedException;

}
