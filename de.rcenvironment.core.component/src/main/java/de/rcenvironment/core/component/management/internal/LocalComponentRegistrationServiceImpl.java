/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupListener;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.component.api.ComponentIdRules;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.impl.ComponentAuthorizationSelectorImpl;
import de.rcenvironment.core.component.authorization.impl.NamedComponentAuthorizationSelectorImpl;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.management.api.PermissionMatrixChangeListener;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Default {@link LocalComponentRegistrationService} implementation.
 *
 * @author Robert Mischke
 * @author Brigitte Boden
 */
@Component
// TODO the handling of internal component publication data has become too complex; needs refactoring, probably a further split
public class LocalComponentRegistrationServiceImpl implements LocalComponentRegistrationService, AuthorizationAccessGroupListener {

    private static final String ERROR_PLACEHOLDER_EXTERNAL_ID = "rce/UNKNOWN";

    // TODO can probably be removed completely; simply shortened to avoid threading changes close to release
    private static final int ACTIVATION_TO_COMPONENT_PUBLISHING_DELAY_MSEC = 50;

    private final Map<String, DistributedComponentEntry> componentEntriesByInstallationId = new HashMap<>();

    private final Map<ComponentAuthorizationSelector, Collection<DistributedComponentEntry>> remotableCompEntriesByAuthorizationSelector =
        new HashMap<>();

    private final Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> nonLocalPermissionSetsBySelector =
        new HashMap<>();

    private volatile boolean startupPublishDelayExceeded = false;

    private UserComponentIdMappingService userComponentIdMappingService;

    private AuthorizationService authorizationService;

    private DistributedComponentKnowledgeService componentDistributor;

    private AuthorizationPermissionSet permissionSetLocalOnly;

    private final Object installationsAndPermissionsLock = new Object();

    private final Log log = LogFactory.getLog(getClass());

    private final AsyncOrderedCallbackManager<PermissionMatrixChangeListener> permissionMatrixChangesCallbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private List<AuthorizationAccessGroup> availableAccessGroups;

    private ComponentPermissionStorage permissionStorage;

    // the component registrations by other services; used to wait until starting internal initialization
    private final CountDownLatch localComponentRegistrationCompleteLatch = new CountDownLatch(2); // built-in + TI

    // the component initialization done internally; used to wait on by external code
    private final CountDownLatch localComponentInitializationCompleteLatch = new CountDownLatch(1);

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService(); // TODO inject instead

    private volatile boolean initializing;

    // stores the "last seen" display name for each known component selector;
    // TODO (p2) technically a memory leak, as this is never reduced; should be insignificant in practice, though
    private Map<String, String> associatedSelectorDisplayNames = new HashMap<>();

    @Override
    public void registerOrUpdateLocalComponentInstallation(ComponentInstallation componentInstallation) {

        Optional<String> validationError = ComponentIdRules.validateComponentInterfaceIds(componentInstallation.getComponentInterface());
        if (validationError.isPresent()) {
            log.error("Skipping registration of local component/tool " + componentInstallation.getInstallationId() + "; reason: "
                + validationError.get());
            // currently only logged and ignored; change to throwing an exception if this case can be reached by typical user action
            return;
        }

        // note: in case of a built-in component, getting the selector requires an already-registered name mapping
        final ComponentAuthorizationSelector componentSelector = getComponentSelector(componentInstallation);
        // use the current permission settings for this component's selector
        final AuthorizationPermissionSet permissionSet = getComponentPermissionSet(componentSelector, true);
        DistributedComponentEntryImpl newEntry =
            ComponentDataConverter.createLocalDistributedComponentEntry(componentInstallation, permissionSet, authorizationService);

        synchronized (installationsAndPermissionsLock) {

            // consistency/safety check
            if (!permissionSet.isLocalOnly() && componentInstallation.getComponentInterface().getLocalExecutionOnly()) {
                log.warn("Registering the local-only component " + componentInstallation.getInstallationId()
                    + " while remote access permissions are set for its selector; this is not an immediate problem, "
                    + "as publication is still prevented by the local-only filter, but the setting is unusual/inconsistent");
            }

            // note: this simple approach is only valid in the "one component version/revision per selector" case
            DistributedComponentEntry replacedEntry =
                componentEntriesByInstallationId.put(componentInstallation.getInstallationId(), newEntry);
            if (!componentInstallation.getComponentInterface().getLocalExecutionOnly()) {
                addRemotableComponentToAuthorizationTracking(newEntry, componentSelector);
            }
            // probably a bit more efficient to remove the old entry after adding the new one (to avoid a removed empty set in-between)
            if (replacedEntry != null) {
                removeComponentFromAuthorizationTracking(replacedEntry, componentSelector);
            }
            updatePublicationEntriesForLocalComponents();
        }

        // TODO (p3) currently, this is always claiming that component selectors have changed; actually they just *might* have; improve?
        notifyChangeListenersAsync(false, true, false);
        log.debug(StringUtils.format("Registered or updated local component %s, reusing the previous access type %s (access groups: %s)",
            componentInstallation.getComponentInterface().getIdentifierAndVersion(), newEntry.getType(), permissionSet));
    }

