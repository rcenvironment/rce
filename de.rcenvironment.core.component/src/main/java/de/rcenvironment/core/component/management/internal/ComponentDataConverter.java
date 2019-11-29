/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.management.utils.JsonDataEncryptionUtils;
import de.rcenvironment.core.component.management.utils.JsonDataWithOptionalEncryption;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.impl.ComponentInstallationImpl;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Utility class for converting between different component representations, especially its object and its serialized form.
 *
 * @author Robert Mischke
 * @author Alexander Weinert (accounted for DistributedComponentEntries without ComponentInstallation)
 */
public final class ComponentDataConverter {

    private static final ObjectMapper sharedJsonMapper = JsonUtils.getDefaultObjectMapper();

    private ComponentDataConverter() {}

    /**
     * Converts a {@link ComponentInstallation} to its serialized representation. Note that this is a trivial JSON mapping at the moment; in
     * the future, this may use a more detailed mapping.
     * 
     * @param ci the {@link ComponentInstallation}
     * @return the serialized form
     * @throws OperationFailureException on failure, e.g. malformed field data
     */
    public static String serializeComponentInstallationData(ComponentInstallation ci) {
        try {
            return sharedJsonMapper.writeValueAsString(ci);
        } catch (IOException e) {
            throw new ComponentPublicationException("Error serializing component descriptor", e);
        }
    }

    /**
     * Reconstructs a {@link ComponentInstallation} from its serialized representation. Note that the input is a trivial JSON mapping at the
     * moment; in the future, this may use a more detailed mapping.
     * 
     * @param jsonData the serialized form
     * @return the reconstructed {@link ComponentInstallation}
     * @throws OperationFailureException on failure, e.g. format errors
     */
    public static ComponentInstallation deserializeComponentInstallationData(String jsonData) throws OperationFailureException {
        try {
            return sharedJsonMapper.readValue(jsonData, ComponentInstallationImpl.class);
        } catch (IOException e) {
            throw new OperationFailureException("Error deserializing component descriptor from JSON data: " + jsonData, e);
        }
    }

    /**
     * Converts a {@link DistributedComponentEntry} to its serialized representation.
     * <p>
     * Note that this API will have to change if/when the external property representation is changed to support smaller update deltas.
     * Right now, all related information (main component data, authorization information, encryption keys, icon data) is bundled into a
     * single string for simplicity.
     * 
     * @param componentInstallation the {@link DistributedComponentEntry} to represent
     * @param permissionSet the {@link AuthorizationPermissionSet} of the new {@link DistributedComponentEntry}
     * @param authorizationService the {@link AuthorizationService} to fetch required key data from; can be null for local-only components
     * @return a new {@link DistributedComponentEntry} containing additional publication data if the component's permissions are non-local
     */
    public static DistributedComponentEntryImpl createLocalDistributedComponentEntry(ComponentInstallation componentInstallation,
        AuthorizationPermissionSet permissionSet, AuthorizationService authorizationService) {
        if (permissionSet == null || permissionSet.isLocalOnly()) {
            return new DistributedComponentEntryImpl(componentInstallation.getComponentInterface().getDisplayName(), componentInstallation,
                permissionSet, permissionSet, false, null);
        }
        try {

            final String rawComponentData = serializeComponentInstallationData(componentInstallation);
            final JsonDataWithOptionalEncryption transferObject;

            if (permissionSet.isPublic()) {
                // public -> no encryption; simply embed the serialized component data
                transferObject = JsonDataEncryptionUtils.asPublicData(serializeComponentInstallationData(componentInstallation));
            } else {
                // TODO (p2) 9.0.0 the content data is encrypted, but the component's id is still publicly visible; encrypt or hash it

                // gather each group's individual key
                Map<String, SymmetricKey> keyData = new HashMap<>();
                for (AuthorizationAccessGroup group : permissionSet.getAccessGroups()) {
                    final AuthorizationAccessGroupKeyData keyDataForGroup = authorizationService.getKeyDataForGroup(group);
                    if (keyDataForGroup == null) {
                        LogFactory.getLog(ComponentDataConverter.class)
                            .warn("Found no key data for assigned access group " + group.getFullId()
                                + " when creating publication data for " + componentInstallation.getInstallationId() + "; skipping group");
                        continue;
                    }
                    keyData.put(group.getFullId(), keyDataForGroup.getSymmetricKey());
                }

                // delegate to the utility method to encrypt with a common key, and individually encrypt that key for each group
                transferObject = JsonDataEncryptionUtils.encryptForKeys(rawComponentData, keyData,
                    authorizationService.getCryptographyOperationsProvider());
            }

            final String serializedForm = sharedJsonMapper.writeValueAsString(transferObject);
            return new DistributedComponentEntryImpl(componentInstallation.getComponentInterface().getDisplayName(), componentInstallation,
                permissionSet, permissionSet, false, serializedForm);
        } catch (IOException | OperationFailureException e) {
            throw new ComponentPublicationException("Error serializing component descriptor", e);
        }
    }

