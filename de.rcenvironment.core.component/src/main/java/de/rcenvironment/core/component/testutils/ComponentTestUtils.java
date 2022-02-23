/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.internal.ComponentDataConverter;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationExtensionDefinitionImpl;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDefinitionsProviderImpl;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.component.model.impl.ComponentInterfaceImpl;
import de.rcenvironment.core.component.model.impl.ComponentRevisionImpl;

/**
 * Test utilities for {@link DistributedComponentKnowledge} and {@link DistributedComponentEntry}.
 *
 * @author Robert Mischke
 */
public final class ComponentTestUtils {

    private ComponentTestUtils() {}

    /**
     * Creates a {@link ComponentInstallation} instance for unit and integration testing.
     * 
     * @param identifier the component's identifier; also used as display name
     * @param version the version of the component
     * @param nodeId the node id of the component's location
     * @return placeholder {@link ComponentDescription}
     */
    public static ComponentInstallation createTestComponentInstallation(String identifier, String version,
        LogicalNodeId nodeId) {

        ComponentInterfaceImpl componentInterface = new ComponentInterfaceImpl();
        componentInterface.setIdentifier(identifier);
        componentInterface.setIdentifiers(Arrays.asList(identifier));
        componentInterface.setDisplayName(identifier);
        componentInterface.setVersion(version);
        componentInterface.setGroupName(ComponentConstants.COMPONENT_GROUP_UNKNOWN);
        componentInterface.setInputDefinitionsProvider(new EndpointDefinitionsProviderImpl());
        componentInterface.setOutputDefinitionsProvider(new EndpointDefinitionsProviderImpl());
        componentInterface.setConfigurationDefinition(new ConfigurationDefinitionImpl());
        componentInterface.setConfigurationExtensionDefinitions(new HashSet<ConfigurationExtensionDefinitionImpl>());

        ComponentRevisionImpl componentRevision = new ComponentRevisionImpl();
        componentRevision.setComponentInterface(componentInterface);

        ComponentInstallationImpl componentInstallation = new ComponentInstallationImpl();
        componentInstallation.setComponentRevision(componentRevision);
        componentInstallation.setInstallationId(componentInterface.getIdentifierAndVersion());
        componentInstallation.setNodeIdObject(nodeId);

        return componentInstallation;
    }

    /**
     * Creates a {@link ComponentInstallation} instance for unit and integration testing.
     * 
     * @param identifier the component's identifier; also used as display name
     * @param version the version of the component
     * @param nodeId the node id of the component's location
     * @param permissionSet the permissions for this component
     * @param authorizationService the {@link AuthorizationService} to fetch required key material from; can be null if no publication data
     *        is required for a test
     * @return placeholder {@link ComponentDescription}
     */
    public static DistributedComponentEntry createTestDistributedComponentEntry(String identifier, String version,
        LogicalNodeId nodeId, AuthorizationPermissionSet permissionSet, AuthorizationService authorizationService) {
        ComponentInstallation compInst = createTestComponentInstallation(identifier, version, nodeId);
        return wrapIntoDistributedComponentEntry(compInst, permissionSet, authorizationService);
    }

    /**
     * Wraps a single {@link ComponentInstallation} into a {@link DistributedComponentEntry} containing it.
     * 
     * @param compInst the {@link ComponentInstallation} to wrap
     * @param permissionSet the permissions for this component
     * @param authorizationService the {@link AuthorizationService} to fetch required key data from; can be null for local-only components
     * @return the generated {@link DistributedComponentEntry}
     */
    public static DistributedComponentEntry wrapIntoDistributedComponentEntry(ComponentInstallation compInst,
        AuthorizationPermissionSet permissionSet, AuthorizationService authorizationService) {
        return ComponentDataConverter.createLocalDistributedComponentEntry(compInst, permissionSet, authorizationService);
    }

    /**
     * Converts a list of {@link ComponentInstallation}s into a list with each element wrapped into a {@link DistributedComponentEntry} with
     * a local-only permission set.
     * 
     * @param compInsts the input list
     * @return the converted list
     */
    public static List<DistributedComponentEntry> convertToListOfDistributedComponentEntries(Collection<ComponentInstallation> compInsts) {
        return convertToListOfDistributedComponentEntries(compInsts, null, null);
    }

    /**
     * Converts a list of {@link ComponentInstallation}s into a list with each element wrapped into a {@link DistributedComponentEntry} with
     * a custom permission set.
     * 
     * @param compInsts the input list
     * @param permissionSet the permission set to attach to all installations; may be null
     * @param authorizationService the {@link AuthorizationService} to fetch required key data from; can be null for local-only components
     * 
     * @return the converted list
     */
    public static List<DistributedComponentEntry> convertToListOfDistributedComponentEntries(Collection<ComponentInstallation> compInsts,
        AuthorizationPermissionSet permissionSet, AuthorizationService authorizationService) {
        final List<DistributedComponentEntry> result = new ArrayList<>();
        for (ComponentInstallation ci : compInsts) {
            result.add(wrapIntoDistributedComponentEntry(ci, permissionSet, authorizationService));
        }
        return result;
    }
}