    @Override
    public void registerOrUpdateSingleVersionLocalComponentInstallation(ComponentInstallation componentInstallation,
        AuthorizationPermissionSet permissionSet) {

        Objects.requireNonNull(permissionSet); // make sure no "local only" permission is represented as null here

        if (!permissionSet.isLocalOnly() && componentInstallation.getComponentInterface().getLocalExecutionOnly()) {
            log.warn("Setting non-local permissions for the local-only component " + componentInstallation.getInstallationId()
                + "; this will be shown on the local node, but the component will still not be published");
        }

        final ComponentAuthorizationSelector authorizationSelector = getComponentSelector(componentInstallation);

        // temporarily restrict access to the common denominator between old and new permissions;
        // note that this may trigger a publication change already
        // TODO 9.0.0 review after service reorganization; not critical yet, so there is no harm in disabling it for now

        DistributedComponentEntryImpl newEntry =
            ComponentDataConverter.createLocalDistributedComponentEntry(componentInstallation, permissionSet, authorizationService);

        final boolean permissionAssignmentsChanged;
        synchronized (installationsAndPermissionsLock) {
            // note: this simple approach is only valid in the "one component version/revision per selector" case
            DistributedComponentEntry replacedEntry =
                componentEntriesByInstallationId.put(componentInstallation.getInstallationId(), newEntry);
            permissionAssignmentsChanged = updatePermissionSet(authorizationSelector, permissionSet);
            if (!componentInstallation.getComponentInterface().getLocalExecutionOnly()) {
                addRemotableComponentToAuthorizationTracking(newEntry, authorizationSelector);
            }
            // probably a bit more efficient to remove the old entry after adding the new one (to avoid a removed empty set in-between)
            if (replacedEntry != null) {
                removeComponentFromAuthorizationTracking(replacedEntry, authorizationSelector);
            }
            updatePublicationEntriesForLocalComponents();
        }
        // TODO (p3) currently, this is always claiming that component selectors have changed; actually they just *might* have; improve?
        notifyChangeListenersAsync(false, true, permissionAssignmentsChanged);
        log.debug(StringUtils.format("Registered or updated local component %s with access type %s (access groups: %s)",
            componentInstallation.getComponentInterface().getIdentifierAndVersion(), newEntry.getType(), permissionSet));
    }