    /**
     * Restores a {@link DistributedComponentEntry} from its serialized representation.
     * <p>
     * Note that this API will have to change if/when the external property representation is changed to support smaller update deltas.
     * Right now, all related information (main component data, authorization information, encryption keys, icon data) is bundled into a
     * single string for simplicity.
     * 
     * @param jsonData the serialized {@link DistributedComponentEntry} data
     * @param authorizationService the {@link AuthorizationService} to fetch key material from
     * @return the serialized form
     * @throws OperationFailureException on failure, e.g. malformed field data
     */
    public static DistributedComponentEntry deserializeRemoteDistributedComponentEntry(String jsonData,
        AuthorizationService authorizationService) throws OperationFailureException {
        final JsonDataWithOptionalEncryption transferObject;
        try {
            transferObject = sharedJsonMapper.readValue(jsonData, JsonDataWithOptionalEncryption.class);
        } catch (IOException e) {
            throw new OperationFailureException("Error deserializing component entry from JSON data: " + jsonData, e);
        }
        final ComponentInstallation componentInstallation;
        final String displayName;
        final AuthorizationPermissionSet declaredPermissionSet;
        final AuthorizationPermissionSet matchingPermissionSet;

        if (JsonDataEncryptionUtils.isPublic(transferObject)) {
            // public access; no decryption required
            declaredPermissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();
            matchingPermissionSet = declaredPermissionSet;
            componentInstallation = deserializeComponentInstallationData(JsonDataEncryptionUtils.getPublicData(transferObject));
            displayName = componentInstallation.getComponentInterface().getDisplayName();
        } else {
            // resolve the declared authorization groups and their intersection with locally accessible groups
            final Set<String> declaredGroupIds = JsonDataEncryptionUtils.getKeyIds(transferObject);
            final Set<AuthorizationAccessGroup> declaredGroups = authorizationService.representRemoteGroupIds(declaredGroupIds);
            final Set<AuthorizationAccessGroup> matchingGroups = authorizationService.intersectWithAccessibleGroups(declaredGroups);

            // convert to permission sets
            declaredPermissionSet = authorizationService.buildPermissionSet(declaredGroups);
            matchingPermissionSet = authorizationService.buildPermissionSet(matchingGroups);
            if (!matchingPermissionSet.isLocalOnly()) {
                // there is at least one matching remote group -> pick an arbitrary one and attempt decryption
                final CryptographyOperationsProvider cryptographyOperations = authorizationService.getCryptographyOperationsProvider();
                final AuthorizationAccessGroup groupForDecryption = matchingPermissionSet.getArbitraryGroup();
                final String groupId = groupForDecryption.getFullId();
                final SymmetricKey groupKey = authorizationService.getKeyDataForGroup(groupForDecryption).getSymmetricKey();
                // attempt decryption of the component data
                String decryptedComponentData =
                    JsonDataEncryptionUtils.attemptDecryption(transferObject, groupId, groupKey, cryptographyOperations);
                // success -> deserialize
                componentInstallation = deserializeComponentInstallationData(decryptedComponentData);
                displayName = componentInstallation.getComponentInterface().getDisplayName();
            } else {
                componentInstallation = null;
                // Since the component installation contains the display name of the component, we have no way to access that name at this
                // point. Instead, we display a default name
                displayName = "Inaccessible component";
            }
        }
        return new DistributedComponentEntryImpl(displayName, componentInstallation, declaredPermissionSet, matchingPermissionSet, true,
            null);
    }
}
