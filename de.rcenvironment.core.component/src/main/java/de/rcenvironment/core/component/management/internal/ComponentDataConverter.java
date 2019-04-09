/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.management.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.authorization.cryptography.api.SymmetricKey;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
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

    /**
     * A simple bean representing the component entry data to be published as JSON.
     *
     * @author Robert Mischke
     */
    public static class ComponentEntryTransferObject {

        private Map<String, String> authData; // null = public access

        private String data; // serialized, and encrypted if non-public

        @SuppressWarnings("unused") // used for JSON deserialization
        public ComponentEntryTransferObject() {}

        ComponentEntryTransferObject(String data, Map<String, String> authKeys) {
            this.authData = authKeys;
            this.data = data;
        }

        public Map<String, String> getAuthData() {
            return authData;
        }

        public void setAuthData(Map<String, String> authKeys) {
            this.authData = authKeys;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

    }

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
            // final String rawInstallationData = Objects.requireNonNull(entry.getPublicationData());
            final String rawComponentData = serializeComponentInstallationData(componentInstallation);
            final ComponentEntryTransferObject transferObject;
            if (permissionSet.isPublic()) {
                // public -> no encryption; simply embed the serialized component data
                transferObject = new ComponentEntryTransferObject(serializeComponentInstallationData(componentInstallation), null);
            } else {
                // generate a common symmetric encryption key to encrypt the installation data with
                final CryptographyOperationsProvider cryptographyOperations = authorizationService.getCryptographyOperationsProvider();
                // encrypt the data
                SymmetricKey componentDataEncryptionKey = cryptographyOperations.generateSymmetricKey();
                String encryptedComponentData = cryptographyOperations.encryptAndEncodeString(componentDataEncryptionKey, rawComponentData);
                // encrypt the common key with each group's individual key
                Map<String, String> authKeys = new HashMap<>();
                for (AuthorizationAccessGroup group : permissionSet.getAccessGroups()) {
                    final AuthorizationAccessGroupKeyData keyDataForGroup = authorizationService.getKeyDataForGroup(group);
                    if (keyDataForGroup == null) {
                        LogFactory.getLog(ComponentDataConverter.class)
                            .warn("Found no key data for assigned access group " + group.getFullId()
                                + " when creating publication data for " + componentInstallation.getInstallationId() + "; skipping group");
                        continue;
                    }
                    SymmetricKey groupKey = keyDataForGroup.getSymmetricKey();
                    // TODO (p3) improve: this is slightly inefficient, as the common key was encoded already, and is now encrypted as that
                    // string form's byte array representation - ideally, the key's byte array form would be encrypted directly
                    authKeys.put(group.getFullId(),
                        cryptographyOperations.encryptAndEncodeString(groupKey, componentDataEncryptionKey.getEncodedForm()));
                }

                // TODO (p2) 9.0.0 the content data is now encrypted, but the component's id is still publicly visible; encrypt or hash it
                transferObject = new ComponentEntryTransferObject(encryptedComponentData, authKeys);
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
        final ComponentEntryTransferObject transferObject;
        try {
            transferObject = sharedJsonMapper.readValue(jsonData, ComponentEntryTransferObject.class);
        } catch (IOException e) {
            throw new OperationFailureException("Error deserializing component entry from JSON data: " + jsonData, e);
        }
        final Map<String, String> authKeys = transferObject.getAuthData();
        final ComponentInstallation componentInstallation;
        final String displayName;
        final AuthorizationPermissionSet declaredPermissionSet;
        final AuthorizationPermissionSet matchingPermissionSet;
        if (authKeys == null) {
            // public access; no decryption required
            declaredPermissionSet = authorizationService.getDefaultAuthorizationObjects().permissionSetPublicInLocalNetwork();
            matchingPermissionSet = declaredPermissionSet;
            componentInstallation = deserializeComponentInstallationData(transferObject.getData());
            displayName = componentInstallation.getComponentInterface().getDisplayName();
        } else {
            // group-based authorization
            final List<AuthorizationAccessGroup> declaredGroups = new ArrayList<>(authKeys.size());
            final List<AuthorizationAccessGroup> matchingGroups = new ArrayList<>(authKeys.size());
            String authMapValueForFirstMatchingGroup = null;
            for (Entry<String, String> authEntry : authKeys.entrySet()) {
                final AuthorizationAccessGroup remoteGroup = authorizationService.representRemoteGroupId(authEntry.getKey());
                declaredGroups.add(remoteGroup);
                if (authorizationService.isGroupAccessible(remoteGroup)) {
                    matchingGroups.add(remoteGroup);
                    // note: it is important that the group list maintains its ordering so this actually matches the first group
                    if (authMapValueForFirstMatchingGroup == null) {
                        authMapValueForFirstMatchingGroup = authEntry.getValue();
                    }
                }
            }
            declaredPermissionSet = authorizationService.buildPermissionSet(declaredGroups);
            matchingPermissionSet = authorizationService.buildPermissionSet(matchingGroups);
            if (!matchingPermissionSet.isLocalOnly()) { // at least one matching remote group
                final CryptographyOperationsProvider cryptographyOperations = authorizationService.getCryptographyOperationsProvider();
                // pick the first authorized group (arbitrary, but must match the map value stored above)
                final AuthorizationAccessGroup groupForDecryption = matchingGroups.get(0);
                final SymmetricKey groupKey = authorizationService.getKeyDataForGroup(groupForDecryption).getSymmetricKey();
                // decrypt the common component data key
                SymmetricKey componentDataKey = cryptographyOperations.decodeSymmetricKey(
                    cryptographyOperations.decodeAndDecryptString(groupKey, authMapValueForFirstMatchingGroup));
                // use the common component data key to decrypt the component data
                final String decryptedComponentData =
                    cryptographyOperations.decodeAndDecryptString(componentDataKey, transferObject.getData());
                // parse the decrypted component data
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