    @Override
    public void unregisterLocalComponentInstallation(String compInstallationId) {
        synchronized (installationsAndPermissionsLock) {
            // remove from the "by id" mapping
            DistributedComponentEntry removedCompEntry = componentEntriesByInstallationId.remove(compInstallationId);
            if (removedCompEntry == null) {
                log.warn("Received a call to remove component " + compInstallationId + ", but no matching component was found");
                return;
            }
            if (!removedCompEntry.getComponentInterface().getLocalExecutionOnly()) {
                // remove from the map that tracks which permission sets affect which component entries
                ComponentAuthorizationSelector authSelector = getComponentSelector(removedCompEntry.getComponentInstallation());
                removeComponentFromAuthorizationTracking(removedCompEntry, authSelector);
            }
            // trigger local and remote updates
            updatePublicationEntriesForLocalComponents();
        }
        // TODO (p3) currently, this is always fired, but once multi-versioning is actually used, the selector may still be present
        notifyChangeListenersAsync(false, true, false);
        log.debug("Removed component: " + compInstallationId);
    }

    @Override
    public void reportBuiltinComponentLoadingComplete() {
        localComponentRegistrationCompleteLatch.countDown();
        log.debug("Built-in component loading reported as finished");
    }

    @Override
    public void reportToolIntegrationRegistrationComplete() {
        localComponentRegistrationCompleteLatch.countDown();
        log.debug("Tool integration component loading reported as finished");
    }

    @Override
    public boolean waitForLocalComponentInitialization(int maxWait, TimeUnit timeUnit) throws InterruptedException {
        return localComponentInitializationCompleteLatch.await(maxWait, timeUnit);
    }

    @Activate
    protected void activate() {
        availableAccessGroups = authorizationService.listAccessibleGroups(true);

        initializing = true;
        asyncTaskService.execute("Asynchronous component registry initialization", this::initializeAsync);
    }

    private void initializeAsync() {
        try {
            // wait for all local components to be *registered*
            if (!localComponentRegistrationCompleteLatch.await(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Component registration did not complete within a reasonable time");
            }
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            log.info("Interrupted while waiting for component registration to complete - "
                + "assuming early shutdown, skipping component publication");
            initializing = false;
            return;
        }
        log.debug("Startup component registration complete, restoring and applying component authorization entries");

        final Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> restoredAssignments =
            permissionStorage.restorePersistedAssignments();
        synchronized (installationsAndPermissionsLock) {
            for (Entry<ComponentAuthorizationSelector, AuthorizationPermissionSet> e : restoredAssignments.entrySet()) {
                final ComponentAuthorizationSelector selector = e.getKey();
                final AuthorizationPermissionSet newPermissionSet = e.getValue();
                if (!updatePermissionSet(selector, newPermissionSet)) {
                    // consistency check failed
                    log.warn("Unexpected result: Applying the restored permission assignment " + e.getValue() + " for " + e.getKey()
                        + " caused no local change");
                }
                // update affected DistributedComponentEntries to actually apply the loaded permissions
                updatePermissionSetsOfRegisteredComponents(selector, newPermissionSet);
            }
        }
        notifyChangeListenersAsync(false, true, true);

        initializing = false;

        asyncTaskService.scheduleAfterDelay(new Runnable() {

            @Override
            @TaskDescription("Publish workflow components")
            public void run() {
                synchronized (installationsAndPermissionsLock) {
                    startupPublishDelayExceeded = true;
                    log.debug("Startup component initialization complete, triggering initial publication");
                    updatePublicationEntriesForLocalComponents();
                    // mark local *initialization* as complete
                    localComponentInitializationCompleteLatch.countDown();
                    log.debug("Local component initialization complete");
                }
            }

        }, ACTIVATION_TO_COMPONENT_PUBLISHING_DELAY_MSEC);
    }

    @Reference
    protected void bindUserComponentIdMappingService(UserComponentIdMappingService newInstance) {
        this.userComponentIdMappingService = newInstance;
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService newInstance) {
        this.authorizationService = newInstance;
        synchronized (installationsAndPermissionsLock) { // ensure thread visibility
            this.permissionSetLocalOnly = authorizationService.getDefaultAuthorizationObjects().permissionSetLocalOnly();
        }
    }

    @Reference
    protected void bindPermissionStorage(ComponentPermissionStorage newInstance) {
        this.permissionStorage = newInstance;
    }

    @Reference
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        componentDistributor = newInstance;
    }

