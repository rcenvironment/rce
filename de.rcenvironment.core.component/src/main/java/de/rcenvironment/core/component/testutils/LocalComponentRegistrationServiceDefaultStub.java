/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.management.api.PermissionMatrixChangeListener;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Default stub implementation of {@link LocalComponentRegistrationService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class LocalComponentRegistrationServiceDefaultStub implements LocalComponentRegistrationService {

    @Override
    public void registerOrUpdateLocalComponentInstallation(ComponentInstallation componentInstallation) {}

    @Override
    public void registerOrUpdateSingleVersionLocalComponentInstallation(ComponentInstallation componentInstallation,
        AuthorizationPermissionSet permissionSet) {}

    @Override
    public void unregisterLocalComponentInstallation(String compInstallationId) {}

    @Override
    public AuthorizationPermissionSet getComponentPermissionSet(ComponentAuthorizationSelector component,
        boolean replaceNullValue) {
        return null;
    }

    @Override
    public ComponentAuthorizationSelector getComponentSelector(String componentIdentifier) {
        return null;
    }

    @Override
    public ComponentAuthorizationSelector getComponentSelector(ComponentInstallation component) {
        return null;
    }

    @Override
    public List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForRemotableComponents() {
        return null;
    }

    @Override
    public Map<ComponentAuthorizationSelector, AuthorizationPermissionSet> listAssignedComponentPermissions() {
        return null;
    }

    @Override
    public void setComponentPermissions(ComponentAuthorizationSelector selector, AuthorizationPermissionSet permissionSet) {}

    @Override
    public void setComponentPermissionState(ComponentAuthorizationSelector selector, AuthorizationAccessGroup accessGroup,
        boolean newState) {}

    @Override
    public void restrictComponentPermissionsToIntersectionWith(ComponentAuthorizationSelector selector,
        AuthorizationPermissionSet permissionSet) {}

    @Override
    public List<AuthorizationAccessGroup> listAvailableAuthorizationAccessGroups() {
        return null;
    }

    @Override
    public void addPermissionMatrixChangeListener(PermissionMatrixChangeListener listener) {}

    @Override
    public void removePermissionMatrixChangeListener(PermissionMatrixChangeListener listener) {}

    @Override
    public void reportBuiltinComponentLoadingComplete() {}

    @Override
    public void reportToolIntegrationRegistrationComplete() {}

    @Override
    public boolean waitForLocalComponentInitialization(int maxWait, TimeUnit timeUnit) {
        return false;
    }

    @Override
    public List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForAccessGroup(AuthorizationAccessGroup accessGroup,
        boolean includeOrphanedSelectors) {
        return null;
    }

    @Override
    public List<NamedComponentAuthorizationSelector> listAuthorizationSelectorsForRemotableComponentsIncludingOrphans() {
        return null;
    }
}
