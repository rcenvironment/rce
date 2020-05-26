/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.api.DistributedComponentEntryType;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.api.ComponentRevision;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link DistributedComponentEntry} implementation.
 *
 * @author Robert Mischke
 * @author Alexander Weinert (Added displayName)
 */
public class DistributedComponentEntryImpl implements DistributedComponentEntry {
    
    private final String displayName;

    private final ComponentInstallation componentInstallation;

    private final DistributedComponentEntryType type;

    private final AuthorizationPermissionSet declaredPermissionSet;

    private final AuthorizationPermissionSet matchingPermissionSet;

    private final String publicationData;

    public DistributedComponentEntryImpl(String displayNameParam, ComponentInstallation componentInstallation,
        AuthorizationPermissionSet declaredPermissionSetParam, AuthorizationPermissionSet matchingPermissionSetParam, boolean remote,
        String publicationDataParam) {
        this.displayName = displayNameParam;
        this.componentInstallation = componentInstallation;
        this.declaredPermissionSet = declaredPermissionSetParam;
        this.matchingPermissionSet = matchingPermissionSetParam;

        /*
         * The first null check is necessary as the componentInterface field can be null if this is a remote component without matching
         * local authorization data (yet). The second check is necessary as unit/integration tests may use "thin" ComponenInstallation mocks
         * with a null ComponentInterface.
         */
        final boolean localExecutionOnly =
            componentInstallation != null && componentInstallation.getComponentInterface() != null
                && componentInstallation.getComponentInterface().getLocalExecutionOnly();
        if (remote) {
            if (localExecutionOnly) {
                // log this unusual state, but keep the "remote" flag
                LogFactory.getLog(getClass()).error("Inconsistent state: Received a remote component from "
                    + componentInstallation.getNodeId() + " with the local-only flag set");
            }
            this.type = DistributedComponentEntryType.REMOTE;
        } else if (localExecutionOnly) {
            this.type = DistributedComponentEntryType.FORCED_LOCAL;
        } else {
            if (declaredPermissionSetParam == null || declaredPermissionSetParam.isLocalOnly()) {
                this.type = DistributedComponentEntryType.LOCAL;
            } else {
                this.type = DistributedComponentEntryType.SHARED;
            }
        }

        this.publicationData = publicationDataParam;
    }

    @Override
    public DistributedComponentEntryType getType() {
        return type;
    }

    @Override
    public AuthorizationPermissionSet getDeclaredPermissionSet() {
        return declaredPermissionSet;
    }

    @Override
    public AuthorizationPermissionSet getMatchingPermissionSet() {
        return matchingPermissionSet;
    }

    @Override
    public boolean isAccessible() {
        return componentInstallation != null; // having an explicit "accessible" field would be redundant
    }

    @Override
    public ComponentInstallation getComponentInstallation() {
        return componentInstallation;
    }

    @Override
    public ComponentRevision getComponentRevision() {
        return componentInstallation.getComponentRevision();
    }

    @Override
    public ComponentInterface getComponentInterface() {
        return componentInstallation.getComponentInterface();
    }

    @Override
    public String getPublicationData() {
        return publicationData;
    }

    @Override
    public String getNodeId() {
        return componentInstallation.getNodeId();
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return StringUtils.format("%s(%s)", componentInstallation.getInstallationId(), declaredPermissionSet.toString());
    }

}