    @Override
    public AuthorizationPermissionSet getComponentPermissionSet(ComponentAuthorizationSelector selector,
        boolean replaceNullResult) {
        // note: component versions are ignored for now; all components of the same id share authorization settings
        synchronized (installationsAndPermissionsLock) {
            final AuthorizationPermissionSet permissionSet = nonLocalPermissionSetsBySelector.get(selector);
            if (permissionSet != null) {
                return permissionSet;
            } else {
                if (replaceNullResult) {
                    return permissionSetLocalOnly;
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public ComponentAuthorizationSelector getComponentSelector(String internalId) {
        String externalId;
        try {
            externalId = userComponentIdMappingService.fromInternalToExternalId(internalId);
        } catch (OperationFailureException e) {
            log.error("Failed to generate external id from internal id " + internalId
                + "; falling back to " + ERROR_PLACEHOLDER_EXTERNAL_ID + " placeholder (as this should never happen)", e);
            externalId = ERROR_PLACEHOLDER_EXTERNAL_ID;
        }
        return new ComponentAuthorizationSelectorImpl(externalId);
    }

    @Override
    public ComponentAuthorizationSelector getComponentSelector(ComponentInstallation component) {

        if (component.isMappedComponent()) {
            return new ComponentAuthorizationSelectorImpl(component.getInstallationId(), false);
        }

        final String internalId = component.getComponentInterface().getIdentifier();
        String externalId;
        try {
            externalId = userComponentIdMappingService.fromInternalToExternalId(internalId);
        } catch (OperationFailureException e) {
            log.error("Failed to generate external id from internal id " + internalId
                + "; falling back to " + ERROR_PLACEHOLDER_EXTERNAL_ID + " placeholder (as this should never happen)", e);
            externalId = ERROR_PLACEHOLDER_EXTERNAL_ID;
        }
        synchronized (associatedSelectorDisplayNames) {
            // for wrapping later using the selector's id, so this must use the external id, too
            associatedSelectorDisplayNames.put(externalId, component.getComponentInterface().getDisplayName());
        }
        return new ComponentAuthorizationSelectorImpl(externalId);
    }

    @Override
    public List<AuthorizationAccessGroup> listAvailableAuthorizationAccessGroups() {
        synchronized (installationsAndPermissionsLock) {
            return availableAccessGroups; // immutable
        }
    }

    @Override
    public List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForRemotableComponents() {
        final ArrayList<NamedComponentAuthorizationSelector> result = new ArrayList<>();
        final Set<ComponentAuthorizationSelector> selectors;
        synchronized (installationsAndPermissionsLock) {
            // using keys of this map as it only contains actual registered components, omitting orphaned/dangling selectors
            selectors = remotableCompEntriesByAuthorizationSelector.keySet();
        }
        synchronized (associatedSelectorDisplayNames) {
            for (ComponentAuthorizationSelector selector : selectors) {
                if (selector.isAssignable()) {
                    String associatedName = associatedSelectorDisplayNames.get(selector.getId());
                    if (associatedName == null) {
                        associatedName = "<" + selector.getId() + ">"; // TODO improve?
                    }
                    result.add(new NamedComponentAuthorizationSelectorImpl(selector.getId(), associatedName));
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForRemotableComponentsIncludingOrphans() {
        final Set<ComponentAuthorizationSelector> selectors = new HashSet<>();
        synchronized (installationsAndPermissionsLock) {
            // using keys of this map as it only contains actual registered components, omitting orphaned/dangling selectors
            selectors.addAll(remotableCompEntriesByAuthorizationSelector.keySet());
            // The map nonLocalPermissionSetsBySelector includes all dangling/orphaned ComponentAuthorizationSelectors. Since the caller
            // requires us to also return such orphans, we add them here.
            selectors.addAll(nonLocalPermissionSetsBySelector.keySet());
        }
        final ArrayList<NamedComponentAuthorizationSelector> result = new ArrayList<>();
        synchronized (associatedSelectorDisplayNames) {
            for (ComponentAuthorizationSelector selector : selectors) {
                if (selector.isAssignable()) {
                    final String displayName;
                    if (associatedSelectorDisplayNames.containsKey(selector.getId())) {
                        displayName = associatedSelectorDisplayNames.get(selector.getId());
                    } else {
                        displayName = selector.getId();
                    }
                    result.add(new NamedComponentAuthorizationSelectorImpl(selector.getId(), displayName));
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForAccessGroup(AuthorizationAccessGroup accessGroup,
        boolean includeOrphanedSelectors) {
        final Set<ComponentAuthorizationSelector> selectors;
        synchronized (installationsAndPermissionsLock) {
            if (includeOrphanedSelectors) {
                selectors = nonLocalPermissionSetsBySelector.keySet();
            } else {
                // using keys of this map as it only contains actual registered components, omitting orphaned/dangling selectors
                selectors = remotableCompEntriesByAuthorizationSelector.keySet();
            }
        }
        final Set<ComponentAuthorizationSelector> selectorsForAccessGroup = selectors.stream()
            // Only retain those selectors whose associated permission set includes the given access group
            .filter(selector -> getComponentPermissionSet(selector, true).includesAccessGroup(accessGroup))
            .collect(Collectors.toSet());

        final ArrayList<NamedComponentAuthorizationSelector> result = new ArrayList<>();
        synchronized (associatedSelectorDisplayNames) {
            for (ComponentAuthorizationSelector selector : selectorsForAccessGroup) {
                if (selector.isAssignable()) {
                    final String displayName;
                    if (associatedSelectorDisplayNames.containsKey(selector.getId())) {
                        displayName = associatedSelectorDisplayNames.get(selector.getId());
                    } else {
                        displayName = selector.getId();
                    }
                    result.add(new NamedComponentAuthorizationSelectorImpl(selector.getId(), displayName));
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> listAssignedComponentPermissions() {
        synchronized (installationsAndPermissionsLock) {
            // TODO migrate to NamedComponentAuthorizationSelector
            final Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> result =
                new TreeMap<>(new Comparator<ComponentAuthorizationSelector>() {

                    @Override
                    public int compare(ComponentAuthorizationSelector o1, ComponentAuthorizationSelector o2) {
                        return o1.getId().compareTo(o2.getId());
                    }
                });
            result.putAll(nonLocalPermissionSetsBySelector);
            return result;
        }
    }

    @Override
    public void setComponentPermissions(ComponentAuthorizationSelector selector,
        AuthorizationPermissionSet newPermissionSet) {
        final boolean permissionsChanged;
        synchronized (installationsAndPermissionsLock) {
            permissionsChanged = updatePermissionSet(selector, newPermissionSet);
            if (permissionsChanged) {
                updatePermissionSetsOfRegisteredComponents(selector, newPermissionSet);
                updatePublicationEntriesForLocalComponents();
            } else {
                log.debug("No change in effective permissions for component selector \"" + selector + "\"; not updating component pool");
            }
        }
        if (permissionsChanged) {
            notifyChangeListenersAsync(false, false, true);
        }
    }

    @Override
    public void setComponentPermissionState(ComponentAuthorizationSelector selector, AuthorizationAccessGroup accessGroup,
        boolean newState) {
        AuthorizationPermissionSet modifiedPermissionSet = null;
        synchronized (installationsAndPermissionsLock) {

            final AuthorizationPermissionSet existingPermissionSet = getComponentPermissionSet(selector, true);

            // handle special case of setting the "public" group first
            if (authorizationService.isPublicAccessGroup(accessGroup)) {
                if (newState && !existingPermissionSet.isPublic()) {
                    // if set to "true", override any existing groups with public permission set
                    modifiedPermissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();
                } else if (!newState && !existingPermissionSet.isLocalOnly()) {
                    // if set to "false", override any existing groups with local-only permission set; this is unusual usage, though
                    modifiedPermissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetLocalOnly();
                }
            } else {
                // standard change
                if (newState && !existingPermissionSet.includesAccessGroup(accessGroup)) {
                    // if the old permission set was already public, ignore the new request
                    if (existingPermissionSet.isPublic()) {
                        log.info(StringUtils.format(
                            "Ignored request to add group permission %s to component selector %s as it is already set to public access",
                            accessGroup, selector));
                    } else {
                        // add group to permission set
                        final ArrayList<AuthorizationAccessGroup> newGroups = new ArrayList<>(existingPermissionSet.getAccessGroups());
                        newGroups.add(accessGroup);
                        modifiedPermissionSet = authorizationService.buildPermissionSet(newGroups);
                        log.debug(StringUtils.format(
                            "Adding access group %s to the permission set of component selector %s; new list of permissions: %s",
                            accessGroup.getFullId(), selector.getId(), modifiedPermissionSet.getSignature()));
                    }
                } else if (!newState && existingPermissionSet.includesAccessGroup(accessGroup)) {
                    // if the old permission set was already public, then the new behavior is undefined; set to "local" for safety
                    if (existingPermissionSet.isPublic()) {
                        log.error(StringUtils.format(
                            "Ignoring attempt to remove group permission %s from component selector %s which is set to public access; "
                                + "the result of this operation would be undefined. Setting the new access to \"local\" for safety.",
                            accessGroup, selector));
                        modifiedPermissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetLocalOnly();
                    } else {
                        // remove group from permission set
                        final ArrayList<AuthorizationAccessGroup> newGroups = new ArrayList<>(existingPermissionSet.getAccessGroups());
                        newGroups.remove(accessGroup);
                        modifiedPermissionSet = authorizationService.buildPermissionSet(newGroups);
                        log.debug(StringUtils.format(
                            "Removing access group %s from the permission set of component selector %s; new list of permissions: %s",
                            accessGroup.getFullId(), selector.getId(), modifiedPermissionSet.getSignature()));
                    }
                }
            }

            // apply modified set if required
            if (modifiedPermissionSet != null) {
                if (!updatePermissionSet(selector, modifiedPermissionSet)) {
                    // sanity check failed
                    log.error("Unexpected state: modifying a single permission entry did not result in an overall permission change?");
                }
                updatePermissionSetsOfRegisteredComponents(selector, modifiedPermissionSet);
                updatePublicationEntriesForLocalComponents();
            }
        }
        if (modifiedPermissionSet != null) {
            notifyChangeListenersAsync(false, false, true);
        }
    }

    @Override
    public void restrictComponentPermissionsToIntersectionWith(ComponentAuthorizationSelector selector,
        AuthorizationPermissionSet maximumPermissionSet) {
        final boolean permissionsChanged;
        synchronized (installationsAndPermissionsLock) {
            final AuthorizationPermissionSet oldPermissionSet = nonLocalPermissionSetsBySelector.get(selector);
            final AuthorizationPermissionSet newPermissionSet;
            if (oldPermissionSet == null) {
                newPermissionSet = permissionSetLocalOnly;
            } else {
                newPermissionSet = oldPermissionSet.intersectWith(maximumPermissionSet);
            }
            permissionsChanged = updatePermissionSet(selector, newPermissionSet);
            if (permissionsChanged) {
                updatePermissionSetsOfRegisteredComponents(selector, newPermissionSet);
                updatePublicationEntriesForLocalComponents();
            }
        }
        if (permissionsChanged) {
            notifyChangeListenersAsync(false, false, true);
        }
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, // forced line break
        unbind = "removePermissionMatrixChangeListener")
    public void addPermissionMatrixChangeListener(PermissionMatrixChangeListener listener) {
        permissionMatrixChangesCallbackManager.addListener(listener);
    }

    @Override
    public void removePermissionMatrixChangeListener(PermissionMatrixChangeListener listener) {
        permissionMatrixChangesCallbackManager.removeListener(listener);
    }

    @Override
    public void onAvailableAuthorizationAccessGroupsChanged(List<AuthorizationAccessGroup> accessGroups) {
        boolean assignmentsChanged = false;
        synchronized (installationsAndPermissionsLock) {
            this.availableAccessGroups = accessGroups;
            // check all present group-component associations, and delete any that do not match a local group anymore.
            // this relies on the AuthorizationService being properly initialized before this service is started, otherwise
            // an empty group list may delete all local permission settings.

            // create a copy to avoid conflicts while updating permissions within the loop
            final Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> stableIterationCopy =
                new HashMap<>(nonLocalPermissionSetsBySelector);
            for (Entry<ComponentAuthorizationSelector, AuthorizationPermissionSet> entry : stableIterationCopy.entrySet()) {
                AuthorizationPermissionSet permSet = entry.getValue();
                if (permSet.isPublic()) {
                    continue; // nothing to check for public access
                }
                // could probably be optimized, but there is no point as this event is fired rarely
                final Collection<AuthorizationAccessGroup> originalAccessGroups = permSet.getAccessGroups();
                final Collection<AuthorizationAccessGroup> filteredAccessGroups = new HashSet<>(originalAccessGroups);
                filteredAccessGroups.retainAll(availableAccessGroups);
                if (originalAccessGroups.size() != filteredAccessGroups.size()) {
                    // apply the filtered group list, effectively removing all assignments that used removed groups
                    final ComponentAuthorizationSelector componentSelector = entry.getKey();
                    final AuthorizationPermissionSet newPermissionSet = authorizationService.buildPermissionSet(filteredAccessGroups);
                    updatePermissionSet(componentSelector, newPermissionSet);
                    log.debug(
                        "Reduced permission set for component selector " + componentSelector + " from " + originalAccessGroups + " to "
                            + filteredAccessGroups + " after available access groups have changed");
                    updatePermissionSetsOfRegisteredComponents(componentSelector, newPermissionSet);
                    assignmentsChanged = true;
                }
            }
            if (assignmentsChanged) {
                // remove publication entries for locally deleted groups
                updatePublicationEntriesForLocalComponents();
            }
        }
        notifyChangeListenersAsync(true, false, assignmentsChanged);
    }

    // note: expected to be called with installationsAndPermissionsLock lock held
    private void updatePublicationEntriesForLocalComponents() {
        // once graceful shutdown is implemented, add the "shutting down" flag here -- misc_ro
        componentDistributor.updateLocalComponentInstallations(componentEntriesByInstallationId.values(), startupPublishDelayExceeded);
    }

    /**
     * @return true if the effective permissions have changed; null and the "local only" permission set are considered the same
     */
    // note: expected to be called with installationsAndPermissionsLock lock held
    private boolean updatePermissionSet(ComponentAuthorizationSelector selector, AuthorizationPermissionSet newPermissionSet) {
        // TODO (p2) document the individual steps below
        AuthorizationPermissionSet oldPermissionSet;
        if (newPermissionSet == null || newPermissionSet.isLocalOnly()) {
            newPermissionSet = permissionSetLocalOnly;
            oldPermissionSet = nonLocalPermissionSetsBySelector.remove(selector);
        } else {
            oldPermissionSet = nonLocalPermissionSetsBySelector.put(selector, newPermissionSet);
        }
        if (oldPermissionSet == newPermissionSet) {
            return false;
        }
        if (oldPermissionSet == null) {
            oldPermissionSet = permissionSetLocalOnly;
        }
        final boolean modified = !oldPermissionSet.equals(newPermissionSet);
        if (modified && !initializing && selector.isAssignable()) {
            try {
                permissionStorage.persistAssignment(selector, newPermissionSet);
            } catch (OperationFailureException e) {
                log.error("Error saving component authorization change: " + e.toString());
            }
        }
        return modified;
    }

    // note: expected to be called with installationsAndPermissionsLock lock held
    private void addRemotableComponentToAuthorizationTracking(DistributedComponentEntryImpl newEntry,
        ComponentAuthorizationSelector authSelector) {
        // consistency check
        if (newEntry.getComponentInterface().getLocalExecutionOnly()) {
            log.warn("Ignoring request to add local-only component " + newEntry.getComponentInstallation().getInstallationId()
                + " to authorization tracking; this should be filtered earlier in the call chain");
            return;
        }
        Collection<DistributedComponentEntry> entrySet = remotableCompEntriesByAuthorizationSelector.get(authSelector);
        if (entrySet == null) {
            entrySet = new ArrayList<>(2); // sets of same comp ids but different versions are usually small
            remotableCompEntriesByAuthorizationSelector.put(authSelector, entrySet);
        }
        entrySet.add(newEntry);
    }

    // note: expected to be called with installationsAndPermissionsLock lock held
    private void removeComponentFromAuthorizationTracking(DistributedComponentEntry removedCompEntry,
        ComponentAuthorizationSelector authSelector) {
        // consistency check
        if (removedCompEntry.getComponentInterface().getLocalExecutionOnly()) {
            log.warn("Ignoring request to remove local-only component " + removedCompEntry.getComponentInstallation().getInstallationId()
                + " from authorization tracking; this should be filtered earlier in the call chain");
            return;
        }
        Collection<DistributedComponentEntry> entrySet = remotableCompEntriesByAuthorizationSelector.get(authSelector);
        // this should be the same entry object that was registered, so remove() should always match
        if (!entrySet.remove(removedCompEntry)) {
            // consistency error
            throw new IllegalStateException(
                "Unregistered local component " + removedCompEntry.getComponentInstallation().getInstallationId()
                    + ", but it was not present in the authorization map");
        }
        // remove the collection if there are no remaining entries; this is why this should be called after "add" on an update
        if (entrySet.isEmpty()) {
            remotableCompEntriesByAuthorizationSelector.remove(authSelector);
        }
    }

    // note: expected to be called with installationsAndPermissionsLock lock held
    private void updatePermissionSetsOfRegisteredComponents(ComponentAuthorizationSelector authSelector,
        AuthorizationPermissionSet newPermissionSet) {
        Collection<DistributedComponentEntry> oldEntries = remotableCompEntriesByAuthorizationSelector.get(authSelector);
        if (oldEntries == null || oldEntries.isEmpty()) {
            return;
        }
        List<DistributedComponentEntry> newEntries = new ArrayList<>(oldEntries.size());
        for (DistributedComponentEntry old : oldEntries) {
            ComponentInstallation componentInstallation = old.getComponentInstallation();
            // consistency check
            if (!newPermissionSet.isLocalOnly() && componentInstallation.getComponentInterface().getLocalExecutionOnly()) {
                log.warn("Setting non-local permissions for local-only component " + componentInstallation.getInstallationId()
                    + "; this will be shown on the local node, but the component will not be actually published");
            }
            DistributedComponentEntryImpl newEntry =
                ComponentDataConverter.createLocalDistributedComponentEntry(componentInstallation, newPermissionSet, authorizationService);
            newEntries.add(newEntry);
            componentEntriesByInstallationId.put(componentInstallation.getInstallationId(), newEntry);
        }
        remotableCompEntriesByAuthorizationSelector.put(authSelector, newEntries);
    }

    private void notifyChangeListenersAsync(final boolean accessGroupsChanged, final boolean componentSelectorsChanged,
        final boolean assignmentsChanged) {
        log.debug(StringUtils.format("Notifying %d listener(s) of a permission matrix change: AGC=%s, CSC=%s, AC=%s",
            permissionMatrixChangesCallbackManager.getListenerCount(), accessGroupsChanged, componentSelectorsChanged, assignmentsChanged));

        permissionMatrixChangesCallbackManager.enqueueCallback(new AsyncCallback<PermissionMatrixChangeListener>() {

            @Override
            public void performCallback(PermissionMatrixChangeListener listener) {
                listener.onPermissionMatrixChanged(accessGroupsChanged, componentSelectorsChanged, assignmentsChanged);
            }
        });
    }

}
